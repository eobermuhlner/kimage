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
    name = "test-args-double"
    title = "Test script to show how to handle arguments in a kimage script"
    description = """
                Example script as starting point for developers.
                """
    arguments {
        double("doubleArg") {
            min = 0.0
            max = 100.0
            default = 50.0
        }
        double("mandatoryDoubleArg") {
            min = 0.0
            max = 100.0
        }
        optionalDouble("optionalDoubleArg") {
            min = 0.0
            max = 100.0
        }
    }

    multi {
    }
}
