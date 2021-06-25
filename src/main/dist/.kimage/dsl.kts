import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import java.nio.file.*
import kotlin.math.*

kimage {
    name = "DSL Test"
    description = "A test for the new kimage DSL"

    arguments {
        string {
            name = "alpha"
            description = "The alpha argument"
        }
        int {
            name = "beta"
            description = "The beta argument"
        }
    }

    single {
        println("inputFile : $inputFile")
    }

    multi {
        println("inputFiles : $inputFiles")
    }
}
