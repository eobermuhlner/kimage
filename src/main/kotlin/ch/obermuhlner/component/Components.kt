package ch.obermuhlner.component

import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.ImageReader
import ch.obermuhlner.kimage.io.ImageWriter
import ch.obermuhlner.kimage.math.Histogram
import ch.obermuhlner.kimage.math.SplineInterpolator
import ch.obermuhlner.kimage.math.clamp
import ch.obermuhlner.kimage.matrix.DoubleMatrix
import ch.obermuhlner.kimage.matrix.Matrix
import ch.obermuhlner.util.StreamGobbler
import java.io.File
import java.lang.RuntimeException
import java.nio.file.Files
import java.util.concurrent.Executors
import kotlin.math.*

enum class ValueType {
    Any,
    Boolean,
    Int,
    Double,
    String,
    File,
    Image,
    Matrix
}


data class FieldType(
    val type: ValueType,
    val optional: Boolean = false,
    val list: Boolean = false)

data class ComponentType(
    val name: String,
    val input: Map<String, FieldType>,
    val output: Map<String, FieldType>)

data class ComponentField(val component: Component, val name: String)

abstract class Component(
    val type: ComponentType,
    val name: String = toComponentName(type.name)) {
    protected val input: MutableMap<String, Any?> = mutableMapOf()
    private var dirty = false
    public val output: MutableMap<String, MutableList<ComponentField>> = mutableMapOf()

    fun isDirty() = dirty

    fun setInput(name: String, value: Any?) {
        if (!input.containsKey(name) || (value != input[name])) {
            input[name] = value
            dirty = true
        }
    }

    fun connectOutput(outputName: String, component: Component, name: String) {
        if (!type.output.containsKey(outputName)) {
            throw java.lang.IllegalArgumentException("${type.name} output not found: $outputName")
        }
        if (!component.type.input.containsKey(name)) {
            throw java.lang.IllegalArgumentException("${type.name} input not found: $outputName")
        }
        val outputType = type.output[outputName]!!.type
        val inputType = component.type.input[name]!!.type
        if (outputType != ValueType.Any && outputType != inputType) {
            throw java.lang.IllegalArgumentException("${type.name} output $outputName type $outputType does not match ${component.type.name} input $name type $inputType")
        }
        output.computeIfAbsent(outputName) { mutableListOf() }.add(ComponentField(component, name))
        dirty = true
    }

    protected fun setOutput(name: String, value: Any?) {
        val outputFields = output[name]
        if (outputFields != null) {
            for(outputField in outputFields) {
                println("  setting ${outputField.component.type.name}.${outputField.name} = $value")
                outputField.component.setInput(outputField.name, value)
            }
        }
    }

    protected fun hasOutput(name: String): Boolean {
        return output.containsKey(name)
    }

    fun run() {
        if (dirty) {
            println("Running ${type.name} $name $input")
            val startMillis = System.currentTimeMillis()
            runInternal()
            val endMillis = System.currentTimeMillis()
            val deltaMillis = endMillis - startMillis
            println("  in $deltaMillis ms")
            dirty = false
        }
    }

    abstract fun runInternal()

    companion object {
        private var index = 1;
        private fun toComponentName(name: String) = name.replaceFirstChar { it.lowercase() } + index++
    }
}

abstract class SimpleValueConverterComponent<T1, T2>(
    inputType: ValueType,
    initialInputValue: T1? = null,
    outputType: ValueType,
    val convertFunc: (T1) -> T2,
    typeName: String = "${inputType.name}-${outputType.name}"
) : Component(
    ComponentType(
        name = typeName,
        input = mapOf(value to FieldType(inputType, optional = true)),
        output = mapOf(result to FieldType(outputType)))) {

    init {
        input[value] = initialInputValue
    }

    override fun runInternal() {
        val value: T1? by input

        value?.let {
            setOutput(result, convertFunc(it))
        }
    }

    companion object {
        const val value: String = "value"
        const val result: String = "result"
    }
}

abstract class SimpleValueComponent<T>(type: ValueType, initialValue: T? = null) : SimpleValueConverterComponent<T, T>(type, initialValue, type, { it }, type.name)

class ImageFileReaderComponent(initialValue: File? = null) : SimpleValueConverterComponent<File, Image>(ValueType.File, initialValue, ValueType.Image, { ImageReader.read(it) }) {
    companion object {
        const val value: String = SimpleValueConverterComponent.value
        const val result: String = SimpleValueConverterComponent.result
    }
}

