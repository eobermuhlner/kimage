package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.MatrixImage
import ch.obermuhlner.kimage.io.ImageReader
import ch.obermuhlner.kimage.io.ImageWriter
import java.io.File

@DslMarker
annotation class KotlinDSL

@KotlinDSL
sealed class Script(val version: Double) {
    var name: String = ""
}

object ScriptExecutor {
    fun executeScript(
        script: Script,
        arguments: Map<String, String>,
        inputFiles: List<File>,
        helpMode: Boolean,
        verboseMode: Boolean,
        debugMode: Boolean,
        outputPrefix: String,
        outputDirectory: String
    ) {
        if (helpMode) {
            when (script) {
                is ScriptV0_1 -> {
                    script.help()
                }
            }
        } else {
            when (script) {
                is ScriptV0_1 -> {
                    script.execute(inputFiles, arguments, verboseMode, debugMode) { inputFile, output ->
                        outputHandler(outputFile(inputFile, outputPrefix, outputDirectory), output)
                    }
                }
            }
        }
    }

    // TODO get rid of duplicate impl
    private fun outputHandler(outputFile: File, output: Any?): Unit {
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
}

@KotlinDSL
class ScriptV0_1 : Script(0.1) {
    var description = ""

    private var scriptArguments: ScriptArguments = ScriptArguments()

    private var scriptSingle: ScriptSingle? = null
    private var scriptMulti: ScriptMulti? = null

    fun arguments(initializer: ScriptArguments.() -> Unit) {
        scriptArguments = ScriptArguments().apply(initializer)
    }

    fun single(executable: ScriptSingle.() -> Any?) {
        scriptSingle = ScriptSingle(executable)
    }

    fun multi(executable: ScriptMulti.() -> Any?) {
        scriptMulti = ScriptMulti(executable)
    }

    fun help() {
        println("NAME: $name")

        print("USAGE: kimage [OPTIONS] $name")
        for (arg in scriptArguments.arguments) {
            print(" ")
            if (!arg.mandatory) {
                print("[")
            }
            print("--param ${arg.name}=${arg.type.toUpperCase()}")
            if (!arg.mandatory) {
                print("]")
            }
        }
        println()

        if (description.isNotBlank()) {
            println("DESCR:")
            println(description.trimLeadingIndent().prependIndent("  "))
        }
        println()
        for (arg in scriptArguments.arguments) {
            println("ARG: ${arg.name}")
            if (arg.description.isNotBlank()) {
                println("  DESCRIPTION:")
                println(arg.description.trimLeadingIndent().prependIndent("    "))
            }
            println("  TYPE: ${arg.type}")
            when (arg) {
                is ScriptIntArg -> {
                    if (arg.min != null) {
                        println("  MIN: ${arg.min}")
                    }
                    if (arg.max != null) {
                        println("  MAX: ${arg.max}")
                    }
                    if (arg.default != null) {
                        println("  DEFAULT: ${arg.default}")
                    }
                }
                is ScriptDoubleArg -> {
                    if (arg.min != null) {
                        println("  MIN: ${arg.min}")
                    }
                    if (arg.max != null) {
                        println("  MAX: ${arg.max}")
                    }
                    if (arg.default != null) {
                        println("  DEFAULT: ${arg.default}")
                    }
                }
                is ScriptStringArg -> {
                    if (arg.allowed.isNotEmpty()) {
                        println("  ALLOWED: ${arg.allowed}")
                    }
                    if (arg.default != null) {
                        println("  DEFAULT: ${arg.default}")
                    }
                }
            }
        }
        println()
    }

    fun execute(inputFiles: List<File>, arguments: Map<String, String>, verboseMode: Boolean, debugMode: Boolean, outputHandler: (File, Any?) -> Unit) {
        return if (scriptMulti != null) {
            executeMulti(inputFiles, arguments, verboseMode, debugMode, outputHandler)
        } else if (scriptSingle != null) {
            executeSingle(inputFiles, arguments, verboseMode, debugMode, outputHandler)
        } else {
            throw java.lang.RuntimeException("Script has no execution block.")
        }
    }

