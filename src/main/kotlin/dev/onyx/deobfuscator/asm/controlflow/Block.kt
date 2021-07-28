package dev.onyx.deobfuscator.asm.controlflow

class Block {

    var startInsn = 0

    var endInsn = 0

    var next: Block? = null

    var prev: Block? = null

    val origin: Block get() {
        var b = this
        var last = prev
        while(last != null) {
            b = last
            last = b.prev
        }
        return b
    }

    val branches = mutableListOf<Block>()

}