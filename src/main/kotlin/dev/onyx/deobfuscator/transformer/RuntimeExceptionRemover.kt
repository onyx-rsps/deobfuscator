package dev.onyx.deobfuscator.transformer

import dev.onyx.deobfuscator.Transformer
import dev.onyx.deobfuscator.asm.ClassPool
import org.objectweb.asm.Type
import org.tinylog.kotlin.Logger
import java.lang.RuntimeException

class RuntimeExceptionRemover : Transformer {

    private var count = 0

    override fun transform(pool: ClassPool) {
        pool.forEach { cls ->
            cls.methods.forEach { method ->
                val size = method.tryCatchBlocks.size
                method.tryCatchBlocks.removeIf { it.type == RUNTIME_EXCEPTION }
                count += size - method.tryCatchBlocks.size
            }
        }

        Logger.info("Removed $count RuntimeException try-catch blocks.")
    }

    companion object {
        private val RUNTIME_EXCEPTION = Type.getInternalName(RuntimeException::class.java)
    }
}