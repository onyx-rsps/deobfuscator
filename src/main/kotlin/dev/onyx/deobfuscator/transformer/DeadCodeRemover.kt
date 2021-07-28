package dev.onyx.deobfuscator.transformer

import dev.onyx.deobfuscator.Transformer
import dev.onyx.deobfuscator.asm.ClassPool
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.tinylog.kotlin.Logger

class DeadCodeRemover : Transformer {

    private var count = 0

    override fun transform(pool: ClassPool) {
        pool.forEach { c ->
            c.methods.forEach { m ->
                val frames = Analyzer(BasicInterpreter()).analyze(c.name, m)
                val insns = m.instructions.toArray()
                for(i in frames.indices) {
                    if(frames[i] == null) {
                        m.instructions.remove(insns[i])
                        count++
                    }
                }
            }
        }

        Logger.info("Removed $count dead code instructions.")
    }
}