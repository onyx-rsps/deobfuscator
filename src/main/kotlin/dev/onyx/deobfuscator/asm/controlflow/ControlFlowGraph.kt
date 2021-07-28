package dev.onyx.deobfuscator.asm.controlflow

import dev.onyx.deobfuscator.asm.owner
import org.objectweb.asm.tree.AbstractInsnNode.*
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue

class ControlFlowGraph(val method: MethodNode) : Analyzer<BasicValue>(BasicInterpreter()) {

    val blocks = mutableListOf<Block>()

    fun generate() {
        this.analyze(method.owner.name, method)
    }

    override fun init(owner: String, method: MethodNode) {
        val insns = method.instructions
        var currentBlock = Block()
        blocks.add(currentBlock)
        for(i in 0 until insns.size()) {
            val insn = insns[i]
            currentBlock.endInsn++
            if(insn.next == null) break
            if(insn.next.type == LABEL ||
                    insn.type == JUMP_INSN ||
                    insn.type == LOOKUPSWITCH_INSN ||
                    insn.type == TABLESWITCH_INSN) {
                currentBlock = Block()
                currentBlock.startInsn = i + 1
                currentBlock.endInsn = i + 1
                blocks.add(currentBlock)
            }
        }
    }

    override fun newControlFlowEdge(insnIndex: Int, successorIndex: Int) {
        val currentBlock = findBlock(insnIndex)
        val nextBlock = findBlock(successorIndex)
        if(currentBlock != nextBlock) {
            if(insnIndex + 1 == successorIndex) {
                currentBlock.next = nextBlock
                nextBlock.prev = currentBlock
            } else {
                currentBlock.branches.add(nextBlock)
            }
        }
    }

    private fun findBlock(insnIndex: Int): Block {
        return blocks.first { insnIndex in it.startInsn until it.endInsn }
    }
}