package dev.onyx.deobfuscator.transformer

import dev.onyx.deobfuscator.Transformer
import dev.onyx.deobfuscator.asm.ClassPool
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodNode
import org.tinylog.kotlin.Logger

class MethodSorter : Transformer {

    private var count = 0

    override fun transform(pool: ClassPool) {
        pool.forEach { c ->
            val lineNumbers = c.methods.associateWith { (it.firstLineNumber ?: Int.MAX_VALUE) }
            c.methods = c.methods.sortedBy { lineNumbers.getValue(it) }
            count += c.methods.size
        }

        Logger.info("Sorted $count methods.")
    }

    private val MethodNode.firstLineNumber: Int? get() {
        for(insn in instructions) {
            if(insn is LineNumberNode) {
                return insn.line
            }
        }
        return null
    }
}