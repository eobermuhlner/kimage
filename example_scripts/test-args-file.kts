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
    name = "test-args-file"
    title = "Test script to show how to handle arguments in a kimage script"
    description = """
                Example script as starting point for developers.
                """
    arguments {
        file("fileArg") {
            isFile = true
            default = File("unknown.txt")
        }
        file("fileExistsArg") {
            isFile = true
            exists = true
            default = File("unknown.txt")
        }
        file("mandatoryFileArg") {
            isFile = true
        }
        file("mandatoryExistsFileArg") {
            isFile = true
            exists = true
        }
        file("dirArg") {
            isDirectory = true
            default = File(".")
        }
        file("mandatoryDirArg") {
            isDirectory = true
        }
        file("mandatoryExistsDirArg") {
            isDirectory = true
            exists = true
        }
        optionalFile("optionalFileArg") {
            isFile = true
        }
        optionalFile("optionalExistsFileArg") {
            isFile = true
            exists = true
        }
        optionalFile("optionalDirArg") {
            isDirectory = true
        }
        optionalFile("optionalExistsDirArg") {
            isDirectory = true
            exists = true
        }
    }

    multi {
    }
}