class IntValueComponent(initialValue: Int? = null) : SimpleValueComponent<Int>(ValueType.Int, initialValue) {
    companion object {
        const val value: String = SimpleValueConverterComponent.value
        const val result: String = SimpleValueConverterComponent.result
    }
}

class DoubleValueComponent(initialValue: Double? = null) : SimpleValueComponent<Double>(ValueType.Double, initialValue) {
    companion object {
        const val value: String = SimpleValueConverterComponent.value
        const val result: String = SimpleValueConverterComponent.result
    }
}

class ImageValueComponent(initialValue: Image? = null) : SimpleValueComponent<Image>(ValueType.Image, initialValue) {
    companion object {
        const val value: String = SimpleValueConverterComponent.value
        const val result: String = SimpleValueConverterComponent.result
    }
}
class FileValueComponent(initialValue: File? = null) : SimpleValueComponent<File>(ValueType.File, initialValue) {
    companion object {
        const val value: String = SimpleValueConverterComponent.value
        const val result: String = SimpleValueConverterComponent.result
    }
}

class ImageRemoveBackground4Component : Component(
    ComponentType(
        name = "RemoveBackground4",
        input = mapOf(
            x1 to FieldType(ValueType.Int),
            y1 to FieldType(ValueType.Int),
            x2 to FieldType(ValueType.Int),
            y2 to FieldType(ValueType.Int),
            x3 to FieldType(ValueType.Int),
            y3 to FieldType(ValueType.Int),
            x4 to FieldType(ValueType.Int),
            y4 to FieldType(ValueType.Int),
            medianRadius to FieldType(ValueType.Int),
            inputImage to FieldType(ValueType.Image)),
        output = mapOf(
            result to FieldType(ValueType.Image),
            background to FieldType(ValueType.Image)
        ))) {

    init {
        input[medianRadius] = 50
    }

    override fun runInternal() {
        val x1: Int by input
        val y1: Int by input
        val x2: Int by input
        val y2: Int by input
        val x3: Int by input
        val y3: Int by input
        val x4: Int by input
        val y4: Int by input
        val medianRadius: Int by input
        val inputImage: Image by input

        val points = listOf(x1 to y1, x2 to y2, x3 to y3, x4 to y4)
        var backgroundImage = inputImage.interpolate(points, medianRadius = medianRadius)
        val resultImage = inputImage - backgroundImage

        setOutput(background, backgroundImage)
        setOutput(result, resultImage)
    }

    companion object {
        const val x1: String = "x1"
        const val y1: String = "y1"
        const val x2: String = "x2"
        const val y2: String = "y2"
        const val x3: String = "x3"
        const val y3: String = "y3"
        const val x4: String = "x4"
        const val y4: String = "y4"
        const val medianRadius: String = "medianRadius"
        const val inputImage: String = "inputImage"
        const val result: String = "result"
        const val background: String = "background"
    }
}

class ImageColorStretchLinearComponent : Component(
    ComponentType(
        name = "ColorStretchLinear",
        input = mapOf(
            low to FieldType(ValueType.Double),
            high to FieldType(ValueType.Double),
            channel to FieldType(ValueType.String),
            inputImage to FieldType(ValueType.Image)),
        output = mapOf(
            result to FieldType(ValueType.Image),
        ))) {

    init {
        input[low] = 0.001
        input[high] = 0.999
        input[channel] = "RGB"
    }

    override fun runInternal() {
        val low: Double by input
        val high: Double by input
        val channel: String by input
        val inputImage: Image by input

        val channels = when (channel) {
            "Gray" -> listOf(Channel.Gray)
            "Luminance" -> listOf(Channel.Luminance)
            "Red" -> listOf(Channel.Red)
            "Green" -> listOf(Channel.Green)
            "Blue" -> listOf(Channel.Blue)
            "RGB" -> listOf(Channel.Red, Channel.Green, Channel.Blue)
            else -> throw IllegalArgumentException("Unknown channel: $channel")
        }

        val histogram = Histogram()
        for (measureChannel in channels) {
            histogram.add(inputImage[measureChannel])
        }

        val lowValue = histogram.estimatePercentile(low)
        val highValue = histogram.estimatePercentile(high)

        val range = highValue - lowValue

        setOutput(outputLowValue, lowValue)
        setOutput(outputHighValue, highValue)

        val outputMatrices = mutableMapOf<Channel, Matrix>()
        for (processChannel in inputImage.channels) {
            val matrix = inputImage[processChannel]

            val m = matrix.create()
            for (y in 0 until matrix.height) {
                for (x in 0 until matrix.width) {
                    val value = matrix[x, y]
                    m[x, y] = (value - lowValue) / range
                }
            }

            outputMatrices[processChannel] = m
        }

        val resultImage = MatrixImage(inputImage.width, inputImage.height, inputImage.channels) { channel, _, _ -> outputMatrices[channel]!! }
        setOutput(result, resultImage)
    }

    companion object {
        const val low: String = "low"
        const val high: String = "high"
        const val channel: String = "channel"
        const val inputImage: String = "inputImage"

        const val outputLowValue: String = "lowValue"
        const val outputHighValue: String = "highValue"
        const val result: String = "result"
    }
}

