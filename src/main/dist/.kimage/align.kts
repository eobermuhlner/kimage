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
    name = "align"
    title = "Align multiple images"
    description = """
                The base image is the first image argument.
                The remaining image arguments are aligned to the base image by searching for a matching feature.
                
                The feature to match is defined by the `centerX`/`centerY` coordinates in the base image and the `checkRadius`.
                The `searchRadius` defines how far the matching feature is searched.

                Use the --debug option to save intermediate images for manual analysis.
                """
    arguments {
        optionalInt("checkRadius") {
            description = """
                        The radius to check for similarity.
                        The default value is calculated from the base image.
                        """
            min = 0
        }
        optionalInt("searchRadius") {
            description = """
                        The search radius defining the maximum offset to align.
                        The default value is calculated from the base image.
                        """
            min = 0
        }
        optionalInt("centerX") {
            description = """
                        The X coordinate of the center to check for alignment.
                        The default value is calculated from the base image.
                        """
            hint = Hint.ImageX
            min = 0
        }
        optionalInt("centerY") {
            description = """
                        The Y coordinate of the center to check for alignment.
                        The default value is calculated from the base image.
                        """
            hint = Hint.ImageY
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
            default = 0.001
        }
        double("subPixelStep") {
            description = """
                        """
            //allowed = listOf(0.5, 0.2, 0.1, 0.05, 0.02, 0.01)
            default = 0.1
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
            enabledWhen = Reference("saveBad").isEqual(true)
            default = "badaligned"
        }
        boolean("sort") {
            description = "Sort output files by error (best aligned first)."
            default = true
        }
    }

    multi {
        val baseInputFile = inputFiles[0]
        println("Loading base image: $baseInputFile")
        val baseImage = ImageReader.read(baseInputFile)
        println("Base image: $baseImage")
        println()

        val baseImageMinSize = min(baseImage.width, baseImage.height)
        val defaultCheckRadius = sqrt(baseImageMinSize.toDouble()).toInt()
        val defaultSearchRadius = min(baseImageMinSize, defaultCheckRadius * 4)

        var checkRadius: Optional<Int> by arguments
        var searchRadius: Optional<Int> by arguments
        if (!checkRadius.isPresent) {
            checkRadius = Optional.of(defaultCheckRadius)
        }
        if (!searchRadius.isPresent) {
            searchRadius = Optional.of(defaultSearchRadius)
        }

        val imageAligner = ImageAligner(checkRadius.get())
        val (autoCenterX, autoCenterY) = imageAligner.findInterestingCropCenter(baseImage)

        var centerX: Optional<Int> by arguments
        var centerY: Optional<Int> by arguments
        if (!centerX.isPresent) {
            centerX = Optional.of(autoCenterX)
        }
        if (!centerY.isPresent) {
            centerY = Optional.of(autoCenterY)
        }

        val errorThreshold: Double by arguments
        val subPixelStep: Double by arguments
        val prefix: String by arguments
        val saveBad: Boolean by arguments
        val prefixBad: String by arguments
        val sort: Boolean by arguments

        println("Arguments (calculated from input):")
        println("  checkRadius = ${checkRadius.get()}")
        println("  searchRadius = ${searchRadius.get()}")
        println("  centerX = ${centerX.get()}")
        println("  centerY = ${centerY.get()}")
        println()

        if (debugMode) {
            val checkImage = baseImage.cropCenter(checkRadius.get(), centerX.get(), centerY.get())
            val checkFile = baseInputFile.prefixName("check_")
            println("Saving $checkFile for manual analysis")
            ImageWriter.write(checkImage, checkFile)
            println()
        }

        val outputFilesAlignment = mutableListOf<Pair<File, Alignment>>()

        for (inputFile in inputFiles) {
            println("Loading image: $inputFile")

            val image = ImageReader.read(inputFile)
            if (verboseMode) println("Aligning image: $image")

            val alignment = imageAligner.align(
                baseImage,
                image,
                centerX = centerX.get(),
                centerY = centerY.get(),
                maxOffset = searchRadius.get(),
                subPixelStep = subPixelStep
            )
            println("Alignment: $alignment")

            val alignedImage = if (alignment.subPixelX != 0.0 || alignment.subPixelY != 0.0) {
                image.scaleBy(1.0, 1.0, alignment.subPixelX, alignment.subPixelY).crop(alignment.x, alignment.y, baseImage.width, baseImage.height)
            } else {
                image.crop(alignment.x, alignment.y, baseImage.width, baseImage.height)
            }

            val error = baseImage.averageError(alignedImage)
            if (error <= errorThreshold) {
                val alignedFile = inputFile.prefixName("${prefix}_")
                println("Error $error <= $errorThreshold : saving $alignedFile")
                ImageWriter.write(alignedImage, alignedFile)
                outputFilesAlignment.add(Pair(alignedFile, alignment))
            } else {
                if (saveBad) {
                    val badalignedFile = inputFile.prefixName("${prefixBad}_")
                    println("Error $error > $errorThreshold : saving $badalignedFile")
                    ImageWriter.write(alignedImage, badalignedFile)
                    outputFilesAlignment.add(Pair(badalignedFile, alignment))
                } else {
                    println("Error $error > $errorThreshold : ignoring badly aligned image")
                }
            }

            if (debugMode) {
                val deltaFile = inputFile.prefixName("delta_${prefix}_")
                println("Saving $deltaFile for manual analysis")
                val deltaImage = deltaChannel(baseImage, alignedImage)
                ImageWriter.write(deltaImage, deltaFile)
            }

            println()
        }

        // sort images by error
        if (sort) {
            outputFilesAlignment.sortBy { it.second.error }

            var outputFileIndex = 0
            for (outputFileAlignment in outputFilesAlignment) {
                val fileName = outputFileAlignment.first.name
                val sortString = String.format("_%04d", outputFileIndex)
                val sortedFileName = when {
                    fileName.startsWith(prefix) -> prefix + sortString + fileName.removePrefix(prefix)
                    fileName.startsWith(prefixBad) -> prefixBad + sortString + fileName.removePrefix(prefixBad)
                    else -> sortString + fileName
                }
                println("Renaming $fileName to $sortedFileName")
                Files.move(outputFileAlignment.first.toPath(), File(outputFileAlignment.first.parent, sortedFileName).toPath(), StandardCopyOption.REPLACE_EXISTING)
                outputFileIndex++;
            }

            println()
        }
    }
}
