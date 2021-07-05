package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.MatrixImage
import ch.obermuhlner.kimage.io.ImageReader
import ch.obermuhlner.kimage.io.ImageReader.read
import ch.obermuhlner.kimage.io.ImageWriter
import java.io.File
import java.util.*

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
        println("---")
        println()

        println("## Script: $name")
        println()

        println("    kimage [OPTIONS] $name")
        for (arg in scriptArguments.arguments) {
            print("        ")
            if (!arg.mandatory || arg.hasDefault) {
                print("[")
            }
            print("--arg ${arg.name}=${arg.type.toUpperCase()}")
            if (!arg.mandatory || arg.hasDefault) {
                print("]")
            }
            println()
        }
        println("        [FILES]")
        println()

        if (description.isNotBlank()) {
            println(description.trimIndent())
            println()
        }

        for (arg in scriptArguments.arguments) {
            println("### Argument: ${arg.name}")
            println()
            println("- Type: ${arg.type}")
            if (arg.mandatory && !arg.hasDefault) {
                println("- Mandatory: yes")
            }

            when (arg) {
                is ScriptIntArg -> {
                    if (arg.min != null) {
                        println("- Minimum value: ${arg.min}")
                    }
                    if (arg.max != null) {
                        println("- Maximum value: ${arg.max}")
                    }
                    if (arg.default != null) {
                        println("- Default value: ${arg.default}")
                    }
                }
                is ScriptDoubleArg -> {
                    if (arg.min != null) {
                        println("- Minimum value: ${arg.min}")
                    }
                    if (arg.max != null) {
                        println("- Maximum value: ${arg.max}")
                    }
                    if (arg.default != null) {
                        println("- Default value: ${arg.default}")
                    }
                }
                is ScriptStringArg -> {
                    if (arg.allowed.isNotEmpty()) {
                        println("- Allowed values:")
                        for (allowed in arg.allowed) {
                            println("  - `${allowed}`")
                        }
                    }
                    if (arg.regex != null) {
                        println("- Must match regular expression: `${arg.regex}`")
                    }
                    if (arg.default != null) {
                        println("- Default value: `${arg.default}`")
                    }
                }
                is ScriptFileArg -> {
                    if (arg.isDirectory != null) {
                        println("- Must be directory: ${arg.isDirectory}")
                    }
                    if (arg.isFile != null) {
                        println("- Must be file: ${arg.isFile}")
                    }
                    if (arg.exists != null) {
                        println("- Must exist: ${arg.exists}")
                    }
                    arg.allowedExtensions?.let {
                        println("- Allowed extensions:")
                        for (allowed in it) {
                            println("  - `${allowed}`")
                        }
                    }
                    if (arg.default != null) {
                        println("- Default path: `${arg.default}`")
                    }
                }
                is ScriptImageArg -> {
                    if (arg.default != null) {
                        println("- Default path: `${arg.default}`")
                    }
                }
            }
            println()

            if (arg.description.isNotBlank()) {
                println(arg.description.trimIndent())
                println()
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
                val inputImage = read(inputFile)
                it.executeSingleScript(
                    inputFile,
                    inputImage,
                    scriptArguments,
                    arguments,
                    verboseMode,
                    debugMode,
                    outputHandler
                )
            }
        }
    }

    fun executeMulti(inputFiles: List<File>, arguments: Map<String, String>, verboseMode: Boolean, debugMode: Boolean, outputHandler: (File, Any?) -> Unit) {
        scriptMulti?.let {
            it.executeMultiScript(inputFiles, scriptArguments, arguments, verboseMode, debugMode, outputHandler)
        }
    }
}

@KotlinDSL
class ScriptArguments {
    val arguments: MutableList<ScriptArg> = mutableListOf()

    fun int(initializer: ScriptIntArg.() -> Unit) =
        arguments.add(ScriptIntArg().apply(initializer))

    fun int(name: String, initializer: ScriptIntArg.() -> Unit) =
        arguments.add(ScriptIntArg().apply { this.name = name }.apply(initializer))

