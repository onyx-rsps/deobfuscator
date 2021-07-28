package dev.onyx.deobfuscator.transformer

import com.google.common.collect.MultimapBuilder
import dev.onyx.deobfuscator.Transformer
import dev.onyx.deobfuscator.asm.ClassPool
import dev.onyx.deobfuscator.asm.argumentTypes
import dev.onyx.deobfuscator.asm.identifier
import dev.onyx.deobfuscator.asm.pushesInt
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.tinylog.kotlin.Logger
import java.lang.reflect.Modifier

class OpaquePredicateArgumentRemover : Transformer {

    private var count = 0
    private var insnCount = 0

    override fun transform(pool: ClassPool) {
        val classNameMap = pool.toList().associateBy { it.name }
        val topMethods = HashSet<String>()

        for(c in pool.toList()) {
            val superClasses = getSuperClasses(c, classNameMap)
            for(m in c.methods) {
                if(superClasses.none { it.methods.any { it.name == m.name && it.desc == m.desc } }) {
                    topMethods.add("${c.name}.${m.name}${m.desc}")
                }
            }
        }

        val implementationsMultiMap = MultimapBuilder.hashKeys().arrayListValues().build<String, Pair<ClassNode, MethodNode>>()
        val implementations = implementationsMultiMap.asMap()

        for(c in pool.toList()) {
            for(m in c.methods) {
                val s = overrides(c.name, m.name + m.desc, topMethods, classNameMap) ?: continue
                implementationsMultiMap.put(s, c to m)
            }
        }

        val itr = implementations.iterator()
        for(e in itr) {
            if(e.value.any { !it.second.hasOpaquePredicateArgument() }) {
                itr.remove()
            }
        }

        for(c in pool.toList()) {
            for(m in c.methods) {
                val insns = m.instructions
                for(insn in insns) {
                    if(insn !is MethodInsnNode) continue
                    val s = overrides(insn.owner, insn.name + insn.desc, implementations.keys, classNameMap) ?: continue
                    if(!insn.previous.pushesInt()) {
                        implementations.remove(s)
                    }
                }
            }
        }

        implementationsMultiMap.values().forEach {
            val oldDesc = it.second.desc
            val newDesc = removeLastArgument(oldDesc)
            it.second.desc = newDesc
            count++
        }

        for(c in pool.toList()) {
            for(m in c.methods) {
                val insns = m.instructions
                for(insn in insns) {
                    if(insn !is MethodInsnNode) continue
                    if(overrides(insn.owner, insn.name + insn.desc, implementations.keys, classNameMap) != null) {
                        insn.desc = removeLastArgument(insn.desc)
                        val prev = insn.previous
                        check(prev.pushesInt())
                        insns.remove(prev)
                        insnCount++
                    }
                }
            }
        }

        Logger.info("Removed $count opaque predicate arguments and updated $insnCount method invoke instructions.")
    }

    private fun overrides(
        owner: String,
        signature: String,
        methods: Set<String>,
        classNameMap: Map<String, ClassNode>
    ): String? {
        val s = "$owner.$signature"
        if (s in methods) return s
        if (signature.startsWith("<init>")) return null
        val classNode = classNameMap[owner] ?: return null
        for (sup in getSuperClasses(classNode, classNameMap)) {
            return overrides(sup.name, signature, methods, classNameMap) ?: continue
        }
        return null
    }

    private fun getSuperClasses(cls: ClassNode, classNameMap: Map<String, ClassNode>): Collection<ClassNode> {
        return cls.interfaces.plus(cls.superName)
            .mapNotNull { classNameMap[it] }
            .flatMap { getSuperClasses(it, classNameMap).plus(it) }
    }

    private fun MethodNode.hasOpaquePredicateArgument(): Boolean {
        val argTypes = Type.getArgumentTypes(desc)
        if (argTypes.isEmpty()) return false
        val lastArg = argTypes.last()
        if (lastArg != Type.BYTE_TYPE && lastArg != Type.SHORT_TYPE && lastArg != Type.INT_TYPE) return false
        if (Modifier.isAbstract(access)) return true
        val lastParamLocalIndex = (if (Modifier.isStatic(access)) -1 else 0) + (Type.getArgumentsAndReturnSizes(desc) shr 2) - 1
        for (insn in instructions) {
            if (insn !is VarInsnNode) continue
            if (insn.`var` == lastParamLocalIndex) return false
        }
        return true
    }

    private fun removeLastArgument(desc: String): String {
        val type = Type.getMethodType(desc)
        return Type.getMethodDescriptor(type.returnType, *type.argumentTypes.copyOf(type.argumentTypes.size - 1))
    }
}