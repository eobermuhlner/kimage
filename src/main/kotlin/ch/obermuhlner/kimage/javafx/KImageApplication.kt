package ch.obermuhlner.kimage.javafx

import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.io.ImageReader
import ch.obermuhlner.kotlin.javafx.*
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.css.PseudoClass
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import javafx.scene.web.WebView
import javafx.stage.*
import javafx.util.converter.IntegerStringConverter
import org.kordamp.ikonli.javafx.FontIcon
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.text.DecimalFormat


class KImageApplication : Application() {

    private lateinit var primaryStage: Stage

    private val inputImageView = ImageView()
    private val outputImageView = ImageView()
    private val commandArgumentEditor = VBox(SPACING)
    private val logTextArea = TextArea()
    private val docuTextArea = TextArea()
    private val docuWebView = WebView()
    private val inputDirectoryProperty = SimpleStringProperty(Paths.get(System.getProperty("user.home", ".")).toString())
    private val useInputDirectoryAsOutputDirectoryProperty = SimpleBooleanProperty(true)
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

        inputDirectoryProperty.addListener { _, _, value ->
            if (useInputDirectoryAsOutputDirectoryProperty.get()) {
                outputDirectoryProperty.set(value)
            }
        }
        useInputDirectoryAsOutputDirectoryProperty.addListener { _, _, value ->
            if (value) {
                outputDirectoryProperty.set(inputDirectoryProperty.get())
            }
        }

        scriptNames.setAll(KImageManager.scriptNames)
        updateOutputDirectoryFiles(outputHideOldFilesProperty.get())

        primaryStage.scene = scene
        primaryStage.show()
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

