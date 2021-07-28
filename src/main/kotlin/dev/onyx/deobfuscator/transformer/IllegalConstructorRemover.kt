package dev.onyx.deobfuscator.transformer

import dev.onyx.deobfuscator.Transformer
import dev.onyx.deobfuscator.asm.ClassPool
import dev.onyx.deobfuscator.asm.argumentTypes
import dev.onyx.deobfuscator.asm.isConstructor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.MethodNode
import org.tinylog.kotlin.Logger

class IllegalConstructorRemover : Transformer {

    private var count = 0

    override fun transform(pool: ClassPool) {
        pool.forEach { c ->
            val methods = c.methods.iterator()
            while(methods.hasNext()) {
                val method = methods.next()
                if(method.isIllegalConstructor()) {
                    methods.remove()
                    count++
                }
            }
        }

        Logger.info("Removed $count illegal method constructors.")
    }

    private fun MethodNode.isIllegalConstructor(): Boolean {
        if(!this.isConstructor()) return false
        if(this.argumentTypes.isNotEmpty()) return false
        if(exceptions != listOf(THROWABLE)) return false

        val insns = instructions.toArray().filter { it.opcode > 0 }.iterator()

        if(!insns.hasNext() || insns.next().opcode != ALOAD) return false
        if(!insns.hasNext() || insns.next().opcode != INVOKESPECIAL) return false
        if(!insns.hasNext() || insns.next().opcode != NEW) return false
        if(!insns.hasNext() || insns.next().opcode != DUP) return false
        if(!insns.hasNext() || insns.next().opcode != INVOKESPECIAL) return false
        if(!insns.hasNext() || insns.next().opcode != ATHROW) return false
        return !insns.hasNext()
    }

    companion object {
        private val THROWABLE = Type.getType(Throwable::class.java).internalName
    }
}