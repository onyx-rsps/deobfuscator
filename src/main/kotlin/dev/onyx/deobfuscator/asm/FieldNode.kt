package dev.onyx.deobfuscator.asm

import dev.onyx.deobfuscator.util.mixin
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode

var FieldNode.owner: ClassNode by mixin()
    internal set

val FieldNode.pool: ClassPool get() = this.owner.pool
val FieldNode.type: Type get() = Type.getType(this.desc)
val FieldNode.identifier: String get() = "${this.owner.identifier}.${this.name}"

internal fun FieldNode.init(owner: ClassNode) {
    this.owner = owner
}