    fun executeSingle(inputFiles: List<File>, arguments: Map<String, String>, verboseMode: Boolean, debugMode: Boolean, outputHandler: (File, Any?) -> Unit) {
        scriptSingle?.let {
            for (inputFile in inputFiles) {
                val inputImage = ImageReader.readMatrixImage(inputFile)
                it.executeSingleScript(inputFile, inputImage, scriptArguments, arguments, verboseMode, debugMode, outputHandler)
            }
        }
    }

    fun executeMulti(inputFiles: List<File>, arguments: Map<String, String>, verboseMode: Boolean, debugMode: Boolean, outputHandler: (File, Any?) -> Unit) {
        scriptMulti?.let {
            it.executeMultiScript(inputFiles, scriptArguments, arguments, verboseMode, debugMode, outputHandler)
        }
    }
}

private fun String.trimLeadingIndent(): String =
    lines()
        .filter(String::isNotBlank)
        .map { it.trim() }
        .joinTo(StringBuilder(), "\n")
        .toString()


@KotlinDSL
class ScriptArguments {
    val arguments: MutableList<ScriptArg> = mutableListOf()

    fun int(initializer: ScriptIntArg.() -> Unit) =
        arguments.add(ScriptIntArg().apply(initializer))

    fun int(name: String, initializer: ScriptIntArg.() -> Unit) =
        arguments.add(ScriptIntArg().apply { this.name = name }.apply(initializer))

    fun double(initializer: ScriptDoubleArg.() -> Unit) =
        arguments.add(ScriptDoubleArg().apply(initializer))

    fun double(name: String, initializer: ScriptDoubleArg.() -> Unit) =
        arguments.add(ScriptDoubleArg().apply { this.name = name }.apply(initializer))

    fun boolean(initializer: ScriptBooleanArg.() -> Unit) =
        arguments.add(ScriptBooleanArg().apply(initializer))

    fun boolean(name:String, initializer: ScriptBooleanArg.() -> Unit) =
        arguments.add(ScriptBooleanArg().apply { this.name = name }.apply(initializer))

    fun string(initializer: ScriptStringArg.() -> Unit) =
        arguments.add(ScriptStringArg().apply(initializer))

    fun string(name: String, initializer: ScriptStringArg.() -> Unit) =
        arguments.add(ScriptStringArg().apply { this.name = name }.apply(initializer))
}

class ExecutionArguments(
    scriptArguments: ScriptArguments,
    argumentValues: Map<String, String>,
    ) {
    val int = mutableMapOf<String, Int>()
    val double = mutableMapOf<String, Double>()
    val boolean = mutableMapOf<String, Boolean>()
    val string = mutableMapOf<String, String>()

    init {
        for (argument in scriptArguments.arguments) {
            when (argument) {
                is ScriptIntArg -> {
                    val value = argument.toValue(argumentValues[argument.name])
                    if (value != null) {
                        int[argument.name] = value
                    }
                }
                is ScriptDoubleArg -> {
                    val value = argument.toValue(argumentValues[argument.name])
                    if (value != null) {
                        double[argument.name] = value
                    }
                }
                is ScriptBooleanArg -> {
                    val value = argument.toValue(argumentValues[argument.name])
                    if (value != null) {
                        boolean[argument.name] = value
                    }
                }
                is ScriptStringArg -> {
                    val value = argument.toValue(argumentValues[argument.name])
                    if (value != null) {
                        string[argument.name] = value
                    }
                }
            }
        }
    }
}

@KotlinDSL
sealed class ScriptArg(val type: String) {
    var name: String = ""
    var description = ""
    var mandatory: Boolean = false
}

@KotlinDSL
class ScriptIntArg : ScriptArg("int") {
    var min: Int? = null
    var max: Int? = null
    var default: Int? = null

    fun toValue(stringValue: String?): Int? {
        if (stringValue == null) {
            if (default == null && mandatory) {
                throw ScriptArgumentException("Argument $name is mandatory")
            }
            return default
        }
        val value = stringValue.toInt()
        min?.let {
            if (value < it) {
                throw ScriptArgumentException("Argument $name must be >= $it but is $value")
            }
        }
        max?.let {
            if (value > it) {
                throw ScriptArgumentException("Argument $name must be <= $it but is $value")
            }
        }
        return value
    }
}

@KotlinDSL
class ScriptDoubleArg : ScriptArg("double") {
    var min: Double? = null
    var max: Double? = null
    var default: Double? = null

