import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.huge.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import ch.obermuhlner.kimage.matrix.*

import java.io.*
import java.nio.file.*
import java.util.*
import kotlin.math.*

kimage(0.1) {
    name = "test-args"
    title = "Test script to show how to handle arguments in a kimage script"
    description = """
                Example script as starting point for developers.
                """
    arguments {
        int("intArg") {
            description = "Example argument for an int value."
            min = -1
            max = 100
            default = -1
        }
        int("mandatoryIntArg") {
            description = "Example argument for a mandatory int value."
            min = 0
            max = 100
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
        double("mandatoryDoubleArg") {
            description = "Example argument for a mandatory double value."
            min = 0.0
            max = 100.0
        }
        optionalDouble("optionalDoubleArg") {
            description = "Example argument for an optional double value."
            min = 0.0
            max = 100.0
        }
        boolean("booleanArg") {
            description = "Example argument for a boolean value."
            default = false
        }
        optionalBoolean("optionalBooleanArg") {
            description = "Example argument for am optional boolean value."
        }
        string("stringArg") {
            description = "Example argument for a string value."
            default = "undefined"
        }
        optionalString("optionalStringArg") {
            description = "Example argument for an optional string value."
        }
        string("allowedStringArg") {
            description = "Example argument for a string value with some allowed strings."
            allowed = listOf("red", "green", "blue")
            default = "red"
        }
        optionalString("optionalStringArg") {
            description = "Example argument for an optional string value with some allowed strings."
            allowed = listOf("red", "green", "blue")
        }
        string("regexStringArg") {
            description = """
                Example argument for a string value with regular expression.
                The input only allows `a` characters (at least one).
                """
            regex = "a+"
            default = "aaa"
        }
        file("fileArg") {
            description = "Example argument for a file."
            isFile = true
            default = File("unknown.txt")
        }
        file("mandatoryFileArg") {
            description = "Example argument for a mandatory file."
            isFile = true
        }
        file("dirArg") {
            description = "Example argument for a directory with default."
            isDirectory = true
            default = File(".")
        }
        file("mandatoryDirArg") {
            description = "Example argument for a mandatory directory."
            isDirectory = true
        }
        optionalFile("optionalFileArg") {
            description = "Example argument for an optional file."
            isFile = true
        }
        optionalFile("optionalDirArg") {
            description = "Example argument for an optional directory."
            isDirectory = true
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
        record("recordArg") {
            description = "Example argument for a record containing different values."

            int("recordInt") {
                default = 2
            }
            string("recordString") {
                default = "hello"
            }
            double("recordDouble") {
                default = 3.14
            }
        }
        optionalRecord("optionalRecordArg") {
            description = "Example argument for an optional record containing different values."

            int("optionalRecordInt") {
            }
            string("optionalRecordString") {
            }
            double("optionalRecordDouble") {
            }
        }
    }

    multi {
        val intArg: Int by arguments
        val optionalIntArg: Optional<Int> by arguments
        val doubleArg: Double by arguments
        val optionalDoubleArg: Optional<Double> by arguments
        val booleanArg: Boolean by arguments
        val optionalBooleanArg: Optional<Boolean> by arguments
        val stringArg: String by arguments
        val optionalStringArg: Optional<String> by arguments
        val allowedStringArg: String by arguments
        val regexStringArg: String by arguments
        val fileArg: File by arguments
        val mandatoryFileArg: File by arguments
        val dirArg: File by arguments
        val mandatoryDirArg: File by arguments
        val optionalFileArg: Optional<File> by arguments
        val optionalDirArg: Optional<File> by arguments

        val listOfIntArg: List<Int> by arguments
        val optionalListOfIntArg: Optional<List<Int>> by arguments

        val recordArg: Map<String, Any> by arguments
        val recordInt: Int by recordArg
        val recordString: String by recordArg
        val recordDouble: Double by recordArg

        val optionalRecordArg: Optional<Map<String, Any>> by arguments

        println("Raw Arguments:")
        for (rawArgument in rawArguments) {
            val key: String = rawArgument.key
            val value: String = rawArgument.value
            println("  ${key} = ${value}")
        }
        println()

        println("Processed Arguments as Variables:")
        println("  intArg = $intArg")
        println("  optionalIntArg = $optionalIntArg")
        println("  doubleArg = $doubleArg")
        println("  optionalDoubleArg = $optionalDoubleArg")
        println("  booleanArg = $booleanArg")
        println("  optionalBooleanArg = $optionalBooleanArg")
        println("  stringArg = $stringArg")
        println("  optionalStringArg = $optionalStringArg")
        println("  allowedStringArg = $allowedStringArg")
        println("  regexStringArg = $regexStringArg")
        println("  fileArg = $fileArg")
        println("  mandatoryFileArg = $mandatoryFileArg")
        println("  dirArg = $dirArg")
        println("  mandatoryDirArg = $mandatoryDirArg")
        println("  optionalFileArg = $optionalFileArg")
        println("  optionalDirArg = $optionalDirArg")
        println("  listOfIntArg = $listOfIntArg")
        println("  optionalListOfIntArg = $optionalListOfIntArg")
        println("  recordArg = $recordArg")
        println("  recordInt = $recordInt")
        println("  recordString = $recordString")
        println("  recordDouble = $recordDouble")
        println("  optionalRecordArg = $optionalRecordArg")

        println("Input Files:")
        for (file in inputFiles) {
            println("  File: $file exists=${file.exists()}")
        }
    }
}
