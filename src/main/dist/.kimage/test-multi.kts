import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import java.util.Optional
import kotlin.math.*

kimage(0.1) {
    name = "test-multi"
    title = "Test script to show how to handle multiple images in a kimage script"
    description = """
                Example script as starting point for developers.
                """
    arguments {
        int("intArg") {
            description = "Example argument for an int value."
            min = 0
            max = 100
            default = 0
        }
        optionalInt("optionalIntArg") {
            description = "Example argument for an optional int value."
            min = 0
            max = 100
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
        list("listOfIntArg") {
            description = "Example argument for a list of integer values."
            min = 1
            default = listOf(1, 2, 3)

            int {
                description = "A single integer value"
                min = 0
                max = 9
            }
        }
        optionalList("optionalListOfIntArg") {
            description = "Example argument for an optional list of integer values."
            min = 1

            int {
                description = "A single integer value"
                min = 0
                max = 9
            }
        }
    }

    multi {
        val intArg: Int by arguments
        val optionalIntArg: Optional<Int> by arguments
        val doubleArg: Double by arguments
        val booleanArg: Boolean by arguments
        val stringArg: String by arguments
        val allowedStringArg: String by arguments
        val regexStringArg: String by arguments
        val listOfIntArg: List<Int> by arguments

        println("Raw Arguments:")
        for (rawArgument in rawArguments) {
            val key: String = rawArgument.key
            val value: String = rawArgument.value
            println("  Argument: ${key} = ${value}")
        }
        println()

        println("Input Files:")
        for (file in inputFiles) {
            println("  File: $file exists=${file.exists()}")
        }
    }
}
