package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.image.Channel
import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.MatrixImage
import ch.obermuhlner.kimage.io.ImageReader
import ch.obermuhlner.kimage.io.ImageReader.read
import ch.obermuhlner.kimage.math.clamp
import ch.obermuhlner.kimage.matrix.DoubleMatrix
import ch.obermuhlner.kimage.matrix.Matrix
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.*

@DslMarker
annotation class KotlinDSL

@KotlinDSL
sealed class Script(val version: Double) {
    var name: String = ""
    var code: String = ""
}

@KotlinDSL
class ScriptV0_1 : Script(0.1) {
    var title = ""
    var description = ""

    var scriptArguments: ScriptArguments = ScriptArguments()

    private var scriptSingle: ScriptSingle? = null
    private var scriptMulti: ScriptMulti? = null

    fun isSingle(): Boolean = scriptSingle != null

    fun arguments(initializer: ScriptArguments.() -> Unit) {
        scriptArguments = ScriptArguments().apply(initializer)
    }

    fun single(executable: ScriptSingle.() -> Any?) {
        scriptSingle = ScriptSingle(executable)
    }

    fun multi(executable: ScriptMulti.() -> Any?) {
        scriptMulti = ScriptMulti(executable)
    }

    fun documentation(commandline: Boolean = true): String {
        val systemOut = System.out
        val bufferedOutputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(bufferedOutputStream))

        try {
            printDocumentation(commandline)
        } finally {
            System.setOut(systemOut)
        }

        return bufferedOutputStream.toString()
    }

    fun printDocumentation(commandline: Boolean = true) {
        println("## Script: `$name`")
        println()

        if (commandline) {
            println("    kimage [OPTIONS] $name")
            for (arg in scriptArguments.arguments) {
                print("        ")
                if (!arg.mandatory || arg.hasDefault) {
                    print("[")
                }
                print("--arg ${arg.name}=${arg.type.uppercase()}")
                if (!arg.mandatory || arg.hasDefault) {
                    print("]")
                }
                println()
            }
            println("        [FILES]")
            println()
        }

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

        if (level > 0 && arg.name.isNotBlank()) {
            println("${indent}- Name: `${arg.name}`")
        }

        println("${indent}- Type: ${arg.type}")
        if (level == 0) {
            if (arg.mandatory && !arg.hasDefault) {
                println("${indent}- Mandatory: yes")
            }
        }

        when (arg) {
            is ScriptBooleanArg -> {
            }
            is ScriptIntArg -> {
                if (arg.min != null) {
                    println("${indent}- Minimum value: ${arg.min}")
                }
                if (arg.max != null) {
                    println("${indent}- Maximum value: ${arg.max}")
                }
            }
            is ScriptDoubleArg -> {
                if (arg.min != null) {
                    println("${indent}- Minimum value: ${arg.min}")
                }
                if (arg.max != null) {
                    println("${indent}- Maximum value: ${arg.max}")
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
            }
            is ScriptImageArg -> {
            }
            is ScriptPointArg -> {
                if (arg.min != null) {
                    println("${indent}- Minimum value: ${arg.min}")
                }
                if (arg.max != null) {
                    println("${indent}- Maximum value: ${arg.max}")
                }
            }
            is ScriptListArg -> {
                println("${indent}- List Elements:")
                for (listElement in arg.arguments) {
                    printArgumentType(listElement, level+1)
                }
                if (arg.min != null) {
                    println("${indent}- Minimum length: ${arg.min}")
                }
                if (arg.max != null) {
                    println("${indent}- Maximum length: ${arg.max}")
                }
            }
            is ScriptRecordArg -> {
                println("${indent}- Record Elements:")
                for (recordElement in arg.arguments) {
                    printArgumentType(recordElement, level+1)
                }
            }
        }

        if (arg.hasDefault) {
            println("${indent}- Default value: ${arg.toValue(null)}")
        }

        println()
    }

    fun execute(
        inputFiles: List<File>,
        arguments: Map<String, Any>,
        verboseMode: Boolean,
        debugMode: Boolean,
        maskMatrix: Matrix?,
        progress: Progress,
        outputDirectory: File,
        outputHandler: (File, Any?) -> Unit
    ) {
        if (title.isNotBlank()) {
            print(title)
        } else {
            print(name)
        }
        println()

        val startMillis = System.currentTimeMillis()
        return try {
            when {
                scriptMulti != null -> {
                    executeMulti(inputFiles, arguments, verboseMode, debugMode, progress, outputDirectory, outputHandler)
                }
                scriptSingle != null -> {
                    executeSingle(inputFiles, arguments, verboseMode, debugMode, maskMatrix, progress, outputDirectory, outputHandler)
                }
                else -> {
                    throw java.lang.RuntimeException("Script has no execution block.")
                }
            }
        } finally {
            val endMillis = System.currentTimeMillis()
            val deltaMillis = endMillis - startMillis
            println("Processed in $deltaMillis ms")
        }
    }

    fun executeSingle(
        inputFiles: List<File>,
        arguments: Map<String, Any>,
        verboseMode: Boolean,
        debugMode: Boolean,
        maskMatrix: Matrix?,
        progress: Progress,
        outputDirectory: File,
        outputHandler: (File, Any?) -> Unit
    ) {
        scriptSingle?.let {
            progress.addTotal(inputFiles.size)
            for (inputFile in inputFiles) {
                val inputImage = read(inputFile)
                it.executeSingleScript(
                    inputFiles,
                    inputFile,
                    inputImage,
                    scriptArguments,
                    arguments,
                    verboseMode,
                    debugMode,
                    maskMatrix,
                    progress,
                    outputDirectory,
                    outputHandler
                )
                progress.step(1, inputFile.name)
            }
        }
    }

    fun executeMulti(
        inputFiles: List<File>,
        arguments: Map<String, Any>,
        verboseMode: Boolean,
        debugMode: Boolean,
        progress: Progress,
        outputDirectory: File,
        outputHandler: (File, Any?) -> Unit
    ) {
        scriptMulti?.let {
            it.executeMultiScript(
                inputFiles,
                scriptArguments,
                arguments,
                verboseMode,
                debugMode,
                progress,
                outputDirectory,
                outputHandler)
        }
    }
}