    fun toValue(stringValue: String?): Double? {
        if (stringValue == null) {
            if (default == null && mandatory) {
                throw ScriptArgumentException("Argument $name is mandatory")
            }
            return default
        }
        val value = stringValue.toDouble()
        min?.let {
            if (value < it) {
                throw ScriptArgumentException("Argument $name must be >= $it but is $value")
            }
        }
        max?.let {
            if (value > it) {
                throw ScriptArgumentException("Argument $name must be <= $it but is $value")
            }
        }
        return value
    }
}

@KotlinDSL
class ScriptBooleanArg : ScriptArg("boolean") {
    var default: Boolean? = null

    fun toValue(stringValue: String?): Boolean? {
        if (stringValue == null) {
            if (default == null && mandatory) {
                throw ScriptArgumentException("Argument $name is mandatory")
            }
            return default
        }
        return stringValue.toBoolean()
    }
}

@KotlinDSL
class ScriptStringArg : ScriptArg("string") {
    var allowed: List<String> = mutableListOf()
    var regex: String? = null
    var default: String? = null

    fun toValue(stringValue: String?): String? {
        if (stringValue == null) {
            if (default == null && mandatory) {
                throw ScriptArgumentException("Argument $name is mandatory")
            }
            return default
        }
        if (allowed.isNotEmpty()) {
            if (!allowed.contains(stringValue)) {
                throw ScriptArgumentException("Argument $name must be one of $allowed but is $stringValue")
            }
        }
        regex?.let {
            if (!stringValue.matches(Regex(it))) {
                throw ScriptArgumentException("Argument $name fails regular expression check: $stringValue")
            }
        }
        return stringValue
    }
}

class ScriptArgumentException(message: String): RuntimeException(message)

@KotlinDSL
class ScriptSingle(val executable: ScriptSingle.() -> Any?) {
    var inputFile: File = File(".")
    var inputImage: Image = MatrixImage(0, 0)
    val rawArguments: MutableMap<String, String> = mutableMapOf()
    var arguments: ExecutionArguments = ExecutionArguments(ScriptArguments(), mapOf())
    var verboseMode: Boolean = false
    var debugMode: Boolean = false

    fun executeSingleScript(inputFile: File, inputImage: Image, scriptArguments: ScriptArguments, arguments: Map<String, String>, verboseMode: Boolean, debugMode: Boolean, outputHandler: (File, Any?) -> Unit) {
        this.inputFile = inputFile
        this.inputImage = inputImage
        this.rawArguments.putAll(arguments)
        this.arguments = ExecutionArguments(scriptArguments, arguments)
        this.verboseMode = verboseMode
        this.debugMode = debugMode

        val output = executable()
        outputHandler(inputFile, output)
    }
}

@KotlinDSL
class ScriptMulti(val executable: ScriptMulti.() -> Any?) {
    var inputFiles: List<File> = listOf()
    val rawArguments: MutableMap<String, String> = mutableMapOf()
    var arguments: ExecutionArguments = ExecutionArguments(ScriptArguments(), mapOf())
    var verboseMode: Boolean = false
    var debugMode: Boolean = false

    fun executeMultiScript(inputFiles: List<File>, scriptArguments: ScriptArguments, arguments: Map<String, String>, verboseMode: Boolean, debugMode: Boolean, outputHandler: (File, Any?) -> Unit) {
        this.inputFiles = inputFiles
        this.rawArguments.putAll(arguments)
        this.arguments = ExecutionArguments(scriptArguments, arguments)
        this.verboseMode = verboseMode
        this.debugMode = debugMode

        val output = executable()
        val referenceFile = if (inputFiles.isEmpty()) {
            File("noinput.png")
        } else {
            inputFiles[0]
        }
        outputHandler(referenceFile, output)
    }
}

fun kimage(version: Double, initializer: ScriptV0_1.() -> Unit): Script {
    val script = when (version) {
        0.1 -> ScriptV0_1().apply(initializer)
        else -> throw ScriptArgumentException("Version not supported: $version")
    }

    KImageSingleton.scripts[script.name] = script
    return script
}

object KImageSingleton {
    val scripts: MutableMap<String, Script> = mutableMapOf()
}
