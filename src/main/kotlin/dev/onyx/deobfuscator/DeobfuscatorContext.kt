package dev.onyx.deobfuscator

import java.io.File

data class DeobfuscatorContext(
    val inputJar: File,
    val outputJar: File
)