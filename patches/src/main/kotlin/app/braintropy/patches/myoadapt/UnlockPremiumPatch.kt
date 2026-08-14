package app.braintropy.patches.myoadapt

import app.braintropy.patches.shared.Constants.COMPATIBILITY_MYOADAPT
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

/**
 * MyoAdapt (Expo/React Native) gates premium on two layers:
 *
 * 1. RevenueCat, both the native SDK and the hybridcommon bridge that
 *    forwards CustomerInfo to the JS layer.
 * 2. The coach.myoadapt.app backend session payloads (GetUserInfo, login,
 *    subscription endpoints), which drive the home screen gate.
 *
 * This patch spoofs both layers.
 *
 * Based on the MyoAdapt patch by Xhehab (GPL-3.0):
 * https://github.com/Xhehab/Xhehab-Patches
 * Modified to remove its storage wipe: that patch deleted files/mmkv on
 * every cold start from MainApplication.onCreate, racing MMKV init and
 * randomly destroying the login session. This patch never touches
 * app-local storage.
 */

private const val PRIMARY_ENTITLEMENT = "core-access"
private const val PRIMARY_PRODUCT = "main_sub"
private const val PRIMARY_PLAN = "monthly-introductory-affiliate"
private const val PURCHASE_DATE = "2026-07-09T00:00:00.000Z"
private const val EXPIRATION_DATE = "2035-07-09T00:00:00.000Z"

// PURCHASE_DATE and EXPIRATION_DATE as epoch millis.
private const val PURCHASE_DATE_MILLIS = "0x19f442c9400L"
private const val EXPIRATION_DATE_MILLIS = "0x1e163b3d800L"

private const val HELPER = "Lapp/braintropy/extension/myoadapt/MyoAdaptUnlock;"

private val ENTITLEMENT_IDS = listOf(
    PRIMARY_ENTITLEMENT,
    "duo-access"
)

private val PRODUCT_IDS = listOf(
    PRIMARY_PRODUCT,
    "main_sub:monthly",
    "main_sub:annual",
    "main_sub:monthly-introductory-affiliate",
    "solo_sub_monthly",
    "solo_sub_annual",
    "duo_sub_monthly",
    "duo_sub_annual"
)

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium",
    description = "Unlocks all premium content and fixes logouts.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MYOADAPT)

    extendWith("extensions/extension.mpe")

    execute {
        // 1) Spoof the CustomerInfo map the hybrid bridge returns to the JS layer.
        CustomerInfoMapperFingerprint
            .match(classDefBy(CustomerInfoMapperFingerprint.definingClass!!))
            .method
            .injectCustomerInfoMap()

        // 2) Native RevenueCat entitlement state: non-empty entitlement maps,
        //    mapped hybrid representation, and isActive == true everywhere.
        val entitlementInfosClass = classDefBy("Lcom/revenuecat/purchases/EntitlementInfos;")
        val entitlementMapSmali = buildNativeEntitlementMapSmali()
        listOf(
            EntitlementInfosActiveFingerprint,
            EntitlementInfosAllFingerprint
        ).forEach { fingerprint ->
            fingerprint.match(entitlementInfosClass).let { match ->
                match.classDef.replaceImplementation(
                    match.method,
                    registerCount = 4,
                    smali = entitlementMapSmali
                )
            }
        }

        EntitlementInfosMapperFingerprint
            .match(classDefBy(EntitlementInfosMapperFingerprint.definingClass!!))
            .let { match ->
                match.classDef.replaceImplementation(
                    match.method,
                    registerCount = 10,
                    smali = buildEntitlementInfosMapperSmali()
                )
            }

        EntitlementInfoIsActiveFingerprint.method.returnEarly(true)

        // Present in the RevenueCat version bundled with 1.5.1, but tolerant
        // in case an update drops it.
        runCatching {
            SubscriptionInfoIsActiveFingerprint.method.returnEarly(true)
        }

        // 3) Rewrite the CustomerInfo JSON before RevenueCat parses it.
        patchCustomerInfoFactory(
            mutableClassDefBy("Lcom/revenuecat/purchases/common/CustomerInfoFactory;")
        )

        // 4) Install the OkHttp interceptor factory that forces the backend's
        //    subscription gate responses (GetUserInfo, login, subscription
        //    endpoints) to report an active subscription.
        MainApplicationOnCreateFingerprint.method.addInstructions(
            0,
            "invoke-static {}, $HELPER->install()V"
        )
    }
}

