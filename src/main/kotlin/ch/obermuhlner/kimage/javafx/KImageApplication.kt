package ch.obermuhlner.kimage.javafx

import ch.obermuhlner.kimage.KImageManager
import ch.obermuhlner.kimage.ScriptV0_1
import ch.obermuhlner.kimage.io.ImageReader
import ch.obermuhlner.kotlin.javafx.*
import javafx.application.Application
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Tooltip
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.File
import java.nio.file.Paths
import java.text.DecimalFormat

class KImageApplication : Application() {

    private lateinit var primaryStage: Stage

    val inputImageView = ImageView()
    val outputImageView = ImageView()
    val commandArgumentEditor = VBox(SPACING)

    val inputDirectoryProperty = SimpleStringProperty(Paths.get(System.getProperty("user.home", ".")).toString())
    val outputDirectoryProperty = SimpleStringProperty(Paths.get(System.getProperty("user.home", ".")).toString())

    private val inputFiles = FXCollections.observableArrayList<File>()
    private val scriptNames = FXCollections.observableArrayList<String>()
    private val outputFiles = FXCollections.observableArrayList<File>()


    override fun start(primaryStage: Stage) {
        this.primaryStage = primaryStage

        val root = Group()
        val scene = Scene(root)

        root.children += createMainEditor()

        primaryStage.scene = scene
        primaryStage.show()

        scriptNames.setAll(KImageManager.scriptNames)
        updateOutputDirectoryFiles()
    }

    private fun createMainEditor(): Node {
        return borderpane {
            left = createInputFilesEditor()

            center = borderpane {
                padding = Insets(0.0, SPACING, 0.0, SPACING)

                top = label("Processing:") {
                    font = Font.font(font.size * TITLE_FONT_SIZE)
                }

                left = listview(scriptNames) {
                    selectionModel.selectedItemProperty().addListener { _, _, selected ->
                        selected?.let {
                            showCommandEditor(it)
                        }
                    }
                }

                center = vbox(SPACING) {
                    padding = Insets(0.0, SPACING, 0.0, SPACING)

                    minWidth = ARGUMENT_EDITOR_WIDTH.toDouble()
                    minHeight = ARGUMENT_EDITOR_HEIGHT.toDouble()
                    children += commandArgumentEditor
                }
            }

            right = createOutputFilesEditor()
        }
    }

    private fun createInputFilesEditor(): Node {
        return vbox(SPACING) {
            children += label("Input Files:") {
                font = Font.font(font.size * TITLE_FONT_SIZE)
            }

            children += hbox(SPACING) {
                children += button("Add...") {
                    onAction = EventHandler {
                        val files = openImageFiles()
                        files?.let {
                            inputFiles.clear()
                            inputFiles.addAll(it)
                        }
                    }
                }
            }

            children += tableview(inputFiles) {
                minWidth = FILE_TABLE_WIDTH.toDouble()
                minHeight = FILE_TABLE_HEIGHT.toDouble()

                column<String>("Name", { file -> ReadOnlyStringWrapper(file?.name) }) {
                }
                column<String>("Path", { file -> ReadOnlyStringWrapper(file?.path) }) {
                }

                selectionModel.selectedItemProperty().addListener { _, _, selected ->
                    updateInputImageView(selected)
                }
            }

            children += node(inputImageView) {
                isPreserveRatio = true
                fitWidth = IMAGE_WIDTH.toDouble()
                fitHeight = IMAGE_HEIGHT.toDouble()
            }
        }
    }

    private fun createOutputFilesEditor(): Node {
        return vbox(SPACING) {
            children += label("Output Files:") {
                font = Font.font(font.size * TITLE_FONT_SIZE)
            }

            children += hbox(SPACING) {
                children += label("Output Directory:")
                children += textfield(outputDirectoryProperty) {
                    textProperty().addListener { _, _, _ ->
                        updateOutputDirectoryFiles()
                    }
                }
                children += button("Pick...") {
                    onAction = EventHandler {
                        val file = openDir(File(outputDirectoryProperty.get()))
                        file?.let {
                            outputDirectoryProperty.set(it.path)
                        }
                    }
                }
            }

            children += tableview(outputFiles) {
                minWidth = FILE_TABLE_WIDTH.toDouble()
                minHeight = FILE_TABLE_HEIGHT.toDouble()

                column<String>("Name", { file -> ReadOnlyStringWrapper(file?.name) }) {
                }
                column<String>("Path", { file -> ReadOnlyStringWrapper(file?.path) }) {
                }

                selectionModel.selectedItemProperty().addListener { _, _, selected ->
                    updateOutputImageView(selected)
                }
            }

            children += node(outputImageView) {
                isPreserveRatio = true
                fitWidth = IMAGE_WIDTH.toDouble()
                fitHeight = IMAGE_HEIGHT.toDouble()
            }
        }
    }

