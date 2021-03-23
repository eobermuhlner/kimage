package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.image.Channel.*
import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.MatrixImage
import ch.obermuhlner.kimage.io.ImageReader
import ch.obermuhlner.kimage.io.ImageWriter
import ch.obermuhlner.kimage.javafx.KImageApplication
import ch.obermuhlner.kimage.javafx.KImageApplication.Companion.interactive
import ch.obermuhlner.kotlin.javafx.*
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import java.io.File
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

object KImage {
    @JvmStatic
    fun main(args: Array<String>) {
        //example()
        execute(args)

        //execute(arrayOf("images/lena512color.tiff"))
        //execute(arrayOf("images/orion_32bit.tif"))
        //execute(arrayOf("images/animal.png"))
        //execute(arrayOf("images/eric_avatar_diving.png"))
    }

    fun execute(args: Array<String>) {
        val parser = ArgParser(args)

        val verbose by parser.flagging(
            "-v", "--verbose",
            help = "enable verbose mode")

        val parameters by parser.adding(
            "-p", "--param",
            help = "add parameter key=value") {
            val split = split("=")
            Pair(split[0], split[1])
        }

        val scriptFilename: String by parser.storing(
            "-f", "--file",
            help = "script file to execute").default("kimage.kts")

        val scriptString: String by parser.storing(
            "-s", "--script",
            help = "script to execute").default("")

        val multi by parser.flagging(
            "-m", "--multi",
            help = "enable multi input mode")

        val outputPrefix: String by parser.storing(
            "-o", "--output-prefix",
            help = "output prefix").default("output")

        val outputDirectory: String by parser.storing(
            "-d", "--dir",
            help = "output directory").default("")


        val filenames by parser.positionalList("FILES", "image filename", 0..Int.MAX_VALUE)

        val scriptFile = File(scriptFilename)
        val script = when {
            scriptString != "" -> scriptString
            scriptFile.exists() -> scriptFile.readText()
            else -> "println(\"Missing script\")"
        }

        val manager = ScriptEngineManager()
        val engine = manager.getEngineByName("kotlin")!!

        if (filenames.isEmpty()) {
            parameters.forEach {
                if (verbose) {
                    println("  ${it.first} = ${it.second}" )
                }
                engine.put(it.first, it.second)
            }

            executeScript(engine, script, outputFile(File("kimage.png"), outputPrefix, outputDirectory))
        } else {
            if (multi) {
                println("Processing $filenames")

                parameters.forEach {
                    if (verbose) {
                        println("  ${it.first} = ${it.second}" )
                    }
                    engine.put(it.first, it.second)
                }

                val inputFiles = filenames.map { File(it) }
                if (verbose) {
                    println("  inputFiles = $inputFiles" )
                }
                engine.put("inputFiles", inputFiles)

                executeScript(engine, script, outputFile(inputFiles[0], outputPrefix, outputDirectory))
            } else {
                for (filename in filenames) {
                    val inputFile = File(filename)
                    if (inputFile.exists()) {
                        println("Processing $inputFile")

                        val inputImage = ImageReader.readMatrixImage(inputFile)

                        parameters.forEach {
                            if (verbose) {
                                println("  ${it.first} = ${it.second}" )
                            }
                            engine.put(it.first, it.second)
                        }
                        if (verbose) {
                            println("  inputFile = $inputFile" )
                            println("  input = $inputImage" )
                        }
                        engine.put("inputFile", inputFile)
                        engine.put("input", inputImage)

                        executeScript(engine, script, outputFile(inputFile, outputPrefix, outputDirectory))
                    } else {
                        println("File not found: $inputFile")
                    }
                }
            }
        }
    }

    private fun executeScript(engine: ScriptEngine, script: String, outputFile: File) {
        val result = engine.eval(script)

        var output = engine.get("output")
        if (output == null) {
            output = result
        }

        when(output) {
            is Image -> {
                println("Output file: $outputFile")
                ImageWriter.write(output, outputFile)
            }
        }
    }

    private fun outputFile(imageFile: File, prefix: String, directoryName: String): File {
        val directoryFile = if (directoryName != "") File(directoryName) else File(imageFile.parent)
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
                medianPixel(radiusProperty.get())
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
                gaussianBlur(radiusProperty.get())
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
                val croppedOriginalImage = originalImage.croppedImage(x, y, w, h)
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
}