class ImageColorStretchSigmoidCurveComponent : Component(
    ComponentType(
        name = "ColorStretchSigmoidCurve",
        input = mapOf(
            power to FieldType(ValueType.Double),
            midpoint to FieldType(ValueType.Double),
            inputImage to FieldType(ValueType.Image)),
        output = mapOf(
            result to FieldType(ValueType.Image),
        ))) {

    init {
        input[power] = 2.0
        input[midpoint] = 0.05
    }

    override fun runInternal() {
        val power: Double by input
        val midpoint: Double by input
        val inputImage: Image by input

        val r = -ln(2.0) / ln(midpoint)

        val func: (Double) -> Double = { x ->
            1.0/(1.0+(x.pow(r)/(1-x.pow(r))).pow(-power))
        }

        val resultImage = MatrixImage(inputImage.width, inputImage.height, inputImage.channels) { channel, width, height ->
            DoubleMatrix(width, height) { x, y -> clamp(func(inputImage[channel][x, y]), 0.0, 1.0) }
        }
        setOutput(result, resultImage)
    }

    companion object {
        const val power: String = "power"
        const val midpoint: String = "midpoint"
        const val inputImage: String = "inputImage"

        const val result: String = "result"
    }
}

class ImageColorStretchCurveComponent : Component(
    ComponentType(
        name = "ColorStretchCurve",
        input = mapOf(
            low to FieldType(ValueType.Double),
            high to FieldType(ValueType.Double),
            brightness to FieldType(ValueType.Double),
            curve to FieldType(ValueType.String),
            inputImage to FieldType(ValueType.Image)),
        output = mapOf(
            result to FieldType(ValueType.Image),
        ))) {

    init {
        input[low] = 0.0
        input[high] = 1.0
        input[brightness] = 2.0
        input[curve] = "s-curve-super-strong"
    }

    override fun runInternal() {
        val low: Double by input
        val high: Double by input
        val brightness: Double by input
        val curve: String by input
        val inputImage: Image by input
        val repeat = 1

        val (power1, power2) = if (brightness < 1000.0) {
            Pair(brightness, 1.0)
        } else {
            Pair(brightness.pow(1.0 / 5.0), 5.0)
        }

        var image = inputImage

        if (power1 != 1.0) {
            image = image.onEach { v -> (v/high - low).pow(1.0 / power1) }
        }
        if (power2 != 1.0) {
            image = image.onEach { v -> (v/high - low).pow(1.0 / power2) }
        }

        val (curvePointsX, curvePointsY) = when (curve) {
            "linear" -> {
                Pair(
                    listOf(0.0, 1.0),
                    listOf(0.0, 1.0)
                )
            }

            "s-curve" -> {
                Pair(
                    listOf(0.0, 0.3, 0.7, 1.0),
                    listOf(0.0, 0.2, 0.8, 1.0)
                )
            }

            "s-curve-bright" -> {
                Pair(
                    listOf(0.0, 0.2, 0.7, 1.0),
                    listOf(0.0, 0.18, 0.8, 1.0)
                )
            }

            "s-curve-dark" -> {
                Pair(
                    listOf(0.0, 0.3, 0.7, 1.0),
                    listOf(0.0, 0.2, 0.72, 1.0)
                )
            }

            "s-curve-strong" -> {
                Pair(
                    listOf(0.0, 0.2, 0.8, 1.0),
                    listOf(0.0, 0.1, 0.9, 1.0)
                )
            }

            "s-curve-super-strong" -> {
                Pair(
                    listOf(0.0, 0.2, 0.8, 1.0),
                    listOf(0.0, 0.05, 0.95, 1.0)
                )
            }

            "s-curve-extreme" -> {
                Pair(
                    listOf(0.0, 0.2, 0.8, 1.0),
                    listOf(0.0, 0.01, 0.99, 1.0)
                )
            }

            "bright+" -> {
                Pair(
                    listOf(0.0, 0.6, 1.0),
                    listOf(0.0, 0.7, 1.0)
                )
            }

            "dark+" -> {
                Pair(
                    listOf(0.0, 0.4, 1.0),
                    listOf(0.0, 0.5, 1.0)
                )
            }

            "bright-" -> {
                Pair(
                    listOf(0.0, 0.6, 1.0),
                    listOf(0.0, 0.5, 1.0)
                )
            }

            "dark-" -> {
                Pair(
                    listOf(0.0, 0.4, 1.0),
                    listOf(0.0, 0.3, 1.0)
                )
            }

            else -> throw IllegalArgumentException("Unknown curve: $curve")
        }

        for (i in 1 .. repeat) {
            val spline: SplineInterpolator =
                SplineInterpolator.createMonotoneCubicSpline(curvePointsX, curvePointsY)

            image = image.onEach { v -> spline.interpolate(v) }
        }

        setOutput(result, image)
    }

    companion object {
        const val low: String = "low"
        const val high: String = "high"
        const val brightness: String = "brightness"
        const val curve: String = "curve"
        const val inputImage: String = "inputImage"

        const val result: String = "result"
    }
}

