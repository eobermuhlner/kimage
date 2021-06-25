package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.image.Channel.*
import ch.obermuhlner.kimage.io.ImageReader
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
            //example()
            execute()
        }
    }
}

class KimageCli(parser: ArgParser) {

    private val versionMode by parser.flagging(
        "--version",
        help = "print version")

    private val verboseMode by parser.flagging(
        "-v", "--verbose",
        help = "enable verbose mode")

    private val parameters by parser.adding(
        "-p", "--param",
        help = "add parameter key=value") {
        val split = split("=")
        Pair(split[0], split[1])
    }

    private val singleMode by parser.flagging(
        "--single",
        help = "enable single mode").default(false)

    private val outputPrefix: String by parser.storing(
        "-o", "--output-prefix",
        help = "output prefix").default("output")

    private val outputDirectory: String by parser.storing(
        "-d", "--dir",
        help = "output directory").default("")

    private val command by parser.positional(
        "COMMAND",
        help = "command to execute").default("")

    private val filenames by parser.positionalList(
        "FILES",
        help = "image files to process", 0..Int.MAX_VALUE)

    fun execute() {
        if (versionMode) {
            println(Companion.VERSION)
            return
        }

        val scriptFileMap: MutableMap<String, File> = mutableMapOf()
        fillScriptFiles(scriptFileMap, File(KImage.javaClass.protectionDomain.codeSource.location.toURI()).absolutePath)
        fillScriptFiles(scriptFileMap, System.getProperty("user.home"))
        fillScriptFiles(scriptFileMap, System.getProperty("user.dir"))

        if (command == "") {
            println("Scripts:")
            scriptFileMap.keys.sorted().forEach {
                println("  $it")
            }
            return
        }

        val (originalScript, extension) = when {
            scriptFileMap.containsKey(command) -> Pair(scriptFileMap[command]!!.readText(), scriptFileMap[command]!!.extension)
            File(command).exists() -> Pair(File(command).readText(), File(command).extension)
            else -> Pair(command, "kts")
        }

        val parametersMap: Map<String, String> = mapOf(*parameters.toTypedArray())

        try {
            val script = addImportsToScript(originalScript, extension)

            val determinedSingleMode = script.contains("require(singleMode)") || originalScript == command

            val manager = ScriptEngineManager()
            val engine = manager.getEngineByExtension(extension)

            if (engine == null) {
                println("Script language not supported: $extension")
                return
            }

            val inputFiles = filenames.map { File(it) }

            if (filenames.isEmpty()) {
                initCommonParameters(engine, false, inputFiles, parametersMap)

                executeScript(engine, script, outputFile(File("kimage.png"), outputPrefix, outputDirectory))
            } else {
                if (singleMode || determinedSingleMode) {
                    for (filename in filenames) {
                        val inputFile = File(filename)
                        if (inputFile.exists()) {
                            println("Processing single file: $inputFile")

                            val inputImage = ImageReader.readMatrixImage(inputFile)
                            initCommonParameters(engine, true, inputFiles, parametersMap)
                            initSingleFileParameters(engine, inputFile, inputImage)

                            executeScript(engine, script, outputFile(inputFile, outputPrefix, outputDirectory))
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

                    executeScript(engine, script, outputFile(inputFiles[0], outputPrefix, outputDirectory))
                }
            }
        } catch (ex: Exception) {
            if (verboseMode) {
                ex.printStackTrace()
            } else {
                println("Failed to execute:")
                println(ex.message)
            }
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
        setVariable(engine, "kimageVersion", VERSION)
        setVariable(engine, "verboseMode", verboseMode)
        setVariable(engine, "outputDirectory", outputDirectory)
        setVariable(engine, "outputPrefix", outputPrefix)

        setVariable(engine, "singleMode", singleMode)
        setVariable(engine, "multiMode", !singleMode)
        setVariable(engine, "inputFiles", inputFiles)

        engine.put("inputParameters", parametersMap)
        if (verboseMode) {
            parameters.forEach {
                println("  inputParameters[${it.first}] = ${it.second}")
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
        inputImage: MatrixImage
    ) {
        setVariable(engine, "inputFile", inputFile)
        setVariable(engine, "inputImage", inputImage)
    }

    private fun executeScript(engine: ScriptEngine, script: String, outputFile: File) {
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

        when(output) {
            is Image -> {
                println("Output file: $outputFile")
                ImageWriter.write(output, outputFile)
            }
            is Script -> {
                output.help()
                //output.runSingle(listOf())
            }
            else -> {
                if (output != null) {
                    println("Output: $output")
                }
            }
        }
    }

    private fun outputFile(imageFile: File, prefix: String, directoryName: String): File {
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

    private fun example() {
        //val originalImage = ImageReader.readMatrixImage(File("images/animal.png"))
        //val originalImage = ImageReader.readMatrixImage(File("images/orion_small_compress0.png"))
        val originalImage = ImageReader.readMatrixImage(File("images/orion_32bit.tif"))

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

            filter ("Median") {
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
                    Blue to croppedOriginalImage[Blue] - this[Blue] * removalFactorProperty.get())
            }
        }

        interactive {
            ImageWriter.write(currentImage!!, File("images/background_removed.png"))
        }
    }

    companion object {
        private const val VERSION: String = "0.1.0"

        private val IMPORT_STATEMENTS = """
            import ch.obermuhlner.kimage.*
            import ch.obermuhlner.kimage.align.*
            import ch.obermuhlner.kimage.filter.*
            import ch.obermuhlner.kimage.image.*
            import ch.obermuhlner.kimage.io.*
            import java.io.*
            import kotlin.math.*
        """.trimIndent()

        private val IMPORT_REGEX = Regex("^import")
    }
}
