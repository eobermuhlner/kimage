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
    name = "test-args-list"
    title = "Test script to show how to handle arguments in a kimage script"
    description = """
                Example script as starting point for developers.
                """
    arguments {
        list("listOfIntArg") {
            min = 1
            default = listOf(1, 2, 3)

            int {
                min = 0
                max = 9
            }
        }
        optionalList("optionalListOfIntArg") {
            min = 1

            int {
                min = 0
                max = 9
            }
        }
    }

    multi {
    }
}
