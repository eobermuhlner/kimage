import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.javafx.AwtImageUtil
import ch.obermuhlner.util.SimpleTokenizer
import java.awt.BasicStroke
import java.awt.Color
import java.io.*
import java.util.*
import java.util.regex.Pattern
import kotlin.math.*

kimage(0.1) {
    name = "annotate"
    title = "Annotate an image with text and drawings."
    description = """
                Annotate an image with text and drawings as specified in an annotations script.
                """
    arguments {
        file("annotation") {
            description = """
                The commands to annotate.
                """
        }
        int("marginTop") {
            description = """
                The top margin.
                """
            default = 0
        }
        int("marginLeft") {
            description = """
                The left margin.
                """
            default = 0
        }
        int("marginBottom") {
            description = """
                The bottom margin.
                """
            default = 0
        }
        int("marginRight") {
            description = """
                The right margin.
                """
            default = 0
        }
    }

    single {
        val annotation: File by arguments
        val marginTop: Int by arguments
        val marginLeft: Int by arguments
        val marginBottom: Int by arguments
        val marginRight: Int by arguments

        AwtImageUtil.graphics(inputImage, marginTop, marginLeft, marginBottom, marginRight) { graphics, width, height ->
            var zoomTargetStartX = 0
            var zoomTargetX = 0
            var zoomTargetY = 0
            var zoomTargetStep = 0
            var zoomTargetRowHeight = 0
            var zoomTargetIndex = 1
            for (line in annotation.readLines()) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue
                }
                val tokens = SimpleTokenizer.tokenize(line)
                if (tokens.isNotEmpty()) {
                    when(tokens[0]) {
                        "color" -> {
                            graphics.color = java.awt.Color(tokens[1].toInt(16))
                        }
                        "stroke" -> {
                            graphics.stroke = java.awt.BasicStroke(tokens[1].toFloat())
                        }
                        "line" -> {
                            val x1 = tokens[1].toInt()
                            val y1 = tokens[2].toInt()
                            val x2 = tokens[3].toInt()
                            val y2 = tokens[4].toInt()
                            graphics.drawLine(x1, y1, x2, y2)
                        }
                        "rect" -> {
                            val x = tokens[1].toInt()
                            val y = tokens[2].toInt()
                            val w = tokens[3].toInt()
                            val h = tokens[4].toInt()
                            graphics.drawRect(x, y, w, h)
                        }
                        "rect-center" -> {
                            val x = tokens[1].toInt()
                            val y = tokens[2].toInt()
                            val radius = tokens[3].toInt()
                            graphics.drawRect(x-radius/2, y-radius/2, radius, radius)
                        }
                        "font-size" -> {
                            val size = tokens[1].toFloat()
                            graphics.font = graphics.font.deriveFont(size)
                        }
                        "text" -> {
                            val x = tokens[1].toInt()
                            val y = tokens[2].toInt()
                            val text = tokens.subList(3, tokens.size-1).joinToString(" ")
                            graphics.drawString(text, x, y)
                        }
                        "zoom-target" -> {
                            zoomTargetStartX = tokens[1].toInt()
                            zoomTargetX = zoomTargetStartX
                            zoomTargetY = tokens[2].toInt()
                            zoomTargetStep = tokens[3].toInt()
                        }
                        "zoom-center" -> {
                            val name = tokens[1]
                            val x = tokens[2].toInt()
                            val y = tokens[3].toInt()
                            val radius = tokens[4].toInt()
                            val scale = tokens[5].toDouble()
                            val crop = AwtImageUtil.toBufferedImage(inputImage.cropCenter(radius, x, y).scaleBy(scale, scale))
                            if (zoomTargetX + crop.width > width) {
                                zoomTargetX = zoomTargetStartX
                                zoomTargetY += zoomTargetRowHeight + zoomTargetStep + graphics.fontMetrics.height
                                zoomTargetRowHeight = 0
                            }
                            graphics.drawString(zoomTargetIndex.toString(), x-radius, y-radius)
                            graphics.drawRect(x-radius, y-radius, radius*2, radius*2)
                            graphics.drawImage(crop, zoomTargetX, zoomTargetY, null)
                            graphics.drawString(name, zoomTargetX, zoomTargetY)
                            graphics.drawString(zoomTargetIndex.toString(), zoomTargetX, zoomTargetY+graphics.fontMetrics.height)
                            graphics.drawRect(zoomTargetX, zoomTargetY, crop.width, crop.height)
                            zoomTargetX += crop.width + zoomTargetStep
                            zoomTargetRowHeight = max(zoomTargetRowHeight, crop.height)
                            zoomTargetIndex++
                        }
                        else ->
                            throw java.lang.IllegalArgumentException("Unknown annotation: $line")
                    }

                }
            }
        }
    }
}
