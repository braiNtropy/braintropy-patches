package app.braintropy.patches.myoadapt

import app.morphe.patcher.Fingerprint

/**
 * Fingerprints for MyoAdapt v1.5.1 (com.myoadapt.app.android).
 *
 * All RevenueCat classes below ship unobfuscated in this build
 * (verified in classes4.dex), so exact names are stable for this version.
 */

/**
 * Hybrid bridge: maps the native CustomerInfo to the map handed to the
 * React Native JS layer.
 */
object CustomerInfoMapperFingerprint : Fingerprint(
    definingClass = "Lcom/revenuecat/purchases/hybridcommon/mappers/CustomerInfoMapperKt;",
    name = "map",
    parameters = listOf("Lcom/revenuecat/purchases/CustomerInfo;"),
    returnType = "Ljava/util/Map;"
)

object EntitlementInfosActiveFingerprint : Fingerprint(
    definingClass = "Lcom/revenuecat/purchases/EntitlementInfos;",
    name = "getActive",
    parameters = emptyList(),
    returnType = "Ljava/util/Map;"
)

object EntitlementInfosAllFingerprint : Fingerprint(
    definingClass = "Lcom/revenuecat/purchases/EntitlementInfos;",
    name = "getAll",
    parameters = emptyList(),
    returnType = "Ljava/util/Map;"
)

object EntitlementInfosMapperFingerprint : Fingerprint(
    definingClass = "Lcom/revenuecat/purchases/hybridcommon/mappers/EntitlementInfosMapperKt;",
    name = "map",
    parameters = listOf("Lcom/revenuecat/purchases/EntitlementInfos;"),
    returnType = "Ljava/util/Map;"
)

object EntitlementInfoIsActiveFingerprint : Fingerprint(
    definingClass = "Lcom/revenuecat/purchases/EntitlementInfo;",
    name = "isActive",
    parameters = emptyList(),
    returnType = "Z"
)

object SubscriptionInfoIsActiveFingerprint : Fingerprint(
    definingClass = "Lcom/revenuecat/purchases/SubscriptionInfo;",
    name = "isActive",
    parameters = emptyList(),
    returnType = "Z"
)

/**
 * App entry point. Used to install the OkHttp interceptor factory that
 * rewrites the backend's subscription gate responses.
 */
object MainApplicationOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/myoadapt/MainApplication;",
    name = "onCreate",
    parameters = emptyList(),
    returnType = "V"
)
