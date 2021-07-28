package dev.onyx.deobfuscator.asm.graph

import dev.onyx.deobfuscator.asm.ClassPool
import org.jgrapht.alg.connectivity.ConnectivityInspector
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DirectedPseudograph

class InheritanceGraph(val pool: ClassPool) {

    private val graph = DirectedPseudograph<String, DefaultEdge>(DefaultEdge::class.java)

    init {
        /*
         * Add all classes in the pool as vertices to the graph
         */
        pool.forEach { cls ->
            graph.addVertex(cls.name)

            if(cls.superName != null) {
                graph.addVertex(cls.superName)
            }

            if(cls.interfaces != null) {
                cls.interfaces.forEach { itf ->
                    graph.addVertex(itf)
                }
            }
        }

        /*
         * Loop through each class and create edges between the super class and interface classes
         */
        pool.forEach classLoop@ { cls ->
            if(cls.superName != null) {
                graph.addEdge(cls.name, cls.superName)
            }

            if(cls.interfaces != null) {
                cls.interfaces.forEach itfLoop@ { itf ->
                    graph.addEdge(cls.name, itf)
                }
            }
        }
    }

    fun findChildren(className: String): List<String> {
        val inspector = ConnectivityInspector(graph)
        return inspector.connectedSetOf(className).toList()
    }
}