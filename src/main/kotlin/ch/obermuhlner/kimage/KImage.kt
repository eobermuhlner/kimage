package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.image.Channel.*
import ch.obermuhlner.kimage.io.ImageReader.read
import ch.obermuhlner.kimage.io.ImageWriter
import ch.obermuhlner.kimage.javafx.KImageApplication
import ch.obermuhlner.kimage.javafx.KImageApplication.Companion.interactive
import ch.obermuhlner.kotlin.javafx.*
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
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

        val scriptFileMap: MutableMap<String, File> = mutableMapOf()
        if (scriptDirectory != "") {
            fillScriptFiles(scriptFileMap, scriptDirectory)
        }
        fillScriptFiles(scriptFileMap, File(KImage.javaClass.protectionDomain.codeSource.location.toURI()).absolutePath)
        fillScriptFiles(scriptFileMap, System.getProperty("user.home"))
        fillScriptFiles(scriptFileMap, System.getProperty("user.dir"))

        val commands = mutableListOf<String>()
        if (command == "") {
            if (helpMode) {
                commands.addAll(scriptFileMap.keys.sorted())
            } else {
                println("Scripts:")
                scriptFileMap.keys.sorted().forEach {
                    println("  $it")
                }
                return
            }
        } else {
            commands.add(command)
        }

        for (command in commands) {
            val (commandName, script, extension) = when {
                scriptFileMap.containsKey(command) -> Triple(
                    command,
                    scriptFileMap[command]!!.readText(),
                    scriptFileMap[command]!!.extension
                )
                File(command).exists() -> Triple(
                    File(command).nameWithoutExtension,
                    File(command).readText(),
                    File(command).extension
                )
                else -> Triple("output", command, "kts")
            }

            val parametersMap: Map<String, String> = mapOf(*parameters.toTypedArray())

            KImageExecution(
                commandName,
                command == script,
                script,
                extension,
                parametersMap,
                fileNames,
                if (outputPrefix == "") commandName else outputPrefix,
                outputDirectory,
                helpMode,
                verboseMode,
                debugMode
            ).execute()
        }
    }

    private fun fillScriptFiles(scriptFilesMap: MutableMap<String, File>, path: String?) {
        if (path == null) {
            return
        }

        var current: File? = File(path)
        while (current != null && current.exists()) {
            if (current.isDirectory) {
                val currentScriptDir = File(current, ".kimage")
                if (currentScriptDir.exists() && currentScriptDir.isDirectory) {
                    currentScriptDir.listFiles { file ->
                        file.isFile && (file.extension == "kts" || file.extension == "kimage")
                    }.forEach {
                        addScriptFile(scriptFilesMap, it)
                    }
                }
                current.listFiles { file ->
                    file.isFile && (file.extension == "kimage" || file.name.endsWith(".kimage.kts"))
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
}

class KImageExecution(
    private val commandName: String,
    private val lowLevelExecution: Boolean,
    private val originalScript: String,
    private val extension: String,
    private val parametersMap: Map<String, String>,
    private val filenames: List<String>,
    private val outputPrefix: String,
    private val outputDirectory: String,
    private val helpMode: Boolean,
    private val verboseMode: Boolean,
    private val debugMode: Boolean
) {
    fun execute() {
        try {
            val script = addImportsToScript(originalScript, extension)

            val determinedSingleMode = script.contains("require(singleMode)") || lowLevelExecution

            val manager = ScriptEngineManager()
            val engine = manager.getEngineByExtension(extension)

            if (engine == null) {
                println("Script language not supported: $extension")
                return
            }

            val inputFiles = filenames.map { File(it) }

            executeScript(engine, script, inputFiles, determinedSingleMode)
        } catch (ex: Exception) {
            println("Failed to execute $commandName:")
            ex.printStackTrace()
        }
    }

    private fun executeScript(
        engine: ScriptEngine,
        script: String,
        inputFiles: List<File>,
        determinedSingleMode: Boolean
    ) {
        val scriptInfo = executeScriptLowLevel(engine, script, inputFiles, parametersMap, determinedSingleMode)
        if (scriptInfo != null) {
            if (scriptInfo.name == "") {
                scriptInfo.name = commandName
            }
            if (scriptInfo.name != commandName) {
                println("Warning: Script file name '$commandName' does not match name declared in script '${scriptInfo.name}'")
            }
            ScriptExecutor.executeScript(scriptInfo, parametersMap, inputFiles, helpMode, verboseMode, debugMode, outputPrefix, outputDirectory)
        }
    }

    private fun executeScriptLowLevel(
        engine: ScriptEngine,
        script: String,
        inputFiles: List<File>,
        parametersMap: Map<String, String>,
        singleMode: Boolean
    ): Script? {
        if (filenames.isEmpty()) {
            initCommonParameters(engine, false, inputFiles, parametersMap)

            val scriptInfo = executeScriptLowLevel(engine, script, ScriptExecutor.outputFile(File("noinput.png"), outputPrefix, outputDirectory))
            if (scriptInfo != null) {
                return scriptInfo
            }
        } else {
            if (singleMode) {
                for (filename in filenames) {
                    val inputFile = File(filename)
                    if (inputFile.exists()) {
                        if (verboseMode) {
                            println("Processing single file: $inputFile")
                        }

                        val inputImage = read(inputFile)
                        initCommonParameters(engine, true, inputFiles, parametersMap)
                        initSingleFileParameters(engine, inputFile, inputImage)

                        val scriptInfo = executeScriptLowLevel(
                            engine,
                            script,
                            ScriptExecutor.outputFile(inputFile, outputPrefix, outputDirectory)
                        )
                        if (scriptInfo != null) {
                            return scriptInfo
                        }
                    } else {
                        println("File not found: $inputFile")
                    }
                    println()
                }
            } else {
                if (verboseMode) {
                    println("Processing files: $filenames")
                }

                initCommonParameters(engine, false, inputFiles, parametersMap)

                val scriptInfo = executeScriptLowLevel(engine, script, ScriptExecutor.outputFile(inputFiles[0], outputPrefix, outputDirectory))
                if (scriptInfo != null) {
                    return scriptInfo
                }
            }
        }

        return null
    }

    private fun addImportsToScript(originalScript: String, extension: String): String {
        return if (extension != "kts" || originalScript.contains(IMPORT_REGEX)) {
            originalScript
        } else {
            IMPORT_STATEMENTS + originalScript
        }
    }

    private fun initCommonParameters(
        engine: ScriptEngine,
        singleMode: Boolean,
        inputFiles: List<File>,
        parametersMap: Map<String, String>
    ) {
        setVariable(engine, "kimageVersion", KImage.VERSION)
        setVariable(engine, "verboseMode", verboseMode)
        setVariable(engine, "outputDirectory", outputDirectory)
        setVariable(engine, "outputPrefix", outputPrefix)

        setVariable(engine, "singleMode", singleMode)
        setVariable(engine, "multiMode", !singleMode)
        setVariable(engine, "inputFiles", inputFiles)

        engine.put("inputParameters", parametersMap)
        if (verboseMode) {
            parametersMap.forEach {
                println("  inputParameters[${it.key}] = ${it.value}")
            }
        }
    }

    private fun setVariable(
        engine: ScriptEngine,
        name: String,
        value: Any?
    ) {
        engine.put(name, value)
        if (verboseMode) {
            println("  $name = $value")
        }
    }

    private fun initSingleFileParameters(
        engine: ScriptEngine,
        inputFile: File,
        inputImage: Image
    ) {
        setVariable(engine, "inputFile", inputFile)
        setVariable(engine, "inputImage", inputImage)
    }

    private fun executeScriptLowLevel(engine: ScriptEngine, script: String, outputFile: File): Script? {
        val startMillis = System.currentTimeMillis()
        val result = engine.eval(script)
        val endMillis = System.currentTimeMillis()
        val deltaMillis = endMillis - startMillis

        if (verboseMode) {
            println("Processed in $deltaMillis ms")
        }

        var output = engine.get("output")
        if (output == null) {
            output = result
        }

        return when (output) {
            is Script -> output
            else -> {
                outputHandler(outputFile, output)
                null
            }
        }
    }

    private fun outputHandler(outputFile: File, output: Any?): Unit {
        when(output) {
            is Image -> {
                println("Output file: $outputFile")
                ImageWriter.write(output, outputFile)
                println()
            }
            Unit -> {}
            null -> {}
            else -> {
                println("Output: $output")
                println()
            }
        }
    }

    private fun example() {
        //val originalImage = ImageReader.read(File("images/animal.png"))
        //val originalImage = ImageReader.read(File("images/orion_small_compress0.png"))
        val originalImage = read(File("images/orion_32bit.tif"))

//        val originalImage = interactive {
//            openImageFile(initialDirectory = File("images"))
//        }

        interactive {
            setCurrentImage(originalImage, "Original")

            val radiusProperty = SimpleIntegerProperty(3)
            val recursiveProperty = SimpleBooleanProperty(true)
            form {
                children += vbox {
                    children += label("Median Radius:")
                    children += textfield(radiusProperty) {}
                    children += checkbox(recursiveProperty) {}
                }
            }

            filter("Median") {
                medianPixelFilter(radiusProperty.get())
            }
        }

        interactive {
            val radiusProperty = SimpleIntegerProperty(3)
            form {
                children += vbox {
                    children += label("Blur Radius:")
                    children += textfield(radiusProperty) {}
                }
            }
            filter("Blur") {
                gaussianBlurFilter(radiusProperty.get())
            }
        }

        interactive {
            val removalFactorProperty = SimpleDoubleProperty(1.0)
            form {
                children += vbox {
                    children += label("Removal Factor:")
                    children += textfield(removalFactorProperty, KImageApplication.PERCENT_FORMAT) {}
                }
            }
            filterArea("Subtract") { x, y, w, h ->
                val croppedOriginalImage = originalImage.crop(x, y, w, h)
                MatrixImage(
                    this.width,
                    this.height,
                    Red to croppedOriginalImage[Red] - this[Red] * removalFactorProperty.get(),
                    Green to croppedOriginalImage[Green] - this[Green] * removalFactorProperty.get(),
                    Blue to croppedOriginalImage[Blue] - this[Blue] * removalFactorProperty.get()
                )
            }
        }

        interactive {
            ImageWriter.write(currentImage!!, File("images/background_removed.png"))
        }
    }

    companion object {
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
    }
}

data class Point(val x: Double, val y: Double)

data class Rectangle(val x: Double, val y: Double, val width: Double, val height: Double)
