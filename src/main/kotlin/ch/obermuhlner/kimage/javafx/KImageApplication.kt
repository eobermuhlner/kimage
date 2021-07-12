package ch.obermuhlner.kimage.javafx

import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.io.ImageReader
import ch.obermuhlner.kotlin.javafx.*
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.css.PseudoClass
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.util.converter.IntegerStringConverter
import java.io.*
import java.nio.file.Paths
import java.text.DecimalFormat

class KImageApplication : Application() {

    private lateinit var primaryStage: Stage

    private val inputImageView = ImageView()
    private val outputImageView = ImageView()
    private val commandArgumentEditor = VBox(SPACING)
    private val logTextArea = TextArea()

    private val inputDirectoryProperty = SimpleStringProperty(Paths.get(System.getProperty("user.home", ".")).toString())
    private val outputDirectoryProperty = SimpleStringProperty(Paths.get(System.getProperty("user.home", ".")).toString())
    private val outputHideOldFilesProperty = SimpleBooleanProperty()

    private val inputFiles = FXCollections.observableArrayList<File>()
    private val scriptNames = FXCollections.observableArrayList<String>()
    private val outputFiles = FXCollections.observableArrayList<File>()
    private val hiddenOutputFiles = mutableListOf<File>()

    override fun start(primaryStage: Stage) {
        this.primaryStage = primaryStage

        val root = VBox()
        val scene = Scene(root)

        root.children += createMainEditor()

        scene.stylesheets.add(KImageApplication::class.java.getResource("/application.css").toExternalForm());

        primaryStage.scene = scene
        primaryStage.show()

        scriptNames.setAll(KImageManager.scriptNames)
        updateOutputDirectoryFiles(outputHideOldFilesProperty.get())
    }

    private fun createMainEditor(): Node {
        return borderpane {
            left = createInputFilesEditor()

            center = borderpane {
                padding = Insets(0.0, SPACING, 0.0, SPACING)

                top = label("Processing:") {
                    styleClass += "header1"
                }

                center = gridpane {
                    row {
                        cell {
                            listview(scriptNames) {
                                selectionModel.selectedItemProperty().addListener { _, _, selected ->
                                    selected?.let {
                                        showCommandEditor(it)
                                    }
                                }
                            }
                        }
                        cell {
                            vbox(SPACING) {
                                padding = Insets(0.0, SPACING, 0.0, SPACING)

                                minWidth = ARGUMENT_EDITOR_WIDTH.toDouble()
                                minHeight = ARGUMENT_EDITOR_HEIGHT.toDouble()
                                children += commandArgumentEditor
                            }
                        }
                    }
                    row {
                        cell(2, 1) {
                            node(logTextArea) {
                            }
                        }
                    }
                }
            }

            right = createOutputFilesEditor()
        }
    }

