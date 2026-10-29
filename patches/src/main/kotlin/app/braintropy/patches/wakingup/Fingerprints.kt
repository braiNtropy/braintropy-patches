package app.braintropy.patches.wakingup

import app.morphe.patcher.Fingerprint

/**
 * Fingerprints for org.wakingup.android.
 *
 * The model fingerprints below are matched by the property labels baked into
 * each model's toString() string literals instead of R8-obfuscated class and
 * member names, so they survive re-obfuscation across app versions. Every
 * layer carrying the flags is matched (domain model, data/entity model, view
 * items) and each matched class gets its flags overridden in its constructor.
 *
 * Verified on v3.24.1 and v3.27.0. Note the layer naming swapped between
 * those versions ("UserModel" was the domain model in 3.24.1 but is a
 * different layer in 3.27.0, whose domain model is "UserDataModel") - another
 * reason not to match by class name.
 */

/**
 * toString() of every user model layer carrying the subscription flags.
 */
object SubscribedUserModelToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    strings = listOf(", isSubscribed=", ", isTrialSubscription=")
)

/**
 * toString() of every course model layer carrying the availability flag.
 * The packTitle label excludes unrelated models that also have an
 * isAvailable flag (packs, agenda items, widgets).
 */
object AvailableCourseModelToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    strings = listOf(", isAvailable=", ", packTitle=")
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
