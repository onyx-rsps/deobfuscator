package dev.onyx.deobfuscator.asm

import dev.onyx.deobfuscator.util.mixin
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.lang.reflect.Modifier

var ClassNode.pool: ClassPool by mixin()
    internal set

val ClassNode.type: Type get() = Type.getObjectType(this.name)
val ClassNode.identifier: String get() = this.name

val ClassNode.isStatic: Boolean get() = Modifier.isStatic(this.access)
val ClassNode.isAbstract: Boolean get() = Modifier.isAbstract(this.access)
val ClassNode.isInterface: Boolean get() = Modifier.isInterface(this.access)

internal fun ClassNode.init(pool: ClassPool) {
    this.pool = pool
    this.methods.forEach { it.init(this) }
    this.fields.forEach { it.init(this) }
}

fun ClassNode.findMethod(name: String, desc: String): MethodNode? {
    return this.methods.firstOrNull { it.name == name && it.desc == desc }
}

fun ClassNode.findField(name: String, desc: String): FieldNode? {
    return this.fields.firstOrNull { it.name == name && it.desc == desc }
}

fun ClassNode.toByteCode(): ByteArray {
    val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
    this.accept(writer)
    return writer.toByteArray()
}

