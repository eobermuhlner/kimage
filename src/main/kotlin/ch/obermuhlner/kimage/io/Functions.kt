package ch.obermuhlner.kimage.io

import java.io.File

fun File.prefixName(prefix: String): File {
    return File(this.parent, prefix + this.name)
}

fun File.suffixName(suffix: String): File {
    return File(this.parent, this.nameWithoutExtension + suffix + this.extension)
}

