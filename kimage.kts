import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import kotlin.math.min
import kotlin.math.max

println("Background removal using median filter + gaussian blur")

val size = max(1, min(input.width, input.height) / 100)
//val size = 5
println("Kernel size = $size")

val background = input.median(size * 2).gaussianBlur(size)
input - background * 0.99

