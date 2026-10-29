package app.braintropy.patches.wakingup

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.braintropy.patches.shared.Constants.COMPATIBILITY_WAKING_UP
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val disableUpdatePromptPatch = bytecodePatch(
    name = "Disable update prompt",
    description = "Suppresses the \"Update Required\" dialog shown on launch when the server " +
        "reports a newer preferred build. Zeroes the remote build-number settings " +
        "(preferredBuildNumber, criticalBuildNumber, preferredOSVersion) so the version " +
        "check always passes. Does not affect the version shown in the app.",
    default = true
) {
    category("Updates")
    compatibleWith(COMPATIBILITY_WAKING_UP)

    execute {
        // RemoteSettingsModel's constructor assigns its int fields from the
        // constructor parameters in order. Field names are R8-obfuscated, so
        // resolve them from the constructor's IPUT instructions instead of
        // hardcoding them.
        val constructor = RemoteSettingsModelToStringFingerprint.classDef.methods
            .first { it.name == "<init>" }

        val intFields = constructor.implementation!!.instructions
            .asSequence()
            .filter { it.opcode == Opcode.IPUT }
            .map { (it as ReferenceInstruction).reference as FieldReference }
            .filter { it.type == "I" }
            .distinct()
            .toList()

        check(intFields.isNotEmpty()) {
            "RemoteSettingsModel constructor assigns no int fields; patch needs updating."
        }

        // Insert before the final return-void. The constructor declares no
        // locals, so v-registers alias the parameter registers (v0 IS p0);
        // a "fresh" v-register cannot be used. p1 is dead once its field is
        // assigned, so reuse it as the scratch register.
        val insertIndex = constructor.implementation!!.instructions.size - 1
        constructor.addInstructions(
            insertIndex,
            "const/4 p1, 0x0\n" + intFields.joinToString("\n") { "iput p1, p0, $it" }
        )
    }
}
