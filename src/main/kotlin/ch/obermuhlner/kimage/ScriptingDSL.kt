package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.MatrixImage
import ch.obermuhlner.kimage.io.ImageReader
import java.io.File

@DslMarker
annotation class KotlinDSL

@KotlinDSL
sealed class Script(val version: Double) {
}

@KotlinDSL
class ScriptV0_1 : Script(0.1) {
    var name: String = ""
    var description = ""

    private var scriptArguments: ScriptArguments = ScriptArguments()

    private var scriptSingle: ScriptSingle? = null
    private var scriptMulti: ScriptMulti? = null

    fun arguments(initializer: ScriptArguments.() -> Unit) {
        scriptArguments = ScriptArguments().apply(initializer)
    }

    fun single(executable: ScriptSingle.() -> Unit) {
        scriptSingle = ScriptSingle(executable)
    }

    fun multi(executable: ScriptMulti.() -> Unit) {
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
                    if (arg.min != Int.MIN_VALUE) {
                        println("  MIN: ${arg.min}")
                    }
                    if (arg.max != Int.MAX_VALUE) {
                        println("  MAX: ${arg.max}")
                    }
                    if (arg.default != null) {
                        println("  DEFAULT: ${arg.default}")
                    }
                }
                is ScriptDoubleArg -> {
                    if (arg.min != Double.MAX_VALUE) {
                        println("  MIN: ${arg.min}")
                    }
                    if (arg.max != -Double.MAX_VALUE) {
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

    fun execute(inputFiles: List<File>, arguments: Map<String, String>) {
        if (scriptMulti != null) {
            executeMulti(inputFiles, arguments)
        } else if (scriptSingle != null) {
            executeSingle(inputFiles, arguments)
        }
    }

    fun executeSingle(inputFiles: List<File>, arguments: Map<String, String>) {
        scriptSingle?.let {
            for (inputFile in inputFiles) {
                val inputImage = ImageReader.readMatrixImage(inputFile)
                it.executeSingleScript(inputFile, inputImage, scriptArguments, arguments)
            }
        }
    }

    fun executeMulti(inputFiles: List<File>, arguments: Map<String, String>) {
        scriptMulti?.let {
            it.executeMultiScript(inputFiles, scriptArguments, arguments)
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

class ExecutionArguments(scriptArguments: ScriptArguments, argumentValues: Map<String, String>) {
    val int: Map<String, Int>
    val double: Map<String, Double>
    val boolean: Map<String, Boolean>
    val string: Map<String, String>

    init {
        val intMap = mutableMapOf<String, Int>()
        val doubleMap = mutableMapOf<String, Double>()
        val booleanMap = mutableMapOf<String, Boolean>()
        val stringMap = mutableMapOf<String, String>()

        for (argument in scriptArguments.arguments) {
            when (argument) {
                is ScriptIntArg -> {
                    val value = argument.toValue(argumentValues[argument.name])
                    if (value != null) {
                        intMap[argument.name] = value
                    }
                }
                is ScriptDoubleArg -> {
                    val value = argument.toValue(argumentValues[argument.name])
                    if (value != null) {
                        doubleMap[argument.name] = value
                    }
                }
                is ScriptBooleanArg -> {
                    val value = argument.toValue(argumentValues[argument.name])
                    if (value != null) {
                        booleanMap[argument.name] = value
                    }
                }
                is ScriptStringArg -> {
                    val value = argument.toValue(argumentValues[argument.name])
                    if (value != null) {
                        stringMap[argument.name] = value
                    }
                }
            }
        }

        int = intMap
        double = doubleMap
        boolean = booleanMap
        string = stringMap
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
class ScriptSingle(val executable: ScriptSingle.() -> Unit) {
    var inputFile: File = File(".")
    var inputImage: Image = MatrixImage(0, 0)
    val rawArguments: MutableMap<String, String> = mutableMapOf()
    var arguments: ExecutionArguments = ExecutionArguments(ScriptArguments(), mapOf())

    fun executeSingleScript(inputFile: File, inputImage: Image, scriptArguments: ScriptArguments, arguments: Map<String, String>) {
        this.inputFile = inputFile
        this.inputImage = inputImage
        this.rawArguments.putAll(arguments)
        this.arguments = ExecutionArguments(scriptArguments, arguments)
        executable()
    }
}

@KotlinDSL
class ScriptMulti(val executable: ScriptMulti.() -> Unit) {
    var inputFiles: List<File> = listOf()
    val rawArguments: MutableMap<String, String> = mutableMapOf()
    var arguments: ExecutionArguments = ExecutionArguments(ScriptArguments(), mapOf())

    fun executeMultiScript(inputFiles: List<File>, scriptArguments: ScriptArguments, arguments: Map<String, String>) {
        this.inputFiles = inputFiles
        this.rawArguments.putAll(arguments)
        this.arguments = ExecutionArguments(scriptArguments, arguments)

        executable()
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
