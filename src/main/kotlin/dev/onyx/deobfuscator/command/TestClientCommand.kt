package dev.onyx.deobfuscator.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import org.tinylog.kotlin.Logger
import java.applet.Applet
import java.applet.AppletContext
import java.applet.AppletStub
import java.awt.Color
import java.awt.Dimension
import java.awt.GridLayout
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import javax.swing.JFrame

class TestClientCommand : CliktCommand(
    name = "testclient",
    help = "Starts a simple RuneScape client using a gamepack JAR.",
    invokeWithoutSubcommand = true,
    printHelpOnEmptyArgs = true
) {

    private val gamepackJar by option("-g", "--gamepack-jar", help = "Path to the gamepack JAR file to test.")
        .file(mustExist = true, canBeDir = false)
        .required()

    override fun run() {
        val client = TestClient(gamepackJar)
        client.start()
    }
}

class TestClient(private val gamepackJar: File) {

    fun start() {
        Logger.info("Starting test RuneScape client using gamepack JAR: ${gamepackJar.path}.")

        val params = fetchJavConfig()
        val classloader = URLClassLoader(arrayOf(gamepackJar.toURI().toURL()))
        val main = params["initial_class"]!!.replace(".class", "")
        val applet = classloader.loadClass(main).newInstance() as Applet
        applet.background = Color.BLACK
        applet.preferredSize = Dimension(params["applet_minwidth"]!!.toInt(), params["applet_minheight"]!!.toInt())
        applet.size = applet.preferredSize
        applet.layout = null
        applet.setStub(ClientAppletStub(applet, params))
        applet.isVisible = true
        applet.init()

        val frame = JFrame("Test Client")
        frame.layout = GridLayout(1, 0)
        frame.add(applet)
        frame.pack()
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }

    private fun fetchJavConfig(): Map<String, String> {
        val params = mutableMapOf<String, String>()
        val lines = URL(JAV_CONFIG_URL).readText(Charsets.UTF_8).split("\n")
        lines.forEach {
            var line = it
            if(line.startsWith("param=")) {
                line = line.substring(6)
            }
            val idx = line.indexOf("=")
            if(idx >= 0) {
                params[line.substring(0, idx)] = line.substring(idx + 1)
            }
        }

        return params
    }

    private class ClientAppletStub(private val applet: Applet, private val params: Map<String, String>) : AppletStub {
        override fun getCodeBase(): URL = URL(params["codebase"])
        override fun getDocumentBase(): URL = URL(params["codebase"])
        override fun getParameter(name: String): String? = params[name]
        override fun getAppletContext(): AppletContext? = null
        override fun isActive(): Boolean = true
        override fun appletResize(width: Int, height: Int) {
            applet.size = Dimension(width, height)
        }
    }

    companion object {
        private const val JAV_CONFIG_URL = "http://oldschool1.runescape.com/jav_config.ws"
    }
}