@KotlinDSL
interface ScriptNamedTypes {
    val arguments: MutableList<ScriptArg>

    fun int(name: String, initializer: ScriptIntArg.() -> Unit) =
        arguments.addIfNotContains(ScriptIntArg().apply { this.name = name }.apply(initializer))

    fun optionalInt(name: String, initializer: ScriptOptionalIntArg.() -> Unit) =
        arguments.addIfNotContains(ScriptOptionalIntArg().apply { this.name = name }.apply(initializer))

    fun double(name: String, initializer: ScriptDoubleArg.() -> Unit) =
        arguments.addIfNotContains(ScriptDoubleArg().apply { this.name = name }.apply(initializer))

    fun optionalDouble(name: String, initializer: ScriptOptionalDoubleArg.() -> Unit) =
        arguments.addIfNotContains(ScriptOptionalDoubleArg().apply { this.name = name }.apply(initializer))

    fun boolean(name:String, initializer: ScriptBooleanArg.() -> Unit) =
        arguments.addIfNotContains(ScriptBooleanArg().apply { this.name = name }.apply(initializer))

    fun optionalBoolean(name:String, initializer: ScriptOptionalBooleanArg.() -> Unit) =
        arguments.addIfNotContains(ScriptOptionalBooleanArg().apply { this.name = name }.apply(initializer))

    fun string(name: String, initializer: ScriptStringArg.() -> Unit) =
        arguments.addIfNotContains(ScriptStringArg().apply { this.name = name }.apply(initializer))

    fun optionalString(name: String, initializer: ScriptOptionalStringArg.() -> Unit) =
        arguments.addIfNotContains(ScriptOptionalStringArg().apply { this.name = name }.apply(initializer))

