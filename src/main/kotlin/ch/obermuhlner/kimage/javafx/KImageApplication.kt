package ch.obermuhlner.kimage.javafx

import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.math.clamp
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
import javafx.scene.image.WritableImage
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.web.WebView
import javafx.stage.*
import javafx.util.converter.IntegerStringConverter
import org.kordamp.ikonli.javafx.FontIcon
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.text.DecimalFormat
import kotlin.math.max
import kotlin.math.min
import ch.obermuhlner.kimage.math.clamp as clamp


class KImageApplication : Application() {

    private lateinit var primaryStage: Stage

    private val inputZoomImage = WritableImage(ZOOM_WIDTH, ZOOM_HEIGHT)
    private val outputZoomImage = WritableImage(ZOOM_WIDTH, ZOOM_HEIGHT)
    private val deltaZoomImage = WritableImage(ZOOM_WIDTH, ZOOM_HEIGHT)
    private val dummyImage = WritableImage(1, 1)

    private val inputImageView = ImageView(dummyImage)
    private val outputImageView = ImageView(dummyImage)

    private val inputZoomImageView = ImageView(inputZoomImage)
    private val outputZoomImageView = ImageView(outputZoomImage)
    private val deltaZoomImageView = ImageView(deltaZoomImage)

    private val infoTabPane = TabPane()
    private lateinit var infoTabLog: Tab
    private lateinit var infoTabDocu: Tab
    private lateinit var infoTabZoom: Tab

    private val commandArgumentEditor = VBox(SPACING)
    private val logTextArea = TextArea().apply {
        font = Font.font("monospace")
    }
    private val docuTextArea = TextArea()
    private val docuWebView = WebView()
    private val codeTextArea = TextArea().apply {
        font = Font.font("monospace")
    }

    private val zoomCenterXProperty = SimpleIntegerProperty()
    private val zoomCenterYProperty = SimpleIntegerProperty()

    private val zoomDeltaFactorProperty = SimpleDoubleProperty()

    private val inputDirectoryProperty = SimpleStringProperty(Paths.get(".").toString())
    private val useInputDirectoryAsOutputDirectoryProperty = SimpleBooleanProperty(true)
    private val outputDirectoryProperty = SimpleStringProperty(Paths.get(".").toString())
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
        if (scriptNames.isNotEmpty()) {
            KImageManager.script(scriptNames[0]) // trigger compilation of first script
        }
        updateOutputDirectoryFiles(outputHideOldFilesProperty.get())

        setupImageSelectionListener(inputImageView)
        setupImageSelectionListener(outputImageView)
        setupZoomDragEvents(inputZoomImageView)
        setupZoomDragEvents(deltaZoomImageView)
        setupZoomDragEvents(outputZoomImageView)

