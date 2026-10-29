package app.braintropy.patches.wakingup

import app.morphe.patcher.Fingerprint

/**
 * Fingerprints for org.wakingup.android v3.24.1.
 *
 * The app is R8 obfuscated, so class and member names below are the
 * obfuscated names found in this exact app version:
 *
 * - Le40/u;  -> UserModel (domain user model, field k = isSubscribed,
 *   field n = isTrialSubscription)
 * - Lo20/b;  -> CourseModel (domain course model, field c = isAvailable,
 *   computed from the server-side "is_unlocked" flag)
 * - Lya0/h;  -> SubscribeRow ("Subscribe to unlock" upsell composable)
 * - Lu40/b;  -> WidgetCTAScreen (home/explore "Become Subscriber" upsell banner)
 */

/**
 * UserModel constructor. Matched by the parameter names used in the
 * Intrinsics.checkNotNullParameter calls at the start of the constructor.
 */
object UserModelConstructorFingerprint : Fingerprint(
    definingClass = "Le40/u;",
    name = "<init>",
    strings = listOf(
        "id",
        "email",
        "token",
        "firstName",
        "lastName",
        "fullName",
        "joinedAt",
        "dailyDurationType",
        "uuid",
        "momentsSettings",
        "quoteSettings",
        "shareCode",
        "stats",
        "subscription"
    )
)

/**
 * CourseModel constructor. Matched by the parameter names used in the
 * Intrinsics.checkNotNullParameter calls at the start of the constructor.
 */
object CourseModelConstructorFingerprint : Fingerprint(
    definingClass = "Lo20/b;",
    name = "<init>",
    strings = listOf(
        "title",
        "type",
        "id",
        "description",
        "courseSubtitle",
        "createdAt",
        "authors",
        "packAuthors",
        "dailyDetails"
    )
)

/**
 * The "Subscribe to unlock" upsell row composable.
 * Only one method named "a" exists in this class.
 */
object SubscribeRowFingerprint : Fingerprint(
    definingClass = "Lya0/h;",
    name = "a",
    returnType = "V",
    parameters = listOf(
        "I",
        "Landroidx/compose/runtime/Composer;",
        "Landroidx/compose/ui/Modifier;",
        "Lkotlin/jvm/functions/Function0;"
    )
)

/**
 * RemoteSettingsModel.toString(). The model carries the server-driven version
 * gate (preferredBuildNumber / criticalBuildNumber / preferredOSVersion) that
 * decides whether the "Update Required" dialog is shown. Matched by the field
 * names baked into the toString() string literals, so it survives R8
 * re-obfuscation as long as the model keeps its shape.
 */
object RemoteSettingsModelToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    strings = listOf(
        "RemoteSettingsModel(preferredBuildNumber=",
        ", criticalBuildNumber=",
        ", preferredOSVersion="
    )
)

/**
 * The OkHttp auth interceptor that decorates every API request with
 * app-version / app-build headers. The server (AWS WAF) rejects requests
 * from outdated builds with a 404, so these headers must be spoofed for
 * an old build to keep working. Matched by its unique string literals,
 * so it survives R8 re-obfuscation.
 */
object ApiHeadersInterceptorFingerprint : Fingerprint(
    strings = listOf(
        "Maintenance Mode Active - all requests should be blocked",
        "Authorization",
        "app-version",
        "app-build"
    )
)

/**
 * The home/explore CTA widget entry composable (WidgetCTAScreen), which renders
 * the "Join Waking Up" / "Get started" BecomeSubscriber banner.
 * The banner's visibility is driven by the remote home_cta configuration and is
 * not tied to the local subscription state, so it must be suppressed explicitly.
 * Only one method named "b" exists in this class.
 */
object HomeCtaBannerFingerprint : Fingerprint(
    definingClass = "Lu40/b;",
    name = "b",
    returnType = "V",
    parameters = listOf(
        "Lt40/k;",
        "La50/o;",
        "Landroidx/compose/runtime/Composer;",
        "I"
    )
)