    fun file(name: String, initializer: ScriptFileArg.() -> Unit) =
        arguments.addIfNotContains(ScriptFileArg().apply { this.name = name }.apply(initializer))

    fun optionalFile(name: String, initializer: ScriptOptionalFileArg.() -> Unit) =
        arguments.addIfNotContains(ScriptOptionalFileArg().apply { this.name = name }.apply(initializer))

    fun image(name: String, initializer: ScriptImageArg.() -> Unit) =
        arguments.addIfNotContains(ScriptImageArg().apply { this.name = name }.apply(initializer))

    fun optionalImage(name: String, initializer: ScriptOptionalImageArg.() -> Unit) =
        arguments.addIfNotContains(ScriptOptionalImageArg().apply { this.name = name }.apply(initializer))

    fun point(name: String, initializer: ScriptPointArg.() -> Unit) =
        arguments.addIfNotContains(ScriptPointArg().apply { this.name = name }.apply(initializer))

    fun optionalPoint(name: String, initializer: ScriptOptionalPointArg.() -> Unit) =
        arguments.addIfNotContains(ScriptOptionalPointArg().apply { this.name = name }.apply(initializer))

    fun list(name: String, initializer: ScriptListArg.() -> Unit) =
        arguments.addIfNotContains(ScriptListArg().apply { this.name = name }.apply(initializer))

    fun optionalList(name: String, initializer: ScriptOptionalListArg.() -> Unit) =
        arguments.addIfNotContains(ScriptOptionalListArg().apply { this.name = name }.apply(initializer))

    fun record(name: String, initializer: ScriptRecordArg.() -> Unit) =
        arguments.addIfNotContains(ScriptRecordArg().apply { this.name = name }.apply(initializer))

    fun optionalRecord(name: String, initializer: ScriptOptionalRecordArg.() -> Unit) =
        arguments.addIfNotContains(ScriptOptionalRecordArg().apply { this.name = name }.apply(initializer))
}

private fun MutableList<ScriptArg>.addIfNotContains(element: ScriptArg): Boolean {
    if (filter { arg -> arg.name == element.name }.any()) {
        throw IllegalArgumentException("Argument already exists: ${element.name}")
    }
    return add(element)
}

@KotlinDSL
interface ScriptUnnamedTypes {
    val arguments: MutableList<ScriptArg>

    fun int(initializer: ScriptIntArg.() -> Unit) =
        arguments.add(ScriptIntArg().apply(initializer))

    fun optionalInt(initializer: ScriptOptionalIntArg.() -> Unit) =
        arguments.add(ScriptOptionalIntArg().apply(initializer))

    fun double(initializer: ScriptDoubleArg.() -> Unit) =
        arguments.add(ScriptDoubleArg().apply(initializer))

    fun optionalDouble(initializer: ScriptOptionalDoubleArg.() -> Unit) =
        arguments.add(ScriptOptionalDoubleArg().apply(initializer))

    fun boolean(initializer: ScriptBooleanArg.() -> Unit) =
        arguments.add(ScriptBooleanArg().apply(initializer))

    fun string(initializer: ScriptStringArg.() -> Unit) =
        arguments.add(ScriptStringArg().apply(initializer))

    fun file(initializer: ScriptFileArg.() -> Unit) =
        arguments.add(ScriptFileArg().apply(initializer))

    fun image(initializer: ScriptImageArg.() -> Unit) =
        arguments.add(ScriptImageArg().apply(initializer))

    fun optionalFile(initializer: ScriptFileArg.() -> Unit) =
        arguments.add(ScriptFileArg().apply(initializer))

    fun optionalImage(initializer: ScriptImageArg.() -> Unit) =
        arguments.add(ScriptImageArg().apply(initializer))

    fun point(initializer: ScriptPointArg.() -> Unit) =
        arguments.add(ScriptPointArg().apply(initializer))

