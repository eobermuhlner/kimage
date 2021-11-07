package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.ImageWriter
import ch.obermuhlner.kimage.javafx.KImageApplication
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import javafx.application.Application
import java.io.File
import javax.script.*

object KImage {
    @JvmStatic
    fun main(args: Array<String>) = mainBody {
        ArgParser(args).parseInto(::KimageCli).run {
            execute()
        }
    }

    const val VERSION: String = "0.1.0"
}

class KimageCli(parser: ArgParser) {

    private val versionMode by parser.flagging(
        "--version",
        help = "print version"
    )

    private val verboseMode by parser.flagging(
        "-v", "--verbose",
        help = "enable verbose mode"
    )
        .default(false)

    private val debugMode by parser.flagging(
        "--debug",
        help = "enable debug mode"
    )
        .default(false)

    private val parameters by parser.adding(
        "-p", "--param", "--arg",
        help = "add parameter key=value"
    ) {
        val split = split("=")
        Pair(split[0], split[1])
    }

    private val singleMode by parser.flagging(
        "--single",
        help = "enable single mode"
    )
        .default(false)

    private val helpMode by parser.flagging(
        "--docu", // TODO integrate with automatic --help
        help = "get help about the current script"
    )
        .default(false)

    private val uiMode by parser.flagging(
        "--ui",
        help = "UI mode"
    ).default(false)

    private val scriptDirectory: String by parser.storing(
        "--script-dir",
        help = "script directory"
    )
        .default("")

    private val outputPrefix: String by parser.storing(
        "-o", "--output-prefix",
        help = "output prefix"
    )
        .default("")

    private val outputDirectory: String by parser.storing(
        "-d", "--dir",
        help = "output directory"
    )
        .default("")

    private val command by parser.positional(
        "COMMAND",
        help = "command to execute"
    )
        .default("")

    private val fileNames by parser.positionalList(
        "FILES",
        help = "image files to process",
        0..Int.MAX_VALUE
    )

    fun execute() {
        if (versionMode) {
            println(KImage.VERSION)
            return
        }

        if (scriptDirectory != "") {
            KImageManager.addScriptDirectory(File(scriptDirectory))
        }

        if (uiMode) {
            Application.launch(KImageApplication::class.java)
            return
        }

        val commands = mutableListOf<String>()
        if (command == "") {
            if (helpMode) {
                commands.addAll(KImageManager.scriptNames)
            } else {
                println("Scripts:")
                KImageManager.scriptNames.forEach {
                    println("  $it")
                }
                return
            }
        } else {
            commands.add(command)
        }

        for (command in commands) {
            val (commandName, scriptCode, extension) = when {
                KImageManager.scriptNames.contains(command) -> Triple(
                    command,
                    KImageManager.scriptFile(command).readText(),
                    KImageManager.scriptFile(command).extension
                )
                File(command).exists() -> Triple(
                    File(command).nameWithoutExtension,
                    File(command).readText(),
                    File(command).extension
                )
                else -> Triple("execute", command, "kts")
            }

            val parametersMap: Map<String, String> = mapOf(*parameters.toTypedArray())

            val script = KImageManager.script(command)

            KImageManager.executeScript(
                script,
                parametersMap,
                fileNames.map { File(it) },
                helpMode,
                verboseMode,
                debugMode,
                if (outputPrefix == "") commandName else outputPrefix,
                outputDirectory
            )
        }
    }
}

object KImageManager {
    private val IMPORT_STATEMENTS = """
        import ch.obermuhlner.kimage.*
        import ch.obermuhlner.kimage.align.*
        import ch.obermuhlner.kimage.filter.*
        import ch.obermuhlner.kimage.image.*
        import ch.obermuhlner.kimage.io.*
        import ch.obermuhlner.kimage.math.*
        import java.io.*
        import java.util.Optional
        import kotlin.math.*
        
    """.trimIndent()

    private val IMPORT_REGEX = Regex("^import")

    private val mutableScriptFiles = mutableMapOf<String, File>()
    private val scriptCache = mutableMapOf<String, Script>()

    val scriptNames get() = mutableScriptFiles.keys.sorted()

    init {
        fillScriptFiles(mutableScriptFiles, File(KImage.javaClass.protectionDomain.codeSource.location.toURI()))

        System.getProperty("user.home")?.let {
            fillScriptFiles(mutableScriptFiles, File(it))
        }
        System.getProperty("user.dir")?.let {
            fillScriptFiles(mutableScriptFiles, File(it))
        }
    }

    fun addScriptDirectory(directory: File) {
        fillScriptFiles(mutableScriptFiles, directory)
    }

    fun scriptFile(scriptName: String) = mutableScriptFiles[scriptName]!!

    private fun fillScriptFiles(scriptFilesMap: MutableMap<String, File>, path: File) {
        var current: File? = path
        while (current != null && current.exists()) {
            if (current.isDirectory) {
                val currentScriptDir = File(current, ".kimage")
                if (currentScriptDir.exists() && currentScriptDir.isDirectory) {
                    currentScriptDir.listFiles { file ->
                        file.isFile && (file.extension == "kts" || file.extension == "kimage" || file.name.endsWith(".kimage.kts"))
                    }.forEach {
                        addScriptFile(scriptFilesMap, it)
                    }
                }
                current.listFiles { file ->
                    file.isFile && (file.extension == "kts" || file.extension == "kimage" || file.name.endsWith(".kimage.kts"))
                }.forEach {
                    addScriptFile(scriptFilesMap, it)
                }
            }
            current = current.parentFile
        }
    }

