package ch.obermuhlner.kimage.javafx

import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.ImageReader
import ch.obermuhlner.kotlin.javafx.*
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.css.PseudoClass
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.WritableImage
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.shape.Shape
import javafx.stage.*
import javafx.util.Callback
import java.io.*
import java.nio.file.Paths
import java.text.DecimalFormat
import java.util.*

class ProcessInfo(
    val name: String,
    val jsonArguments: String
)

class ProcessedImage(
    var name: String,
    var image: Image?,
    val thumbnail: WritableImage,
    var file: File,
    val processInfo: ProcessInfo?,
    val outputs: MutableList<ProcessedImage> = mutableListOf()
)

class KImageApplication2 : Application() {

    private lateinit var primaryStage: Stage

    private val inputDirectoryProperty = SimpleStringProperty(Paths.get(".").toString())

    private val scriptNames = FXCollections.observableArrayList<String>()
    private val rootImages = mutableListOf<ProcessedImage>()
    private var selectedImage: ProcessedImage? = null

    private val scriptNameProperty = SimpleStringProperty()
    private val argumentsProperty = SimpleMapProperty<String, Any>(FXCollections.observableHashMap())
    private val commandPresetsNames = FXCollections.observableArrayList<String>()
    private val commandPresets = mutableMapOf<String, Map<String, Any>>()

    private val workPane = Pane()

    override fun start(primaryStage: Stage) {
        this.primaryStage = primaryStage

        val root = StackPane()
        val scene = Scene(root)

        root.children += createMainEditor()

        scene.stylesheets.add(KImageApplication::class.java.getResource("/application.css").toExternalForm());

        primaryStage.scene = scene
        primaryStage.show()
    }

    private fun createMainEditor(): Node {
        return borderpane {
            top = createToolbar()
            center = createWorkArea()
            right = createScriptPicker()
            bottom = createStatusArea()
        }
    }

    private fun createToolbar(): Node {
        return hbox(SPACING) {
            children += button("Add Image...") {
                onAction = EventHandler {
                    val files = openImageFiles(File(inputDirectoryProperty.value))
                    files?.let {
                        loadImageFiles(it)
                    }
                }
            }
        }
    }

