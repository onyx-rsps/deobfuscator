package dev.onyx.deobfuscator.asm

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LdcInsnNode

fun AbstractInsnNode.isIf(): Boolean {
    return this is JumpInsnNode && opcode != GOTO
}

fun AbstractInsnNode.isReturn(): Boolean = when(opcode) {
    in IRETURN..RETURN -> true
    else -> false
}

fun AbstractInsnNode.pushesInt(): Boolean = when(opcode) {
    LDC -> (this as LdcInsnNode).cst is Int
    SIPUSH, BIPUSH -> true
    in ICONST_M1..ICONST_5 -> true
    else -> false
}

val AbstractInsnNode.pushedInt: Int get() = when {
    opcode in 2..8 -> opcode - 3
    opcode == BIPUSH || opcode == SIPUSH -> (this as IntInsnNode).operand
    this is LdcInsnNode && cst is Int -> cst as Int
    else -> throw IllegalStateException()
}