private fun patchCustomerInfoFactory(classDef: MutableClass) {
    val targets = classDef.methods.filterIsInstance<MutableMethod>().filter { method ->
        method.name == "buildCustomerInfo" &&
            method.returnType == "Lcom/revenuecat/purchases/CustomerInfo;" &&
            method.parameters.isNotEmpty() &&
            method.parameters[0].type == "Lorg/json/JSONObject;"
    }

    targets.forEach { method ->
        val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
        val bodyReg = if (isStatic) "p0" else "p1"
        method.addInstructions(
            0,
            """
            invoke-static {$bodyReg}, $HELPER->spoofCustomerInfoJson(Lorg/json/JSONObject;)V
            """.trimIndent()
        )
    }
}

private fun MutableMethod.returnEarly(value: Boolean) {
    addInstructions(
        0,
        """
        const/4 v0, ${if (value) "0x1" else "0x0"}
        return v0
        """.trimIndent()
    )
}

/**
 * Replace a method's whole implementation with [smali] running in
 * [registerCount] fresh registers.
 */
private fun MutableClass.replaceImplementation(
    method: MutableMethod,
    registerCount: Int,
    smali: String
) {
    val replacement = MutableMethod(
        ImmutableMethod(
            method.definingClass,
            method.name,
            method.parameters,
            method.returnType,
            method.accessFlags,
            method.annotations,
            method.hiddenApiRestrictions,
            MutableMethodImplementation(registerCount)
        )
    ).apply {
        addInstructions(0, smali.trimIndent())
    }

    methods.remove(method)
    methods.add(replacement)
}

/**
 * Rewrite the hybridcommon CustomerInfo map just before it is returned to JS.
 * Spoofs entitlements.active/all, activeSubscriptions,
 * allPurchasedProductIdentifiers and latestExpirationDate.
 *
 * Uses v0-v8 as scratch registers; the mapper method in this build has enough.
 */
private fun MutableMethod.injectCustomerInfoMap() {
    if (implementation == null) return

    val returnIndex = instructions.indexOfLast { it.opcode.name.startsWith("return-object") }
    require(returnIndex >= 0) { "CustomerInfo mapper return-object not found." }

    val returnRegister = (instructions[returnIndex] as OneRegisterInstruction).registerA
    val entitlements = ENTITLEMENT_IDS.distinct().joinToString("\n") { id ->
        """
        const-string v2, "$id"
        invoke-interface {v6, v2, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
        invoke-interface {v7, v2, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
        """.trimIndent()
    }
    val products = (ENTITLEMENT_IDS + PRODUCT_IDS).distinct().joinToString("\n") { productId ->
        """
        const-string v1, "$productId"
        invoke-interface {v8, v1}, Ljava/util/List;->add(Ljava/lang/Object;)Z
        """.trimIndent()
    }

    addInstructions(
        returnIndex,
        """
        move-object v8, v$returnRegister

        new-instance v0, Ljava/util/HashMap;
        invoke-direct {v0, v8}, Ljava/util/HashMap;-><init>(Ljava/util/Map;)V

        new-instance v1, Ljava/util/HashMap;
        invoke-direct {v1}, Ljava/util/HashMap;-><init>()V

        const-string v2, "identifier"
        const-string v3, "$PRIMARY_ENTITLEMENT"
        invoke-interface {v1, v2, v3}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "isActive"
        const/4 v4, 0x1
        invoke-static {v4}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
        move-result-object v4
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "willRenew"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "periodType"
        const-string v4, "NORMAL"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "latestPurchaseDate"
        const-string v4, "$PURCHASE_DATE"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "latestPurchaseDateMillis"
        const-wide v8, $PURCHASE_DATE_MILLIS
        invoke-static {v8, v9}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
        move-result-object v8
        invoke-interface {v1, v2, v8}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "originalPurchaseDate"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "originalPurchaseDateMillis"
        invoke-interface {v1, v2, v8}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "expirationDate"
        const-string v4, "$EXPIRATION_DATE"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "expirationDateMillis"
        const-wide v8, $EXPIRATION_DATE_MILLIS
        invoke-static {v8, v9}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
        move-result-object v8
        invoke-interface {v1, v2, v8}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "store"
        const-string v4, "PLAY_STORE"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "productIdentifier"
        const-string v4, "$PRIMARY_PRODUCT"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "productPlanIdentifier"
        const-string v4, "$PRIMARY_PLAN"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "isSandbox"
        const/4 v4, 0x0
        invoke-static {v4}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
        move-result-object v4
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "unsubscribeDetectedAt"
        const/4 v4, 0x0
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "unsubscribeDetectedAtMillis"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "billingIssueDetectedAt"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "billingIssueDetectedAtMillis"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "ownershipType"
        const-string v4, "PURCHASED"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "verification"
        const-string v4, "VERIFIED"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        new-instance v5, Ljava/util/HashMap;
        invoke-direct {v5}, Ljava/util/HashMap;-><init>()V

        new-instance v6, Ljava/util/HashMap;
        invoke-direct {v6}, Ljava/util/HashMap;-><init>()V

        new-instance v7, Ljava/util/HashMap;
        invoke-direct {v7}, Ljava/util/HashMap;-><init>()V

        $entitlements

        const-string v1, "active"
        invoke-interface {v5, v1, v6}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v1, "all"
        invoke-interface {v5, v1, v7}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v1, "verification"
        const-string v2, "VERIFIED"
        invoke-interface {v5, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v1, "entitlements"
        invoke-interface {v0, v1, v5}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v1, "latestExpirationDate"
        const-string v2, "$EXPIRATION_DATE"
        invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v1, "latestExpirationDateMillis"
        const-wide v8, $EXPIRATION_DATE_MILLIS
        invoke-static {v8, v9}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
        move-result-object v8
        invoke-interface {v0, v1, v8}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        new-instance v8, Ljava/util/ArrayList;
        invoke-direct {v8}, Ljava/util/ArrayList;-><init>()V
        $products

        const-string v1, "activeSubscriptions"
        invoke-interface {v0, v1, v8}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v1, "allPurchasedProductIdentifiers"
        invoke-interface {v0, v1, v8}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        move-object v$returnRegister, v0
        """.trimIndent()
    )
}

