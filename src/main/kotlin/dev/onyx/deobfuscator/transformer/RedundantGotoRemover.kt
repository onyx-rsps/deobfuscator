package dev.onyx.deobfuscator.transformer

import dev.onyx.deobfuscator.Transformer
import dev.onyx.deobfuscator.asm.ClassPool
import org.objectweb.asm.Opcodes.GOTO
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.tinylog.kotlin.Logger

class RedundantGotoRemover : Transformer {

    private var count = 0

    override fun transform(pool: ClassPool) {
        pool.forEach { c ->
            c.methods.forEach { m ->
                val insns = m.instructions.iterator()
                while(insns.hasNext()) {
                    val insn0 = insns.next()
                    if(insn0.opcode != GOTO) continue
                    insn0 as JumpInsnNode
                    val insn1 = insn0.next
                    if(insn1 == null || insn1 !is LabelNode) continue
                    if(insn0.label == insn1) {
                        insns.remove()
                        count++
                    }
                }
            }
        }

        Logger.info("Removed $count redundant GOTO instructions.")
    }
}