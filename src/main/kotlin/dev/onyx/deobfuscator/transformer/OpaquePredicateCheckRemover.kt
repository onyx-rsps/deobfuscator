package dev.onyx.deobfuscator.transformer

import dev.onyx.deobfuscator.Transformer
import dev.onyx.deobfuscator.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.tinylog.kotlin.Logger
import java.lang.IllegalStateException
import java.lang.reflect.Modifier

class OpaquePredicateCheckRemover : Transformer {

    private var returnCount = 0
    private var exceptionCount = 0

    override fun transform(pool: ClassPool) {
        pool.forEach { c ->
            c.methods.forEach { m ->
                val insns = m.instructions.iterator()
                val lastArgIndex = m.lastArgIndex
                while(insns.hasNext()) {
                    val insn = insns.next()

                    val removeCount = when {
                        insn.matchesReturnPredicate(lastArgIndex) -> {
                            returnCount++
                            4
                        }

                        insn.matchesExceptionPredicate(lastArgIndex) -> {
                            exceptionCount++
                            7
                        }

                        else -> continue
                    }

                    val label = (insn.next.next as JumpInsnNode).label.label
                    insns.remove()
                    repeat(removeCount - 1) {
                        insns.next()
                        insns.remove()
                    }

                    insns.add(JumpInsnNode(GOTO, LabelNode(label)))
                }
            }
        }

        Logger.info("Removed $returnCount return opaque predicate checks.")
        Logger.info("Removed $exceptionCount exception opaque predicate checks.")
    }

    private val MethodNode.lastArgIndex: Int get() {
        val offset = if(Modifier.isStatic(access)) 1 else 0
        return (Type.getArgumentsAndReturnSizes(desc) shr 2) - offset - 1
    }

    private fun AbstractInsnNode.matchesReturnPredicate(lastArgIndex: Int): Boolean {
        val i0 = this
        if(i0.opcode != ILOAD) return false
        i0 as VarInsnNode
        if(i0.`var` != lastArgIndex) return false
        val i1 = i0.next
        if(!i1.pushesInt()) return false
        val i2 = i1.next
        if(!i2.isIf()) return false
        val i3 = i2.next
        if(!i3.isReturn()) return false
        return true
    }

    private fun AbstractInsnNode.matchesExceptionPredicate(lastArgIndex: Int): Boolean {
        val i0 = this
        if(i0.opcode != ILOAD) return false
        i0 as VarInsnNode
        if(i0.`var` != lastArgIndex) return false
        val i1 = i0.next
        if(!i1.pushesInt()) return false
        val i2 = i1.next
        if(!i2.isIf()) return false
        val i3 = i2.next
        if(i3.opcode != NEW) return false
        val i4 = i3.next
        if(i4.opcode != DUP) return false
        val i5 = i4.next
        if(i5.opcode != INVOKESPECIAL) return false
        i5 as MethodInsnNode
        val i6 = i5.next
        if(i6.opcode != ATHROW) return false
        return true
    }

    companion object {
        private val INVALID_STATE_EXCEPTION = Type.getInternalName(IllegalStateException::class.java)
    }
}