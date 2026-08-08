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
