package dev.onyx.deobfuscator

import dev.onyx.deobfuscator.asm.ClassPool
import dev.onyx.deobfuscator.transformer.*
import org.tinylog.kotlin.Logger

class Deobfuscator(val context: DeobfuscatorContext) {

    private val pool = ClassPool.fromJar(context.inputJar)

    private val transformers = mutableListOf<Transformer>()

    private fun init() {
        Logger.info("Initializing Onyx deobfuscator...")

        /*
         * Clear loaded transformers.
         */
        this.transformers.clear()

        /*
         * Register transformers.
         */
        register<RuntimeExceptionRemover>()
        register<DeadCodeRemover>()
        register<ControlFlowOptimizer>()
        register<OpaquePredicateCheckRemover>()
        register<UnusedFieldRemover>()
        register<IllegalConstructorRemover>()
        register<UnusedMethodRemover>()

        Logger.info("Registered ${transformers.size} bytecode transformers.")
    }

    private inline fun <reified T : Transformer> register() {
        val inst = T::class.java.getDeclaredConstructor().newInstance() as Transformer
        this.transformers.add(inst)
    }

    fun run() {
        /*
         * Init the deobfuscator
         */
        this.init()

        Logger.info("Starting deobfuscation.")

        this.transformers.forEach { transformer ->
            Logger.info("Running bytecode transformer: '${transformer::class.java.simpleName}'.")

            val start = System.currentTimeMillis()
            transformer.transform(pool)
            val delta = System.currentTimeMillis() - start

            Logger.info("Completed bytecode transformer '${transformer::class.java.simpleName}' in ${(delta / 1000)} seconds.")
        }

        Logger.info("Completed all bytecode transformers. Exporting classes to output JAR file.")

        /*
         * Export the class pool to the output JAR file.
         */
        pool.toJar(context.outputJar)

        Logger.info("Deobfuscation completed successfully.")
    }
}