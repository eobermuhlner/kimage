package ch.obermuhlner.kimage.io

enum class ImageFormat(vararg val extensions: String) {
    TIF("tif", "tiff"),
    PNG("png"),
    JPG("jpg", "jpeg"),
    FITS("fits", "fit");
}