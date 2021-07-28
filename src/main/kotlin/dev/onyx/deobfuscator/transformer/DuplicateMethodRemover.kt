package dev.onyx.deobfuscator.transformer

import com.google.common.collect.TreeMultimap
import dev.onyx.deobfuscator.Transformer
import dev.onyx.deobfuscator.asm.ClassPool
import dev.onyx.deobfuscator.asm.identifier
import dev.onyx.deobfuscator.asm.isInitializer
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.lang.reflect.Modifier

class DuplicateMethodRemover : Transformer {

    private var count = 0

    override fun transform(pool: ClassPool) {
        val map = TreeMultimap.create<String, String>()

        pool.forEach { c ->
            c.methods.filter { Modifier.isStatic(it.access) && !it.isInitializer() }.forEach { m ->
                map.put(m.id(), m.identifier)
            }
        }

        map.asMap().entries.removeIf { it.value.size == 1 }
    }

    private fun MethodNode.id(): String {
        return "${ Type.getReturnType(desc)}." + (instructions.lineNumberRange() ?: "*") + "." + instructions.hash()
    }

    private fun InsnList.lineNumberRange(): IntRange? {
        val lns = iterator().asSequence().mapNotNull { it as? LineNumberNode }.mapNotNull { it.line }.toList()
        if(lns.isEmpty()) return null
        return lns.first()..lns.last()
    }

    private fun InsnList.hash(): Int {
        return iterator().asSequence().mapNotNull {
            when(it) {
                is FieldInsnNode -> it.owner + "." + it.name + ":" + it.opcode
                is MethodInsnNode -> it.opcode.toString() + ":" + it.owner + "." + it.name
                is InsnNode -> it.opcode.toString()
                else -> null
            }
        }.toSet().hashCode()
    }
}