        zoomCenterXProperty.addListener { _, _, _ ->
            updateZoom()
        }
        zoomCenterYProperty.addListener { _, _, _ ->
            updateZoom()
        }
        zoomDeltaFactorProperty.addListener { _, _, _ ->
            updateZoom()
        }

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
                            node(infoTabPane) {
                                prefHeight = 400.0

                                tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
                                tabs += tab("Log") {
                                    infoTabLog = this
                                    content = node(logTextArea) {
                                        isEditable = false
                                    }
                                }
                                tabs += tab("Documentation HTML") {
                                    infoTabDocu = this
                                    content = node(docuWebView) {
                                    }
                                }
                                tabs += tab("Documentation Markdown") {
                                    content = node(docuTextArea) {
                                        isEditable = false
                                    }
                                }
                                tabs += tab("Code") {
                                    content = node(codeTextArea) {
                                        isEditable = false
                                    }
                                }
                                tabs += tab("Image Zoom") {
                                    infoTabZoom = this
                                    content = createZoomViewer()
                                }
                            }
                        }
                    }
                }
            }

            right = createOutputFilesEditor()
        }
    }

    private fun createZoomViewer(): Node {
        return gridpane {
            padding = Insets(SPACING)
            hgap = SPACING
            vgap = SPACING

            row {
                cell {
                    label("Input Zoom:")
                }
                cell {
                    label("Delta Zoom:")
                }
                cell {
                    label("Output Zoom:")
                }
            }
            row {
                cell {
                    inputZoomImageView
                }
                cell {
                    deltaZoomImageView
                }
                cell {
                    outputZoomImageView
                }
            }
            row {
                cell {
                    label("")
                }
                cell {
                    vbox(SPACING) {
                        children += slider(1.0, 10.0, 5.0) {
                            tooltip = Tooltip("Factor used to exaggerate the delta value.")
                            isShowTickMarks = true
                            isShowTickLabels = true
                            majorTickUnit = 1.0
                            minorTickCount = 10
                            prefWidth = 80.0
                            zoomDeltaFactorProperty.bind(valueProperty())
                        }
                        children += hbox(SPACING) {
                            children += label("X:")
                            children += textfield(zoomCenterXProperty) {
                                prefWidth = 80.0
                            }
                            children += label("Y:")
                            children += textfield(zoomCenterYProperty) {
                                prefWidth = 80.0
                            }
                        }
                    }
                }
                cell {
                    label("")
                }
            }
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
                selectionModel.selectionMode = SelectionMode.MULTIPLE

                setRowFactory {
                    val tableRow = object: TableRow<File>() {
                        override fun updateItem(item: File?, empty: Boolean) {
                            super.updateItem(item, empty)
                            tooltip = if (item == null) null else Tooltip(item.toString())
                        }
                    }
                    tableRow.contextMenu = ContextMenu(
                        menuitem("Remove from list", FontIcon()) {
                            id = "remove-icon"
                            onAction = EventHandler {
                                selectionModel.selectedItems.toList().forEach {
                                    inputFiles.remove(it)
                                }
                                updateImageView(inputImageView, selectionModel.selectedItem)
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
                        menuitem("Replace input", FontIcon()) {
                            id = "bold-arrow-left-icon"
                            onAction = EventHandler {
                                inputFiles.clear()
                                selectionModel.selectedItems.toList().forEach {
                                    inputFiles.add(it)
                                }
                            }
                        },
                        menuitem("Delete file forever", FontIcon()) {
                            id = "delete-forever-icon"
                            onAction = EventHandler {
                                selectionModel.selectedItems.toList().forEach {
                                    outputFiles.remove(it)
                                    it.delete()
                                }
                                updateImageView(outputImageView, selectionModel.selectedItem)
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
            imageView.image = dummyImage
            updateZoom()
        } else {
            try {
                runWithProgressDialog("Load Image", "Loading image $selectedFile") {
                    val image = ImageReader.read(selectedFile)
                    val writableImage = JavaFXImageUtil.toWritableImage(image)

                    Platform.runLater {
                        imageView.image = writableImage
                        updateZoom()
                    }
                }
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
            runWithProgressDialog("Compiling Script", "Compiling script $command") {
                val script = KImageManager.script(command)

                Platform.runLater {
                    if (script is ScriptV0_1) {
                        val docu = script.documentation(false)
                        docuTextArea.text = docu

                        docuWebView.engine.loadContent(markdownToHtml(docu))

                        codeTextArea.text = script.code

                        infoTabPane.selectionModel.select(infoTabDocu)

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
                                                        tooltip = Tooltip(argument.tooltip())
                                                        argument.default?.let {
                                                            isSelected = it
                                                        }
                                                        selectedProperty().addListener { _, _, value ->
                                                            argumentStrings[argument.name] = value.toString()
                                                        }
                                                    }
                                                }
                                                is ScriptIntArg -> {
                                                    hbox {
                                                        val argProperty = SimpleStringProperty()
                                                        argProperty.addListener { _, _, value ->
                                                            argumentStrings[argument.name] = value.toString()
                                                        }
                                                        children += textfield {
                                                            tooltip = Tooltip(argument.tooltip())
                                                            textFormatter = TextFormatter(IntegerStringConverter(), argument.default) { change ->
                                                                filter(change, Regex("-?[0-9]*"))
                                                            }
                                                            textProperty().bindBidirectional(argProperty)
                                                            setupValidation(this, textProperty(), argument, argProperty)
                                                        }
                                                        setupHints(argument, argProperty)
                                                        argument.default?.let {
                                                            argProperty.set(it.toString())
                                                        }
                                                    }
                                                }
                                                is ScriptDoubleArg -> {
                                                    hbox {
                                                        val argProperty = SimpleStringProperty()
                                                        argProperty.addListener { _, _, value ->
                                                            argumentStrings[argument.name] = value.toString()
                                                        }
                                                        children += textfield {
                                                            tooltip = Tooltip(argument.tooltip())
                                                            textFormatter = textFormatter(argument.default, argument.min, argument.max)
                                                            textProperty().bindBidirectional(argProperty)
                                                            setupValidation(this, textProperty(), argument, argProperty)
                                                        }
                                                        setupHints(argument, argProperty)
                                                        argument.default?.let {
                                                            argProperty.set(it.toString())
                                                        }
                                                    }
                                                }
                                                is ScriptStringArg -> {
                                                    if (argument.allowed.isNotEmpty()) {
                                                        val allowed2 = mutableListOf<String>()
                                                        if (argument is ScriptOptionalStringArg) {
                                                            allowed2.add("")
                                                        }
                                                        allowed2.addAll(argument.allowed)
                                                        combobox(allowed2) {
                                                            tooltip = Tooltip(argument.tooltip())
                                                            value = argument.default
                                                            selectionModel.selectedItemProperty().addListener { _, _, value ->
                                                                argumentStrings[argument.name] = value
                                                            }
                                                        }
                                                    } else {
                                                        hbox {
                                                            val argProperty = SimpleStringProperty()
                                                            argProperty.addListener { _, _, value ->
                                                                argumentStrings[argument.name] = value.toString()
                                                            }
                                                            children += textfield {
                                                                tooltip = Tooltip(argument.tooltip())
                                                                argument.regex?.let {
                                                                    textFormatter = TextFormatter(TextFormatter.IDENTITY_STRING_CONVERTER, argument.default) { change ->
                                                                        filter(change, Regex(it))
                                                                    }
                                                                }
                                                                textProperty().bindBidirectional(argProperty)
                                                                setupValidation(this, textProperty(), argument, argProperty)
                                                            }
                                                            setupHints(argument, argProperty)
                                                            argument.default?.let {
                                                                argProperty.set(it.toString())
                                                            }
                                                        }
                                                    }
                                                }
                                                is ScriptFileArg -> {
                                                    hbox {
                                                        val argProperty = SimpleStringProperty()
                                                        argProperty.addListener { _, _, value ->
                                                            if (value.isNullOrEmpty()) {
                                                                argumentStrings.remove(argument.name)
                                                            } else {
                                                                argumentStrings[argument.name] = File(inputDirectoryProperty.get(), value).toString()
                                                            }
                                                        }
                                                        children += textfield(argProperty) {
                                                            // TODO relative to input or output dir?
                                                            tooltip = Tooltip(argument.tooltip())
                                                            text = argument.default?.toString()
                                                            setupValidation(this, textProperty(), argument, argProperty)
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
                                                                        argProperty.set(it.toRelativeString(File(inputDirectoryProperty.get())))
                                                                    }
                                                                } else {
                                                                    val file = openFile(File(inputDirectoryProperty.get()))
                                                                    file?.let {
                                                                        argProperty.set(it.toRelativeString(File(inputDirectoryProperty.get())))
                                                                    }
                                                                }
                                                            }
                                                            setupHints(argument, argProperty)
                                                            argument.default?.let {
                                                                argProperty.set(it.toString())
                                                            }
                                                        }
                                                    }
                                                }
                                                is ScriptImageArg -> {
                                                    hbox {
                                                        val argProperty = SimpleStringProperty()
                                                        argProperty.addListener { _, _, value ->
                                                            if (value.isNullOrEmpty()) {
                                                                argumentStrings.remove(argument.name)
                                                            } else {
                                                                argumentStrings[argument.name] = File(inputDirectoryProperty.get(), value).toString()
                                                            }
                                                        }
                                                        children += textfield(argProperty) {
                                                            tooltip = Tooltip(argument.tooltip())
                                                            text = argument.default?.toString()
                                                            setupValidation(this, textProperty(), argument, argProperty)
                                                        }
                                                        children += button(FontIcon()) {
                                                            id =  "file-icon"
                                                            tooltip = Tooltip("Select the image file for ${argument.name}.")
                                                            onAction = EventHandler {
                                                                val file = openFile(File(inputDirectoryProperty.get()))
                                                                file?.let {
                                                                    argProperty.set(it.toRelativeString(File(inputDirectoryProperty.get())))
                                                                }
                                                            }
                                                        }
                                                        setupHints(argument, argProperty)
                                                        argument.default?.let {
                                                            argProperty.set(it.toString())
                                                        }
                                                    }
                                                }
                                                else -> {
                                                    hbox {
                                                        val argProperty = SimpleStringProperty()
                                                        argProperty.addListener { _, _, value ->
                                                            argumentStrings[argument.name] = value.toString()
                                                        }
                                                        children += textfield {
                                                            tooltip = Tooltip(argument.tooltip())
                                                        }
                                                        setupHints(argument, argProperty)
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

                                infoTabPane.selectionModel.select(infoTabLog)

                                runWithProgressDialog("Running ${script.name}", script.title) {
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
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Pane.setupHints(
        argument: ScriptArg,
        argProperty: SimpleStringProperty
    ) {
        when (argument.hint) {
            Hint.ImageX -> {
                children += button("Take X") {
                    onAction = EventHandler {
                        argProperty.set(zoomCenterXProperty.get().toString())
                    }
                }
            }
            Hint.ImageY -> {
                children += button("Take Y") {
                    onAction = EventHandler {
                        argProperty.set(zoomCenterYProperty.get().toString())
                    }
                }
            }
            else -> {
            }
        }
    }

    private fun <T> setupValidation(node: Node, property: Property<T>, argument: ScriptArg, argProperty: StringProperty) {
        property.addListener { _, _, value ->
            node.pseudoClassStateChanged(INVALID, !argument.isValid(value?.toString()?:""))
        }
        node.pseudoClassStateChanged(INVALID, !argument.isValid(argProperty.get()?:""))
    }

    private fun markdownToHtml(markdown: String): String {
        val options = MutableDataSet()
        val parser: Parser = Parser.builder(options).build()
        val renderer = HtmlRenderer.builder(options).build()

        return renderer.render(parser.parse(markdown))
    }

    private fun runWithProgressDialog(title: String, message: String, function: () -> Unit) {
        var progressDialog: ProgressDialog? = null
        var finished = false
        var exception: Throwable? = null

        Thread {
            try {
                function()
            } catch (ex: Throwable) {
                exception = ex
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

        Thread.sleep(200)
        synchronized(this) {
            if (!finished) {
                val dialogContent = vbox(SPACING) {
                    padding = Insets(10.0)
                    children += node(ProgressBar()) {
                        prefWidth = 200.0
                    }
                    children += label(message)
                }

                progressDialog = ProgressDialog(dialogContent, title)
                progressDialog?.show()
            }
        }
    }

    private fun filter(change: TextFormatter.Change, regex: Regex, func: (TextFormatter.Change) -> Boolean = { true }): TextFormatter.Change? {
        return if (change.controlNewText.matches(regex) && func(change)) {
            change
        } else {
            null
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

    private fun setupImageSelectionListener(imageView: ImageView) {
        setMouseDragEvents(imageView) { event: MouseEvent ->
            val imageViewWidth = imageView.boundsInLocal.width
            val imageViewHeight = imageView.boundsInLocal.height

            var zoomX = (event.x * imageView.image.width / imageViewWidth).toInt()
            var zoomY = (event.y * imageView.image.height / imageViewHeight).toInt()

            zoomX = max(zoomX, 0)
            zoomY = max(zoomY, 0)
            zoomX = min(zoomX, imageView.image.width.toInt() - 1)
            zoomY = min(zoomY, imageView.image.height.toInt() - 1)

            setZoom(zoomX, zoomY)
        }
    }

    private fun setZoom(x: Int, y: Int) {
        zoomCenterXProperty.set(x)
        zoomCenterYProperty.set(y)
        infoTabPane.selectionModel.select(infoTabZoom)
        updateZoom(x, y)
    }

    private fun setMouseDragEvents(node: Node, handler: EventHandler<in MouseEvent>) {
        node.onMouseClicked = handler
        node.onMouseDragged = handler
        node.onMouseReleased = handler
    }

    private var zoomDragX: Double? = null
    private var zoomDragY: Double? = null
    private fun setupZoomDragEvents(imageView: ImageView) {
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
            zoomCenterXProperty.set(zoomX)
            zoomCenterYProperty.set(zoomY)
            updateZoom(zoomX, zoomY)
        }
        imageView.onMouseDragReleased = EventHandler {
            zoomDragX = null
            zoomDragY = null
        }
    }

    private fun updateZoom(zoomX: Int = zoomCenterXProperty.get(), zoomY: Int = zoomCenterYProperty.get()) {
        val inputImage = inputImageView.image
        val outputImage = outputImageView.image

        val inputImageWidth = inputImage.width.toInt() - 1
        val inputImageHeight = inputImage.height.toInt() - 1

        val outputImageWidth = outputImage.width.toInt() - 1
        val outputImageHeight = outputImage.height.toInt() - 1

        val zoomWidthHalf = ZOOM_WIDTH / 2
        val zoomHeightHalf = ZOOM_HEIGHT / 2

        val deltaFactor = zoomDeltaFactorProperty.get()

        for (y in 0 until ZOOM_HEIGHT) {
            for (x in 0 until ZOOM_WIDTH) {
                val xInput = clamp(zoomX + x - zoomWidthHalf, 0, inputImageWidth)
                val yInput = clamp(zoomY + y - zoomHeightHalf, 0, inputImageHeight)
                val rgbInput = inputImage.pixelReader.getColor(xInput, yInput)
                inputZoomImage.pixelWriter.setColor(x, y, rgbInput)

                val xOutput = clamp(zoomX + x - zoomWidthHalf, 0, outputImageWidth)
                val yOutput = clamp(zoomY + y - zoomHeightHalf, 0, outputImageHeight)
                val rgbOutput = outputImage.pixelReader.getColor(xOutput, yOutput)
                outputZoomImage.pixelWriter.setColor(x, y, rgbOutput)

                val rgbDelta = calculateDeltaColor(rgbInput, rgbOutput, deltaFactor)
                deltaZoomImage.pixelWriter.setColor(x, y, rgbDelta)
            }
        }
    }

    private fun calculateDeltaColor(rgb1: Color, rgb2: Color, deltaFactor: Double): Color {
        val delta = (rgb1.brightness - rgb2.brightness) * deltaFactor
        val exaggeratedDelta = clamp(exaggerate(delta), 0.0, 1.0)

        return if (delta < 0) {
            Color(exaggeratedDelta, 0.0, 0.0, 1.0)
        } else {
            Color(0.0, 0.0, exaggeratedDelta, 1.0)
        }
    }

    private fun exaggerate(x: Double): Double = -1/(x+0.5)+2

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