    fun optionalInt(initializer: ScriptOptionalIntArg.() -> Unit) =
        arguments.add(ScriptOptionalIntArg().apply(initializer))

    fun optionalInt(name: String, initializer: ScriptOptionalIntArg.() -> Unit) =
        arguments.add(ScriptOptionalIntArg().apply { this.name = name }.apply(initializer))

    fun double(initializer: ScriptDoubleArg.() -> Unit) =
        arguments.add(ScriptDoubleArg().apply(initializer))

    fun double(name: String, initializer: ScriptDoubleArg.() -> Unit) =
        arguments.add(ScriptDoubleArg().apply { this.name = name }.apply(initializer))

    fun optionalDouble(initializer: ScriptOptionalDoubleArg.() -> Unit) =
        arguments.add(ScriptOptionalDoubleArg().apply(initializer))

    fun optionalDouble(name: String, initializer: ScriptOptionalDoubleArg.() -> Unit) =
        arguments.add(ScriptOptionalDoubleArg().apply { this.name = name }.apply(initializer))

    fun boolean(initializer: ScriptBooleanArg.() -> Unit) =
        arguments.add(ScriptBooleanArg().apply(initializer))

    fun boolean(name:String, initializer: ScriptBooleanArg.() -> Unit) =
        arguments.add(ScriptBooleanArg().apply { this.name = name }.apply(initializer))

    fun string(initializer: ScriptStringArg.() -> Unit) =
        arguments.add(ScriptStringArg().apply(initializer))

    fun string(name: String, initializer: ScriptStringArg.() -> Unit) =
        arguments.add(ScriptStringArg().apply { this.name = name }.apply(initializer))

    fun file(initializer: ScriptFileArg.() -> Unit) =
        arguments.add(ScriptFileArg().apply(initializer))

    fun file(name: String, initializer: ScriptFileArg.() -> Unit) =
        arguments.add(ScriptFileArg().apply { this.name = name }.apply(initializer))

    fun image(initializer: ScriptImageArg.() -> Unit) =
        arguments.add(ScriptImageArg().apply(initializer))

    fun image(name: String, initializer: ScriptImageArg.() -> Unit) =
        arguments.add(ScriptImageArg().apply { this.name = name }.apply(initializer))

    fun optionalImage(initializer: ScriptImageArg.() -> Unit) =
        arguments.add(ScriptImageArg().apply(initializer))

    fun optionalImage(name: String, initializer: ScriptOptionalImageArg.() -> Unit) =
        arguments.add(ScriptOptionalImageArg().apply { this.name = name }.apply(initializer))
}

@KotlinDSL
sealed class ScriptArg(val type: String, val mandatory: Boolean) {
    var name: String = ""
    var description = ""

    abstract val hasDefault: Boolean
}

@KotlinDSL
open class ScriptIntArg(mandatory: Boolean = true) : ScriptArg("int", mandatory) {
    var min: Int? = null
    var max: Int? = null
    var default: Int? = null

    override val hasDefault: Boolean
        get() = default != null

