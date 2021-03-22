package ch.obermuhlner.kimage.javafx

import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.io.ImageReader
import ch.obermuhlner.kimage.math.clamp
import ch.obermuhlner.kotlin.javafx.*
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.property.SimpleIntegerProperty
import javafx.event.EventHandler
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.input.MouseEvent
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.File
import java.io.IOException
import java.lang.Thread.sleep
import java.nio.file.Paths
import java.text.DecimalFormat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class KImageApplication : Application() {

    private lateinit var primaryStage: Stage
    var currentImage: Image? = null
    val currentImageView = ImageView()
    val workflowEditor = VBox()

    private var currentDirectory = Paths.get(System.getProperty("user.home", ".")).toFile()

    private val zoomInputWritableImage = WritableImage(ZOOM_WIDTH, ZOOM_HEIGHT)
    private val zoomInputImage = JavafxWritableImage(zoomInputWritableImage)

    private val zoomOutputWritableImage = WritableImage(ZOOM_WIDTH, ZOOM_HEIGHT)
    private val zoomOutputImage = JavafxWritableImage(zoomOutputWritableImage)

    override fun start(primaryStage: Stage) {
        this.primaryStage = primaryStage

        val root = Group()
        val scene = Scene(root)

        root.children += createMainEditor()

        primaryStage.scene = scene
        primaryStage.show()
        applicationSingleton = this
    }

    fun openImageFile(initialFileName: String? = null, initialDirectory: File = currentDirectory, title: String = "Open Image"): Image {
        val fileChooser = FileChooser()
        println("FILENAME " + fileChooser.initialFileName)
        fileChooser.initialDirectory = initialDirectory
        fileChooser.title = title
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Image", "*.tif", "*.tiff", "*.png", "*.jpg", "*.jpeg"))
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("All", "*"))
        val chosenFile = fileChooser.showOpenDialog(primaryStage)
        if (chosenFile != null) {
            try {
                setCurrentImage(ImageReader.readMatrixImage(chosenFile), chosenFile.name)
                currentDirectory = chosenFile.parentFile
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return currentImage!!
    }

    fun setCurrentImage(image: Image, title: String = "Image") {
        currentImage = image
        currentImageView.image = JavaFXImageUtil.toWritableImage(image)

        primaryStage.title = title
    }

    private fun createMainEditor(): Node {
        return borderpane {
            center = node(currentImageView) {
                isPreserveRatio = true
                fitWidth = IMAGE_WIDTH.toDouble()
                fitHeight = IMAGE_HEIGHT.toDouble()
            }
            right = vbox {
                children += label("Interactive Workflow")
                children += workflowEditor
            }
        }
    }

    fun form(initializer: VBox.() -> Unit) {
        workflowEditor.children.clear()

        workflowEditor.apply(initializer)
    }

    fun filter(name: String, filter: Image.() -> Image) {
        filterArea(name) { _, _, _, _ ->
            this.filter()
        }
    }

    fun filterArea(name: String, filter: Image.(Int, Int, Int, Int) -> Image) {
        val zoomCenterXProperty = SimpleIntegerProperty()
        val zoomCenterYProperty = SimpleIntegerProperty()

        fun updateZoom(zoomX: Int = zoomCenterXProperty.get(), zoomY: Int = zoomCenterYProperty.get()) {
            val zoomOffsetX = zoomX - ZOOM_WIDTH / 2
            val zoomOffsetY = zoomY - ZOOM_HEIGHT / 2

            currentImage?.let {
                zoomInputImage.setPixels(zoomOffsetX, zoomOffsetY, it, 0, 0, ZOOM_WIDTH, ZOOM_HEIGHT, doubleArrayOf(0.0, 0.0, 0.0))

                zoomOutputImage.setPixels(zoomInputImage.filter(zoomOffsetX, zoomOffsetY, ZOOM_WIDTH, ZOOM_HEIGHT))
            }
        }

        var zoomDragX: Double? = null
        var zoomDragY: Double? = null
        fun setupZoomDragEvents(imageView: ImageView) {
            imageView.onMousePressed = EventHandler { event: MouseEvent ->
                zoomDragX = event.x
                zoomDragY = event.y
            }
            imageView.onMouseDragged = EventHandler { event: MouseEvent ->
                val deltaX = zoomDragX!! - event.x
                val deltaY = zoomDragY!! - event.y
                zoomDragX = event.x
                zoomDragY = event.y
                var zoomX = zoomCenterXProperty.get() + deltaX.toInt()
                var zoomY = zoomCenterYProperty.get() + deltaY.toInt()
                zoomX = clamp(zoomX, 0, currentImage!!.width.toInt() - 1)
                zoomY = clamp(zoomY, 0, currentImage!!.height.toInt() - 1)
                zoomCenterXProperty.set(zoomX)
                zoomCenterYProperty.set(zoomY)
                updateZoom(zoomX, zoomY)
            }
            imageView.onMouseDragReleased = EventHandler {
                zoomDragX = null
                zoomDragY = null
            }
        }

        workflowEditor.children += vbox {
            children += label("Input:")
            children += imageview {
                image = zoomInputWritableImage
                setupZoomDragEvents(this)
            }

            children += label("Output:")
            children += imageview {
                image = zoomOutputWritableImage
                setupZoomDragEvents(this)
            }
        }

        currentImage?.let {
            updateZoom(it.width / 2, it.height / 2)
        }

        workflowEditor.children += button(name) {
            onAction = EventHandler {
                currentImage?.let {
                    val result = it.filter(0, 0, it.width, it.height)
                    setCurrentImage(result, "Filtered")
                    latch.countDown()
                }
            }
        }
    }

    companion object {
        private const val IMAGE_WIDTH = 600
        private const val IMAGE_HEIGHT = 600

        private const val ZOOM_WIDTH = 200
        private const val ZOOM_HEIGHT = 200

        val INTEGER_FORMAT = DecimalFormat("##0")
        val DOUBLE_FORMAT = DecimalFormat("##0.000")
        val PERCENT_FORMAT = DecimalFormat("##0.000%")

        private var singletonLaunched: Boolean = false
        private var applicationSingleton: KImageApplication? = null
        private var latch = CountDownLatch(0)

        @Synchronized fun <T> interactive(func: KImageApplication.() -> T): T {
            if (!singletonLaunched) {
                singletonLaunched = true
                thread {
                    launch(KImageApplication::class.java)
                }
            }
            while (applicationSingleton == null) {
                sleep(10)
            }

            latch = CountDownLatch(1)
            val result = AtomicReference<T>()
            Platform.runLater {
                result.set(applicationSingleton!!.func())

            }
            latch.await()
            return result.get()
        }

        @JvmStatic
        fun main(args: Array<String>) {
            launch(KImageApplication::class.java)
        }
    }
}
