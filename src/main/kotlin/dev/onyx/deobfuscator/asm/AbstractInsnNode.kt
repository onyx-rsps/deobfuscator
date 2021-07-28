package dev.onyx.deobfuscator.asm

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

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

fun loadInt(n: Int): AbstractInsnNode = when (n) {
    in -1..5 -> InsnNode(n + 3)
    in Byte.MIN_VALUE..Byte.MAX_VALUE -> IntInsnNode(BIPUSH, n)
    in Short.MIN_VALUE..Short.MAX_VALUE -> IntInsnNode(SIPUSH, n)
    else -> LdcInsnNode(n)
}

fun loadLong(n: Long): AbstractInsnNode = when (n) {
    0L, 1L -> InsnNode((n + 9).toInt())
    else -> LdcInsnNode(n)
}