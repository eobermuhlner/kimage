require(inputMultiMode)

println("Test simple multi image script")

println("Input files: $inputFiles")
for (parameter in inputParameters as Map<String, String>) {
    val key: String = parameter.key
    val value: String = parameter.value
    println("  Parameter: ${key} = ${value}")
}

for (file in inputFiles as List<File>) {
    println("  File: $file exists=${file.exists()}")
}

null
