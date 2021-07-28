package dev.onyx.deobfuscator.transformer

import dev.onyx.deobfuscator.Transformer
import dev.onyx.deobfuscator.asm.ClassPool
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.tinylog.kotlin.Logger

class StackFrameRebuilder : Transformer {

    override fun transform(pool: ClassPool) {
        val newNodes = mutableListOf<ClassNode>()

        pool.forEach { c ->
            val newNode = ClassNode()
            val writer = Writer(pool.toList().associate { it.name to it })
            c.accept(writer)

            val reader = ClassReader(writer.toByteArray())
            reader.accept(newNode, ClassReader.SKIP_FRAMES)

            newNodes.add(newNode)
        }

        pool.clear()
        newNodes.forEach { pool.addClass(it) }
        pool.init()

        Logger.info("Rebuilt stack frames for ${pool.size} classes.")
    }

    private class Writer(private val classNames: Map<String, ClassNode>) : ClassWriter(COMPUTE_FRAMES) {

        companion object {
            val OBJECT_INTERNAL_NAME: String = Type.getInternalName(Any::class.java)
        }

        override fun getCommonSuperClass(type1: String, type2: String): String {
            if (isAssignable(type1, type2)) return type1
            if (isAssignable(type2, type1)) return type2
            var t1 = type1
            do {
                t1 = checkNotNull(superClassName(t1, classNames))
            } while (!isAssignable(t1, type2))
            return t1
        }

        private fun isAssignable(to: String, from: String): Boolean {
            if (to == from) return true
            val sup = superClassName(from, classNames) ?: return false
            if (isAssignable(to, sup)) return true
            return interfaceNames(from).any { isAssignable(to, it) }
        }

        private fun interfaceNames(type: String): List<String> {
            return if (type in classNames) {
                classNames.getValue(type).interfaces
            } else {
                Class.forName(type.replace('/', '.')).interfaces.map { Type.getInternalName(it) }
            }
        }

        private fun superClassName(type: String, classNames: Map<String, ClassNode>): String? {
            return if (type in classNames) {
                classNames.getValue(type).superName
            } else {
                val c = Class.forName(type.replace('/', '.'))
                if (c.isInterface) {
                    OBJECT_INTERNAL_NAME
                } else {
                    c.superclass?.let { Type.getInternalName(it) }
                }
            }
        }
    }
}