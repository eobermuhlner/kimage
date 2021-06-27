import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.huge.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*

import java.io.*
import kotlin.math.*

kimage(0.1) {
    name = "align"
    description = """
                Align multiple images.
                The base image is the first image argument.
                The remaining image arguments are aligned to the base image by searching for a matching feature.
                
                The feature to match is defined by the centerX/centerY coordinates in the base image and the check radius.
                The searchRadius defines how far the matching feature is searched.
                """
    arguments {
        int("checkRadius") {
            description = """
                        The radius to check for similarity.
                        The default value is calculated from the base image.
                        """
            min = 0
        }
        int("searchRadius") {
            description = """
                        The search radius defining the maximum offset to align.
                        The default value is calculated from the base image.
                        """
            min = 0
        }
        int("centerX") {
            description = """
                        The X coordinate of the center to check for alignment.
                        The default value is calculated from the base image.
                        """
            min = 0
        }
        int("centerY") {
            description = """
                        The Y coordinate of the center to check for alignment.
                        The default value is calculated from the base image.
                        """
            min = 0
        }
        double("errorThreshold") {
            description = """
                        The maximum error threshold for storing an aligned image.
                        Images with an error above the error threshold will be either ignored
                        or saved with a different prefix.
                        See `saveBad`, `prefixBad`.
                        """
            min = 0.0
            default = 1E-3
        }
        string("prefix") {
            description = "The prefix of the aligned output files."
            default = "aligned"
        }
        boolean("saveBad") {
            description = "Controls whether badly aligned images are saved."
            default = true
        }
        string("prefixBad") {
            description = "The prefix of the badly aligned output files."
            default = "badaligned"
        }
        boolean("saveCheck") {
            description = "Controls whether the check file containing the feature to match is saved for manual analysis."
            default = false
        }
        boolean("saveDelta") {
            description = "Controls whether a delta file for every aligned image is saved for manual analysis."
            default = false
        }
    }

    multi {
        println("inputFiles : $inputFiles")

        val baseInputFile = inputFiles[0]
        println("Loading base image: $baseInputFile")
        val baseImage = ImageReader.readMatrixImage(baseInputFile)
        println("Base image: $baseImage")
        println()

        val baseImageMinSize = min(baseImage.width, baseImage.height)
        val defaultCheckRadius = sqrt(baseImageMinSize.toDouble()).toInt()
        val defaultSearchRadius = min(baseImageMinSize, defaultCheckRadius * 4)

        val checkRadius: Int by arguments.int.withDefault { defaultCheckRadius }
        val searchRadius: Int by arguments.int.withDefault { defaultSearchRadius }

        val imageAligner = ImageAligner(checkRadius)
        val (autoCenterX, autoCenterY) = imageAligner.findInterestingCropCenter(baseImage)

        val centerX: Int by arguments.int.withDefault { autoCenterX }
        val centerY: Int by arguments.int.withDefault { autoCenterY }
        val errorThreshold: Double by arguments.double
        val prefix: String by arguments.string
        val saveBad: Boolean by arguments.boolean
        val prefixBad: String by arguments.string
        val saveCheck: Boolean by arguments.boolean
        val saveDelta: Boolean by arguments.boolean

        println("Arguments:")
        println("  checkRadius = $checkRadius")
        println("  searchRadius = $searchRadius")
        println("  centerX = $centerX")
        println("  centerY = $centerY")
        println("  prefix = $prefix")
        println("  saveBad = $saveBad")
        println("  prefixBad = $prefixBad")
        println("  saveCheck = $saveCheck")
        println("  saveDelta = $saveDelta")
        println()

        if (saveCheck) {
            val checkImage = baseImage.cropCenter(checkRadius, centerX, centerY)
            val checkFile = File("check_" + baseInputFile.name)
            println("Saving $checkFile for manual analysis")
            ImageWriter.write(checkImage, checkFile)
            println()
        }

        for (inputFile in inputFiles) {
            println("Loading image: $inputFile")

            val image = ImageReader.readMatrixImage(inputFile)
            println("Aligning image: $image")

            val alignment = imageAligner.align(baseImage, image, centerX = centerX, centerY = centerY, maxOffset = searchRadius)
            println("Alignment: $alignment")

            val alignedImage = image.crop(alignment.x, alignment.y, baseImage.width, baseImage.height)

            val error = baseImage.averageError(alignedImage)
            if (error <= errorThreshold) {
                val alignedFile = File("${prefix}_" + inputFile.name)
                println("Error $error <= $errorThreshold : saving $alignedFile")
                ImageWriter.write(alignedImage, alignedFile)
            } else {
                if (saveBad) {
                    val badalignedFile = File("${prefixBad}_" + inputFile.name)
                    println("Error $error > $errorThreshold : saving $badalignedFile")
                    ImageWriter.write(alignedImage, badalignedFile)
                } else {
                    println("Error $error > $errorThreshold : ignoring badly aligned image")
                }
            }

            if (saveDelta) {
                val deltaFile = File("delta_$prefix" + inputFile.name)
                println("Saving $deltaFile for manual analysis")
                val deltaImage = deltaRGB(baseImage, alignedImage)
                ImageWriter.write(deltaImage, deltaFile)
            }

            println()
        }
    }
}
