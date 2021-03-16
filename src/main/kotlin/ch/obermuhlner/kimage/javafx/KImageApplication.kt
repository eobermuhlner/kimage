package ch.obermuhlner.kimage.javafx

import ch.obermuhlner.kimage.image.Image
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
import javafx.stage.Stage
import java.lang.Thread.sleep
import java.text.DecimalFormat
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

class KImageApplication : Application() {

    var currentImage: Image? = null
    val currentImageView = ImageView()
    val workflowEditor = VBox()

    private val zoomInputWritableImage = WritableImage(ZOOM_WIDTH, ZOOM_HEIGHT)
    private val zoomInputImage = JavafxWritableImage(zoomInputWritableImage)

    private val zoomOutputWritableImage = WritableImage(ZOOM_WIDTH, ZOOM_HEIGHT)
    private val zoomOutputImage = JavafxWritableImage(zoomOutputWritableImage)

    override fun start(primaryStage: Stage) {
        val root = Group()
        val scene = Scene(root)

        root.children += createMainEditor()

        primaryStage.scene = scene
        primaryStage.show()
        applicationSingleton = this
    }

    fun setCurrentImage(image: Image, title: String = "Image") {
        currentImage = image
        currentImageView.image = JavaFXImageUtil.toWritableImage(image)
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

        val zoomCenterXProperty = SimpleIntegerProperty()
        val zoomCenterYProperty = SimpleIntegerProperty()

        zoomInputImage.setPixels(doubleArrayOf(1.0, 0.0, 0.0))
        zoomOutputImage.setPixels(doubleArrayOf(0.0, 0.0, 1.0))

        fun updateZoom(zoomX: Int = zoomCenterXProperty.get(), zoomY: Int = zoomCenterYProperty.get()) {
            val zoomOffsetX = zoomX - ZOOM_WIDTH / 2
            val zoomOffsetY = zoomY - ZOOM_HEIGHT / 2

            zoomInputImage.setPixels(zoomOffsetX, zoomOffsetY, currentImage!!, 0, 0, ZOOM_WIDTH, ZOOM_HEIGHT, doubleArrayOf(0.0, 0.0, 0.0))
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
                zoomX = max(zoomX, 0)
                zoomY = max(zoomY, 0)
                zoomX = min(zoomX, currentImage!!.width.toInt() - 1)
                zoomY = min(zoomY, currentImage!!.height.toInt() - 1)
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

            this.apply(initializer)
        }

        updateZoom(currentImage!!.width / 2, currentImage!!.height / 2)
    }

    fun filter(name: String, filter: Image.() -> Image) {
        workflowEditor.children += button(name) {
            onAction = EventHandler {
                currentImage?.let {
                    val result = it.filter()
                    setCurrentImage(result, "Filtered")
                    latch.countDown()
                }
            }
        }
    }

    companion object {
        private const val IMAGE_WIDTH = 600
        private const val IMAGE_HEIGHT = 600

        private const val ZOOM_WIDTH = 150
        private const val ZOOM_HEIGHT = 150

        val INTEGER_FORMAT = DecimalFormat("##0")
        val DOUBLE_FORMAT = DecimalFormat("##0.000")
        val PERCENT_FORMAT = DecimalFormat("##0.000%")

        private var singletonLaunched: Boolean = false
        private var applicationSingleton: KImageApplication? = null
        private var latch = CountDownLatch(0)

        @Synchronized fun interactive(func: KImageApplication.() -> Unit) {
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
            Platform.runLater {
                applicationSingleton!!.func()

            }
            latch.await()
        }

        @JvmStatic
        fun main(args: Array<String>) {
            launch(KImageApplication::class.java)
        }
    }
}
