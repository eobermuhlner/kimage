import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.javafx.AwtImageUtil
import ch.obermuhlner.util.*
import ch.obermuhlner.astro.*
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.io.*
import java.util.*
import java.util.regex.Pattern
import kotlin.math.*

kimage(0.1) {
    name = "annotate-platesolve"
    title = "Annotate an image using platesolving."
    description = """
                Annotate an image using wcs platesolving result file in wcs format (for example from astap).
                """
    arguments {
        file("wcs") {
            description = """
                The wcs file with the platesolve result.
                See script `platesolve-annotate`.
                """
        }
        optionalDouble("magnitude") {
            description = """
                The limit of the magnitude of deep sky objects to show.
                If no magnitude is specified then all objects are shown.
                """
        }
        string("markerStyle") {
            description = """
                The style of the object marker in the annotated image.
                """
            allowed = listOf("square", "circle", "rect", "oval", "none")
            default = "square"
        }
        string("markerLabelStyle") {
            description = """
                The style of the object marker label in the annotated image.
                """
            allowed = listOf("index", "name", "none")
            default = "index"
        }
        int("minObjectSize") {
            description = """
                The minimum size of the zoomed object in pixels.
                """
            default = 50
            unit = "px"
        }
        int("thumbnailSize") {
            description = """
                The size of the thumbnails in pixels.
                """
            default = 400
            unit = "px"
        }
        int("thumbnailMargin") {
            description = """
                The margin between the thumbnails in pixels.
                """
            default = 20
            unit = "px"
        }
        double("markerIndexFontSize") {
            description = """
                The size of the base fonts in pixels.
                """
            default = 50.0
            unit = "px"
        }
        double("thumbnailIndexFontSize") {
            description = """
                The size of the base fonts in pixels.
                """
            default = 50.0
            unit = "px"
        }
        double("thumbnailLabelFontSize") {
            description = """
                The size of the base fonts in pixels.
                """
            default = 60.0
            unit = "px"
        }
        double("strokeSize") {
            description = """
                The size of the line stroke in pixels.
                """
            default = 3.0
            unit = "px"
        }
        string("markerRectColor") {
            description = """
                The color of the marker rectangle as hexstring in the form RRGGBB.
                """
            default = "008800"
        }
        string("markerIndexColor") {
            description = """
                The color of the marker index label as hexstring in the form RRGGBB.
                """
            default = "00cc00"
        }
        string("thumbnailRectColor") {
            description = """
                The color of the thumbnail label as hexstring in the form RRGGBB.
                """
            default = "ffffff"
        }
        string("thumbnailLabelColor") {
            description = """
                The color of the thumbnail label as hexstring in the form RRGGBB.
                """
            default = "88ff88"
        }
        string("thumbnailIndexColor") {
            description = """
                The color of the thumbnail label as hexstring in the form RRGGBB.
                """
            default = "00cc00"
        }
    }

    single {
        val wcs: File by arguments
        val magnitude: Optional<Double> by arguments
        val minObjectSize: Int by arguments
        val markerStyle: String by arguments
        val markerLabelStyle: String by arguments
        val thumbnailSize: Int by arguments
        val thumbnailMargin: Int by arguments
        val markerIndexFontSize: Double by arguments
        val thumbnailLabelFontSize: Double by arguments
        val thumbnailIndexFontSize: Double by arguments
        val strokeSize: Double by arguments
        val markerRectColor: String by arguments
        val markerIndexColor: String by arguments
        val thumbnailRectColor: String by arguments
        val thumbnailLabelColor: String by arguments
        val thumbnailIndexColor: String by arguments

        val thumbnailInfoFontSize = thumbnailLabelFontSize * 0.5
        val thumbnailInfoColor = thumbnailIndexColor

        val wcsData = WCSParser.parse(wcs)
        val wcsConverter = WCSConverter(wcsData)

        val filteredNGCs = mutableListOf<DeepSkyObjects.NGC>()
        for (ngc in DeepSkyObjects.all()) {
            val (x, y) = wcsConverter.convertRADecToXY(ngc.ra, ngc.dec)
            if (x in 0.0..inputImage.width.toDouble() && y in 0.0..inputImage.height.toDouble()) {
                filteredNGCs += ngc
            }
        }
        if (magnitude.isPresent) {
            filteredNGCs.removeIf { ngc ->
                val mag = ngc.mag
                mag == null || mag > magnitude.get()
            }
        }
        filteredNGCs.removeIf { ngc -> ngc.type == "*" || ngc.type == "Other" }
        filteredNGCs.sortBy { it.mag ?: Double.MAX_VALUE }

        var thumbnailLabelFontHeight = 0
        var thumbnailInfoFontHeight = 0
        AwtImageUtil.graphics(inputImage, 0, 0, 0, 0) { graphics, width, height ->
            graphics.font = graphics.font.deriveFont(thumbnailLabelFontSize.toFloat())
            thumbnailLabelFontHeight = graphics.fontMetrics.height

            graphics.font = graphics.font.deriveFont(thumbnailInfoFontSize.toFloat())
            thumbnailInfoFontHeight = graphics.fontMetrics.height
        }

        val thumbnailStartX = thumbnailMargin
        val thumbnailStartY = inputImage.height + thumbnailMargin + thumbnailLabelFontHeight + thumbnailInfoFontHeight + thumbnailInfoFontHeight
        val thumbnailColWidth = thumbnailSize + thumbnailMargin
        val thumbnailRowHeight = thumbnailSize + thumbnailMargin + thumbnailLabelFontHeight + thumbnailInfoFontHeight + thumbnailInfoFontHeight
        val thumbnailCols = inputImage.width / thumbnailColWidth
        val thumbnailRows = ceil(filteredNGCs.size.toDouble() / thumbnailCols).toInt()

        var thumbnailX = thumbnailStartX
        var thumbnailY = thumbnailStartY
        var thumbnailIndex = 1

        val marginBottom = thumbnailRows * thumbnailRowHeight + thumbnailMargin

        fun setAdaptiveFont(graphics: Graphics2D, font: Font, text: String, maxWidth: Int) {
            graphics.font = font
            val width = graphics.fontMetrics.stringWidth(text)
            if (width > maxWidth) {
                val correctedFontSize = font.size.toDouble() * maxWidth / width
                graphics.font = font.deriveFont(correctedFontSize.toFloat())
            }
        }

        AwtImageUtil.graphics(inputImage, 0, 0, marginBottom, 0) { graphics, width, height ->
            graphics.stroke = java.awt.BasicStroke(strokeSize.toFloat())
            val markerIndexFont = graphics.font.deriveFont(markerIndexFontSize.toFloat())
            val thumbnailLabelFont = graphics.font.deriveFont(thumbnailLabelFontSize.toFloat())
            val thumbnailInfoFont = graphics.font.deriveFont(thumbnailInfoFontSize.toFloat())
            val thumbnailIndexFont = graphics.font.deriveFont(thumbnailIndexFontSize.toFloat())

            for (ngc in filteredNGCs) {
                val (x, y) = wcsConverter.convertRADecToXY(ngc.ra, ngc.dec)
                val name = if (ngc.messier != null) "M${ngc.messier}" else ngc.name
                val info1 = "${ngc.typeEnglish}" + if (ngc.mag != null) " ${ngc.mag}mag" else ""
                val info2 = "${formatDegreesToHMS(ngc.ra)} ${formatDegreesToDMS(ngc.dec)}"
                val pixelX = x.toInt()
                val pixelY = inputImage.height - y.toInt()
                val majAx = ngc?.majAx ?: ngc.minAx
                val minAx = ngc?.minAx ?: ngc.majAx
                val pixelMajAx = if (majAx != null) wcsConverter.convertDegreeToLength(majAx).absoluteValue.toInt() else minObjectSize
                val pixelMinAx = if (minAx != null) wcsConverter.convertDegreeToLength(minAx).absoluteValue.toInt() else minObjectSize
                val pixelSize = if (majAx != null && minAx != null) {
                    max(wcsConverter.convertDegreeToLength(max(majAx, minAx)).absoluteValue.toInt(), minObjectSize)
                } else minObjectSize
                val zoomFactor = thumbnailSize.toDouble() / pixelSize.toDouble()

                println("$pixelX $pixelY $ngc")
                //println("zoom-center \"$name\" $pixelX $pixelY $pixelSize $zoomFactor")

                val crop = AwtImageUtil.toBufferedImage(inputImage.cropCenter(pixelSize/2, pixelX, pixelY).scaleTo(thumbnailSize, thumbnailSize))
                if (thumbnailX + thumbnailSize > width) {
                    thumbnailX = thumbnailStartX
                    thumbnailY += thumbnailRowHeight
                }
                graphics.color = java.awt.Color(markerIndexColor.toInt(16))
                graphics.font = markerIndexFont
                val markerLabel = when (markerLabelStyle) {
                    "index" -> thumbnailIndex.toString()
                    "name" -> name
                    "none" -> ""
                    else -> throw IllegalArgumentException("Unknown markerLabelStyle: $markerLabelStyle")
                }
                when (markerStyle) {
                    "square" -> {
                        graphics.drawString(markerLabel, pixelX-pixelSize/2, pixelY-pixelSize/2 - graphics.fontMetrics.descent)
                    }
                    else -> {
                        val stringWidth = graphics.fontMetrics.stringWidth(markerLabel)
                        graphics.drawString(markerLabel, pixelX - stringWidth/2, pixelY-pixelSize/2 - graphics.fontMetrics.descent)
                    }
                }

                graphics.color = java.awt.Color(markerRectColor.toInt(16))
                when (markerStyle) {
                    "square" -> {
                        graphics.drawRect(pixelX-pixelSize/2, pixelY-pixelSize/2, pixelSize, pixelSize)
                    }
                    "rect" -> {
                        //graphics.drawRect(pixelX-pixelSize/2, pixelY-pixelSize/2, pixelMajAx, pixelMinAx)
                        val backupTransform = graphics.transform
                        graphics.translate(pixelX, pixelY)
                        ngc.posAngle?.let {// TODO use angle corrected to projection
                            graphics.rotate(Math.toRadians(it))
                        }
                        graphics.drawRect(-pixelMajAx/2, -pixelMinAx/2, pixelMajAx, pixelMinAx)
                        graphics.transform = backupTransform
                    }
                    "circle" -> {
                        graphics.drawOval(pixelX-pixelSize/2, pixelY-pixelSize/2, pixelSize, pixelSize)
                    }
                    "oval" -> {
                        val backupTransform = graphics.transform
                        graphics.translate(pixelX, pixelY)
                        ngc.posAngle?.let {// TODO use angle corrected to projection
                            graphics.rotate(Math.toRadians(it))
                        }
                        graphics.drawOval(-pixelMajAx/2, -pixelMinAx/2, pixelMajAx, pixelMinAx)
                        graphics.transform = backupTransform
                    }
                    "none" -> {}
                    else -> throw IllegalArgumentException("Unknown markerStyle: $markerStyle")
                }

                graphics.drawImage(crop, thumbnailX, thumbnailY, null)

                graphics.color = java.awt.Color(thumbnailLabelColor.toInt(16))
                setAdaptiveFont(graphics, thumbnailLabelFont, name, thumbnailSize)
                graphics.drawString(name, thumbnailX, thumbnailY - thumbnailInfoFontHeight - thumbnailInfoFontHeight - graphics.fontMetrics.descent)

                graphics.color = java.awt.Color(thumbnailInfoColor.toInt(16))
                setAdaptiveFont(graphics, thumbnailInfoFont, info1, thumbnailSize)
                graphics.drawString(info1, thumbnailX, thumbnailY - graphics.fontMetrics.height - graphics.fontMetrics.descent)
                setAdaptiveFont(graphics, thumbnailInfoFont, info2, thumbnailSize)
                graphics.drawString(info2, thumbnailX, thumbnailY - graphics.fontMetrics.descent)

                if (markerLabelStyle == "index") {
                    graphics.color = java.awt.Color(thumbnailIndexColor.toInt(16))
                    setAdaptiveFont(graphics, thumbnailIndexFont, markerLabel, thumbnailSize/4)
                    graphics.drawString(markerLabel, thumbnailX + strokeSize.roundToInt(), thumbnailY + strokeSize.roundToInt() + graphics.fontMetrics.height)
                }

                graphics.color = java.awt.Color(thumbnailRectColor.toInt(16))
                graphics.drawRect(thumbnailX, thumbnailY, crop.width, crop.height)

                thumbnailX += thumbnailSize + thumbnailMargin
                thumbnailIndex++
            }
        }
    }
}