    fun optionalPoint(initializer: ScriptOptionalPointArg.() -> Unit) =
        arguments.add(ScriptOptionalPointArg().apply(initializer))

    fun list(initializer: ScriptListArg.() -> Unit) =
        arguments.add(ScriptListArg().apply(initializer))

    fun optionalList(initializer: ScriptOptionalListArg.() -> Unit) =
        arguments.add(ScriptOptionalListArg().apply(initializer))

    fun record(initializer: ScriptRecordArg.() -> Unit) =
        arguments.add(ScriptRecordArg().apply(initializer))

    fun optionalRecord(initializer: ScriptOptionalRecordArg.() -> Unit) =
        arguments.add(ScriptOptionalRecordArg().apply(initializer))
}

@KotlinDSL
class ScriptArguments(override val arguments: MutableList<ScriptArg> = mutableListOf()) : ScriptNamedTypes {
}

enum class Hint {
    ImageX,
    ImageY,
    ImageXY,
    ImageDeltaX,
    ImageDeltaY,
    Curve,
    ImageWidth,
    ImageHeight,
    ColorCurveX,
    ColorCurveY
}

open class Reference(val name: String) {
    fun isEqual(vararg values: Any): Reference = ReferenceIsEqual(name, *values)
}

class ReferenceIsEqual(name: String, vararg val values: Any): Reference(name)

@KotlinDSL
sealed class ScriptArg(val type: String, val mandatory: Boolean) {
    var name: String = ""
    var description = ""
    var unit: String? = null
    var hint: Hint? = null
    var enabledWhen: Reference? = null

    abstract val hasDefault: Boolean

    abstract fun toValue(anyValue: Any?): Any

    open fun isValid(stringValue: String): Boolean {
        return try {
            toValue(stringValue)
            true
        } catch (ex: Exception) {
            false
        }
    }

    fun tooltip(): String = if (description.isNullOrBlank()) name else description.trimIndent()
}

@KotlinDSL
open class ScriptIntArg(mandatory: Boolean = true) : ScriptArg("int", mandatory) {
    var min: Int? = null
    var max: Int? = null
    var default: Int? = null

    override val hasDefault: Boolean
        get() = default != null

    override fun toValue(anyValue: Any?): Any {
        return toIntValue(anyValue)
    }

    fun toIntValue(anyValue: Any?): Int {
        if (anyValue == null || anyValue == "") {
            default?.let {
                return toIntValue(it.toString());
            }
            throw ScriptArgumentException("Argument $name is mandatory")
        }
        try {
            val value = if (anyValue is Int) anyValue else anyValue.toString().toInt()
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
            throw ScriptArgumentException("Argument $name must be an integer value, but is '$anyValue'")
        }
    }
}

@KotlinDSL
class ScriptOptionalIntArg : ScriptIntArg(false) {
    override fun toValue(anyValue: Any?): Any {
        return toOptionalIntValue(anyValue)
    }

    fun toOptionalIntValue(anyValue: Any?): Optional<Int> {
        if (anyValue == null || anyValue == "") {
            if (default == null) {
                return Optional.empty()
            }
            default?.let {
                return Optional.of(toIntValue(it.toString()))
            }
        }
        return Optional.of(toIntValue(anyValue))
    }
}

@KotlinDSL
open class ScriptDoubleArg(mandatory: Boolean = true) : ScriptArg("double", mandatory) {
    var min: Double? = null
    var max: Double? = null
    var default: Double? = null

    override val hasDefault: Boolean
        get() = default != null

    override fun toValue(anyValue: Any?): Any {
        return toDoubleValue(anyValue)
    }

    fun toDoubleValue(anyValue: Any?): Double {
        if (anyValue == null || anyValue == "") {
            default?.let {
                return toDoubleValue(it.toString())
            }
            throw ScriptArgumentException("Argument $name is mandatory")
        }
        try {
            val value = if (anyValue is Double) anyValue else anyValue.toString().toDouble()
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
            throw ScriptArgumentException("Argument $name must be an integer value, but is '$anyValue'")
        }
    }
}