                                prefWidth = ARGUMENT_EDITOR_WIDTH.toDouble()
                                prefHeight = ARGUMENT_EDITOR_HEIGHT.toDouble()
                                children += commandArgumentEditor
                            }
                        }
                    }
                    row {
                        cell(2, 1) {
                            tabpane {
                                prefHeight = 400.0

                                tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
                                tabs += tab("Log") {
                                    content = node(logTextArea) {
                                        isEditable = false
                                    }
                                }
                                tabs += tab("Documentation HTML") {
                                    content = node(docuWebView) {
                                    }
                                }
                                tabs += tab("Documentation Markdown") {
                                    content = node(docuTextArea) {
                                        isEditable = false
                                    }
                                }
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
                children += button(FontIcon()) {
                    id = "files-icon"
                    tooltip = Tooltip("Add input image files to be processed.")
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
                children += button(FontIcon()) {
                    id = "clear-icon"
                    tooltip = Tooltip("Clear the list of input image files.")
                    onAction = EventHandler {
                        inputFiles.clear()
                    }
                }
            }

            children += tableview(inputFiles) {
                minWidth = FILE_TABLE_WIDTH.toDouble()
                minHeight = FILE_TABLE_HEIGHT.toDouble()

                setRowFactory {
                    val tableRow = object: TableRow<File>() {
                        override fun updateItem(item: File?, empty: Boolean) {
                            super.updateItem(item, empty)
                            tooltip = if (item == null) null else Tooltip(item.toString())
                        }
                    }
                    tableRow.contextMenu = ContextMenu(
                        menuitem("Remove", FontIcon()) {
                            id = "remove-icon"
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
                column<Number>("Size", { file -> ReadOnlyLongWrapper(Files.size(file.toPath())) }) {
                    this.prefWidth = 100.0
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
                children += togglebutton(FontIcon()) {
                    id = "bold-arrow-right-icon"
                    tooltip = Tooltip("Use input directory as output directory.")
                    selectedProperty().bindBidirectional(useInputDirectoryAsOutputDirectoryProperty)
                }
                children += textfield(outputDirectoryProperty) {
                    disableProperty().bind(useInputDirectoryAsOutputDirectoryProperty)
                    textProperty().addListener { _, _, value ->
                        val valid = File(value).isDirectory
                        pseudoClassStateChanged(INVALID, !valid)
                        if (valid) {
                            updateOutputDirectoryFiles(outputHideOldFilesProperty.get())
                        }
                    }
                }
                children += button(FontIcon()) {
                    id = "folder-icon"
                    tooltip = Tooltip("Select the output directory for processed image files.")
                    disableProperty().bind(useInputDirectoryAsOutputDirectoryProperty)
                    onAction = EventHandler {
                        val file = openDir(File(outputDirectoryProperty.get()))
                        file?.let {
                            outputDirectoryProperty.set(it.path)
                        }
                    }
                }
                children += button(FontIcon()) {
                    id = "arrow-up-icon"
                    tooltip = Tooltip("Go to parent directory.")
                    disableProperty().bind(useInputDirectoryAsOutputDirectoryProperty)
                    onAction = EventHandler {
                        val file = File(outputDirectoryProperty.get()).parentFile
                        file?.let {
                            outputDirectoryProperty.set(it.path)
                        }
                    }
                }
                children += togglebutton(FontIcon()) {
                    id = "hide-icon"
                    tooltip = Tooltip("Hide already existing files and show only newly added output files.")
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
                selectionModel.selectionMode = SelectionMode.MULTIPLE

                setRowFactory {
                    val tableRow = object: TableRow<File>() {
                        override fun updateItem(item: File?, empty: Boolean) {
                            super.updateItem(item, empty)
                            tooltip = if (item == null) null else Tooltip(item.toString())
                        }
                    }
                    tableRow.contextMenu = ContextMenu(
                        menuitem("Delete", FontIcon()) {
                            id = "delete-forever-icon"
                            onAction = EventHandler {
                                outputFiles.remove(tableRow.item)
                                updateImageView(outputImageView, null)
                                tableRow.item.delete()
                            }
                        }
                    )
                    tableRow
                }

                column<String>("Name", { file -> ReadOnlyStringWrapper(file?.name) }) {
                    this.prefWidth = 200.0
                }
                column<Number>("Size", { file -> ReadOnlyLongWrapper(Files.size(file.toPath())) }) {
                    this.prefWidth = 100.0
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
            try {
                val image = ImageReader.read(selectedFile)
                val writableImage = JavaFXImageUtil.toWritableImage(image)
                imageView.image = writableImage
            } catch (ex: Exception) {
                // ignore
            }
        }
    }

    private fun showCommandEditor(command: String) {
        commandArgumentEditor.children.clear()

        val verboseModeProperty = SimpleBooleanProperty(false)
        val debugModeProperty = SimpleBooleanProperty(false)

        node(commandArgumentEditor) {
            val script = KImageManager.script(command)

            if (script is ScriptV0_1) {
                val docu = script.documentation(false)
                docuTextArea.text = docu

                val options = MutableDataSet()
                val parser: Parser = Parser.builder(options).build()
                val renderer = HtmlRenderer.builder(options).build()
                val html = renderer.render(parser.parse(docu))
                docuWebView.engine.loadContent(html)

                val argumentStrings = mutableMapOf<String, String>()

                children += label(script.title) {
                    styleClass += "header2"
                }

                children += scrollpane {
                    content = gridpane {
                        padding = Insets(SPACING)
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
                                                tooltip = Tooltip(argument.description)
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
                                                tooltip = Tooltip(argument.description)
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
                                                tooltip = Tooltip(argument.description)
                                                textFormatter = textFormatter(argument.default, argument.min, argument.max)
                                                textProperty().addListener { _, _, value ->
                                                    argumentStrings[argument.name] = value
                                                }
                                            }
                                        }
                                        is ScriptStringArg -> {
                                            if (argument.allowed.isNotEmpty()) {
                                                combobox(argument.allowed) {
                                                    tooltip = Tooltip(argument.description)
                                                    value = argument.default
                                                    selectionModel.selectedItemProperty().addListener { _, _, value ->
                                                        argumentStrings[argument.name] = value
                                                    }
                                                }
                                            } else {
                                                textfield {
                                                    tooltip = Tooltip(argument.description)
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
                                            hbox {
                                                val fileProperty = SimpleStringProperty()
                                                fileProperty.addListener { _, _, value ->
                                                    if (value.isNullOrEmpty()) {
                                                        argumentStrings.remove(argument.name)
                                                    } else {
                                                        argumentStrings[argument.name] = File(inputDirectoryProperty.get(), value).toString()
                                                    }
                                                }
                                                children += textfield(fileProperty) {
                                                    // TODO relative to input or output dir?
                                                    tooltip = Tooltip(argument.description)
                                                    text = argument.default?.toString()

                                                    fun validate(value: String?) {
                                                        pseudoClassStateChanged(INVALID, !argument.isValid(value?:"", inputDirectoryProperty.get()))
                                                    }
                                                    textProperty().addListener { _, _, value ->
                                                        validate(value)
                                                    }
                                                    validate(fileProperty.get())
                                                }
                                                children += button(FontIcon()) {
                                                    if (argument.isDirectory == true) {
                                                        id =  "folder-icon"
                                                        tooltip = Tooltip("Select the directory for ${argument.name}.")
                                                    } else {
                                                        id =  "file-icon"
                                                        tooltip = Tooltip("Select the file for ${argument.name}.")
                                                    }
                                                    onAction = EventHandler {
                                                        if (argument.isDirectory == true) {
                                                            val file = openDir(File(inputDirectoryProperty.get()))
                                                            file?.let {
                                                                fileProperty.set(it.toRelativeString(File(inputDirectoryProperty.get())))
                                                            }
                                                        } else {
                                                            val file = openFile(File(inputDirectoryProperty.get()))
                                                            file?.let {
                                                                fileProperty.set(it.toRelativeString(File(inputDirectoryProperty.get())))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        is ScriptImageArg -> {
                                            hbox {
                                                val fileProperty = SimpleStringProperty()
                                                fileProperty.addListener { _, _, value ->
                                                    if (value.isNullOrEmpty()) {
                                                        argumentStrings.remove(argument.name)
                                                    } else {
                                                        argumentStrings[argument.name] = File(inputDirectoryProperty.get(), value).toString()
                                                    }
                                                }
                                                children += textfield(fileProperty) {
                                                    tooltip = Tooltip(argument.description)
                                                    text = argument.default?.toString()
                                                    textProperty().addListener { _, _, value ->
                                                        pseudoClassStateChanged(INVALID, !argument.isValid(value, inputDirectoryProperty.get()))
                                                    }
                                                }
                                                children += button(FontIcon()) {
                                                    id =  "file-icon"
                                                    tooltip = Tooltip("Select the image file for ${argument.name}.")
                                                    onAction = EventHandler {
                                                        val file = openFile(File(inputDirectoryProperty.get()))
                                                        file?.let {
                                                            fileProperty.set(it.toRelativeString(File(inputDirectoryProperty.get())))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        else -> {
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
                }

                children += button(FontIcon()) {
                    id = "play-icon"
                    tooltip = Tooltip("Run the ${script.name} script.")
                    onAction = EventHandler {
                        isDisable = true
                        updateOutputDirectoryFiles(outputHideOldFilesProperty.get())

                        val dialogContent = vbox(SPACING) {
                            padding = Insets(10.0)
                            children += node(ProgressBar()) {
                                prefWidth = 200.0
                            }
                            children += label(script.title)
                        }
                        val progressDialog = ProgressDialog(dialogContent, "Running ${script.name}")
                        progressDialog.show()

                        Thread {
                            val systemOut = System.out
                            try {
                                logTextArea.clear()
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
                            } catch (ex: Exception) {
                                println()
                                ex.printStackTrace(System.out)
                            } finally {
                                System.setOut(systemOut)
                                Platform.runLater {
                                    updateOutputDirectoryFiles(false)
                                    this@button.isDisable = false
                                    progressDialog.close()
                                }
                            }
                        }.start()
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

        val files = dir.listFiles { f -> f.isFile }.toMutableList()
        if (hideOldFiles) {
            hiddenOutputFiles.clear()
            hiddenOutputFiles.addAll(files)
            files.clear()
        } else {
            files.removeIf { f -> hiddenOutputFiles.contains(f) }
        }

        outputFiles.addAll(files)
    }

    fun openImageFiles(initialDirectory: File, title: String = "Open Images"): List<File>? {
        val fileChooser = FileChooser()
        fileChooser.initialDirectory = initialDirectory
        fileChooser.title = title
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Image", "*.tif", "*.tiff", "*.png", "*.jpg", "*.jpeg"))
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("All", "*"))
        return fileChooser.showOpenMultipleDialog(primaryStage)
    }

    fun openFile(initialDirectory: File, title: String = "Select File"): File? {
        val fileChooser = FileChooser()
        fileChooser.initialDirectory = initialDirectory
        fileChooser.title = title
        return fileChooser.showOpenDialog(primaryStage)
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

        val INVALID: PseudoClass = PseudoClass.getPseudoClass("invalid")

        @JvmStatic
        fun main(args: Array<String>) {
            launch(KImageApplication::class.java)
        }
    }
}

class ProgressDialog(private val content: Parent, private val title: String? = null) {

    private val dialogStage = Stage()

    fun show() {
        dialogStage.initStyle(StageStyle.UTILITY)
        dialogStage.isResizable = false
        dialogStage.title = title
        dialogStage.initModality(Modality.APPLICATION_MODAL)

        dialogStage.scene = Scene(content)
        dialogStage.sizeToScene()
        dialogStage.show()
    }

    fun close() {
        dialogStage.close()
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