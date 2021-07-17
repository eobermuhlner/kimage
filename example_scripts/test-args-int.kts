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
    name = "test-args-int"
    title = "Test int args"
    description = """
                Test script.
                """
    arguments {
        int("intArg") {
            min = -1
            max = 100
            default = -1
        }
        int("mandatoryIntArg") {
            min = 0
            max = 100
        }
        optionalInt("optionalIntArg") {
            min = 0
            max = 100
        }
    }

    multi {
    }
}