    private fun createInputFilesEditor(): Node {
        return vbox(SPACING) {
            children += label("Input Files:") {
                styleClass += "header1"
            }

            children += hbox(SPACING) {
                children += button("Add...") {
                    onAction = EventHandler {
                        val files = openImageFiles(File(inputDirectoryProperty.get()))
                        files?.let {
                            if (it.isNotEmpty()) {
                                inputDirectoryProperty.set(it[0].parent)
                            }
                            inputFiles.addAll(it)
                        }
                    }
                }
                children += button("Clear") {
                    onAction = EventHandler {
                        inputFiles.clear()
                    }
                }
            }

            children += tableview(inputFiles) {
                minWidth = FILE_TABLE_WIDTH.toDouble()
                minHeight = FILE_TABLE_HEIGHT.toDouble()

                setRowFactory {
                    val tableRow = TableRow<File>()
                    tableRow.contextMenu = ContextMenu(
                        menuitem("Remove") {
                            onAction = EventHandler {
                                inputFiles.remove(tableRow.item)
                                updateImageView(inputImageView, null)
                            }
                        }
                    )
                    tableRow
                }

                column<String>("Name", { file -> ReadOnlyStringWrapper(file?.name) }) {
                    this.prefWidth = 200.0
                }
                column<String>("Path", { file -> ReadOnlyStringWrapper(file?.path) }) {
                    this.prefWidth = 200.0
                }

                selectionModel.selectedItemProperty().addListener { _, _, selected ->
                    updateImageView(inputImageView, selected)
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
                styleClass += "header1"
            }

            children += hbox(SPACING) {
                children += label("Directory:")
                children += textfield(outputDirectoryProperty) {
                    textProperty().addListener { _, _, _ ->
                        updateOutputDirectoryFiles(outputHideOldFilesProperty.get())
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
                children += togglebutton("Hide") {
                    selectedProperty().bindBidirectional(outputHideOldFilesProperty)
                    outputHideOldFilesProperty.addListener { _, _, value ->
                        if (value == false) {
                            hiddenOutputFiles.clear()
                        }
                        updateOutputDirectoryFiles(value)
                    }
                }
            }

            children += tableview(outputFiles) {
                minWidth = FILE_TABLE_WIDTH.toDouble()
                minHeight = FILE_TABLE_HEIGHT.toDouble()

                column<String>("Name", { file -> ReadOnlyStringWrapper(file?.name) }) {
                    this.prefWidth = 200.0
                }
                column<String>("Path", { file -> ReadOnlyStringWrapper(file?.path) }) {
                    this.prefWidth = 200.0
                }

                selectionModel.selectedItemProperty().addListener { _, _, selected ->
                    updateImageView(outputImageView, selected)
                }
            }

            children += node(outputImageView) {
                isPreserveRatio = true
                fitWidth = IMAGE_WIDTH.toDouble()
                fitHeight = IMAGE_HEIGHT.toDouble()
            }
        }
    }

    private fun updateImageView(imageView: ImageView, selectedFile: File?) {
        if (selectedFile == null) {
            imageView.image = null
        } else {
            val image = ImageReader.read(selectedFile)
            val writableImage = JavaFXImageUtil.toWritableImage(image)
            imageView.image = writableImage
        }
    }

    private fun showCommandEditor(command: String) {
        commandArgumentEditor.children.clear()

        val verboseModeProperty = SimpleBooleanProperty(false)
        val debugModeProperty = SimpleBooleanProperty(false)

        node(commandArgumentEditor) {
            val script = KImageManager.script(command)

            if (script is ScriptV0_1) {
                children += label(script.title) {
                    styleClass += "header2"
                }

                val argumentStrings = mutableMapOf<String, String>()
                children += gridpane {
                    hgap = SPACING
                    vgap = SPACING

                    for (argument in script.scriptArguments.arguments) {
                        row {
                            cell {
                                label(argument.name) {
                                }
                            }
                            cell {
                                when (argument) {
                                    is ScriptBooleanArg -> {
                                        checkbox {
                                            argument.default?.let {
                                                isSelected = it
                                            }
                                            textProperty().addListener { _, _, value ->
                                                argumentStrings[argument.name] = value
                                            }
                                        }
                                    }
                                    is ScriptIntArg -> {
                                        textfield {
                                            textFormatter = TextFormatter(IntegerStringConverter(), argument.default) { change ->
                                                filter(change, Regex("-?[0-9]*"))
                                            }
                                            textProperty().addListener { _, _, value ->
                                                argumentStrings[argument.name] = value
                                            }
                                        }
                                    }
                                    is ScriptDoubleArg -> {
                                        textfield {
                                            textFormatter = textFormatter(argument.default, argument.min, argument.max)
                                            textProperty().addListener { _, _, value ->
                                                argumentStrings[argument.name] = value
                                            }
                                        }
                                    }
                                    is ScriptStringArg -> {
                                        if (argument.allowed.isNotEmpty()) {
                                            combobox(argument.allowed) {
                                                value = argument.default
                                                selectionModel.selectedItemProperty().addListener { _, _, value ->
                                                    argumentStrings[argument.name] = value
                                                }
                                            }
                                        } else {
                                            textfield {
                                                textProperty().addListener { _, _, value ->
                                                    argumentStrings[argument.name] = value
                                                }
                                                argument.regex?.let {
                                                    textFormatter = TextFormatter(TextFormatter.IDENTITY_STRING_CONVERTER, argument.default) { change ->
                                                        filter(change, Regex(it))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    is ScriptFileArg -> {
                                        textfield {
                                            text = argument.default?.toString()
                                            textProperty().addListener { _, _, value ->
                                                pseudoClassStateChanged(INVALID, !argument.isValid(value, inputDirectoryProperty.get()))
                                                argumentStrings[argument.name] = value
                                            }
                                        }
                                        button() {

                                        }
                                    }
                                    else -> {
                                        textfield {
                                            textProperty().addListener { _, _, value ->
                                                argumentStrings[argument.name] = value
                                            }
                                        }
                                    }
                                }.apply {
                                    tooltip = Tooltip(argument.description)
                                }
                            }
                        }
                    }

                    row {
                        cell {
                            label("Verbose")
                        }
                        cell {
                            checkbox(verboseModeProperty) {
                            }
                        }
                    }
                    row {
                        cell {
                            label("Debug")
                        }
                        cell {
                            checkbox(debugModeProperty) {
                            }
                        }
                    }

                }

                children += button("Run") {
                    onAction = EventHandler {
                        isDisable = true
                        Thread {
                            val systemOut = System.out
                            try {
                                System.setOut(PrintStream(LogOutputStream(logTextArea)))

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
                            } finally {
                                System.setOut(systemOut)
                                Platform.runLater {
                                    this@button.isDisable = false
                                }
                            }
                        }.start()

                        updateOutputDirectoryFiles(false)
                    }
                }
            }
        }
    }

    private fun filter(change: TextFormatter.Change, regex: Regex, func: (TextFormatter.Change) -> Boolean = { true }): TextFormatter.Change? {
        if (change.controlNewText.matches(regex)) {
            return change
        } else {
            return null
        }
    }

    private fun updateOutputDirectoryFiles(hideOldFiles: Boolean) {
        outputFiles.clear()

        val dir = File(outputDirectoryProperty.get())
        if (!dir.isDirectory) {
            return
        }

        val files = dir.listFiles { f -> f.isFile }
        if (hideOldFiles) {
            hiddenOutputFiles.clear()
            hiddenOutputFiles.addAll(files)
        } else {
            outputFiles.addAll(files)
        }
    }

    fun openImageFiles(initialDirectory: File, title: String = "Open Images"): List<File>? {
        val fileChooser = FileChooser()
        fileChooser.initialDirectory = initialDirectory
        fileChooser.title = title
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Image", "*.tif", "*.tiff", "*.png", "*.jpg", "*.jpeg"))
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("All", "*"))
        return fileChooser.showOpenMultipleDialog(primaryStage)
    }

    fun openDir(initialDirectory: File, title: String = "Select Directory"): File? {
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

        val INTEGER_FORMAT = DecimalFormat("##0")
        val DOUBLE_FORMAT = DecimalFormat("##0.000")
        val PERCENT_FORMAT = DecimalFormat("##0.000%")

        val INVALID = PseudoClass.getPseudoClass("invalid")

        @JvmStatic
        fun main(args: Array<String>) {
            launch(KImageApplication::class.java)
        }
    }
}

class LogOutputStream(private val textArea: TextArea) : OutputStream() {
    private val buffer = mutableListOf<Byte>()

    override fun write(b: Int) {
        buffer.add(b.toByte())
        if (b.toChar() == '\n') {
            val string = String(buffer.toByteArray())
            buffer.clear()

            Platform.runLater {
                textArea.appendText(string)
            }
        }
    }
}