class ImageErodeComponent(initialStrength: Double = 1.0, initialRepeat: Int = 1) : Component(
    ComponentType(
        name = "Erode",
        input = mapOf(
            strength to FieldType(ValueType.Double),
            repeat to FieldType(ValueType.Int),
            inputImage to FieldType(ValueType.Image)),
        output = mapOf(
            result to FieldType(ValueType.Image),
        ))) {

    init {
        input[strength] = initialStrength
        input[repeat] = initialRepeat
    }

    override fun runInternal() {
        val inputImage: Image by input
        val strength: Double by input
        val repeat: Int by input

        val kernel3 = Matrix.matrixOf(3, 3,
            0.0, 1.0, 0.0,
            1.0, 1.0, 1.0,
            0.0, 1.0, 0.0)
        val kernel5 = Matrix.matrixOf(5, 5,
            0.0, 1.0, 1.0, 1.0, 0.0,
            1.0, 1.0, 1.0, 1.0, 1.0,
            1.0, 1.0, 1.0, 1.0, 1.0,
            1.0, 1.0, 1.0, 1.0, 1.0,
            0.0, 1.0, 1.0, 1.0, 0.0)
        val resultImage = inputImage.erode(kernel3, strength, repeat)

        setOutput(result, resultImage)
    }

    companion object {
        const val strength: String = "strength"
        const val repeat: String = "repeat"
        const val inputImage: String = "inputImage"

        const val result: String = "result"
    }
}

class ImageChannelMatrixComponent : Component(
    ComponentType(
        name = "ImageChannelMatrix",
        input = mapOf(
            inputImage to FieldType(ValueType.Image)),
        output = mapOf(
            red to FieldType(ValueType.Matrix),
            green to FieldType(ValueType.Matrix),
            blue to FieldType(ValueType.Matrix),
            gray to FieldType(ValueType.Matrix),
            luminance to FieldType(ValueType.Matrix),
            brightness to FieldType(ValueType.Matrix),
            saturation to FieldType(ValueType.Matrix),
            alpha to FieldType(ValueType.Matrix),
            u to FieldType(ValueType.Matrix),
            v to FieldType(ValueType.Matrix),
            y to FieldType(ValueType.Matrix),
        ))) {

    override fun runInternal() {
        val inputImage: Image by input

        setChannelOutput(red, inputImage)
        setChannelOutput(green, inputImage)
        setChannelOutput(blue, inputImage)
        setChannelOutput(gray, inputImage)
        setChannelOutput(luminance, inputImage)
        setChannelOutput(brightness, inputImage)
        setChannelOutput(saturation, inputImage)
        setChannelOutput(alpha, inputImage)
        setChannelOutput(u, inputImage)
        setChannelOutput(v, inputImage)
        setChannelOutput(y, inputImage)
    }

    private fun setChannelOutput(channel: String, inputImage: Image) {
        if (hasOutput(channel)) {
            val channelName = channel.replaceFirstChar { it.uppercase() }
            setOutput(channel, inputImage[Channel.valueOf(channelName)])
        }
    }

    companion object {
        const val inputImage: String = "inputImage"

        const val red: String = "red"
        const val green: String = "green"
        const val blue: String = "blue"
        const val gray: String = "gray"
        const val luminance: String = "luminance"
        const val brightness: String = "brightness"
        const val saturation: String = "saturation"
        const val alpha: String = "alpha"
        const val u: String = "u"
        const val v: String = "v"
        const val y: String = "y"
    }
}

