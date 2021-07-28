package dev.onyx.deobfuscator.command

import com.github.ajalt.clikt.core.CliktCommand

class RootCommand : CliktCommand(
    name = "Onyx Deobfuscator",
    help = "Deobfuscates the Jagex vanilla obfuscated gamepack.",
    invokeWithoutSubcommand = false,
    printHelpOnEmptyArgs = true
) {

    override fun run() {
        /*
         * Do nothing.
         */
    }
}