    private fun updateInputImageView(selectedFile: File) {
        val image = ImageReader.read(selectedFile)
        val writableImage = JavaFXImageUtil.toWritableImage(image)
        inputImageView.image = writableImage
    }

    private fun updateOutputImageView(selectedFile: File) {
        val image = ImageReader.read(selectedFile)
        val writableImage = JavaFXImageUtil.toWritableImage(image)
        outputImageView.image = writableImage
    }

    private fun showCommandEditor(command: String) {
        commandArgumentEditor.children.clear()

        val verboseModeProperty = SimpleBooleanProperty(false)
        val debugModeProperty = SimpleBooleanProperty(false)

        node(commandArgumentEditor) {
            children += checkbox(verboseModeProperty) {
                text = "Verbose"
            }
            children += checkbox(debugModeProperty) {
                text = "Debug"
            }

            children += label("Command: $command")

            val script = KImageManager.script(command)

            if (script is ScriptV0_1) {
                children += label(script.title)

                children += textarea {
                    text = script.description.trimIndent()
                }

                val argumentStrings = mutableMapOf<String, String>()
                children += gridpane {
                    spacing = SPACING

                    for (argument in script.scriptArguments.arguments) {
                        row {
                            cell {
                                label(argument.name) {
                                }
                            }
                            cell {
                                textfield {
                                    tooltip = Tooltip(argument.description)
                                    textProperty().addListener { _, _, value ->
                                        argumentStrings[argument.name] = value
                                    }
                                }
                            }
                        }
                    }
                }

                children += button("Run") {
                    onAction = EventHandler {
                        KImageManager.executeScript(
                            script,
                            argumentStrings,
                            inputFiles,
                            false,
                            verboseModeProperty.get(),
                            debugModeProperty.get(),
                            command,
                            outputDirectoryProperty.get()
                        )
                        updateOutputDirectoryFiles()
                    }
                }
            }
        }
    }

    private fun updateOutputDirectoryFiles() {
        outputFiles.clear()

        val dir = File(outputDirectoryProperty.get())
        if (!dir.isDirectory) {
            return
        }

        val files = dir.listFiles { f -> f.isFile }
        outputFiles.addAll(files)
    }

    fun openImageFiles(initialDirectory: File = File(inputDirectoryProperty.get()), title: String = "Open Images"): List<File>? {
        val fileChooser = FileChooser()
        fileChooser.initialDirectory = initialDirectory
        fileChooser.title = title
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Image", "*.tif", "*.tiff", "*.png", "*.jpg", "*.jpeg"))
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("All", "*"))
        return fileChooser.showOpenMultipleDialog(primaryStage)
    }

    fun openDir(initialDirectory: File = File(outputDirectoryProperty.get()), title: String = "Pick Directory"): File? {
        val directoryChooser = DirectoryChooser()
        directoryChooser.initialDirectory = initialDirectory
        directoryChooser.title = title
        return directoryChooser.showDialog(primaryStage)
    }

    companion object {
        private const val SPACING = 4.0

        private const val IMAGE_WIDTH = 400
        private const val IMAGE_HEIGHT = 400

        private const val ZOOM_WIDTH = 200
        private const val ZOOM_HEIGHT = 200

        private const val FILE_TABLE_WIDTH = 400
        private const val FILE_TABLE_HEIGHT = 200

        private const val ARGUMENT_EDITOR_WIDTH = 400
        private const val ARGUMENT_EDITOR_HEIGHT = 400

        private const val TITLE_FONT_SIZE = 1.5

        val INTEGER_FORMAT = DecimalFormat("##0")
        val DOUBLE_FORMAT = DecimalFormat("##0.000")
        val PERCENT_FORMAT = DecimalFormat("##0.000%")

        @JvmStatic
        fun main(args: Array<String>) {
            launch(KImageApplication::class.java)
        }
    }
}
