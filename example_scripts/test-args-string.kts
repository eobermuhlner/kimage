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
    name = "test-args-string"
    title = "Test script to show how to handle arguments in a kimage script"
    description = """
                Example script as starting point for developers.
                """
    arguments {
        string("stringArg") {
            default = "undefined"
        }
        string("mandatoryStringArg") {
        }
        optionalString("optionalStringArg") {
        }
        string("allowedStringArg") {
            allowed = listOf("red", "green", "blue")
            default = "red"
        }
        optionalString("optionalAllowedStringArg") {
            allowed = listOf("red", "green", "blue")
        }
        string("regexStringArg") {
            regex = "a+"
            default = "aaa"
        }
    }

    multi {
    }
}