class ImageFileWriterComponent(initialOutputFile: File? = null) : Component(
    ComponentType(
        name = "ImageFileWriter",
        input = mapOf(
            inputImage to FieldType(ValueType.Image),
            outputFile to FieldType(ValueType.File)),
        output = mapOf(
        ))) {

    init {
        input[outputFile] = initialOutputFile
    }

    override fun runInternal() {
        val inputImage: Image by input
        val outputFile: File by input

        ImageWriter.write(inputImage, outputFile)
    }

    companion object {
        const val inputImage: String = "inputImage"
        const val outputFile: String = "outputFile"
    }
}

class DoubleAddComponent : Component(
    ComponentType(
        name = "DoubleAdd",
        input = mapOf(
            a to FieldType(ValueType.Double),
            b to FieldType(ValueType.Double)),
        output = mapOf(
            result to FieldType(ValueType.Double)))) {

    override fun runInternal() {
        val a: Double by input
        val b: Double by input
        setOutput(result, a + b)
    }

    companion object {
        const val a: String = "a"
        const val b: String = "b"

        const val result: String = "result"
    }
}

class ImageAddComponent : Component(
    ComponentType(
        name = "ImageAdd",
        input = mapOf(
            a to FieldType(ValueType.Image),
            b to FieldType(ValueType.Image)),
        output = mapOf(
            result to FieldType(ValueType.Image)))) {

    override fun runInternal() {
        val a: Image by input
        val b: Image by input
        setOutput(result, a + b)
    }

    companion object {
        const val a: String = "a"
        const val b: String = "b"

        const val result: String = "result"
    }
}

class ImageMultiplyDoubleComponent : Component(
    ComponentType(
        name = "ImageMultiplyDouble",
        input = mapOf(
            a to FieldType(ValueType.Image),
            b to FieldType(ValueType.Double)),
        output = mapOf(
            result to FieldType(ValueType.Image)))) {

    override fun runInternal() {
        val a: Image by input
        val b: Double by input
        setOutput(result, a * b)
    }

    companion object {
        const val a: String = "a"
        const val b: String = "b"

        const val result: String = "result"
    }
}

class ImageMaskMergeComponent() : Component(
    ComponentType(
        name = "ImageMaskMerge",
        input = mapOf(
            a to FieldType(ValueType.Image),
            b to FieldType(ValueType.Image),
            mask to FieldType(ValueType.Matrix)),
        output = mapOf(
            result to FieldType(ValueType.Image)))) {

    override fun runInternal() {
        val a: Image by input
        val b: Image by input
        val mask: Matrix by input

        val resultImage = MatrixImage(a.width, a.height, a.channels) { channel, width, height ->
            DoubleMatrix(width, height) { x, y ->
                val aa = a[channel][x, y]
                val bb = b[channel][x, y]
                val m = mask[x, y]
                val v = aa * m + bb * (1.0 - m)
                clamp(v, 0.0, 1.0)
            }
        }

        setOutput(result, resultImage)
    }

    companion object {
        const val a: String = "a"
        const val b: String = "b"
        const val mask: String = "mask"

        const val result: String = "result"
    }
}

class ImageMergeComponent(initialOperation: String) : Component(
    ComponentType(
        name = "ImageMerge",
        input = mapOf(
            a to FieldType(ValueType.Image),
            b to FieldType(ValueType.Image),
            operation to FieldType(ValueType.String)),
        output = mapOf(
            result to FieldType(ValueType.Image)))) {

    init {
        input[operation] = initialOperation
    }

    override fun runInternal() {
        val a: Image by input
        val b: Image by input
        val operation: String by input

        val func: (Double, Double) -> Double = when(operation) {
            "plus" -> { a, b -> a + b }
            "minus" -> { a, b -> a - b }
            "multiply" -> { a, b -> a * b }
            "divide" -> { a, b -> if (b == 0.0) 1.0 else a / b }
            "min" -> { a, b -> min(a, b) }
            "max" -> { a, b -> max(a, b) }
            "screen" -> { a, b -> 1.0 - (1.0 - a) * (1.0 - b) }
            "avg" -> { a, b -> (a + b) / 2.0 }
            "overlay" -> { a, b -> a * (a + 2*b + (1.0 - a)) }
            "dodge" -> { a, b ->  if (b == 0.0) 1.0 else a / (1.0 - b) }
            "burn" -> { a, b ->  if (b == 0.0) 1.0 else 1.0 - (1.0 - a) / b }
            "hardlight" -> { a, b ->  if (b > 0.5) (1.0 - (1.0 - 2*(b-0.5)) * (1.0-a)) else (2*a*b) }
            "softlight" -> { a, b ->
                val r = 1.0 - (1.0 - a) * (1.0 - b)
                ((1.0 - a) * b + r) * a
            }
            "grainextract" -> { a, b -> a - b + 0.5 }
            "grainmerge" -> { a, b -> a + b - 0.5 }
            "difference" -> { a, b -> abs(a - b) }
            "approach" -> { a, b ->
                val diff = b - a
                val f = diff * diff
                a + f * sign(diff)
            }
            else -> throw IllegalArgumentException("Unknown operation: $operation")
        }

        val resultImage = MatrixImage(a.width, a.height, a.channels) { channel, width, height ->
            DoubleMatrix(width, height) { x, y -> clamp(func(a[channel][x, y], b[channel][x, y]), 0.0, 1.0) }
        }

        setOutput(result, resultImage)
    }

    companion object {
        const val a: String = "a"
        const val b: String = "b"
        const val operation: String = "operation"

        const val result: String = "result"
    }
}

