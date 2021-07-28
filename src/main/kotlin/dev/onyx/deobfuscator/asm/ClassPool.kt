package dev.onyx.deobfuscator.asm

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.tinylog.kotlin.Logger
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

class ClassPool private constructor() {

    private val classMap = mutableMapOf<String, ClassNode>()

    val size: Int get() = classMap.values.size

    /**
     * Initializes the class pool.
     */
    internal fun init() {
        this.forEach { it.init(this) }
    }

    fun addClass(cls: ClassNode) {
        classMap[cls.name] = cls
    }

    fun removeClass(cls: ClassNode) {
        classMap.remove(cls.name)
    }

    fun toList(): List<ClassNode> = classMap.values.toList()

    fun forEach(action: (ClassNode) -> Unit) = toList().forEach(action)

    fun first(predicate: (ClassNode) -> Boolean) = toList().first(predicate)

    fun firstOrNull(predicate: (ClassNode) -> Boolean) = toList().firstOrNull(predicate)

    fun <T> map(transform: (ClassNode) -> T) = toList().map(transform)

    fun findClass(className: String): ClassNode? = toList().firstOrNull { it.name == className }

    fun toJar(file: File) {
        if(file.exists()) {
            file.delete()
        }

        val jos = JarOutputStream(FileOutputStream(file))
        this.forEach { cls ->
            jos.putNextEntry(JarEntry(cls.name + ".class"))

            val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
            cls.accept(writer)

            jos.write(writer.toByteArray())
            jos.closeEntry()
        }

        jos.close()
    }

    companion object {

        fun fromJar(file: File): ClassPool {
            if(!file.exists()) throw FileNotFoundException()

            val pool = ClassPool()

            JarFile(file).use { jar ->
                jar.entries().asSequence()
                    .filter { it.name.endsWith(".class") }
                    .forEach { entry ->
                        val node = ClassNode()
                        val reader = ClassReader(jar.getInputStream(entry))
                        reader.accept(node, ClassReader.SKIP_FRAMES)
                        pool.addClass(node)
                    }
            }

            pool.init()

            return pool
        }
    }
}