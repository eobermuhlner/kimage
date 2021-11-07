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
    name = "test-args-point"
    title = "Test script to show how to handle arguments in a kimage script"
    description = """
                Example script as starting point for developers.
                """
    arguments {
        point("pointArg") {
            min = Point(0.0, 0.0)
            max = Point(100.0, 200.0)
            default = Point(50.0, 50.0)
            hint = Hint.ImageXY
        }
        point("mandatoryPointArg") {
            min = Point(0.0, 0.0)
            max = Point(100.0, 200.0)
        }
        optionalPoint("optionalPointArg") {
            min = Point(0.0, 0.0)
            max = Point(100.0, 200.0)
        }
    }

    multi {
    }
}
