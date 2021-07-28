package dev.onyx.deobfuscator.transformer

import dev.onyx.deobfuscator.Transformer
import dev.onyx.deobfuscator.asm.ClassPool
import dev.onyx.deobfuscator.asm.type
import org.objectweb.asm.tree.FieldNode
import org.tinylog.kotlin.Logger
import java.lang.reflect.Modifier

class FieldSorter : Transformer {

    private var count = 0

    override fun transform(pool: ClassPool) {
        pool.forEach { c ->
            c.fields = c.fields.sortedWith(COMPARATOR)
            count += c.fields.size
        }

        Logger.info("Sorted $count fields.")
    }

    companion object {
        private val COMPARATOR: Comparator<FieldNode> = compareBy<FieldNode> { !Modifier.isStatic(it.access) }
            .thenBy { Modifier.toString(it.access and Modifier.fieldModifiers()) }
            .thenBy { it.type.className }
            .thenBy { it.name }
    }
}