    private fun addScriptFile(scriptFilesMap: MutableMap<String, File>, file: File) {
        var name = file.name
        if (name.endsWith(".kimage.kts")) {
            name = name.removeSuffix(".kimage.kts")
        } else if (name.endsWith(".kimage")) {
            name = name.removeSuffix(".kimage")
        } else if (name.endsWith(".kts")) {
            name = name.removeSuffix(".kts")
        }

        if (!scriptFilesMap.containsKey(name)) {
            scriptFilesMap[name] = file
        }
    }

    fun script(command: String): Script {
        val cachedScript = scriptCache[command]
        cachedScript?.let {
            return it
        }

        val file = scriptFile(command)
        val code = file.readText()
        val extension = file.extension

        val manager = ScriptEngineManager()
        val engine = manager.getEngineByExtension(extension)
            ?: throw IllegalArgumentException("Script language not supported: $extension")

        val script = createScript(command, engine, addImportsToScript(code, extension))
        scriptCache[command] = script
        return script
    }

    fun executeScript(
        script: Script,
        arguments: Map<String, Any>,
        inputFiles: List<File>,
        helpMode: Boolean,
        verboseMode: Boolean,
        debugMode: Boolean,
        outputPrefix: String,
        outputDirectory: String,
        progress: Progress = object : Progress {
            override fun addTotal(totalStepCount: Int, message: String) {}
            override fun step(stepCount: Int, message: String) {}
        },
        outputHandler: (inputFile: File, output: Any?) -> Unit = { inputFile, output -> defaultOutputHandler(outputFile(inputFile, outputPrefix, outputDirectory), output) }
    ) {
        if (helpMode) {
            when (script) {
                is ScriptV0_1 -> {
                    script.printDocumentation()
                }
            }
        } else {
            when (script) {
                is ScriptV0_1 -> {
                    script.execute(inputFiles, arguments, verboseMode, debugMode, progress, File(outputDirectory), outputHandler)
                }
            }
        }
    }

    fun defaultOutputHandler(outputFile: File, output: Any?) {
        when(output) {
            is Image -> {
                println("Output file: $outputFile")
                ImageWriter.write(output, outputFile)
            }
            Unit -> {}
            null -> {}
            else -> {
                println("Output: $output")
                println()
            }
        }
    }

    fun outputFile(imageFile: File, prefix: String, directoryName: String): File {
        val directoryFile = when {
            directoryName != "" -> File(directoryName)
            imageFile.parent != null -> File(imageFile.parent)
            else -> File(".")
        }

        var file = File(directoryFile, "${prefix}_" + imageFile.name)
        var index = 1
        while (file.exists()) {
            file = File(directoryFile, "${prefix}_${index}_" + imageFile.name)
            index++
        }
        return file
    }

    private fun createScript(command: String, engine: ScriptEngine, code: String): Script {
        val script = executeScriptLowLevel(engine, code)

        script.code = code

        if (script.name == "") {
            script.name = command
        }
        if (script.name != command) {
            println("Warning: Script file name '$command' does not match name declared in script '${script.name}'")
        }

        return script
    }

    private fun addImportsToScript(originalScript: String, extension: String): String {
        return if (extension != "kts" || originalScript.contains(IMPORT_REGEX)) {
            originalScript
        } else {
            IMPORT_STATEMENTS + "\n" + originalScript
        }
    }

    private fun executeScriptLowLevel(engine: ScriptEngine, code: String): Script {
        if (!code.contains(Regex("kimage\\([0-9]+.[0-9]+\\)"))) {
            return createDummyScript(engine, code)
        }

        val startMillis = System.currentTimeMillis()
        val result = engine.eval(code)
        val endMillis = System.currentTimeMillis()
        val deltaMillis = endMillis - startMillis

        return when (result) {
            is Script -> result
            else -> throw IllegalArgumentException("Script code does not return a valid kimage script: $result")
        }
    }

    private fun createDummyScript(engine: ScriptEngine, code: String): Script {
        return kimage(0.1) {
            title = "Executes code passed directly to kimage"
            description = """
                This is a custom command executing the code directly.
            """
            single {
                setVariable(engine, "kimageVersion", KImage.VERSION)
                setVariable(engine, "verboseMode", verboseMode)
//                setVariable(engine, "outputDirectory", outputDirectory)
//                setVariable(engine, "outputPrefix", outputPrefix)

                setVariable(engine, "singleMode", true)
                setVariable(engine, "multiMode", false)
                setVariable(engine, "inputFiles", inputFiles)

                setVariable(engine, "inputFile", inputFile)
                setVariable(engine, "inputImage", inputImage)

                engine.put("inputParameters", rawArguments)

                engine.eval(code)
            }
        }
    }

    private fun setVariable(
        engine: ScriptEngine,
        name: String,
        value: Any?
    ) {
        engine.put(name, value)
//        if (verboseMode) {
//            println("  $name = $value")
//        }
    }
}

data class Point(val x: Double, val y: Double) {

    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())

    override fun toString(): String {
        return "$x, $y"
    }
}

data class Rectangle(val x: Double, val y: Double, val width: Double, val height: Double) {

    constructor(x: Int, y: Int, width: Int, height: Int) : this(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
}

