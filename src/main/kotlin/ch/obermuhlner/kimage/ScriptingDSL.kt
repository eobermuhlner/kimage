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
    var title = ""
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
        println("## Script: `$name`")
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

        if (title.isNotBlank()) {
            println("### ${title.trimIndent()}")
            println()
        }

        if (description.isNotBlank()) {
            println(description.trimIndent())
            println()
        }

        for (arg in scriptArguments.arguments) {
            println("#### Argument: `${arg.name}`")
            println()

            printArgumentType(arg, 0)

            if (arg.description.isNotBlank()) {
                println(arg.description.trimIndent())
                println()
            }
        }

        println("---")
        println()
    }

    private fun printArgumentType(arg: ScriptArg, level: Int) {
        val indent = "  ".repeat(level)

        println("${indent}- Type: ${arg.type}")
        if (level == 0) {
            if (arg.mandatory && !arg.hasDefault) {
                println("${indent}- Mandatory: yes")
            }
        }

        when (arg) {
            is ScriptListArg -> {
                if (arg.min != null) {
                    println("${indent}- Minimum length: ${arg.min}")
                }
                if (arg.max != null) {
                    println("${indent}- Maximum length: ${arg.max}")
                }
                if (arg.default != null) {
                    println("${indent}- Default values: ${arg.default}")
                }
                for (listElement in arg.arguments) {
                    println("${indent}- List Elements:")
                    printArgumentType(listElement, level+1)
                }
            }
            is ScriptIntArg -> {
                if (arg.min != null) {
                    println("${indent}- Minimum value: ${arg.min}")
                }
                if (arg.max != null) {
                    println("${indent}- Maximum value: ${arg.max}")
                }
                if (arg.default != null) {
                    println("${indent}- Default value: ${arg.default}")
                }
            }
            is ScriptDoubleArg -> {
                if (arg.min != null) {
                    println("${indent}- Minimum value: ${arg.min}")
                }
                if (arg.max != null) {
                    println("${indent}- Maximum value: ${arg.max}")
                }
                if (arg.default != null) {
                    println("${indent}- Default value: ${arg.default}")
                }
            }
            is ScriptStringArg -> {
                if (arg.allowed.isNotEmpty()) {
                    println("${indent}- Allowed values:")
                    for (allowed in arg.allowed) {
                        println("${indent}  - `${allowed}`")
                    }
                }
                if (arg.regex != null) {
                    println("${indent}- Must match regular expression: `${arg.regex}`")
                }
                if (arg.default != null) {
                    println("${indent}- Default value: `${arg.default}`")
                }
            }
            is ScriptFileArg -> {
                if (arg.isDirectory != null) {
                    println("${indent}- Must be directory: ${arg.isDirectory}")
                }
                if (arg.isFile != null) {
                    println("${indent}- Must be file: ${arg.isFile}")
                }
                if (arg.exists != null) {
                    println("${indent}- Must exist: ${arg.exists}")
                }
                arg.allowedExtensions?.let {
                    println("${indent}- Allowed extensions:")
                    for (allowed in it) {
                        println("${indent}  - `${allowed}`")
                    }
                }
                if (arg.default != null) {
                    println("${indent}- Default path: `${arg.default}`")
                }
            }
            is ScriptImageArg -> {
                if (arg.default != null) {
                    println("${indent}- Default path: `${arg.default}`")
                }
            }
        }
        println()
    }

    fun execute(inputFiles: List<File>, arguments: Map<String, String>, verboseMode: Boolean, debugMode: Boolean, outputHandler: (File, Any?) -> Unit) {
        if (title.isNotBlank()) {
            print(title)
        } else {
            print(name)
        }
        println()

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
interface ScriptTypes {
    val arguments: MutableList<ScriptArg>

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

    fun optionalFile(initializer: ScriptFileArg.() -> Unit) =
        arguments.add(ScriptFileArg().apply(initializer))

    fun optionalFile(name: String, initializer: ScriptOptionalFileArg.() -> Unit) =
        arguments.add(ScriptOptionalFileArg().apply { this.name = name }.apply(initializer))

    fun optionalImage(initializer: ScriptImageArg.() -> Unit) =
        arguments.add(ScriptImageArg().apply(initializer))

    fun optionalImage(name: String, initializer: ScriptOptionalImageArg.() -> Unit) =
        arguments.add(ScriptOptionalImageArg().apply { this.name = name }.apply(initializer))

    fun list(initializer: ScriptListArg.() -> Unit) =
        arguments.add(ScriptListArg().apply(initializer))

    fun list(name: String, initializer: ScriptListArg.() -> Unit) =
        arguments.add(ScriptListArg().apply { this.name = name }.apply(initializer))

    fun optionalList(initializer: ScriptOptionalListArg.() -> Unit) =
        arguments.add(ScriptOptionalListArg().apply(initializer))

    fun optionalList(name: String, initializer: ScriptOptionalListArg.() -> Unit) =
        arguments.add(ScriptOptionalListArg().apply { this.name = name }.apply(initializer))

    fun record(initializer: ScriptRecordArg.() -> Unit) =
        arguments.add(ScriptRecordArg().apply(initializer))

    fun record(name: String, initializer: ScriptRecordArg.() -> Unit) =
        arguments.add(ScriptRecordArg().apply { this.name = name }.apply(initializer))

    fun optionalRecord(initializer: ScriptOptionalRecordArg.() -> Unit) =
        arguments.add(ScriptOptionalRecordArg().apply(initializer))

    fun optionalRecord(name: String, initializer: ScriptOptionalRecordArg.() -> Unit) =
        arguments.add(ScriptOptionalRecordArg().apply { this.name = name }.apply(initializer))
}

@KotlinDSL
class ScriptArguments(override val arguments: MutableList<ScriptArg> = mutableListOf()) : ScriptTypes {
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
        try {
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
        } catch (ex: NumberFormatException) {
            throw ScriptArgumentException("Argument $name must be an integer value, but is '$stringValue'")
        }
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
        try {
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
        } catch (ex: NumberFormatException) {
            throw ScriptArgumentException("Argument $name must be an integer value, but is '$stringValue'")
        }
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

@KotlinDSL
open class ScriptListArg(override val arguments: MutableList<ScriptArg> = mutableListOf(), mandatory: Boolean = true) : ScriptArg("list", mandatory), ScriptTypes {
    var min: Int? = null
    var max: Int? = null
    var default: List<Any>? = null

    override val hasDefault: Boolean
        get() = default != null

    fun toListValue(stringValue: String?): List<Any> {
        if (arguments.isEmpty()) {
            throw ScriptArgumentException("Argument $name is a list, but the element type is not defined")
        }
        if (arguments.size > 1) {
            throw ScriptArgumentException("Argument $name is a list, but only one element type is allowed")
        }
        if (stringValue == null) {
            default?.let {
                return it;
            }
            throw ScriptArgumentException("Argument $name is mandatory")
        }

        val values = mutableListOf<Any>()
        val scriptArg = arguments[0]
        val stringElements = stringValue.split(',')

        min?.let {
            if (stringElements.size < it) {
                throw ScriptArgumentException("Argument $name must be a list of minimum length $it: ${stringElements.size} elements found")
            }
        }
        max?.let {
            if (stringElements.size > it) {
                throw ScriptArgumentException("Argument $name must be a list of maximum length $it: ${stringElements.size} elements found")
            }
        }

        for (stringElement in stringElements) {
            values.add(processArgument(scriptArg, stringElement))
        }
        return values
    }
}

@KotlinDSL
class ScriptOptionalListArg(arguments: MutableList<ScriptArg> = mutableListOf()) : ScriptListArg(arguments, false) {
    fun toOptionalListValue(stringValue: String?): Optional<List<Any>> {
        if (stringValue == null) {
            if (default == null) {
                return Optional.empty()
            }
            default?.let {
                return Optional.of(it)
            }
        }
        return Optional.of(toListValue(stringValue))
    }
}

@KotlinDSL
open class ScriptRecordArg(override val arguments: MutableList<ScriptArg> = mutableListOf(), mandatory: Boolean = true) : ScriptArg("record", mandatory), ScriptTypes {
    var default: Map<String, Any>? = null

    override val hasDefault: Boolean
        get() = default != null

    fun toRecordValue(stringValue: String?): Map<String, Any> {
        if (stringValue == null) {
            default?.let {
                return it;
            }
            throw ScriptArgumentException("Argument $name is mandatory")
        }

        val valueMap = mutableMapOf<String, Any>()
        val stringElements = stringValue.split(',')

        if (stringElements.size != arguments.size) {
            throw ScriptArgumentException("Argument $name is a record of ${arguments.size} elements: ${stringElements.size} elements found")
        }

        for (i in stringElements.indices) {
            valueMap[arguments[i].name] = processArgument(arguments[i], stringElements[i])
        }
        return valueMap
    }
}

@KotlinDSL
class ScriptOptionalRecordArg(arguments: MutableList<ScriptArg> = mutableListOf()) : ScriptRecordArg(arguments, false) {
    fun toOptionalRecordValue(stringValue: String?): Optional<Map<String, Any>> {
        if (stringValue == null) {
            if (default == null) {
                return Optional.empty()
            }
            default?.let {
                return Optional.of(it)
            }
        }
        return Optional.of(toRecordValue(stringValue))
    }
}

class ScriptArgumentException(message: String): RuntimeException(message)

sealed class AbstractScript {
    var scriptArguments: ScriptArguments = ScriptArguments()
    val rawArguments: MutableMap<String, String> = mutableMapOf()
    var arguments: MutableMap<String, Any> = mutableMapOf()
    var verboseMode: Boolean = false
    var debugMode: Boolean = false

    fun printExecution() {
        println()
        if (arguments.isNotEmpty()) {
            println("Arguments:")
            for ((name, value) in arguments.entries) {
                println("  $name = $value")
            }
            println()
        }
    }
}

@KotlinDSL
class ScriptSingle(val executable: ScriptSingle.() -> Any?) : AbstractScript() {
    var inputFile: File = File(".")
    var inputImage: Image = MatrixImage(0, 0)

    fun executeSingleScript(inputFile: File, inputImage: Image, scriptArguments: ScriptArguments, arguments: Map<String, String>, verboseMode: Boolean, debugMode: Boolean, outputHandler: (File, Any?) -> Unit) {
        this.inputFile = inputFile
        this.inputImage = inputImage
        this.scriptArguments = scriptArguments
        this.rawArguments.putAll(arguments)
        this.arguments = processArguments(scriptArguments.arguments, arguments)
        this.verboseMode = verboseMode
        this.debugMode = debugMode

        printExecution()

        val output = executable()
        outputHandler(inputFile, output)
    }
}

@KotlinDSL
class ScriptMulti(val executable: ScriptMulti.() -> Any?) : AbstractScript() {
    var inputFiles: List<File> = listOf()

    fun executeMultiScript(inputFiles: List<File>, scriptArguments: ScriptArguments, arguments: Map<String, String>, verboseMode: Boolean, debugMode: Boolean, outputHandler: (File, Any?) -> Unit) {
        this.inputFiles = inputFiles
        this.scriptArguments = scriptArguments
        this.rawArguments.putAll(arguments)
        this.arguments = processArguments(scriptArguments.arguments, arguments)
        this.verboseMode = verboseMode
        this.debugMode = debugMode

        printExecution()

        val output = executable()
        val referenceFile = if (inputFiles.isEmpty()) {
            File("noinput.png")
        } else {
            inputFiles[0]
        }
        outputHandler(referenceFile, output)
    }
}

private fun processArguments(scriptArguments: List<ScriptArg>, rawArguments: Map<String, String>): MutableMap<String, Any> {
    val processed = mutableMapOf<String, Any>()

    for (scriptArgument in scriptArguments) {
        processed[scriptArgument.name] = processArgument(scriptArgument, rawArguments[scriptArgument.name])
    }

    return processed
}

private fun processArgument(scriptArgument: ScriptArg, rawArgument: String?): Any {
    when (scriptArgument) {
        is ScriptOptionalIntArg -> {
            return scriptArgument.toOptionalIntValue(rawArgument)
        }
        is ScriptIntArg -> {
            return scriptArgument.toIntValue(rawArgument)
        }
        is ScriptOptionalDoubleArg -> {
            return scriptArgument.toOptionalDoubleValue(rawArgument)
        }
        is ScriptDoubleArg -> {
            return scriptArgument.toDoubleValue(rawArgument)
        }
        is ScriptOptionalBooleanArg -> {
            return scriptArgument.toOptionalBooleanValue(rawArgument)
        }
        is ScriptBooleanArg -> {
            return scriptArgument.toBooleanValue(rawArgument)
        }
        is ScriptOptionalStringArg -> {
            return scriptArgument.toOptionalStringValue(rawArgument)
        }
        is ScriptStringArg -> {
            return scriptArgument.toStringValue(rawArgument)
        }
        is ScriptOptionalFileArg -> {
            return scriptArgument.toOptionalFileValue(rawArgument)
        }
        is ScriptFileArg -> {
            return scriptArgument.toFileValue(rawArgument)
        }
        is ScriptOptionalImageArg -> {
            return scriptArgument.toOptionalImageValue(rawArgument)
        }
        is ScriptImageArg -> {
            return scriptArgument.toImageValue(rawArgument)
        }
        is ScriptOptionalListArg -> {
            return scriptArgument.toOptionalListValue(rawArgument)
        }
        is ScriptListArg -> {
            return scriptArgument.toListValue(rawArgument)
        }
        is ScriptOptionalRecordArg -> {
            return scriptArgument.toOptionalRecordValue(rawArgument)
        }
        is ScriptRecordArg -> {
            return scriptArgument.toRecordValue(rawArgument)
        }
    }

    throw IllegalArgumentException("Unknown argument: ${scriptArgument.name}")
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
