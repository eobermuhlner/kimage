println("Test simple single image script")

println("Input file: inputFile ${inputFile.name}")
println("Input image: $inputImage width=${inputImage.width} height=${inputImage.height}")
for (parameter in inputParameters as Map<String, String>) {
    val key: String = parameter.key
    val value: String = parameter.value
    println("  Parameter: ${key} = ${value}")
}

null
