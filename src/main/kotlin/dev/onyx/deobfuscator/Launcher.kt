package dev.onyx.deobfuscator

import com.github.ajalt.clikt.core.subcommands
import dev.onyx.deobfuscator.command.DeobfuscateCommand
import dev.onyx.deobfuscator.command.RootCommand
import dev.onyx.deobfuscator.command.TestClientCommand

object Launcher {

    @JvmStatic
    fun main(args: Array<String>) = RootCommand()
        .subcommands(
            DeobfuscateCommand(),
            TestClientCommand()
        )
        .main(args)

}