    fun toIntValue(stringValue: String?): Int {
        if (stringValue == null) {
            default?.let {
                return it;
            }
            throw ScriptArgumentException("Argument $name is mandatory")
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
class ScriptOptionalIntArg : ScriptIntArg(false) {
    fun toOptionalIntValue(stringValue: String?): Optional<Int> {
        if (stringValue == null) {
            if (default == null) {
                return Optional.empty()
            }
            default?.let {
                return Optional.of(it)
            }
        }
        return Optional.of(toIntValue(stringValue))
    }
}

@KotlinDSL
open class ScriptDoubleArg(mandatory: Boolean = true) : ScriptArg("double", mandatory) {
    var min: Double? = null
    var max: Double? = null
    var default: Double? = null

    override val hasDefault: Boolean
        get() = default != null

    fun toDoubleValue(stringValue: String?): Double {
        if (stringValue == null) {
            default?.let {
                return it;
            }
            throw ScriptArgumentException("Argument $name is mandatory")
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
class ScriptOptionalDoubleArg : ScriptDoubleArg(false) {
    fun toOptionalDoubleValue(stringValue: String?): Optional<Double> {
        if (stringValue == null) {
            if (default == null) {
                return Optional.empty()
            }
            default?.let {
                return Optional.of(it)
            }
        }
        return Optional.of(toDoubleValue(stringValue))
    }
}

@KotlinDSL
open class ScriptBooleanArg(mandatory: Boolean = true) : ScriptArg("boolean", mandatory) {
    var default: Boolean? = null

    override val hasDefault: Boolean
        get() = default != null

    fun toBooleanValue(stringValue: String?): Boolean {
        if (stringValue == null) {
            default?.let {
                return it;
            }
            throw ScriptArgumentException("Argument $name is mandatory")
        }
        return stringValue.toBoolean()
    }
}

@KotlinDSL
class ScriptOptionalBooleanArg : ScriptBooleanArg(false) {
    fun toOptionalBooleanValue(stringValue: String?): Optional<Boolean> {
        if (stringValue == null) {
            if (default == null) {
                return Optional.empty()
            }
            default?.let {
                return Optional.of(it)
            }
        }
        return Optional.of(toBooleanValue(stringValue))
    }
}

@KotlinDSL
open class ScriptStringArg(mandatory: Boolean = true) : ScriptArg("string", mandatory) {
    var allowed: List<String> = mutableListOf()
    var regex: String? = null
    var default: String? = null

    override val hasDefault: Boolean
        get() = default != null

    fun toStringValue(stringValue: String?): String {
        if (stringValue == null) {
            default?.let {
                return it;
            }
            throw ScriptArgumentException("Argument $name is mandatory")
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

@KotlinDSL
class ScriptOptionalStringArg : ScriptStringArg(false) {
    fun toOptionalStringValue(stringValue: String?): Optional<String> {
        if (stringValue == null) {
            if (default == null) {
                return Optional.empty()
            }
            default?.let {
                return Optional.of(it)
            }
        }
        return Optional.of(toStringValue(stringValue))
    }
}

@KotlinDSL
open class ScriptFileArg(mandatory: Boolean = true) : ScriptArg("file", mandatory) {
    var default: File? = null
    var allowedExtensions: List<String>? = null
    var exists: Boolean? = null
    var isFile: Boolean? = null
    var isDirectory: Boolean? = null
    var canRead: Boolean? = null
    var canWrite: Boolean? = null

    override val hasDefault: Boolean
        get() = default != null

    fun toFileValue(stringValue: String?): File {
        if (stringValue == null) {
            default?.let {
                return it;
            }
            throw ScriptArgumentException("Argument $name is mandatory")
        }
        val file = File(stringValue)
        allowedExtensions?.let {
            if (file.extension in it) {
                throw ScriptArgumentException("Argument $name must have one of the extensions $allowedExtensions but has ${file.extension}: $file")
            }
        }
        exists?.let {
            if (!file.exists()) {
                throw ScriptArgumentException("Argument $name must exist: $file")
            }
        }
        isFile?.let {
            if (!file.isFile) {
                throw ScriptArgumentException("Argument $name must be a file: $file")
            }
        }
        isDirectory?.let {
            if (!file.isDirectory) {
                throw ScriptArgumentException("Argument $name must be a directory: $file")
            }
        }
        canRead?.let {
            if (!file.canRead()) {
                throw ScriptArgumentException("Argument $name must be readable: $file")
            }
        }
        canWrite?.let {
            if (!file.canWrite()) {
                throw ScriptArgumentException("Argument $name must be writeable: $file")
            }
        }
        return file
    }
}

@KotlinDSL
class ScriptOptionalFileArg : ScriptFileArg(false) {
    fun toOptionalFileValue(stringValue: String?): Optional<File> {
        if (stringValue == null) {
            if (default == null) {
                return Optional.empty()
            }
            default?.let {
                return Optional.of(it)
            }
        }
        return Optional.of(toFileValue(stringValue))
    }
}

@KotlinDSL
open class ScriptImageArg(mandatory: Boolean = true) : ScriptArg("image", mandatory) {
    var default: File? = null

    override val hasDefault: Boolean
        get() = default != null

    fun toImageValue(stringValue: String?): Image {
        if (stringValue == null) {
            default?.let {
                return ImageReader.read(it)
            }
            throw ScriptArgumentException("Argument $name is mandatory")
        }
        return ImageReader.read(File(stringValue))
    }
}

@KotlinDSL
class ScriptOptionalImageArg() : ScriptImageArg(false) {
    fun toOptionalImageValue(stringValue: String?): Optional<Image> {
        if (stringValue == null) {
            if (default == null) {
                return Optional.empty()
            }
            default?.let {
                return Optional.of(ImageReader.read(it))
            }
        }
        return Optional.of(ImageReader.read(File(stringValue)))
    }
}

class ScriptArgumentException(message: String): RuntimeException(message)

@KotlinDSL
class ScriptSingle(val executable: ScriptSingle.() -> Any?) {
    var inputFile: File = File(".")
    var inputImage: Image = MatrixImage(0, 0)
    val rawArguments: MutableMap<String, String> = mutableMapOf()
    var arguments: MutableMap<String, Any> = mutableMapOf()
    var verboseMode: Boolean = false
    var debugMode: Boolean = false

    fun executeSingleScript(inputFile: File, inputImage: Image, scriptArguments: ScriptArguments, arguments: Map<String, String>, verboseMode: Boolean, debugMode: Boolean, outputHandler: (File, Any?) -> Unit) {
        this.inputFile = inputFile
        this.inputImage = inputImage
        this.rawArguments.putAll(arguments)
        this.arguments = processArguments(scriptArguments, arguments)
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
    var arguments: MutableMap<String, Any> = mutableMapOf()
    var verboseMode: Boolean = false
    var debugMode: Boolean = false

    fun executeMultiScript(inputFiles: List<File>, scriptArguments: ScriptArguments, arguments: Map<String, String>, verboseMode: Boolean, debugMode: Boolean, outputHandler: (File, Any?) -> Unit) {
        this.inputFiles = inputFiles
        this.rawArguments.putAll(arguments)
        this.arguments = processArguments(scriptArguments, arguments)
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

private fun processArguments(scriptArguments: ScriptArguments, rawArguments: Map<String, String>): MutableMap<String, Any> {
    val processed = mutableMapOf<String, Any>()

    for (argument in scriptArguments.arguments) {
        when (argument) {
            is ScriptOptionalIntArg -> {
                processed[argument.name] = argument.toOptionalIntValue(rawArguments[argument.name])
            }
            is ScriptIntArg -> {
                processed[argument.name] = argument.toIntValue(rawArguments[argument.name])
            }
            is ScriptOptionalDoubleArg -> {
                processed[argument.name] = argument.toOptionalDoubleValue(rawArguments[argument.name])
            }
            is ScriptDoubleArg -> {
                processed[argument.name] = argument.toDoubleValue(rawArguments[argument.name])
            }
            is ScriptOptionalBooleanArg -> {
                processed[argument.name] = argument.toOptionalBooleanValue(rawArguments[argument.name])
            }
            is ScriptBooleanArg -> {
                processed[argument.name] = argument.toBooleanValue(rawArguments[argument.name])
            }
            is ScriptOptionalStringArg -> {
                processed[argument.name] = argument.toOptionalStringValue(rawArguments[argument.name])
            }
            is ScriptStringArg -> {
                processed[argument.name] = argument.toStringValue(rawArguments[argument.name])
            }
            is ScriptOptionalFileArg -> {
                processed[argument.name] = argument.toOptionalFileValue(rawArguments[argument.name])
            }
            is ScriptFileArg -> {
                processed[argument.name] = argument.toFileValue(rawArguments[argument.name])
            }
            is ScriptOptionalImageArg -> {
                processed[argument.name] = argument.toOptionalImageValue(rawArguments[argument.name])
            }
            is ScriptImageArg -> {
                processed[argument.name] = argument.toImageValue(rawArguments[argument.name])
            }
        }
    }

    return processed
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
