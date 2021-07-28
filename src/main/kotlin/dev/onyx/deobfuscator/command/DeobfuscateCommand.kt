package dev.onyx.deobfuscator.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import dev.onyx.deobfuscator.Deobfuscator
import dev.onyx.deobfuscator.DeobfuscatorContext

class DeobfuscateCommand : CliktCommand(
    name = "deobfuscate",
    help = "Runs the deobfuscator on an input gamepack JAR file.",
    invokeWithoutSubcommand = true,
    printHelpOnEmptyArgs = true
) {

    private val inputJar by option("-i", "--input-jar", help = "Path to the input JAR file.")
        .file(mustExist = true, canBeDir = false)
        .required()

    private val outputJar by option("-o", "--output-jar", help = "Path to the output JAR file.")
        .file(canBeDir = false)
        .required()

    override fun run() {
        val context = DeobfuscatorContext(inputJar, outputJar)
        val deobfuscator = Deobfuscator(context)
        deobfuscator.run()
    }
}