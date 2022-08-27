package ch.obermuhlner.kimage.awt

import ch.obermuhlner.kimage.image.Channel
import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.MatrixImage
import ch.obermuhlner.kimage.math.mixLinear
import java.awt.Graphics2D
import java.awt.image.BufferedImage

fun Image.drawAwt(func: (Graphics2D) -> Unit): Image {
    val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val gc = img.createGraphics()

    func(gc)

    val red = this[Channel.Red].copy()
    val green = this[Channel.Green].copy()
    val blue = this[Channel.Blue].copy()
    for (y in 0 until height) {
        for (x in 0 until width) {
            val argb = img.getRGB(x, y)
            val a = ((argb shr 24) and 0xff) / 255.0
            val r = ((argb shr 16) and 0xff) / 255.0
            val g = ((argb shr 8) and 0xff) / 255.0
            val b = (argb and 0xff) / 255.0
            red[x, y] = mixLinear(red[x, y], r, a)
            green[x, y] = mixLinear(green[x, y], g, a)
            blue[x, y] = mixLinear(blue[x, y], b, a)
        }
    }

    return MatrixImage(red, green, blue)
}