@KotlinDSL
class ScriptOptionalDoubleArg : ScriptDoubleArg(false) {
    override fun toValue(anyValue: Any?): Any {
        return toOptionalDoubleValue(anyValue)
    }

    fun toOptionalDoubleValue(anyValue: Any?): Optional<Double> {
        if (anyValue == null || anyValue == "") {
            if (default == null) {
                return Optional.empty()
            }
            default?.let {
                return Optional.of(toDoubleValue(it.toString()))
            }
        }
        return Optional.of(toDoubleValue(anyValue))
    }
}

@KotlinDSL
open class ScriptBooleanArg(mandatory: Boolean = true) : ScriptArg("boolean", mandatory) {
    var default: Boolean? = null

    override val hasDefault: Boolean
        get() = default != null

    override fun toValue(anyValue: Any?): Any {
        return toBooleanValue(anyValue)
    }

    fun toBooleanValue(anyValue: Any?): Boolean {
        if (anyValue == null || anyValue == "") {
            default?.let {
                return toBooleanValue(it.toString())
            }
            throw ScriptArgumentException("Argument $name is mandatory")
        }
        return if (anyValue is Boolean) anyValue else anyValue.toString().toBoolean()
    }
}

@KotlinDSL
class ScriptOptionalBooleanArg : ScriptBooleanArg(false) {
    override fun toValue(anyValue: Any?): Any {
        return toOptionalBooleanValue(anyValue)
    }

    fun toOptionalBooleanValue(anyValue: Any?): Optional<Boolean> {
        if (anyValue == null || anyValue == "") {
            if (default == null) {
                return Optional.empty()
            }
            default?.let {
                return Optional.of(toBooleanValue(it.toString()))
            }
        }
        return Optional.of(toBooleanValue(anyValue))
    }
}

@KotlinDSL
open class ScriptStringArg(mandatory: Boolean = true) : ScriptArg("string", mandatory) {
    var allowed: List<String> = mutableListOf()
    var regex: String? = null
    var default: String? = null

    override val hasDefault: Boolean
        get() = default != null

    override fun toValue(anyValue: Any?): Any {
        return toStringValue(anyValue)
    }

    fun toStringValue(anyValue: Any?): String {
        if (anyValue == null || anyValue == "") {
            default?.let {
                return it;
            }
            throw ScriptArgumentException("Argument $name is mandatory")
        }
        if (allowed.isNotEmpty()) {
            if (!allowed.contains(anyValue)) {
                throw ScriptArgumentException("Argument $name must be one of $allowed but is $anyValue")
            }
        }
        regex?.let {
            if (!anyValue.toString().matches(Regex(it))) {
                throw ScriptArgumentException("Argument $name fails regular expression check: $anyValue")
            }
        }
        return anyValue.toString()
    }
}

@KotlinDSL
class ScriptOptionalStringArg : ScriptStringArg(false) {
    override fun toValue(anyValue: Any?): Any {
        return toOptionalStringValue(anyValue)
    }

