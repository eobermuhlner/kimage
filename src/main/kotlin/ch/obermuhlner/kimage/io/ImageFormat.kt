package ch.obermuhlner.kimage.io

enum class ImageFormat(vararg extensions: String) {
    TIF("tif", "tiff"),
    PNG("png"),
    JPG("jpg", "jpeg"),
    FITS("fits", "fit");

    val extensions: Array<String> = extensions as Array<String>

}