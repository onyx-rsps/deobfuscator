package dev.onyx.deobfuscator.asm

import dev.onyx.deobfuscator.util.mixin
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

var MethodNode.owner: ClassNode by mixin()
    internal set

val MethodNode.pool: ClassPool get() = this.owner.pool
val MethodNode.type: Type get() = Type.getMethodType(this.desc)
val MethodNode.identifier: String get() = "${this.owner.identifier}.${this.name}${this.desc}"

val MethodNode.returnType: Type get() = this.type.returnType
val MethodNode.argumentTypes: List<Type> get() = this.type.argumentTypes.toList()

fun MethodNode.isConstructor(): Boolean = name == "<init>"
fun MethodNode.isInitializer(): Boolean = name == "<clinit>"

internal fun MethodNode.init(owner: ClassNode) {
    this.owner = owner
}