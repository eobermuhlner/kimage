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
        file("wcsFile") {
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
        int("minObjectSize") {
            description = """
                The minimum size of the zoomed object in pixels.
                """
            default = 50
            unit = "px"
        }
        boolean("ignoreClipped") {
            description = """
                Do not annotate objects that are only partially inside the image.
                """
            default = true
        }
        optionalString("title") {
            description = """
                The title of the annotated image.
                If undefined a title will be created from the brightest objects.
                """
        }
        optionalString("subTitle") {
            description = """
                The sub-title of the annotated image.
                If undefined a sub-title will be created from the center RA/DEC of the image.
                """
        }
        optionalList("whiteList") {
            description = """
                    If not empty then only the objects specified by this list of object names will be shown.    
                    """
            string {
            }
        }
        optionalList("blackList") {
            description = """
                    The objects specified by this list of object names will not be shown.    
                    """
            string {
            }
        }
        string("manualMarker") {
            description = """
                CSV line to define an additional marker.
                Format: name,x,y,size,info1,info2
                """
            default = ""
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
        double("baseFontSize") {
            description = """
                The size of the base font in pixels.
                """
            default = 50.0
            unit = "px"
        }
        double("baseStrokeSize") {
            description = """
                The size of the base stroke in pixels.
                """
            default = 3.0
            unit = "px"
        }
        double("titleFontSizeFactor") {
            description = """
                The factor of the title font size relative to the baseFontSize.
                """
            default = 2.0
        }
        double("markerIndexFontSizeFactor") {
            description = """
                The factor of the marker index font size relative to the baseFontSize.
                """
            default = 0.8
        }
        double("thumbnailLabelFontSizeFactor") {
            description = """
                The factor of the thumbnail label font size relative to the baseFontSize.
                """
            default = 1.2
        }
        double("thumbnailInfoFontSizeFactor") {
            description = """
                The factor of the thumbnail info font size relative to the baseFontSize.
                """
            default = 1.0
        }
        double("thumbnailIndexFontSizeFactor") {
            description = """
                The factor of the thumbnail index font size relative to the baseFontSize.
                """
            default = 0.8
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
        string("thumbnailInfoColor") {
            description = """
                The color of the thumbnail info as hexstring in the form RRGGBB.
                """
            default = "cccccc"
        }
        string("thumbnailIndexColor") {
            description = """
                The color of the thumbnail index as hexstring in the form RRGGBB.
                """
            default = "00cc00"
        }
    }

    single {
        val wcsFile: File by arguments
        val magnitude: Optional<Double> by arguments
        val minObjectSize: Int by arguments
        val title: Optional<String> by arguments
        val subTitle: Optional<String> by arguments
        val whiteList: Optional<List<String>> by arguments
        val blackList: Optional<List<String>> by arguments
        val manualMarker: String by arguments
        val ignoreClipped: Boolean by arguments
        val markerStyle: String by arguments
        val markerLabelStyle: String by arguments
        val thumbnailSize: Int by arguments
        val thumbnailMargin: Int by arguments
        val baseFontSize: Double by arguments
        val baseStrokeSize: Double by arguments
        val titleFontSizeFactor: Double by arguments
        val markerIndexFontSizeFactor: Double by arguments
        val thumbnailLabelFontSizeFactor: Double by arguments
        val thumbnailInfoFontSizeFactor: Double by arguments
        val thumbnailIndexFontSizeFactor: Double by arguments
        val markerRectColor: String by arguments
        val markerIndexColor: String by arguments
        val thumbnailRectColor: String by arguments
        val thumbnailLabelColor: String by arguments
        val thumbnailInfoColor: String by arguments
        val thumbnailIndexColor: String by arguments

        val titleFontSize = baseFontSize * titleFontSizeFactor
        val markerIndexFontSize = baseFontSize * markerIndexFontSizeFactor
        val thumbnailLabelFontSize = baseFontSize * thumbnailLabelFontSizeFactor
        val thumbnailInfoFontSize = baseFontSize * thumbnailInfoFontSizeFactor
        val thumbnailIndexFontSize = baseFontSize * thumbnailIndexFontSizeFactor

        val wcsData = WCSParser.parse(wcsFile)
        val wcsConverter = WCSConverter(wcsData)

        fun arePointsOnSameSide(ra1: Double, dec1: Double, ra2: Double, dec2: Double): Boolean {
            val x1 = cos(Math.toRadians(ra1)) * cos(Math.toRadians(dec1))
            val y1 = sin(Math.toRadians(ra1)) * cos(Math.toRadians(dec1))
            val z1 = sin(Math.toRadians(dec1))

            val x2 = cos(Math.toRadians(ra2)) * cos(Math.toRadians(dec2))
            val y2 = sin(Math.toRadians(ra2)) * cos(Math.toRadians(dec2))
            val z2 = sin(Math.toRadians(dec2))

            val dotProduct = x1 * x2 + y1 * y2 + z1 * z2

            return dotProduct >= 0
        }

        val (centerRa, centerDec) = wcsConverter.convertXYToRADec(inputImage.width/2.0, inputImage.height/2.0)

        val filteredNGCs = mutableListOf<DeepSkyObjects.NGC>()
        for (ngc in DeepSkyObjects.all()) {
            val (x, y) = wcsConverter.convertRADecToXY(ngc.ra, ngc.dec)
            if (x in 0.0..inputImage.width.toDouble() &&
                y in 0.0..inputImage.height.toDouble() &&
                arePointsOnSameSide(ngc.ra, ngc.dec, centerRa, centerDec) &&
                (!whiteList.isPresent || whiteList.get().contains(ngc.name) || whiteList.get().contains(ngc.messierOrName))) {
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
        if (blackList.isPresent) {
            filteredNGCs.removeIf { ngc -> blackList.get().contains(ngc.name) || blackList.get().contains(ngc.messierOrName)}
        }
        filteredNGCs.sortBy { it.mag ?: Double.MAX_VALUE }

        val titleText: String = if(title.isPresent) title.get() else {
            if (filteredNGCs.isNotEmpty()) {
                filteredNGCs[0].mag?.let { firstMag ->
                    filteredNGCs.filter { ngc ->
                        val mag = ngc.mag
                        mag != null && mag < firstMag * 1.25
                    }.map { ngc -> ngc.messierOrName }.joinToString(" ")
                }
            } else ""
        } ?: ""
        val subtitleText = subTitle.orElse("${formatDegreesToHMS(centerRa)} ${formatDegreesToDMS(centerDec)}")

        var titleFontHeight = 0
        var thumbnailLabelFontHeight = 0
        var thumbnailInfoFontHeight = 0
        AwtImageUtil.graphics(inputImage, 0, 0, 0, 0) { graphics, width, height, offsetX, offsetY ->
            graphics.font = graphics.font.deriveFont(titleFontSize.toFloat())
            titleFontHeight = graphics.fontMetrics.height

            graphics.font = graphics.font.deriveFont(thumbnailLabelFontSize.toFloat())
            thumbnailLabelFontHeight = graphics.fontMetrics.height

            graphics.font = graphics.font.deriveFont(thumbnailInfoFontSize.toFloat())
            thumbnailInfoFontHeight = graphics.fontMetrics.height
        }

        val thumbnailColWidth = thumbnailSize + thumbnailMargin
        val thumbnailRowHeight = thumbnailSize + thumbnailMargin + thumbnailLabelFontHeight + thumbnailInfoFontHeight + thumbnailInfoFontHeight
        val thumbnailCols = inputImage.width / thumbnailColWidth
        val thumbnailRows = ceil(filteredNGCs.size.toDouble() / thumbnailCols).toInt()

        val marginTop = thumbnailMargin + titleFontHeight
        val marginLeft = thumbnailMargin
        val marginBottom = thumbnailRows * thumbnailRowHeight + thumbnailMargin
        val marginRight = thumbnailMargin

        fun setAdaptiveFont(graphics: Graphics2D, font: Font, text: String, maxWidth: Int) {
            graphics.font = font
            val width = graphics.fontMetrics.stringWidth(text)
            if (width > maxWidth) {
                val correctedFontSize = font.size.toDouble() * maxWidth / width
                graphics.font = font.deriveFont(correctedFontSize.toFloat())
            }
        }

        data class Marker(
            val name: String,
            val x: Int,
            val y: Int,
            val size: Int,
            val info1: String,
            val info2: String,
            val majAx: Int = size,
            val minAx: Int = size,
            val posAngle: Double = 0.0
        )

        AwtImageUtil.graphics(inputImage, marginTop, marginLeft, marginBottom, marginRight) { graphics, width, height, offsetX, offsetY ->
            val thumbnailStartX = offsetX
            val thumbnailStartY = offsetY + inputImage.height + thumbnailMargin + thumbnailLabelFontHeight + thumbnailInfoFontHeight + thumbnailInfoFontHeight

            var thumbnailX = thumbnailStartX
            var thumbnailY = thumbnailStartY
            var thumbnailIndex = 1

            graphics.stroke = java.awt.BasicStroke(baseStrokeSize.toFloat())
            val titleFont = graphics.font.deriveFont(titleFontSize.toFloat())
            val markerIndexFont = graphics.font.deriveFont(markerIndexFontSize.toFloat())
            val thumbnailLabelFont = graphics.font.deriveFont(thumbnailLabelFontSize.toFloat())
            val thumbnailInfoFont = graphics.font.deriveFont(thumbnailInfoFontSize.toFloat())
            val thumbnailIndexFont = graphics.font.deriveFont(thumbnailIndexFontSize.toFloat())

            val markers = filteredNGCs.map { ngc ->
                val (x, y) = wcsConverter.convertRADecToXY(ngc.ra, ngc.dec)
                val name = ngc.messierOrName
                val info1 = "${ngc.typeEnglish}" + if (ngc.mag != null) " ${ngc.mag}mag" else ""
                val info2 = "${formatDegreesToHMS(ngc.ra)} ${formatDegreesToDMS(ngc.dec)}"
                val pixelX = x.toInt()
                val pixelY = inputImage.height - y.toInt()
                val majAx = ngc?.majAx ?: ngc.minAx
                val minAx = ngc?.minAx ?: ngc.majAx
                val posAngle = ngc?.posAngle ?: 0.0
                val pixelMajAx =
                    if (majAx != null) wcsConverter.convertDegreeToLength(majAx).absoluteValue.toInt() else minObjectSize
                val pixelMinAx =
                    if (minAx != null) wcsConverter.convertDegreeToLength(minAx).absoluteValue.toInt() else minObjectSize
                val pixelSize = if (majAx != null && minAx != null) {
                    max(
                        wcsConverter.convertDegreeToLength(max(majAx, minAx)).absoluteValue.toInt(),
                        minObjectSize
                    )
                } else minObjectSize

                println("${pixelX} ${pixelY} $ngc")
                Marker(name, pixelX, pixelY, pixelSize, info1, info2, pixelMajAx, pixelMinAx, posAngle)
            }.toMutableList()

            if (manualMarker.isNotEmpty()) {
                val markerValues = manualMarker.split(',')
                val name = markerValues.getOrElse(0) { "" }.trim()
                val x = markerValues.getOrElse(1) { (inputImage.width / 2).toString() }.toInt()
                val y = markerValues.getOrElse(2) { (inputImage.height / 2).toString() }.toInt()
                val size = markerValues.getOrElse(3) { "50" }.toInt()
                val info1 = markerValues.getOrElse(4) { "" }.trim()
                val info2 = markerValues.getOrElse(5) {
                    val raDec = wcsConverter.convertXYToRADec(x.toDouble(), y.toDouble())
                    "${formatDegreesToHMS(raDec.first)} ${formatDegreesToDMS(raDec.second)}"
                }.trim()
                markers += Marker(name, x, y, size, info1, info2)
            }

            for (marker in markers) {
                val zoomFactor = thumbnailSize.toDouble() / marker.size.toDouble()

                if (ignoreClipped) {
                    if (marker.x - marker.size /2 < 0 || marker.x + marker.size /2 > inputImage.width || marker.y - marker.size /2 < 0 || marker.y + marker.size /2 > inputImage.height) {
                        continue
                    }
                }

                val crop = AwtImageUtil.toBufferedImage(inputImage.cropCenter(marker.size /2, marker.x, marker.y).scaleTo(thumbnailSize, thumbnailSize))
                if (thumbnailX + thumbnailSize > width) {
                    thumbnailX = thumbnailStartX
                    thumbnailY += thumbnailRowHeight
                }
                graphics.color = java.awt.Color(markerIndexColor.toInt(16))
                graphics.font = markerIndexFont
                val markerLabel = when (markerLabelStyle) {
                    "index" -> thumbnailIndex.toString()
                    "name" -> marker.name
                    "none" -> ""
                    else -> throw IllegalArgumentException("Unknown markerLabelStyle: $markerLabelStyle")
                }
                when (markerStyle) {
                    "square" -> {
                        graphics.drawString(markerLabel, offsetX+marker.x -marker.size /2, offsetY+marker.y -marker.size /2 - graphics.fontMetrics.descent)
                    }
                    else -> {
                        val stringWidth = graphics.fontMetrics.stringWidth(markerLabel)
                        graphics.drawString(markerLabel, offsetX+marker.x - stringWidth/2, offsetY+marker.y -marker.size /2 - graphics.fontMetrics.descent)
                    }
                }

                graphics.color = java.awt.Color(markerRectColor.toInt(16))
                when (markerStyle) {
                    "square" -> {
                        graphics.drawRect(offsetX+marker.x -marker.size /2, offsetY+marker.y -marker.size /2, marker.size, marker.size)
                    }
                    "rect" -> {
                        //graphics.drawRect(pixelX-pixelSize/2, pixelY-pixelSize/2, pixelMajAx, pixelMinAx)
                        val backupTransform = graphics.transform
                        graphics.translate(offsetX+marker.x, offsetY+marker.y)
                        graphics.rotate(Math.toRadians(marker.posAngle))
                        graphics.drawRect(-marker.majAx/2, -marker.minAx/2, marker.majAx, marker.minAx)
                        graphics.transform = backupTransform
                    }
                    "circle" -> {
                        graphics.drawOval(marker.x -marker.size /2, marker.y -marker.size /2, marker.size, marker.size)
                    }
                    "oval" -> {
                        val backupTransform = graphics.transform
                        graphics.translate(offsetX+marker.x, offsetY+marker.y)
                        graphics.rotate(Math.toRadians(marker.posAngle))
                        graphics.drawOval(-marker.majAx/2, -marker.minAx/2, marker.majAx, marker.minAx)
                        graphics.transform = backupTransform
                    }
                    "none" -> {}
                    else -> throw IllegalArgumentException("Unknown markerStyle: $markerStyle")
                }

                graphics.drawImage(crop, thumbnailX, thumbnailY, null)

                graphics.color = java.awt.Color(thumbnailLabelColor.toInt(16))
                setAdaptiveFont(graphics, thumbnailLabelFont, marker.name, thumbnailSize)
                graphics.drawString(marker.name, thumbnailX, thumbnailY - thumbnailInfoFontHeight - thumbnailInfoFontHeight - graphics.fontMetrics.descent)

                graphics.color = java.awt.Color(thumbnailInfoColor.toInt(16))
                setAdaptiveFont(graphics, thumbnailInfoFont, marker.info1, thumbnailSize)
                graphics.drawString(marker.info1, thumbnailX, thumbnailY - graphics.fontMetrics.height - graphics.fontMetrics.descent)
                setAdaptiveFont(graphics, thumbnailInfoFont, marker.info2, thumbnailSize)
                graphics.drawString(marker.info2, thumbnailX, thumbnailY - graphics.fontMetrics.descent)

                if (markerLabelStyle == "index") {
                    graphics.color = java.awt.Color(thumbnailIndexColor.toInt(16))
                    setAdaptiveFont(graphics, thumbnailIndexFont, markerLabel, thumbnailSize/4)
                    graphics.drawString(markerLabel, thumbnailX + baseStrokeSize.roundToInt(), thumbnailY + baseStrokeSize.roundToInt() + graphics.fontMetrics.height)
                }

                graphics.color = java.awt.Color(thumbnailRectColor.toInt(16))
                graphics.drawRect(thumbnailX, thumbnailY, crop.width, crop.height)

                thumbnailX += thumbnailSize + thumbnailMargin
                thumbnailIndex++
            }

            setAdaptiveFont(graphics, titleFont, titleText, inputImage.width)
            var titleWidth = graphics.fontMetrics.stringWidth(titleText)
            var subtitleWidth = inputImage.width - titleWidth
            if (subtitleWidth < inputImage.width / 3) {
                subtitleWidth = inputImage.width / 3
                titleWidth = inputImage.width - subtitleWidth
            }
            graphics.color = java.awt.Color(thumbnailLabelColor.toInt(16))
            setAdaptiveFont(graphics, titleFont, titleText, titleWidth)
            graphics.drawString(titleText, offsetX, offsetY - graphics.fontMetrics.descent)

            graphics.color = java.awt.Color(thumbnailInfoColor.toInt(16))
            setAdaptiveFont(graphics, titleFont, subtitleText, subtitleWidth)
            graphics.drawString(subtitleText, offsetX + inputImage.width - subtitleWidth, offsetY - graphics.fontMetrics.descent)

            graphics.color = java.awt.Color(thumbnailRectColor.toInt(16))
            graphics.drawRect(offsetX, offsetY, inputImage.width, inputImage.height)
        }
    }
}