    fun toOptionalStringValue(anyValue: Any?): Optional<String> {
        if (anyValue == null || anyValue == "") {
            if (default == null) {
                return Optional.empty()
            }
            default?.let {
                return Optional.of(it)
            }
        }
        return Optional.of(toStringValue(anyValue))
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

    override fun toValue(anyValue: Any?): Any {
        return toFileValue(anyValue)
    }

    fun toFileValue(anyValue: Any?): File {
        if (anyValue == null || anyValue == "") {
            default?.let {
                return toFileValue(it.toString())
            }
            throw ScriptArgumentException("Argument $name is mandatory")
        }
        val file = if (anyValue is File) anyValue else File(anyValue.toString())
        allowedExtensions?.let {
            if (file.extension in it) {
                throw ScriptArgumentException("Argument $name must have one of the extensions $allowedExtensions but has ${file.extension}: $file")
            }
        }
        exists?.let {
            if (!file.exists()) {
                throw ScriptArgumentException("Argument $name must exist: $file")
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
        }
        canRead?.let {
            if (!file.canRead()) {
                throw ScriptArgumentException("Argument $name must be readable: $file")
            }
        }
        canWrite?.let {
            if (!file.canWrite()) {
                throw ScriptArgumentException("Argument $name must be writable: $file")
            }
        }
        return file
    }
}

@KotlinDSL
class ScriptOptionalFileArg : ScriptFileArg(false) {
    override fun toValue(anyValue: Any?): Any {
        return toOptionalFileValue(anyValue)
    }

    fun toOptionalFileValue(anyValue: Any?): Optional<File> {
        if (anyValue == null || anyValue == "") {
            if (default == null) {
                return Optional.empty()
            }
            default?.let {
                return Optional.of(toFileValue(it.toString()))
            }
        }
        return Optional.of(toFileValue(anyValue))
    }
}

@KotlinDSL
open class ScriptImageArg(mandatory: Boolean = true) : ScriptArg("image", mandatory) {
    var default: File? = null

    override val hasDefault: Boolean
        get() = default != null

    override fun toValue(anyValue: Any?): Any {
        return toImageValue(anyValue)
    }

    fun toImageValue(anyValue: Any?): Image {
        if (anyValue == null || anyValue == "") {
            default?.let {
                return ImageReader.read(it)
            }
            throw ScriptArgumentException("Argument $name is mandatory")
        }
        val file = if (anyValue is File) anyValue else File(anyValue.toString())
        return ImageReader.read(file)
    }
}

@KotlinDSL
class ScriptOptionalImageArg() : ScriptImageArg(false) {
    override fun toValue(anyValue: Any?): Any {
        return toOptionalImageValue(anyValue)
    }

    fun toOptionalImageValue(anyValue: Any?): Optional<Image> {
        if (anyValue == null || anyValue == "") {
            if (default == null) {
                return Optional.empty()
            }
            default?.let {
                return Optional.of(toImageValue(it.toString()))
            }
        }
        val file = if (anyValue is File) anyValue else File(anyValue.toString())
        return Optional.of(ImageReader.read(file))
    }
}

@KotlinDSL
open class ScriptPointArg(mandatory: Boolean = true) : ScriptArg("point", mandatory) {
    var min: Point? = null
    var max: Point? = null
    var default: Point? = null

    override val hasDefault: Boolean
        get() = default != null

    override fun toValue(anyValue: Any?): Any {
        return toPointValue(anyValue)
    }

    fun toPointValue(anyValue: Any?): Point {
        if (anyValue == null || anyValue == "") {
            default?.let {
                return toPointValue(it.toString())
            }
            throw ScriptArgumentException("Argument $name is mandatory")
        }
        try {
            val value = if (anyValue is Point) anyValue else toPointValue(anyValue.toString())
            min?.let {
                if (value.x < it.x) {
                    throw ScriptArgumentException("Argument $name.x must be >= $it.x but is ${value.x}")
                }
                if (value.y < it.y) {
                    throw ScriptArgumentException("Argument $name.y must be >= $it.y but is ${value.y}")
                }
            }
            max?.let {
                if (value.x > it.x) {
                    throw ScriptArgumentException("Argument $name.x must be <= $it.x but is ${value.x}")
                }
                if (value.y > it.y) {
                    throw ScriptArgumentException("Argument $name.y must be <= $it.y but is ${value.y}")
                }
            }
            return value
        } catch (ex: NumberFormatException) {
            throw ScriptArgumentException("Argument $name must be an Point value, but is '$anyValue'")
        }
    }

    private fun toPointValue(string: String): Point {
        val parts = string.split(":")
        return Point(parts[0].toDouble(), parts[1].toDouble())
    }
}

@KotlinDSL
class ScriptOptionalPointArg : ScriptPointArg(false) {
    override fun toValue(anyValue: Any?): Any {
        return toOptionalPointValue(anyValue)
    }

    fun toOptionalPointValue(anyValue: Any?): Optional<Point> {
        if (anyValue == null || anyValue == "") {
            if (default == null) {
                return Optional.empty()
            }
            default?.let {
                return Optional.of(toPointValue(it.toString()))
            }
        }
        return Optional.of(toPointValue(anyValue))
    }
}

@KotlinDSL
open class ScriptListArg(override val arguments: MutableList<ScriptArg> = mutableListOf(), mandatory: Boolean = true) : ScriptArg("list", mandatory), ScriptUnnamedTypes {
    var min: Int? = null
    var max: Int? = null
    var default: List<Any>? = null

    override val hasDefault: Boolean
        get() = default != null

    override fun toValue(anyValue: Any?): Any {
        return toListValue(anyValue)
    }

    fun toListValue(anyValue: Any?): List<Any> {
        if (arguments.isEmpty()) {
            throw ScriptArgumentException("Argument $name is a list, but the element type is not defined")
        }
        if (arguments.size > 1) {
            throw ScriptArgumentException("Argument $name is a list, but only one element type is allowed")
        }
        if (anyValue == null || anyValue == "") {
            default?.let {
                return toListValue(it.joinToString(","))
            }
            throw ScriptArgumentException("Argument $name is mandatory")
        }

        val values = mutableListOf<Any>()
        val scriptArg = arguments[0]
        val stringElements = anyValue.toString().trim('[', ']').split(',')

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
    override fun toValue(anyValue: Any?): Any {
        return toOptionalListValue(anyValue)
    }

    fun toOptionalListValue(anyValue: Any?): Optional<List<Any>> {
        if (anyValue == null || anyValue == "") {
            if (default == null) {
                return Optional.empty()
            }
            default?.let {
                return Optional.of(toListValue(it.joinToString(",")))
            }
        }
        return Optional.of(toListValue(anyValue))
    }
}

@KotlinDSL
open class ScriptRecordArg(override val arguments: MutableList<ScriptArg> = mutableListOf(), mandatory: Boolean = true) : ScriptArg("record", mandatory), ScriptNamedTypes {
    override val hasDefault: Boolean
        get() {
            for (argument in arguments) {
                if (!argument.hasDefault) {
                    return false
                }
            }
            return true
        }

    override fun toValue(anyValue: Any?): Any {
        return toRecordValue(anyValue)
    }

    fun toRecordValue(anyValue: Any?): Map<String, Any> {
        if (anyValue == null || anyValue == "") {
            val valueMap = mutableMapOf<String, Any>()

            for (argument in arguments) {
                valueMap[argument.name] = processArgument(argument, null)
            }

            return valueMap
        }

        val valueMap = mutableMapOf<String, Any>()
        val stringElements = anyValue.toString().split(',')

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
    override fun toValue(anyValue: Any?): Any {
        return toOptionalRecordValue(anyValue)
    }

    fun toOptionalRecordValue(anyValue: Any?): Optional<Map<String, Any>> {
        if (anyValue == null || anyValue == "") {
            return try {
                Optional.of(toRecordValue(null))
            } catch (ex: ScriptArgumentException) {
                Optional.empty()
            }
        }
        return Optional.of(toRecordValue(anyValue))
    }
}

class ScriptArgumentException(message: String): RuntimeException(message)

sealed class AbstractScript {
    var scriptArguments: ScriptArguments = ScriptArguments()
    val rawArguments: MutableMap<String, Any> = mutableMapOf()
    val arguments: MutableMap<String, Any> = mutableMapOf()
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

    fun runScript(name: String, files: List<File>, vararg arguments: Pair<String, Any>) {
        runScript(name, files, mutableMapOf(*arguments))
    }

    fun runScript(name: String, files: List<File>, arguments: Map<String, Any>) {
        println("Running $name $arguments : $files")

        val script = KImageManager.script(name)

        val outputDirectory = "."

        KImageManager.executeScript(
            script,
            arguments,
            files,
            false,
            verboseMode,
            debugMode,
            null, // TODO MASK MATRIX
            name,
            outputDirectory
        )
    }
}

@KotlinDSL
class ScriptSingle(val executable: ScriptSingle.() -> Any?) : AbstractScript() {
    var inputFiles: List<File> = listOf()
    var inputFile: File = File(".")
    var inputImage: Image = MatrixImage(0, 0)
    var outputDirectory: File = File(".")

    fun executeSingleScript(
        inputFiles: List<File>,
        inputFile: File,
        inputImage: Image,
        scriptArguments: ScriptArguments,
        rawArguments: Map<String, Any>,
        verboseMode: Boolean,
        debugMode: Boolean,
        maskMatrix: Matrix?,
        progress: Progress,
        outputDirectory: File,
        outputHandler: (File, Any?) -> Unit
    ) {
        this.inputFiles = inputFiles
        this.inputFile = inputFile
        this.inputImage = inputImage
        this.outputDirectory = outputDirectory
        this.scriptArguments = scriptArguments
        this.verboseMode = verboseMode
        this.debugMode = debugMode

        this.rawArguments.clear()
        this.rawArguments.putAll(rawArguments)

        this.arguments.clear()
        this.arguments.putAll(processArguments(scriptArguments.arguments, rawArguments))

        printExecution()

        var output = executable()
        if (output is Image && maskMatrix != null) {
            println("Applying mask.")
            val invertedMaskMatrix = DoubleMatrix(maskMatrix.width, maskMatrix.height) { _, _ -> 1.0 } - maskMatrix
            val channelMatrices = mutableMapOf<Channel, Matrix>()
            for (channel in output.channels) {
                //val matrix = output[channel] elementTimes maskMatrix + inputImage[channel] elementTimes invertedMaskMatrix
                val outputMatrix = output[channel]
                val inputMatrix = inputImage[channel]
                val matrix = DoubleMatrix(output.width, output.height) { x, y ->
                    val v = outputMatrix[x, y] * maskMatrix[x, y] + inputMatrix[x, y] * invertedMaskMatrix[x, y]
                    clamp(v, 0.0, 1.0)
                }
                channelMatrices[channel] = matrix
            }

            output = MatrixImage(inputImage.width, inputImage.height, inputImage.channels) { channel, _, _ ->
                channelMatrices[channel]!!
            }
        }
        outputHandler(inputFile, output)
    }
}

@KotlinDSL
class ScriptMulti(val executable: ScriptMulti.() -> Any?) : AbstractScript() {
    var inputFiles: List<File> = listOf()
    var outputDirectory: File = File(".")

    fun executeMultiScript(
        inputFiles: List<File>,
        scriptArguments: ScriptArguments,
        rawArguments: Map<String, Any>,
        verboseMode: Boolean,
        debugMode: Boolean,
        progress: Progress,
        outputDirectory: File,
        outputHandler: (File, Any?) -> Unit
    ) {
        this.inputFiles = inputFiles
        this.outputDirectory = outputDirectory
        this.scriptArguments = scriptArguments
        this.verboseMode = verboseMode
        this.debugMode = debugMode

        this.rawArguments.clear()
        this.rawArguments.putAll(rawArguments)

        this.arguments.clear()
        this.arguments.putAll(processArguments(scriptArguments.arguments, rawArguments))

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

private fun processArguments(scriptArguments: List<ScriptArg>, rawArguments: Map<String, Any>): MutableMap<String, Any> {
    val processed = mutableMapOf<String, Any>()

    for (scriptArgument in scriptArguments) {
        processed[scriptArgument.name] = processArgument(scriptArgument, rawArguments[scriptArgument.name])
    }

    return processed
}

private fun processArgument(scriptArgument: ScriptArg, rawArgument: Any?): Any {
    return scriptArgument.toValue(rawArgument)
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
