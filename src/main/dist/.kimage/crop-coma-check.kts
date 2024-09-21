import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.matrix.*
import java.io.*
import java.util.*
import kotlin.math.*

kimage(0.1) {
    name = "crop-coma-check"
    title = "Check coma in astrophotography"
    description = """
                Check coma in astrophotography by zooming into 9 areas (center, 4 corners, 4 sides). 
                """
    arguments {
        int("areaSize") {
            description = """
                        The size of a single area in the source image.
                        """
            default = 100
        }
        double("zoom") {
            description = """
                        The zoom factor
                        """
            default = 2.0
        }
    }

    single {
        var areaSize: Int by arguments
        var zoom: Double by arguments

        val targetAreaSize = (areaSize * zoom).toInt()

        val targetImageSize = targetAreaSize * 3
        val outputImage = MatrixImage(targetImageSize, targetImageSize, inputImage.channels) { _, width, height ->
            DoubleMatrix(width, height) { _, _ ->
                0.0
            }
        }

        fun copyImageArea(inputImage: Image, outputImage: Image, sourceX: Int, sourceY: Int, targetX: Int, targetY: Int, areaSize: Int) {
            val areaImage = inputImage.crop(sourceX, sourceY, areaSize, areaSize).scaleBy(zoom, zoom)
            for (y in 0 .. areaImage.height) {
                for (x in 0 .. areaImage.width) {
                    for (channel in inputImage.channels) {
                        outputImage[channel][x + targetX, y + targetY] = areaImage[channel][x, y]
                    }
                }
            }
        }

        val sourceLeftX = 0
        val sourceCenterX = (inputImage.width - areaSize) / 2
        val sourceRightX = (inputImage.width - areaSize)
        val sourceTopY = 0
        val sourceCenterY = (inputImage.height - areaSize) / 2
        val sourceBottomY = (inputImage.height - areaSize)

        val targetLeftX = 0
        val targetCenterX = (outputImage.width - targetAreaSize) / 2
        val targetRightX = (outputImage.width - targetAreaSize)
        val targetTopY = 0
        val targetCenterY = (outputImage.height - targetAreaSize) / 2
        val targetBottomY = (outputImage.height - targetAreaSize)

        copyImageArea(inputImage, outputImage, sourceLeftX, sourceTopY, targetLeftX, targetTopY, areaSize)
        copyImageArea(inputImage, outputImage, sourceCenterX, sourceTopY, targetCenterX, targetTopY, areaSize)
        copyImageArea(inputImage, outputImage, sourceRightX, sourceTopY, targetRightX, targetTopY, areaSize)
        copyImageArea(inputImage, outputImage, sourceLeftX, sourceCenterY, targetLeftX, targetCenterY, areaSize)
        copyImageArea(inputImage, outputImage, sourceCenterX, sourceCenterY, targetCenterX, targetCenterY, areaSize)
        copyImageArea(inputImage, outputImage, sourceRightX, sourceCenterY, targetRightX, targetCenterY, areaSize)
        copyImageArea(inputImage, outputImage, sourceLeftX, sourceBottomY, targetLeftX, targetBottomY, areaSize)
        copyImageArea(inputImage, outputImage, sourceCenterX, sourceBottomY, targetCenterX, targetBottomY, areaSize)
        copyImageArea(inputImage, outputImage, sourceRightX, sourceBottomY, targetRightX, targetBottomY, areaSize)

        outputImage
    }
}
