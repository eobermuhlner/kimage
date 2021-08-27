package ch.obermuhlner.kimage.io

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path

fun File.prefixName(prefix: String): File {
    return prefixName(this.parentFile, prefix)
}

fun File.prefixName(parent: File, prefix: String): File {
    return File(parent, prefix + this.name)
}

fun File.suffixName(suffix: String): File {
    return File(this.parent, this.nameWithoutExtension + suffix + "." + this.extension)
}

fun File.replaceExtension(extension: String): File {
    return File(this.parent, this.nameWithoutExtension + "." + extension)
}

fun File.suffixExtension(suffix: String): File {
    return File(this.parent, this.name + suffix)
}

fun File.matchingFiles(pattern: String): List<File> {
    val pathMatcher = FileSystems.getDefault().getPathMatcher("glob:$pattern")
    return listFiles { file -> pathMatcher.matches(Path.of(file.name)) }.toList()
}

