import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import kotlin.math.*

kimage(0.1) {
    name = "test-single"
    description = """
                Test script to show how to handle single images in a kimage script.
                """
    arguments {
        int("intArg") {
            description = "Example argument for an int value."
            min = 0
            max = 100
            default = 0
        }
        double("doubleArg") {
            description = "Example argument for a double value."
            min = 0.0
            max = 100.0
            default = 50.0
        }
        boolean("booleanArg") {
            description = "Example argument for a boolean value."
            default = false
        }
        string("stringArg") {
            description = "Example argument for a string value."
            default = "undefined"
        }
        string("allowedStringArg") {
            description = "Example argument for a string value with some allowed strings."
            allowed = listOf("red", "green", "blue")
            default = "red"
        }
        string("regexStringArg") {
            description = "Example argument for a string value with regular expression."
            regex = "a+"
            default = "aaa"
        }
    }

    single {
        val intArg: Int by arguments
        val doubleArg: Double by arguments
        val booleanArg: Boolean by arguments
        val stringArg: String by arguments
        val allowedStringArg: String by arguments
        val regexStringArg: String by arguments

        println("Test single image script")
        println()

        println("Raw Arguments:")
        for (rawArgument in rawArguments) {
            val key: String = rawArgument.key
            val value: String = rawArgument.value
            println("  Argument: ${key} = ${value}")
        }
        println()

        println("Arguments:")
        println("  intArg = $intArg")
        println("  doubleArg = $doubleArg")
        println("  booleanArg = $booleanArg")
        println("  stringArg = $stringArg")
        println("  allowedStringArg = $allowedStringArg")
        println("  regexStringArg = $regexStringArg")
        println()

        println("inputFile  = $inputFile")
        println("inputImage  = $inputImage")

        inputImage // will be copied into output
    }
}