class PrintComponent(val text: String) : Component(
    ComponentType(
        name = "Print-$text",
        input = mapOf(value to FieldType(ValueType.Any)),
        output = mapOf())) {

    override fun runInternal() {
        val value: Any by input
        println("$text : $value")
    }

    companion object {
        const val value: String = "value"
    }
}

private fun deleteDirectory(directory: File) {
    val files = directory.listFiles()
    if (files != null) {
        for (file in files) {
            if (file.isDirectory) {
                deleteDirectory(file)
            } else {
                file.delete()
            }
        }
    }
    directory.delete()
}

fun tempDir(name: String, func: (File) -> Unit) {
    val dir = Files.createTempDirectory(name)
    try {
        func(dir.toFile())
    } finally {
        deleteDirectory(dir.toFile())
    }
}

fun runCommand(vararg command: String): Int {
    return runCommand(File("."), *command)
}

fun runCommand(directory: File, vararg command: String): Int {
    val processBuilder = ProcessBuilder()

    processBuilder.directory(directory)
    processBuilder.command(*command)

    val process = processBuilder.start()

    Executors.newSingleThreadExecutor().submit(StreamGobbler(process.inputStream, System.out::println))
    Executors.newSingleThreadExecutor().submit(StreamGobbler(process.errorStream, System.out::println))
    return process.waitFor()
}

class StarnetComponent() : Component(
    ComponentType(
        name = "Starnet",
        input = mapOf(
            starnetExe to FieldType(ValueType.String),
            starnetDir to FieldType(ValueType.String),
            inputImage to FieldType(ValueType.Image)),
        output = mapOf(
            starImage to FieldType(ValueType.Image),
            starlessImage to FieldType(ValueType.Image)
        ))) {

    init {
        input[starnetDir] = "c:/Apps/StarNetv2CLI_Win"
        input[starnetExe] = "starnet++"
    }

    override fun runInternal() {
        val starnetDir: String by input
        val starnetExe: String by input
        val inputImage: Image by input

        tempDir("temp_starnet_") { tmp ->
            val inputFile = File(tmp, "input.tif")
            val outputFile = File(tmp, "output.tif")

            ImageWriter.write(inputImage, inputFile)

            runCommand(File(starnetDir), starnetExe, inputFile.absolutePath, outputFile.absolutePath)

            val starless = ImageReader.read(outputFile)
            setOutput(starlessImage, starless)

            val stars = inputImage - starless
            setOutput(starImage, stars)
        }
    }

    companion object {
        const val starnetDir: String = "starnetDir"
        const val starnetExe: String = "starnetExe"
        const val inputImage: String = "inputImage"

        const val starImage: String = "starImage"
        const val starlessImage: String = "starlessImage"
    }
}


fun runAll(components: List<Component>) {
    var dirty = true
    while (dirty) {
        dirty = false
        for (component in components) {
            if (component.isDirty()) {
                dirty = true
                component.run()
                break;
            }
        }
    }
}

fun main(args: Array<String>) {
    //exampleDoubleAdd()
    exampleProcessAstroPhotography()
    //exampleTestMerge()
}

fun exampleTestMerge() {
    val imageReader1 = ImageFileReaderComponent(File("2023-08-09 NGC6992 starless.tif"))
    val imageReader2 = ImageFileReaderComponent(File("2023-08-09 NGC6992 stars.tif"))
    val imageMerge = ImageMergeComponent("screen")
    val imageWriter = ImageFileWriterComponent(File("merged.tif"))

    imageReader1.connectOutput(ImageFileReaderComponent.result, imageMerge, ImageMergeComponent.a)
    imageReader2.connectOutput(ImageFileReaderComponent.result, imageMerge, ImageMergeComponent.b)

    imageMerge.connectOutput(ImageMergeComponent.result, imageWriter, ImageFileWriterComponent.inputImage)

    runAll(listOf(imageReader1, imageReader2, imageMerge, imageWriter))
}

