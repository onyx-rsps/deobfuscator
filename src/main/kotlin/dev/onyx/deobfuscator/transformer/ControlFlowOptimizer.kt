package dev.onyx.deobfuscator.transformer

import dev.onyx.deobfuscator.Transformer
import dev.onyx.deobfuscator.asm.ClassPool
import dev.onyx.deobfuscator.asm.controlflow.Block
import dev.onyx.deobfuscator.asm.controlflow.ControlFlowGraph
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.LabelNode
import org.tinylog.kotlin.Logger
import java.util.*
import kotlin.collections.AbstractMap

class ControlFlowOptimizer : Transformer {

    private var count = 0

    override fun transform(pool: ClassPool) {
        pool.forEach classLoop@ { c ->
            c.methods.forEach methodLoop@ { m ->
                if(m.tryCatchBlocks.isNotEmpty()) {
                    return@methodLoop
                }

                val controlFlowGraph = ControlFlowGraph(m)
                controlFlowGraph.generate()

                m.instructions = rebuildMethodInsns(m.instructions, controlFlowGraph)
                count += controlFlowGraph.blocks.size
            }
        }

        Logger.info("Reordered $count control-flow blocks.")
    }

    private fun rebuildMethodInsns(origInsns: InsnList, graph: ControlFlowGraph): InsnList {
        val insns = InsnList()
        val labelMap = LabelMap()

        if(graph.blocks.isEmpty()) {
            return insns
        }

        val stack = Stack<Block>()
        val moved = mutableSetOf<Block>()

        stack.add(graph.blocks.first())
        while(stack.isNotEmpty()) {
            val block = stack.pop()
            if(block in moved) continue
            moved.add(block)

            block.branches.forEach { stack.add(it.origin) }
            block.next?.let { stack.add(it) }

            for(i in block.startInsn until block.endInsn) {
                insns.add(origInsns[i].clone(labelMap))
            }
        }

        return insns
    }

    private class LabelMap : AbstractMap<LabelNode, LabelNode>() {
        private val map = hashMapOf<LabelNode, LabelNode>()
        override val entries get() = throw UnsupportedOperationException()
        override fun get(key: LabelNode) = map.getOrPut((key)) { LabelNode() }
    }
}