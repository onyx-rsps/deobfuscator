package dev.onyx.deobfuscator.transformer

import dev.onyx.deobfuscator.Transformer
import dev.onyx.deobfuscator.asm.ClassPool
import dev.onyx.deobfuscator.asm.identifier
import org.objectweb.asm.tree.FieldInsnNode
import org.tinylog.kotlin.Logger
import java.lang.reflect.Modifier

class UnusedFieldRemover : Transformer {

    private var count = 0

    override fun transform(pool: ClassPool) {
        val usedFields = pool.toList().flatMap { it.methods }
            .flatMap { it.instructions.toArray().asIterable() }
            .mapNotNull { it as? FieldInsnNode }
            .map { it.owner + "." + it.name }
            .toSet()

        for(c in pool.toList()) {
            val it = c.fields.iterator()
            while(it.hasNext()) {
                val field = it.next()
                val fieldName = field.identifier
                if(!usedFields.contains(fieldName) && Modifier.isFinal(field.access)) {
                    it.remove()
                    count++
                }
            }
        }

        Logger.info("Remove $count unused fields.")
    }
}