fun exampleProcessAstroPhotography() {
    val x1 = IntValueComponent(100)
    val y1 = IntValueComponent(100)
    val x2 = IntValueComponent(2900)
    val y2 = IntValueComponent(100)
    val x3 = IntValueComponent(100)
    val y3 = IntValueComponent(2900)
    val x4 = IntValueComponent(2900)
    val y4 = IntValueComponent(2900)
    val starlessSigmoidPower = DoubleValueComponent(1.5)
    val starlessSigmoidMidpoint = DoubleValueComponent(0.03)
    val starsSigmoidPower = DoubleValueComponent(1.8)
    val starsSigmoidMidpoint = DoubleValueComponent(0.05)
    val starlessBrightness = DoubleValueComponent(2.0)
    val erodeStrength = DoubleValueComponent(0.1)
    val erodeRepeat = IntValueComponent(3)

    val imageReader = ImageFileReaderComponent(File("images/Autosave 2023-08-09 NGC6992.tif"))
    val backgroundRemover = ImageRemoveBackground4Component()

    val starnet1 = StarnetComponent()
    val starnet2 = StarnetComponent()

    val stretchCurveStars = ImageColorStretchSigmoidCurveComponent()
    val stretchCurveStarless = ImageColorStretchSigmoidCurveComponent()
    val stretchCurveStarless2 = ImageColorStretchCurveComponent()

    val starsMask = ImageChannelMatrixComponent()

    val erodeStars = ImageErodeComponent()

    val erodeMaskMerge = ImageMaskMergeComponent()

    val imageMerge = ImageMergeComponent("screen")

    val outputImageTifWriter = ImageFileWriterComponent(File("Final 2023-08-09 NGC6992.tif"))
    val outputImageJpgWriter = ImageFileWriterComponent(File("Final 2023-08-09 NGC6992.jpg"))

    x1.connectOutput(IntValueComponent.result, backgroundRemover, ImageRemoveBackground4Component.x1)
    y1.connectOutput(IntValueComponent.result, backgroundRemover, ImageRemoveBackground4Component.y1)
    x2.connectOutput(IntValueComponent.result, backgroundRemover, ImageRemoveBackground4Component.x2)
    y2.connectOutput(IntValueComponent.result, backgroundRemover, ImageRemoveBackground4Component.y2)
    x3.connectOutput(IntValueComponent.result, backgroundRemover, ImageRemoveBackground4Component.x3)
    y3.connectOutput(IntValueComponent.result, backgroundRemover, ImageRemoveBackground4Component.y3)
    x4.connectOutput(IntValueComponent.result, backgroundRemover, ImageRemoveBackground4Component.x4)
    y4.connectOutput(IntValueComponent.result, backgroundRemover, ImageRemoveBackground4Component.y4)

    starsSigmoidPower.connectOutput(DoubleValueComponent.result, stretchCurveStars, ImageColorStretchSigmoidCurveComponent.power)
    starsSigmoidMidpoint.connectOutput(DoubleValueComponent.result, stretchCurveStars, ImageColorStretchSigmoidCurveComponent.midpoint)

    starlessSigmoidPower.connectOutput(DoubleValueComponent.result, stretchCurveStarless, ImageColorStretchSigmoidCurveComponent.power)
    starlessSigmoidMidpoint.connectOutput(DoubleValueComponent.result, stretchCurveStarless, ImageColorStretchSigmoidCurveComponent.midpoint)

    starlessBrightness.connectOutput(DoubleValueComponent.result, stretchCurveStarless2, ImageColorStretchCurveComponent.brightness)

    erodeStrength.connectOutput(DoubleValueComponent.result, erodeStars, ImageErodeComponent.strength)
    erodeRepeat.connectOutput(DoubleValueComponent.result, erodeStars, ImageErodeComponent.repeat)

    imageReader.connectOutput(ImageFileReaderComponent.result, backgroundRemover, ImageRemoveBackground4Component.inputImage)

    backgroundRemover.connectOutput(ImageRemoveBackground4Component.result, stretchCurveStars, ImageColorStretchSigmoidCurveComponent.inputImage)
    backgroundRemover.connectOutput(ImageRemoveBackground4Component.result, stretchCurveStarless, ImageColorStretchSigmoidCurveComponent.inputImage)

    stretchCurveStars.connectOutput(ImageColorStretchSigmoidCurveComponent.result, starnet1, StarnetComponent.inputImage)

    starnet1.connectOutput(StarnetComponent.starImage, erodeStars, ImageErodeComponent.inputImage)
    starnet1.connectOutput(StarnetComponent.starImage, erodeMaskMerge, ImageMaskMergeComponent.a)
    starnet1.connectOutput(StarnetComponent.starImage, starsMask, ImageChannelMatrixComponent.inputImage)

    starsMask.connectOutput(ImageChannelMatrixComponent.gray, erodeMaskMerge, ImageMaskMergeComponent.mask)

    erodeStars.connectOutput(ImageErodeComponent.result, erodeMaskMerge, ImageMaskMergeComponent.b)

    erodeMaskMerge.connectOutput(ImageMaskMergeComponent.result, imageMerge, ImageMergeComponent.a)

    stretchCurveStarless.connectOutput(ImageColorStretchSigmoidCurveComponent.result, stretchCurveStarless2, ImageColorStretchCurveComponent.inputImage)

    stretchCurveStarless2.connectOutput(ImageColorStretchCurveComponent.result, starnet2, StarnetComponent.inputImage)

    starnet2.connectOutput(StarnetComponent.starlessImage, imageMerge, ImageMergeComponent.b)

    imageMerge.connectOutput(ImageMergeComponent.result, outputImageTifWriter, ImageFileWriterComponent.inputImage)
    imageMerge.connectOutput(ImageMergeComponent.result, outputImageJpgWriter, ImageFileWriterComponent.inputImage)

    val components = mutableListOf(
        x1, y1, x2, y2, x3, y3, x4, y4,
        starlessSigmoidPower, starlessSigmoidMidpoint,
        starsSigmoidPower, starsSigmoidMidpoint,
        starlessBrightness,
        erodeStrength, erodeRepeat,
        imageReader,
        backgroundRemover,
        stretchCurveStarless,
        stretchCurveStars,
        starnet1,
        stretchCurveStarless2,
        starnet2,
        erodeStars,
        starsMask,
        erodeMaskMerge,
        imageMerge,
        outputImageTifWriter,
        outputImageJpgWriter)

    printDiagram(components)

    addImageFileWriters(components)
    //printDiagram(components)

    runAll(components)
}

