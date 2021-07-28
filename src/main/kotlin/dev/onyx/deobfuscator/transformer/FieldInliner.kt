package dev.onyx.deobfuscator.transformer

import dev.onyx.deobfuscator.Transformer
import dev.onyx.deobfuscator.asm.ClassPool
import org.objectweb.asm.Opcodes.GETSTATIC
import org.objectweb.asm.Opcodes.PUTSTATIC
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.tinylog.kotlin.Logger
import java.lang.reflect.Modifier

class FieldInliner : Transformer {

    private var count = 0

    override fun transform(pool: ClassPool) {
        val resolver = FieldResolver(pool)

        pool.forEach { c ->
            c.methods.forEach { m ->
                m.instructions.iterator().forEach { insn ->
                    if(insn is FieldInsnNode) {
                        val opcode = insn.opcode
                        val oldOwner = insn.owner
                        insn.owner = resolver.getOwner(
                            insn.owner,
                            insn.name,
                            insn.desc,
                            (opcode == GETSTATIC || opcode == PUTSTATIC)
                        )

                        val newOwner = insn.owner
                        if(oldOwner != newOwner) {
                            count++
                        }
                    }
                }
            }
        }

        Logger.info("Inlined $count fields.")
    }

    /**
     * Represents a field call graph resolver object.
     *
     * @property pool ClassGroup
     * @constructor
     */
    private class FieldResolver(private val pool: ClassPool) {

        /**
         * A map of [pool] to the class name as a key.
         */
        private val namedGroup = pool.toList().associateBy { it.name }

        /**
         * Gets the proper owner of a field by analyzing the invoke tree
         * of a given field.
         *
         * @param owner String
         * @param name String
         * @param desc String
         * @param isStatic Boolean
         * @return String
         */
        fun getOwner(owner: String, name: String, desc: String, isStatic: Boolean): String {

            var node = namedGroup[owner] ?: return owner

            /**
             * Loop forever until the block returns a value.
             */
            while(true) {
                if(node.hasDeclaredField(name, desc, isStatic)) {
                    return node.name
                }

                val superName = node.superName
                node = namedGroup[superName] ?: return superName
            }
        }


        /**
         * Checks if a [ClassNode] has a field matching the inputs.
         *
         * @receiver ClassNode
         * @param name String
         * @param desc String
         * @param isStatic Boolean
         * @return Boolean
         */
        private fun ClassNode.hasDeclaredField(name: String, desc: String, isStatic: Boolean): Boolean {
            return this.fields.any {
                it.name == name && it.desc == desc && Modifier.isStatic(it.access) == isStatic
            }
        }
    }
}