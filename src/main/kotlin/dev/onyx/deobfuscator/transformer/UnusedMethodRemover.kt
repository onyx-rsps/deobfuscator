package dev.onyx.deobfuscator.transformer

import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import dev.onyx.deobfuscator.Transformer
import dev.onyx.deobfuscator.asm.ClassPool
import dev.onyx.deobfuscator.asm.identifier
import dev.onyx.deobfuscator.asm.isConstructor
import dev.onyx.deobfuscator.asm.isInitializer
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.tinylog.kotlin.Logger

class UnusedMethodRemover : Transformer {

    private var count = 0

    override fun transform(pool: ClassPool) {
        val methodsToRemove = mutableListOf<String>()

        val classNameMap = pool.toList().associateBy { it.name }

        val superClasses = MultimapBuilder.hashKeys().arrayListValues().build<ClassNode, String>()
        pool.forEach { c ->
            c.interfaces.forEach { i ->
                superClasses.put(c, i)
            }
            superClasses.put(c, c.superName)
        }

        val subClasses = MultimapBuilder.hashKeys().arrayListValues().build<ClassNode, String>()
        superClasses.forEach { cls, superClassName ->
            if(classNameMap.containsKey(superClassName)) {
                subClasses.put(classNameMap.getValue(superClassName), cls.name)
            }
        }

        val usedMethods = pool.toList().asSequence().flatMap { it.methods.asSequence() }
            .flatMap { it.instructions.iterator().asSequence() }
            .mapNotNull { it as? MethodInsnNode }
            .map { it.owner + "." + it.name + it.desc }
            .toSet()

        pool.forEach { c ->
            for(m in c.methods) {
                if(isMethodUsed(c, m, usedMethods, superClasses, subClasses, classNameMap)) {
                    continue
                }
                methodsToRemove.add(m.identifier)
            }
        }

        pool.forEach { c ->
            val methods = c.methods.iterator()
            while(methods.hasNext()) {
                val m = methods.next()
                if(m.identifier !in methodsToRemove) continue
                methods.remove()
                count++
            }
        }

        Logger.info("Found ${methodsToRemove.size} unused methods. Removed $count unused methods invocations.")
    }

    private fun isMethodUsed(
        cls: ClassNode,
        method: MethodNode,
        usedMethods: Set<String>,
        superClasses: Multimap<ClassNode, String>,
        subClasses: Multimap<ClassNode, String>,
        classNameMap: Map<String, ClassNode>
    ): Boolean {
        if(method.isConstructor() || method.isInitializer()) return true
        val methodIdentifier = "${cls.name}.${method.name}${method.desc}"
        if(usedMethods.contains(methodIdentifier)) return true

        var currentSuperClasses = superClasses[cls]
        while(currentSuperClasses.isNotEmpty()) {
            currentSuperClasses.forEach { c ->
                if(isJavaMethod(c, method.name, method.desc)) return true
                if(usedMethods.contains(c + "." + method.name + method.desc)) return true
            }
            currentSuperClasses = currentSuperClasses.filter { classNameMap.containsKey(it) }
                .flatMap { superClasses[classNameMap.getValue(it)] }
        }

        var currentSubClasses = subClasses[cls]
        while(currentSubClasses.isNotEmpty()) {
            currentSubClasses.forEach { c ->
                if(usedMethods.contains(c + "." + method.name + method.desc)) return true
            }
            currentSubClasses = currentSuperClasses.flatMap { subClasses[classNameMap.getValue(it)] }
        }

        return false
    }

    private fun isJavaMethod(owner: String, method: String, desc: String): Boolean {
        try {
            var classes = listOf(Class.forName(Type.getObjectType(owner).className))
            while(classes.isNotEmpty()) {
                classes.forEach { c ->
                    if(c.declaredMethods.any { it.name == method && Type.getMethodDescriptor(it) == desc }) {
                        return true
                    }
                }

                classes = classes.flatMap {
                    ArrayList<Class<*>>().apply {
                        addAll(it.interfaces)
                        if(it.superclass != null) add(it.superclass)
                    }
                }
            }
        } catch (e : Exception) {}
        return false
    }
}