    private fun loadImageFiles(it: List<File>) {
        for (file in it) {
            try {
                val image = ImageReader.read(file)
                val thumbnail = toThumbnail(image)
                rootImages += ProcessedImage(
                    file.name,
                    image,
                    thumbnail,
                    file,
                    null
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        updateWorkArea()
    }

    private fun toThumbnail(image: Image) =
        JavaFXImageUtil.toWritableImage(image.scaleToKeepRatio(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT))

    private fun updateWorkArea() {
        workPane.children.clear()

        var row = 0
        var column = 0
        for (processedImage in rootImages) {
            val (lastRow, lastColumn) = addImageCards(processedImage, row, 0)
            row = lastRow + 1
            column = kotlin.math.max(lastColumn, column)
        }

        workPane.prefWidth = (column+1) * (CARD_WIDTH + PROCESS_WIDTH)
        workPane.prefHeight = (row+1) * CARD_HEIGHT
    }

    private fun addImageCards(processedImage: ProcessedImage, startRow: Int, startColumn: Int): Pair<Int, Int> {
        var row = startRow
        var column = startColumn

        val nodes = createImageCard(processedImage, row, column)
        workPane.children.addAll(nodes)

        for (outputIndex in processedImage.outputs.indices) {
            val outputProcessedImage = processedImage.outputs[outputIndex]
            if (outputIndex > 0) {
                row++
            }
            val (lastRow, lastColumn) = addImageCards(outputProcessedImage, row, startColumn+1)
            row = lastRow
            column = kotlin.math.max(lastColumn, column)
        }
        return Pair(row, column)
    }

    private fun createImageCard(processedImage: ProcessedImage, row: Int, column: Int): List<Node> {
        val nodes = mutableListOf<Node>()

        val processInfo = processedImage.processInfo
        if (processInfo != null) {
            val startX = column * (CARD_WIDTH + PROCESS_WIDTH)-PROCESS_WIDTH // TODO find position of source image
            val startY = row * CARD_HEIGHT + CARD_HEIGHT/2 // TODO find position of source image
            val endX = column * (CARD_WIDTH + PROCESS_WIDTH)
            val endY = row * CARD_HEIGHT + CARD_HEIGHT/2
            nodes += line(startX, startY, endX, endY) {

            }
            val processButton = button(processInfo.name) {
            }
            processButton.layout()
            processButton.layoutX = startX + (endX - startX) / 2 - processButton.layoutBounds.width / 2
            processButton.layoutY = startY + (endY - startY) / 2 - processButton.layoutBounds.height / 2
            nodes += processButton
        }
        nodes += borderpane {
            layoutX = column * (CARD_WIDTH + PROCESS_WIDTH)
            layoutY = row * CARD_HEIGHT

            top = label(processedImage.name)
            center = button(imageview(processedImage.thumbnail) {}) {
                onAction = EventHandler {
                    selectedImage = processedImage
                }
            }
        }

        return nodes
    }

    private fun createWorkArea(): Node {
        workPane.prefWidth = 200.0
        workPane.prefHeight = 200.0

        return scrollpane {
            setPrefSize(600.0, 400.0)
            content = workPane
        }
    }

    private fun createScriptPicker(): Node {
        scriptNames.setAll(KImageManager.scriptNames)
        if (scriptNames.isNotEmpty()) {
            KImageManager.script(scriptNames[0]) // trigger compilation of first script
        }

        return listview(scriptNames) {
            selectionModel.selectedItemProperty().addListener { _, _, selected ->
                selected?.let {
                    loadCommandPresets(it)
                    showCommandEditor(it, mapOf<String, Any>())
                }
            }
        }
    }

    private fun loadCommandPresets(commandName: String) {
        val presetsDir = File(File(File(System.getProperty("user.home"), ".kimage"), "config"), commandName)
        if (!presetsDir.exists()) {
            presetsDir.mkdirs()
        }

        val presetFiles = presetsDir.listFiles() { file ->
            file.extension == "properties"
        }

        commandPresetsNames.clear()
        commandPresets.clear()

        commandPresetsNames.add("")
        commandPresets[""] = mapOf()

        for (presetFile in presetFiles) {
            val presetName = presetFile.nameWithoutExtension
            val properties = Properties()
            val reader = BufferedReader(FileReader(presetFile))
            reader.use {
                properties.load(it)
            }

            val map = mutableMapOf<String, Any>()
            properties.forEach { k, v ->
                map[k.toString()] = v.toString()
            }

            commandPresetsNames.add(presetName)
            commandPresets[presetName] = map
        }
    }

    private fun showCommandEditor(command: String, initialArgumentValues: Map<String, Any>) {
        val input = selectedImage
        if (input != null) {
            val dialog = Dialog<ProcessedImage>()
            dialog.dialogPane.buttonTypes.add(ButtonType.OK)
            dialog.dialogPane.buttonTypes.add(ButtonType.CANCEL)
            dialog.resultConverter = Callback {
                if (it == ButtonType.OK) {
                    val outputImage = input.image!!.gaussianBlurFilter(3)
                    val outputName = "blur_" + input.name
                    ProcessedImage(
                        outputName,
                        outputImage,
                        toThumbnail(outputImage),
                        File(outputName),
                        ProcessInfo("blur", "")
                    )
                } else {
                    null
                }
            }

            val optionalOutput = dialog.showAndWait()
            if (optionalOutput.isPresent()) {
                val output = optionalOutput.get()
                input.outputs += output
                updateWorkArea()
            }
        }
    }

    private fun createStatusArea(): Node {
        return hbox(SPACING) {
            children += label("Status:")
        }
    }

    companion object {
        private const val SPACING = 4.0

        private const val CARD_WIDTH = 400.0
        private const val CARD_HEIGHT = 300.0
        private const val PROCESS_WIDTH = 200.0
        private const val THUMBNAIL_WIDTH = 300
        private const val THUMBNAIL_HEIGHT = 200



        val INTEGER_FORMAT = DecimalFormat("##0")
        val DOUBLE_FORMAT = DecimalFormat("##0.000")
        val PERCENT_FORMAT = DecimalFormat("##0.000%")

        val INVALID: PseudoClass = PseudoClass.getPseudoClass("invalid")

        @JvmStatic
        fun main(args: Array<String>) {
            launch(KImageApplication2::class.java)
        }
    }

    fun openImageFiles(initialDirectory: File, title: String = "Open Images"): List<File>? {
        val fileChooser = FileChooser()
        fileChooser.initialDirectory = ensureDirectoryExists(initialDirectory)
        fileChooser.title = title
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Image", "*.tif", "*.tiff", "*.png", "*.jpg", "*.jpeg", "*.fits", "*.fit", "*.rwz", "*.rw2", "*.cr2", "*.cr3", "*.nrw", "*.eip", "*.raf", "*.erf", "*.arw", "*.k25", "*.dng", "*.srf", "*.dcr", "*.raw", "*.crf", "*.bay"))
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Bitmap Image", "*.tif", "*.tiff", "*.png", "*.jpg", "*.jpeg", "*.fits", "*.fit"))
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("RAW Image", "*.rwz", "*.rw2", "*.cr2", "*.cr3", "*.nrw", "*.eip", "*.raf", "*.erf", "*.arw", "*.k25", "*.dng", "*.srf", "*.dcr", "*.raw", "*.crf", "*.bay"))

        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("All", "*"))
        return fileChooser.showOpenMultipleDialog(primaryStage)
    }

    private fun runWithProgressDialog(title: String, message: String, sleepMillis: Long, function: (Progress) -> Unit) {
        val progress = ProgressProperties()
        var progressDialog: ProgressDialog? = null
        var finished = false

        Thread {
            try {
                function(progress)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            } finally {
                synchronized(this) {
                    finished = true
                }
                Platform.runLater {
                    synchronized(this) {
                        progressDialog?.close()
                        progressDialog = null
                    }
                }
            }
        }.start()

        Thread.sleep(sleepMillis)
        synchronized(this) {
            if (!finished) {
                Platform.runLater {
                    val progressBar = ProgressBar()
                    val dialogContent = vbox(SPACING) {
                        padding = Insets(10.0)
                        children += node(progressBar) {
                            prefWidth = 200.0
                        }
                        children += label(message)
                    }
                    progress.totalProperty.addListener { _, _, value ->
                        progressBar.progress = progress.progressPercent()
                    }
                    progress.stepProperty.addListener { _, _, value ->
                        progressBar.progress = progress.progressPercent()
                    }
                    if (progress.totalProperty.get() > 0) {
                        progressBar.progress = progress.progressPercent()
                    }

                    progressDialog = ProgressDialog(dialogContent, title)

                    progressDialog?.show()
                }
            }
        }
    }

    private fun ensureDirectoryExists(initialDirectory: File): File {
        var directory = initialDirectory
        while (!directory.isDirectory) {
            val parent = directory.parent ?: return File(".")
            directory = File(parent)
        }
        return directory
    }
}