fun addImageFileWriters(components: MutableList<Component>) {
    var index = 1
    val addedComponents = mutableListOf<Component>()
    for (component in components) {
        for (outputName in component.output.keys) {
            if (component.type.output[outputName]!!.type == ValueType.Image) {
                val hasFileWriter = component.output[outputName]!!.any { it.component is ImageFileWriterComponent }
                if (!hasFileWriter) {
                    val fileWriter = ImageFileWriterComponent(File("debug${index}_${component.name}_${component.type.name}_$outputName.tif"))
                    component.connectOutput(outputName, fileWriter, ImageFileWriterComponent.inputImage)
                    addedComponents.add(fileWriter)
                    index++
                }
            }
        }
    }
    components.addAll(addedComponents)
}

fun printDiagram(components: List<Component>, showComponentNames: Boolean = false) {
    println("digraph G {")
    println("  rankdir=TB;")
    val componentNames = mutableMapOf<Component, String>()
    for (i in components.indices) {
        val component = components[i]
        val name = "component$i"
        componentNames[component] = name
        val shape = if (component is SimpleValueComponent<*>) "ellipse" else "box"
        val componentName = if (showComponentNames) "\n${component.name}" else ""
        println("  $name [shape=$shape label=\"${component.type.name}$componentName\"];")
    }

    for (component in components) {
        val name = componentNames[component]

        for (out in component.output.keys) {
            for (outField in component.output[out]!!) {
                val targetName = componentNames[outField.component]
                val taillabel = if (out == "result") "" else out
                val headlabel = if (outField.name == "inputImage") "" else outField.name
                println("  $name -> ${targetName} [taillabel=\"$taillabel\" headlabel=\"$headlabel\"];")
            }
        }
    }
    println("}")
    println()
}

fun exampleDoubleAdd() {
    val a = DoubleValueComponent(2.0)
    val b = DoubleValueComponent(3.0)
    val calc = DoubleAddComponent()
    val print = PrintComponent("result")

    a.connectOutput(SimpleValueConverterComponent.result, calc, DoubleAddComponent.a)
    b.connectOutput(SimpleValueConverterComponent.result, calc, DoubleAddComponent.b)
    calc.connectOutput(DoubleAddComponent.result, print, PrintComponent.value)

    runAll(listOf(a, b, calc, print))

    a.setInput("value", 4.0)
    runAll(listOf(a, b, calc, print))
}