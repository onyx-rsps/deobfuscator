package dev.onyx.deobfuscator.transformer

import dev.onyx.deobfuscator.Transformer
import dev.onyx.deobfuscator.asm.ClassPool
import org.objectweb.asm.Opcodes.ACC_NATIVE
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.ClassNode
import org.tinylog.kotlin.Logger
import java.util.*

class Renamer : Transformer {

    private var classCounter = 0
    private var methodCounter = 0
    private var fieldCounter = 0

    private val mappings = hashMapOf<String, String>()

    override fun transform(pool: ClassPool) {
        this.generateMappings(pool)
        this.applyMappings(pool)

        Logger.info("Renamed $classCounter obfuscated classes.")
        Logger.info("Renamed $methodCounter obfuscated methods.")
        Logger.info("Renamed $fieldCounter obfuscated fields.")
    }

    /**
     * In order to properly generate mappings, we loop through all classes, methods, and fields and generate
     * an incremental name remap.
     *
     * Then, for ones with possible members, we loop back through and set the names from their super member types.
     */
    private fun generateMappings(pool: ClassPool) {
        /**
         * Generate class name mappings.
         */
        pool.forEach classLoop@ { c ->
            if(c.name.isObfuscatedName()) {
                mappings[c.name] = "class${++classCounter}"
            }
        }

        /**
         * Generate method name mappings
         */
        pool.forEach classLoop@ { c ->
            c.methods.filter { it.name.isObfuscatedName() }.forEach methodLoop@ { m ->
                val owner = c
                if(m.name.indexOf("<") != -1 || (m.access and ACC_NATIVE) != 0) {
                    return@methodLoop
                }

                val stack = Stack<ClassNode>()
                stack.add(owner)
                while(stack.isNotEmpty()) {
                    val node = stack.pop()
                    if(node != owner && node.methods.firstOrNull { it.name == m.name && it.desc == m.desc } != null) {
                        return@methodLoop
                    }

                    val parent = pool.findClass(node.superName)
                    if(parent != null) {
                        stack.push(parent)
                    }

                    val interfaces = node.interfaces.mapNotNull { pool.findClass(it) }
                    stack.addAll(interfaces)
                }

                val name = "method${++methodCounter}"

                stack.add(owner)
                while(stack.isNotEmpty()) {
                    val node = stack.pop()
                    val key = node.name + "." + m.name + m.desc
                    mappings[key] = name
                    pool.forEach { k ->
                        if(k.superName == node.name || k.interfaces.contains(node.name)) {
                            stack.push(k)
                        }
                    }
                }
            }
        }

        /**
         * Generate field name mappings
         */
        pool.forEach classLoop@ { c ->
            c.fields.filter { it.name.isObfuscatedName() }.forEach fieldLoop@ { f ->
                val owner = c
                val stack = Stack<ClassNode>()

                stack.add(owner)
                while(stack.isNotEmpty()) {
                    val node = stack.pop()
                    if(node != owner && node.fields.firstOrNull { it.name == f.name && it.desc == f.desc } != null) {
                        return@fieldLoop
                    }

                    val parent = pool.findClass(node.superName)
                    if(parent != null) {
                        stack.push(parent)
                    }

                    val interfaces = node.interfaces.mapNotNull { pool.findClass(it) }
                    stack.addAll(interfaces)
                }

                val name = "field${++fieldCounter}"

                stack.add(owner)
                while(stack.isNotEmpty()) {
                    val node = stack.pop()
                    val key = node.name + "." + f.name
                    mappings[key] = name
                    pool.forEach { k ->
                        if(k.superName == node.name || k.interfaces.contains(node.name)) {
                            stack.push(k)
                        }
                    }
                }
            }
        }

    }

    /**
     * Apply the mappings to the [pool] using the ASM built in
     * class remapping visitor.
     */
    private fun applyMappings(pool: ClassPool) {
        val remapper = SimpleRemapper(mappings)

        val newNodes = mutableListOf<ClassNode>()

        pool.forEach {  c ->
            val newNode = ClassNode()
            c.accept(ClassRemapper(newNode, remapper))
            newNodes.add(newNode)
        }

        pool.clear()
        newNodes.forEach { pool.addClass(it) }
        pool.init()
    }

    private fun String.isObfuscatedName(): Boolean {
        return (this.length <= 2) || (this.length == 3 && listOf("aa", "ab", "ac", "ad", "ae").any { this.startsWith(it) })
    }
}