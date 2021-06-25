package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.MatrixImage
import ch.obermuhlner.kimage.io.ImageReader
import java.io.File

@DslMarker
annotation class KotlinDSL

@KotlinDSL
class Script {
    var name: String = ""
    var description = ""

    private val args: MutableList<ScriptArg> = mutableListOf()

    private var scriptSingle: ScriptSingle? = null
    private var scriptMulti: ScriptMulti? = null

    fun arg(initializer: ScriptArg.() -> Unit) =
        args.add(ScriptArg().apply(initializer))

    fun single(executable: ScriptSingle.() -> Unit) {
        scriptSingle = ScriptSingle(executable)
    }

    fun multi(executable: ScriptMulti.() -> Unit) {
        scriptMulti = ScriptMulti(executable)
    }

    fun help() {
        println("USAGE: $name")
        println("DESCR: $description")
        println()
        for (arg in args) {
            println("ARG: ${arg.name} : ${arg.description}")
        }
        println()
    }

    fun runSingle(inputFiles: List<File>, arguments: Map<String, String>) {
        scriptSingle?.let {
            for (inputFile in inputFiles) {
                val inputImage = ImageReader.readMatrixImage(inputFile)
                it.runSingleScript(inputFile, inputImage, arguments)
            }
        }
    }

    fun runMulti(inputFiles: List<File>, arguments: Map<String, String>) {
        scriptMulti?.let {
            it.runMultiScript(inputFiles, arguments)
        }
    }
}

@KotlinDSL
class ScriptArg {
    var name: String = ""
    var description = ""
}

@KotlinDSL
class ScriptSingle(val executable: ScriptSingle.() -> Unit) {
    var inputFile: File = File(".")
    var inputImage: Image = MatrixImage(0, 0)
    val arguments: MutableMap<String, String> = mutableMapOf()

    fun runSingleScript(inputFile: File, inputImage: Image, arguments: Map<String, String>) {
        this.inputFile = inputFile
        this.inputImage = inputImage
        this.arguments.putAll(arguments)
        executable()
    }
}

@KotlinDSL
class ScriptMulti(val executable: ScriptMulti.() -> Unit) {
    var inputFiles: List<File> = listOf()
    val arguments: MutableMap<String, String> = mutableMapOf()

    fun runMultiScript(inputFiles: List<File>, arguments: Map<String, String>) {
        this.inputFiles = inputFiles
        this.arguments.putAll(arguments)
        executable()
    }
}

fun kimage(initializer: Script.() -> Unit): Script {
    val script = Script().apply(initializer)

    println("ADDING SCRIPT ${script.name}")
    KImageSingleton.scripts[script.name] = script

    return script
}

object KImageSingleton {
    val scripts: MutableMap<String, Script> = mutableMapOf()
}
