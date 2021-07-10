package ch.obermuhlner.kimage.javafx

import ch.obermuhlner.kimage.KImageManager
import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kotlin.javafx.*
import javafx.application.Application
import javafx.collections.FXCollections
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.text.DecimalFormat

class KImageApplication : Application() {

    private lateinit var primaryStage: Stage
    var currentImage: Image? = null
    val currentImageView = ImageView()
    val commandArgumentEditor = VBox()

    private var currentDirectory = Paths.get(System.getProperty("user.home", ".")).toFile()

    private val scriptNames = FXCollections.observableArrayList<String>()

    override fun start(primaryStage: Stage) {
        this.primaryStage = primaryStage

        val root = Group()
        val scene = Scene(root)

        root.children += createMainEditor()

        primaryStage.scene = scene
        primaryStage.show()

        scriptNames.setAll(KImageManager.scriptNames)
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
                // TODO setCurrentImage(read(chosenFile), chosenFile.name)
                currentDirectory = chosenFile.parentFile
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return currentImage!!
    }

    private fun createMainEditor(): Node {
        return borderpane {
            left = listview(scriptNames) {
                selectionModel.selectedItemProperty().addListener { _, _, selected ->
                    selected?.let {
                        showCommandEditor(it)
                    }
                }
            }
//            center = node(currentImageView) {
//                isPreserveRatio = true
//                fitWidth = IMAGE_WIDTH.toDouble()
//                fitHeight = IMAGE_HEIGHT.toDouble()
//            }
            center = vbox {
                minWidth = 400.0
                minHeight = 400.0
                children += commandArgumentEditor
            }
        }
    }

    private fun showCommandEditor(command: String) {
        commandArgumentEditor.children.clear()

        node(commandArgumentEditor) {
            children += label(command)
        }
    }

    fun form(initializer: VBox.() -> Unit) {
        commandArgumentEditor.children.clear()

        commandArgumentEditor.apply(initializer)
    }

    companion object {
        private const val IMAGE_WIDTH = 600
        private const val IMAGE_HEIGHT = 600

        private const val ZOOM_WIDTH = 200
        private const val ZOOM_HEIGHT = 200

        val INTEGER_FORMAT = DecimalFormat("##0")
        val DOUBLE_FORMAT = DecimalFormat("##0.000")
        val PERCENT_FORMAT = DecimalFormat("##0.000%")

        @JvmStatic
        fun main(args: Array<String>) {
            launch(KImageApplication::class.java)
        }
    }
}