private fun buildNativeEntitlementMapSmali(): String {
    val puts = ENTITLEMENT_IDS.distinct().joinToString("\n") { id ->
        """
        const-string v2, "$id"
        invoke-interface {v0, v2, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
        """.trimIndent()
    }
    return """
        new-instance v0, Ljava/util/HashMap;
        invoke-direct {v0}, Ljava/util/HashMap;-><init>()V
        const/4 v1, 0x0
        $puts
        return-object v0
    """.trimIndent()
}

private fun buildEntitlementInfosMapperSmali(): String {
    val entitlementPuts = ENTITLEMENT_IDS.distinct().joinToString("\n") { id ->
        """
        const-string v2, "$id"
        invoke-interface {v6, v2, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
        invoke-interface {v7, v2, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
        """.trimIndent()
    }
    return """
        new-instance v1, Ljava/util/HashMap;
        invoke-direct {v1}, Ljava/util/HashMap;-><init>()V

        const-string v2, "identifier"
        const-string v3, "$PRIMARY_ENTITLEMENT"
        invoke-interface {v1, v2, v3}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "isActive"
        const/4 v4, 0x1
        invoke-static {v4}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
        move-result-object v4
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "willRenew"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "periodType"
        const-string v4, "NORMAL"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "latestPurchaseDate"
        const-string v4, "$PURCHASE_DATE"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "latestPurchaseDateMillis"
        const-wide v8, $PURCHASE_DATE_MILLIS
        invoke-static {v8, v9}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
        move-result-object v8
        invoke-interface {v1, v2, v8}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "originalPurchaseDate"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "originalPurchaseDateMillis"
        invoke-interface {v1, v2, v8}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "expirationDate"
        const-string v4, "$EXPIRATION_DATE"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "expirationDateMillis"
        const-wide v8, $EXPIRATION_DATE_MILLIS
        invoke-static {v8, v9}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
        move-result-object v8
        invoke-interface {v1, v2, v8}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "store"
        const-string v4, "PLAY_STORE"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "productIdentifier"
        const-string v4, "$PRIMARY_PRODUCT"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "productPlanIdentifier"
        const-string v4, "$PRIMARY_PLAN"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "isSandbox"
        const/4 v4, 0x0
        invoke-static {v4}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
        move-result-object v4
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "unsubscribeDetectedAt"
        const/4 v4, 0x0
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "unsubscribeDetectedAtMillis"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "billingIssueDetectedAt"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "billingIssueDetectedAtMillis"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "ownershipType"
        const-string v4, "PURCHASED"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        const-string v2, "verification"
        const-string v4, "VERIFIED"
        invoke-interface {v1, v2, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

        new-instance v5, Ljava/util/HashMap;
        invoke-direct {v5}, Ljava/util/HashMap;-><init>()V
        new-instance v6, Ljava/util/HashMap;
        invoke-direct {v6}, Ljava/util/HashMap;-><init>()V
        new-instance v7, Ljava/util/HashMap;
        invoke-direct {v7}, Ljava/util/HashMap;-><init>()V
        $entitlementPuts

        const-string v1, "active"
        invoke-interface {v5, v1, v6}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
        const-string v1, "all"
        invoke-interface {v5, v1, v7}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
        const-string v1, "verification"
        const-string v2, "VERIFIED"
        invoke-interface {v5, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
        return-object v5
    """.trimIndent()
}
