package dev.onyx.deobfuscator

import dev.onyx.deobfuscator.asm.ClassPool

interface Transformer {

    fun transform(pool: ClassPool)

}