package ch.obermuhlner.kimage.javafx

import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.image.Channel
import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.crop
import ch.obermuhlner.kimage.math.clamp
import ch.obermuhlner.kimage.io.ImageReader
import ch.obermuhlner.kimage.io.ImageWriter
import ch.obermuhlner.kimage.math.SplineInterpolator
import ch.obermuhlner.kimage.matrix.DoubleMatrix
import ch.obermuhlner.kimage.matrix.Matrix
import ch.obermuhlner.kotlin.javafx.*
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.css.PseudoClass
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.web.WebView
import javafx.stage.*
import javafx.util.converter.IntegerStringConverter
import org.apache.commons.imaging.Imaging
import org.apache.commons.imaging.common.GenericImageMetadata
import org.apache.commons.imaging.common.ImageMetadata
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata
import org.kordamp.ikonli.javafx.FontIcon
import java.awt.Desktop
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.text.DecimalFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import ch.obermuhlner.kimage.math.clamp as clamp


class KImageApplication : Application() {

    private lateinit var primaryStage: Stage

    private val red = Color.color(1.0, 0.0, 0.0, 0.8)
    private val green = Color.color(0.0, 1.0, 0.0, 0.8)
    private val blue = Color.color(0.0, 0.0, 1.0, 0.8)

    private val inputZoomImage = WritableImage(ZOOM_WIDTH, ZOOM_HEIGHT)
    private val outputZoomImage = WritableImage(ZOOM_WIDTH, ZOOM_HEIGHT)
    private val deltaZoomImage = WritableImage(ZOOM_WIDTH, ZOOM_HEIGHT)
    private val dummyImage = WritableImage(1, 1)

    private val inputImageView = ImageView(dummyImage)
    private val outputImageView = ImageView(dummyImage)

    private val inputHistogramCanvas = Canvas(HISTOGRAM_WIDTH.toDouble(), HISTOGRAM_HEIGHT.toDouble())
    private val outputHistogramCanvas = Canvas(HISTOGRAM_WIDTH.toDouble(), HISTOGRAM_HEIGHT.toDouble())

    private val inputZoomImageView = ImageView(inputZoomImage)
    private val outputZoomImageView = ImageView(outputZoomImage)
    private val deltaZoomImageView = ImageView(deltaZoomImage)

    private val inputZoomHistogramCanvas = Canvas(HISTOGRAM_WIDTH.toDouble(), HISTOGRAM_HEIGHT.toDouble())
    private val outputZoomHistogramCanvas = Canvas(HISTOGRAM_WIDTH.toDouble(), HISTOGRAM_HEIGHT.toDouble())

    private var currentInputImage: Image? = null

    private val scriptNameProperty = SimpleStringProperty()
    private val argumentsProperty = SimpleMapProperty<String, Any>(FXCollections.observableHashMap())
    private val commandPresetsNames = FXCollections.observableArrayList<String>()
    private val commandPresets = mutableMapOf<String, Map<String, Any>>()

    private var previewScript: ScriptV0_1? = null
    private var previewCommand: String? = null

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

    private val inputDecorationsPane: Pane = Pane()
    private val outputDecorationsPane: Pane = Pane()

    private val inputImageWidthProperty = SimpleIntegerProperty()
    private val inputImageHeightProperty = SimpleIntegerProperty()
    private val inputImageModelProperty = SimpleStringProperty()
    private val inputImageLensModelProperty = SimpleStringProperty()
    private val inputImageExposureTimeProperty = SimpleStringProperty()
    private val inputImagePhotographicSensitivityProperty = SimpleStringProperty()
    private val inputImageApertureValueProperty = SimpleStringProperty()
    private val inputImageBitsPerSampleProperty = SimpleStringProperty()

    private val inputHistogramXProperty = SimpleDoubleProperty()
    private val inputHistogramYRedProperty = SimpleDoubleProperty()
    private val inputHistogramYGreenProperty = SimpleDoubleProperty()
    private val inputHistogramYBlueProperty = SimpleDoubleProperty()

    private val outputImageWidthProperty = SimpleIntegerProperty()
    private val outputImageHeightProperty = SimpleIntegerProperty()
    private val outputImageModelProperty = SimpleStringProperty()
    private val outputImageLensModelProperty = SimpleStringProperty()
    private val outputImageExposureTimeProperty = SimpleStringProperty()
    private val outputImagePhotographicSensitivityProperty = SimpleStringProperty()
    private val outputImageApertureValueProperty = SimpleStringProperty()
    private val outputImageBitsPerSampleProperty = SimpleStringProperty()

    private val outputHistogramXProperty = SimpleDoubleProperty()
    private val outputHistogramYRedProperty = SimpleDoubleProperty()
    private val outputHistogramYGreenProperty = SimpleDoubleProperty()
    private val outputHistogramYBlueProperty = SimpleDoubleProperty()

    private val crosshairColors = listOf(Color.YELLOW, Color.RED, Color.GREEN, Color.BLUE, Color.TRANSPARENT)
    private val crosshairColorProperty: ObjectProperty<Color> = SimpleObjectProperty(crosshairColors[0])

    private val zoomCenterXProperty = SimpleIntegerProperty()
    private val zoomCenterYProperty = SimpleIntegerProperty()
    private val zoomFactorProperty = SimpleIntegerProperty(1)

    private val zoomDeltaFactorProperty = SimpleDoubleProperty()

    private val zoomInputMinRedProperty = SimpleDoubleProperty()
    private val zoomInputMinGreenProperty = SimpleDoubleProperty()
    private val zoomInputMinBlueProperty = SimpleDoubleProperty()
    private val zoomInputMaxRedProperty = SimpleDoubleProperty()
    private val zoomInputMaxGreenProperty = SimpleDoubleProperty()
    private val zoomInputMaxBlueProperty = SimpleDoubleProperty()
    private val zoomInputMedianRedProperty = SimpleDoubleProperty()
    private val zoomInputMedianGreenProperty = SimpleDoubleProperty()
    private val zoomInputMedianBlueProperty = SimpleDoubleProperty()
    private val zoomInputStddevRedProperty = SimpleDoubleProperty()
    private val zoomInputStddevGreenProperty = SimpleDoubleProperty()
    private val zoomInputStddevBlueProperty = SimpleDoubleProperty()

    private val zoomOutputMinRedProperty = SimpleDoubleProperty()
    private val zoomOutputMinGreenProperty = SimpleDoubleProperty()
    private val zoomOutputMinBlueProperty = SimpleDoubleProperty()
    private val zoomOutputMaxRedProperty = SimpleDoubleProperty()
    private val zoomOutputMaxGreenProperty = SimpleDoubleProperty()
    private val zoomOutputMaxBlueProperty = SimpleDoubleProperty()
    private val zoomOutputMedianRedProperty = SimpleDoubleProperty()
    private val zoomOutputMedianGreenProperty = SimpleDoubleProperty()
    private val zoomOutputMedianBlueProperty = SimpleDoubleProperty()
    private val zoomOutputStddevRedProperty = SimpleDoubleProperty()
    private val zoomOutputStddevGreenProperty = SimpleDoubleProperty()
    private val zoomOutputStddevBlueProperty = SimpleDoubleProperty()

    private val inputDirectoryProperty = SimpleStringProperty(Paths.get(".").toString())
    private val useInputDirectoryAsOutputDirectoryProperty = SimpleBooleanProperty(true)
    private val outputDirectoryProperty = SimpleStringProperty(Paths.get(".").toString())
    private val outputHideOldFilesProperty = SimpleBooleanProperty()
    private val autoCreateOutputDirectoryProperty = SimpleBooleanProperty()

    private val runOnlySelectedModeProperty = SimpleBooleanProperty()
    private val previewModeProperty = SimpleBooleanProperty()

    private val inputFiles = FXCollections.observableArrayList<File>()
    private var selectedInputFiles = FXCollections.observableArrayList<File>()
    private val scriptNames = FXCollections.observableArrayList<String>()
    private val outputFiles = FXCollections.observableArrayList<File>()
    private val hiddenOutputFiles = mutableListOf<File>()

    private val tempInputDirectory = Files.createTempDirectory("kimage_in_").toFile()
    private val tempOutputDirectory = Files.createTempDirectory("kimage_out_").toFile()

    private val maskFileProperty = SimpleObjectProperty<File>(null)
    private val maskMatrixProperty = SimpleObjectProperty<Matrix>(null)
    private val maskInvertedMatrixProperty = SimpleObjectProperty<Matrix>(null)
    private val maskActiveProperty = SimpleBooleanProperty(false)
    private val maskShowProperty = SimpleBooleanProperty(false)
    private val maskInvertProperty = SimpleBooleanProperty(false)

    init {
        tempInputDirectory.deleteOnExit()
        tempOutputDirectory.deleteOnExit()
    }

    val rememberedObjects = mutableListOf<Any>()

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
        zoomFactorProperty.addListener { _, _, _ ->
            updateZoom()
        }
        zoomDeltaFactorProperty.addListener { _, _, _ ->
            updateZoom()
        }
        maskShowProperty.addListener { _, _, _ ->
            updateZoom()
        }
        maskInvertProperty.addListener { _, _, _ ->
            updateZoom()
        }

        primaryStage.scene = scene
        primaryStage.show()
    }

    private fun createMainEditor(): Node {
        return gridpane {
            padding = Insets(SPACING)
            hgap = SPACING
            vgap = SPACING

            row {
                cell {
                    createInputFilesEditor()
                }
                cell {
                    gridpane {
                        row {
                            cell(2, 1) {
                                label("Processing:") {
                                    styleClass += "header1"
                                }
                            }
                        }
                        row {
                            cell {
                                listview(scriptNames) {
                                    selectionModel.selectedItemProperty().addListener { _, _, selected ->
                                        selected?.let {
                                            previewModeProperty.value = false
                                            loadCommandPresets(it)
                                            showCommandEditor(it, mapOf<String, Any>())
                                        }
                                    }
                                }
                            }
                            cell {
                                vbox(SPACING) {
                                    padding = Insets(0.0, SPACING, 0.0, SPACING)
                                    prefWidth = ARGUMENT_EDITOR_WIDTH.toDouble()
                                    prefHeight = ARGUMENT_EDITOR_HEIGHT.toDouble()

                                    children += hbox(SPACING) {
                                        val presetCombobox = combobox(commandPresetsNames) {
                                            valueProperty().addListener { _, _, value ->
                                                if (value != null) {
                                                    showCommandEditor(scriptNameProperty.get(), commandPresets.getOrDefault(value, mapOf()))
                                                }
                                            }
                                        }
                                        val savePresetButton = button(FontIcon()) {
                                            id = "save-settings-icon"
                                            onAction = EventHandler {
                                                val presetArguments = argumentsProperty.get().toMap()
                                                val dialog = TextInputDialog("")
                                                dialog.title = "Save script arguments"
                                                dialog.headerText = "Save the arguments for the script '${scriptNameProperty.get()}':\n\n" + presetArguments.entries.joinToString("\n")
                                                dialog.contentText = "Settings name:"
                                                dialog.dialogPane.minWidth = 300.0
                                                val optionalPresetName = dialog.showAndWait()
                                                if (optionalPresetName.isPresent) {
                                                    val presetName = optionalPresetName.get()
                                                    savePreset(scriptNameProperty.get(), presetName, presetArguments)
                                                }
                                            }
                                        }
                                        val deletePresetButton = button(FontIcon()) {
                                            id = "delete-forever-icon"
                                            onAction = EventHandler {
                                                val deletablePresetNames = commandPresetsNames.toMutableList()
                                                deletablePresetNames.remove("")
                                                val dialog = ChoiceDialog(presetCombobox.valueProperty().get(), deletablePresetNames)
                                                dialog.contentText = "Delete the arguments of the script '${scriptNameProperty.get()}:'"
                                                val optionalDeletePreset = dialog.showAndWait()
                                                if (optionalDeletePreset.isPresent) {
                                                    deletePreset(scriptNameProperty.get(), optionalDeletePreset.get())
                                                }
                                            }
                                        }

                                        children += presetCombobox
                                        children += savePresetButton
                                        children += deletePresetButton
                                    }
                                    children += commandArgumentEditor
                                }
                            }
                        }
                    }
                }
                cell {
                    createOutputFilesEditor()
                }
            }
            row {
                cell {
                    tabpane {
                        tabs += tab("Image") {
                            content = node(withZoomRectangle(inputImageView, inputDecorationsPane)) {
                                inputImageView.isPreserveRatio = true
                                inputImageView.fitWidth = IMAGE_WIDTH.toDouble()
                                inputImageView.fitHeight = IMAGE_HEIGHT.toDouble()
                            }
                        }
                        tabs += tab("Info") {
                            content = gridpane {
                                row {
                                    cell {
                                        label("Width")
                                    }
                                    cell {
                                        textfield(inputImageWidthProperty) {
                                            isEditable = false
                                        }
                                    }
                                }
                                row {
                                    cell {
                                        label("Height")
                                    }
                                    cell {
                                        textfield(inputImageHeightProperty) {
                                            isEditable = false
                                        }
                                    }
                                }
                                row {
                                    cell {
                                        label("Model")
                                    }
                                    cell {
                                        textfield(inputImageModelProperty) {
                                            isEditable = false
                                        }
                                    }
                                }
                                row {
                                    cell {
                                        label("Lens Model")
                                    }
                                    cell {
                                        textfield(inputImageLensModelProperty) {
                                            isEditable = false
                                        }
                                    }
                                }
                                row {
                                    cell {
                                        label("Exposure Time")
                                    }
                                    cell {
                                        textfield(inputImageExposureTimeProperty) {
                                            isEditable = false
                                        }
                                    }
                                }
                                row {
                                    cell {
                                        label("ISO")
                                    }
                                    cell {
                                        textfield(inputImagePhotographicSensitivityProperty) {
                                            isEditable = false
                                        }
                                    }
                                }
                                row {
                                    cell {
                                        label("Aperture")
                                    }
                                    cell {
                                        textfield(inputImageApertureValueProperty) {
                                            isEditable = false
                                        }
                                    }
                                }
                                row {
                                    cell {
                                        label("Bits per Sample")
                                    }
                                    cell {
                                        textfield(inputImageBitsPerSampleProperty) {
                                            isEditable = false
                                        }
                                    }
                                }
                            }
                        }
                        tabs += tab("Histogram") {
                            content = gridpane {
                                row {
                                    cell {
                                        node(inputHistogramCanvas) {
                                            inputHistogramCanvas.onMouseDragged = EventHandler { event: MouseEvent ->
                                                val histogram = inputHistogramCanvas.userData as Histogram
                                                val x = clamp(event.x.toInt(), 0, histogram.n - 1)
                                                inputHistogramXProperty.value = x.toDouble() / histogram.n
                                                inputHistogramYRedProperty.value = histogram.red(x).toDouble() / histogram.entries()
                                                inputHistogramYGreenProperty.value = histogram.green(x).toDouble() / histogram.entries()
                                                inputHistogramYBlueProperty.value = histogram.blue(x).toDouble() / histogram.entries()
                                            }
                                        }
                                    }
                                }
                                row {
                                    cell {
                                        hbox {
                                            children += label("X: ")
                                            children += label(inputHistogramXProperty) {
                                            }
                                        }
                                    }
                                }
                                row {
                                    cell {
                                        hbox {
                                            children += label("R: ")
                                            children += label(inputHistogramYRedProperty, PERCENT_HIGH_FORMAT) {
                                            }
                                        }
                                    }
                                }
                                row {
                                    cell {
                                        hbox {
                                            children += label("G: ")
                                            children += label(inputHistogramYGreenProperty, PERCENT_HIGH_FORMAT) {
                                            }
                                        }
                                    }
                                }
                                row {
                                    cell {
                                        hbox {
                                            children += label("B: ")
                                            children += label(inputHistogramYBlueProperty, PERCENT_HIGH_FORMAT) {
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                cell {
                    node(infoTabPane) {
                        prefHeight = 500.0

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
                cell {
                    tabpane {
                        tabs += tab("Image") {
                            content = node(withZoomRectangle(outputImageView, outputDecorationsPane)) {
                                outputImageView.isPreserveRatio = true
                                outputImageView.fitWidth = IMAGE_WIDTH.toDouble()
                                outputImageView.fitHeight = IMAGE_HEIGHT.toDouble()
                            }
                        }
                        tabs += tab("Info") {
                            content = gridpane {
                                row {
                                    cell {
                                        label("Width")
                                    }
                                    cell {
                                        textfield(outputImageWidthProperty) {
                                            isEditable = false
                                        }
                                    }
                                }
                                row {
                                    cell {
                                        label("Height")
                                    }
                                    cell {
                                        textfield(outputImageHeightProperty) {
                                            isEditable = false
                                        }
                                    }
                                }
                                row {
                                    cell {
                                        label("Model")
                                    }
                                    cell {
                                        textfield(outputImageModelProperty) {
                                            isEditable = false
                                        }
                                    }
                                }
                                row {
                                    cell {
                                        label("Lens Model")
                                    }
                                    cell {
                                        textfield(outputImageLensModelProperty) {
                                            isEditable = false
                                        }
                                    }
                                }
                                row {
                                    cell {
                                        label("Exposure Time")
                                    }
                                    cell {
                                        textfield(outputImageExposureTimeProperty) {
                                            isEditable = false
                                        }
                                    }
                                }
                                row {
                                    cell {
                                        label("ISO")
                                    }
                                    cell {
                                        textfield(outputImagePhotographicSensitivityProperty) {
                                            isEditable = false
                                        }
                                    }
                                }
                                row {
                                    cell {
                                        label("Aperture")
                                    }
                                    cell {
                                        textfield(outputImageApertureValueProperty) {
                                            isEditable = false
                                        }
                                    }
                                }
                                row {
                                    cell {
                                        label("Bits per Sample")
                                    }
                                    cell {
                                        textfield(outputImageBitsPerSampleProperty) {
                                            isEditable = false
                                        }
                                    }
                                }
                            }
                        }
                        tabs += tab("Histogram") {
                            content = gridpane {
                                row {
                                    cell {
                                        node(outputHistogramCanvas) {
                                            outputHistogramCanvas.onMouseDragged = EventHandler { event: MouseEvent ->
                                                val histogram = outputHistogramCanvas.userData as Histogram
                                                val x = clamp(event.x.toInt(), 0, histogram.n - 1)
                                                outputHistogramXProperty.value = x.toDouble() / histogram.n
                                                outputHistogramYRedProperty.value = histogram.red(x).toDouble() / histogram.entries()
                                                outputHistogramYGreenProperty.value = histogram.green(x).toDouble() / histogram.entries()
                                                outputHistogramYBlueProperty.value = histogram.blue(x).toDouble() / histogram.entries()
                                            }
                                        }

                                    }
                                }
                                row {
                                    cell {
                                        hbox {
                                            children += label("X: ")
                                            children += label(outputHistogramXProperty) {
                                            }
                                        }
                                    }
                                }
                                row {
                                    cell {
                                        hbox {
                                            children += label("R: ")
                                            children += label(outputHistogramYRedProperty, PERCENT_HIGH_FORMAT) {
                                            }
                                        }
                                    }
                                }
                                row {
                                    cell {
                                        hbox {
                                            children += label("G: ")
                                            children += label(outputHistogramYGreenProperty, PERCENT_HIGH_FORMAT) {
                                            }
                                        }
                                    }
                                }
                                row {
                                    cell {
                                        hbox {
                                            children += label("B: ")
                                            children += label(outputHistogramYBlueProperty, PERCENT_HIGH_FORMAT) {
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
    }

    private fun deletePreset(commandName: String, presetName: String) {
        val presetsDir = File(File(File(System.getProperty("user.home"), ".kimage"), "config"), commandName)

        val presetsFile = File(presetsDir, "${presetName}.properties")
        if (presetsFile.exists()) {
            presetsFile.delete()

            loadCommandPresets(commandName)
        }
    }

    private fun savePreset(commandName: String, presetName: String, arguments: Map<String, Any>) {
        val presetsDir = File(File(File(System.getProperty("user.home"), ".kimage"), "config"), commandName)
        presetsDir.mkdirs()

        val properties = Properties()
        arguments.forEach { (k, v) ->
            properties[k] = v.toString()
        }

        val presetsFile = File(presetsDir, "${presetName}.properties")
        try {
            presetsFile.canonicalPath // throws exception if invalid path

            val writer = BufferedWriter(FileWriter(presetsFile))
            writer.use {
                properties.store(it, "KImage arguments for $commandName")
            }

            loadCommandPresets(commandName)
        } catch (ex: IOException) {
            val alert = Alert(Alert.AlertType.ERROR, "The name '$presetName' is not valid to store script arguments.")
            alert.showAndWait()
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
                    hbox(SPACING) {
                        children += label("Delta Zoom:")
                        children += spinner(1, 256, 1) {
                            prefWidth = 80.0
                            styleClass.add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL)
                            tooltip = Tooltip("The zoom factor.")
                            valueProperty().addListener { _, _, value ->
                                zoomFactorProperty.set(value.toInt())
                            }
                        }
                        children += button {
                            graphic = rectangle(10.0, 10.0) {
                                fill = Color.TRANSPARENT
                                strokeProperty().bind(crosshairColorProperty)
                            }
                            tooltip = Tooltip("Toggles the color of the crosshair in the zoom images.")
                            onAction = EventHandler {
                                var index = crosshairColors.indexOf(crosshairColorProperty.get())
                                index = (index + 1) % crosshairColors.size
                                crosshairColorProperty.setValue(crosshairColors[index])
                            }
                        }
                    }
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
                    inputZoomHistogramCanvas
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
                    outputZoomHistogramCanvas
                }
            }
            row {
                cell {
                    hbox {
                        children += label("Min:")
                        children += label(zoomInputMinRedProperty) {
                            prefWidth = 80.0
                        }
                        children += label(zoomInputMinGreenProperty) {
                            prefWidth = 80.0
                        }
                        children += label(zoomInputMinBlueProperty) {
                            prefWidth = 80.0
                        }
                    }
                }
                cell {
                    label("")
                }
                cell {
                    hbox {
                        children += label("Min:")
                        children += label(zoomOutputMinRedProperty) {
                            prefWidth = 80.0
                        }
                        children += label(zoomOutputMinGreenProperty) {
                            prefWidth = 80.0
                        }
                        children += label(zoomOutputMinBlueProperty) {
                            prefWidth = 80.0
                        }
                    }
                }
            }
            row {
                cell {
                    hbox {
                        children += label("Max:")
                        children += label(zoomInputMaxRedProperty) {
                            prefWidth = 80.0
                        }
                        children += label(zoomInputMaxGreenProperty) {
                            prefWidth = 80.0
                        }
                        children += label(zoomInputMaxBlueProperty) {
                            prefWidth = 80.0
                        }
                    }
                }
                cell {
                    label("")
                }
                cell {
                    hbox {
                        children += label("Max:")
                        children += label(zoomOutputMaxRedProperty) {
                            prefWidth = 80.0
                        }
                        children += label(zoomOutputMaxGreenProperty) {
                            prefWidth = 80.0
                        }
                        children += label(zoomOutputMaxBlueProperty) {
                            prefWidth = 80.0
                        }
                    }
                }
            }
            row {
                cell {
                    hbox {
                        children += label("Median:")
                        children += label(zoomInputMedianRedProperty) {
                            prefWidth = 80.0
                        }
                        children += label(zoomInputMedianGreenProperty) {
                            prefWidth = 80.0
                        }
                        children += label(zoomInputMedianBlueProperty) {
                            prefWidth = 80.0
                        }
                    }
                }
                cell {
                    label("")
                }
                cell {
                    hbox {
                        children += label("Median:")
                        children += label(zoomOutputMedianRedProperty) {
                            prefWidth = 60.0
                        }
                        children += label(zoomOutputMedianGreenProperty) {
                            prefWidth = 60.0
                        }
                        children += label(zoomOutputMedianBlueProperty) {
                            prefWidth = 60.0
                        }
                    }
                }
            }
            row {
                cell {
                    hbox {
                        children += label("Stddev:")
                        children += label(zoomInputStddevRedProperty) {
                            prefWidth = 80.0
                        }
                        children += label(zoomInputStddevGreenProperty) {
                            prefWidth = 80.0
                        }
                        children += label(zoomInputStddevBlueProperty) {
                            prefWidth = 80.0
                        }
                    }
                }
                cell {
                    label("")
                }
                cell {
                    hbox {
                        children += label("Stddev:")
                        children += label(zoomOutputStddevRedProperty) {
                            prefWidth = 80.0
                        }
                        children += label(zoomOutputStddevGreenProperty) {
                            prefWidth = 80.0
                        }
                        children += label(zoomOutputStddevBlueProperty) {
                            prefWidth = 80.0
                        }
                    }
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
                        menuitem("Move to top", FontIcon()) {
                            id = "move-up-icon"
                            onAction = EventHandler {
                                val items = selectionModel.selectedItems.toList()
                                items.reversed().forEach {
                                    inputFiles.remove(it)
                                    inputFiles.add(0, it)
                                }
                                updateImageView(null, inputImageView, inputHistogramCanvas, inputImageWidthProperty, inputImageHeightProperty, inputImageModelProperty, inputImageLensModelProperty, inputImageExposureTimeProperty, inputImagePhotographicSensitivityProperty, inputImageApertureValueProperty, inputImageBitsPerSampleProperty)
                            }
                        },
                        menuitem("Move to bottom", FontIcon()) {
                            id = "move-down-icon"
                            onAction = EventHandler {
                                val items = selectionModel.selectedItems.toList()
                                items.forEach {
                                    inputFiles.remove(it)
                                    inputFiles.add(it)
                                }
                                updateImageView(null, inputImageView, inputHistogramCanvas, inputImageWidthProperty, inputImageHeightProperty, inputImageModelProperty, inputImageLensModelProperty, inputImageExposureTimeProperty, inputImagePhotographicSensitivityProperty, inputImageApertureValueProperty, inputImageBitsPerSampleProperty)
                            }
                        },
                        menuitem("Remove from list", FontIcon()) {
                            id = "remove-icon"
                            onAction = EventHandler {
                                val items = selectionModel.selectedItems.toList()
                                items.forEach {
                                    inputFiles.remove(it)
                                }
                                updateImageView(null, inputImageView, inputHistogramCanvas, inputImageWidthProperty, inputImageHeightProperty, inputImageModelProperty, inputImageLensModelProperty, inputImageExposureTimeProperty, inputImagePhotographicSensitivityProperty, inputImageApertureValueProperty, inputImageBitsPerSampleProperty)
                            }
                        }
                    )
                    tableRow
                }

                column<String>("Name", { file -> ReadOnlyStringWrapper(file?.name) }) {
                    this.prefWidth = 200.0
                }
                column<String>("Type", { file -> ReadOnlyStringWrapper(file.extension) }) {
                    this.prefWidth = 60.0
                }
                column<Number>("Size", { file -> ReadOnlyLongWrapper(fileSize(file)) }) {
                    this.prefWidth = 100.0
                }

                selectionModel.selectedItemProperty().addListener { _, _, selected ->
                    updateImageView(selected, inputImageView, inputHistogramCanvas, inputImageWidthProperty, inputImageHeightProperty, inputImageModelProperty, inputImageLensModelProperty, inputImageExposureTimeProperty, inputImagePhotographicSensitivityProperty, inputImageApertureValueProperty, inputImageBitsPerSampleProperty)
                }
                selectedInputFiles = selectionModel.selectedItems
            }
        }
    }

    private fun fileSize(file: File): Long {
        try {
            println(file.toPath())
            return Files.size(file.toPath())
        } catch (ex: Exception) {
            ex.printStackTrace();
            return -1;
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
                children += togglebutton(FontIcon()) {
                    id = "auto-create-folder-icon"
                    tooltip = Tooltip("Automatically creates a new output directory with the same name as the script and a running number.")
                    selectedProperty().bindBidirectional(autoCreateOutputDirectoryProperty)
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
                children += button(FontIcon()) {
                    id = "refresh"
                    tooltip = Tooltip("Refresh from file system.")
                    onAction = EventHandler {
                        updateOutputDirectoryFiles(outputHideOldFilesProperty.get())
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
                    tableRow.onMouseClicked = EventHandler {
                        if (it.clickCount == 2) {
                            if (tableRow.item.isDirectory && !useInputDirectoryAsOutputDirectoryProperty.get()) {
                                outputDirectoryProperty.set(tableRow.item.path)
                            } else if (tableRow.item.isFile) {
                                Desktop.getDesktop().open(tableRow.item)
                            }
                        }
                    }
                    tableRow.contextMenu = ContextMenu(
                        menuitem("Replace input", FontIcon()) {
                            id = "bold-arrow-left-icon"
                            onAction = EventHandler {
                                inputFiles.clear()

                                val files = selectionModel.selectedItems.toList()
                                if (files.isNotEmpty()) {
                                    inputDirectoryProperty.set( files[0].parent)
                                }
                                files.forEach {
                                    inputFiles.add(it)
                                }
                            }
                        },
                        menuitemSeparator(),
                        menuitem("Open File in Explorer", FontIcon()) {
                            id = "open-file"
                            onAction = EventHandler {
                                val file = selectionModel.selectedItem
                                Desktop.getDesktop().open(file)
                            }
                        },
                        menuitem("Open Directory in Explorer", FontIcon()) {
                            id = "open-folder"
                            onAction = EventHandler {
                                val file = selectionModel.selectedItem
                                if (file.isDirectory) {
                                    Desktop.getDesktop().open(file)
                                } else {
                                    Desktop.getDesktop().open(file.parentFile)
                                }

                            }
                        },
                        menuitemSeparator(),
                        menuitem("New Directory", FontIcon()) {
                            id = "new-folder"
                            onAction = EventHandler {
                                val dialog = TextInputDialog()
                                dialog.title = "Create Directory"
                                dialog.headerText = "Create new directory"
                                dialog.dialogPane.minWidth = 300.0
                                val optionalDirectoryName = dialog.showAndWait()
                                if (optionalDirectoryName.isPresent) {
                                    val file = File(outputDirectoryProperty.get(), optionalDirectoryName.get())
                                    if (!file.exists()) {
                                        file.mkdir()
                                        updateOutputDirectoryFiles(outputHideOldFilesProperty.get())
                                    }
                                }
                            }
                        },
                        menuitem("Rename File", FontIcon()) {
                            id = "edit-file"
                            onAction = EventHandler {
                                val file = selectionModel.selectedItem
                                val dialog = TextInputDialog(file.name)
                                dialog.title = "Rename File"
                                dialog.headerText = "Rename file"
                                dialog.dialogPane.minWidth = 300.0
                                val optionalFileName = dialog.showAndWait()
                                if (optionalFileName.isPresent) {
                                    file.renameTo(File(file.parentFile, optionalFileName.get()))
                                }
                            }
                        },
                        menuitemSeparator(),
                        menuitem("Delete File forever", FontIcon()) {
                            id = "delete-forever-icon"
                            onAction = EventHandler {
                                val filesToDelete = selectionModel.selectedItems.toList()
                                runWithProgressDialog("Delete Files", "Deleting ${filesToDelete.size} files", 200) {
                                    filesToDelete.forEach {
                                        outputFiles.remove(it)
                                        it.delete()
                                    }
                                    updateImageView(null, outputImageView, outputHistogramCanvas, outputImageWidthProperty, outputImageHeightProperty, outputImageModelProperty, outputImageLensModelProperty, outputImageExposureTimeProperty, outputImagePhotographicSensitivityProperty, outputImageApertureValueProperty, outputImageBitsPerSampleProperty)
                                }
                            }
                        }
                    )
                    tableRow
                }

                column<Node>("", { file ->
                    ReadOnlyObjectWrapper<Node>(if (file.isDirectory) FontIcon("mdi2f-folder-outline") else label(""))
                }) {
                    this.prefWidth = 25.0
                }
                column<String>("Name", { file ->
                    ReadOnlyStringWrapper(file.name)
                }) {
                    this.prefWidth = 200.0
                }
                column<String>("Type", { file ->
                    ReadOnlyStringWrapper(file.extension)
                }) {
                    this.prefWidth = 60.0
                }
                column<String>("Size", { file ->
                    ReadOnlyStringWrapper(if (file.isDirectory) "" else Files.size(file.toPath()).toString())
                }) {
                    this.prefWidth = 100.0
                }

                selectionModel.selectedItemProperty().addListener { _, _, selected ->
                    updateImageView(selected, outputImageView, outputHistogramCanvas, outputImageWidthProperty, outputImageHeightProperty, outputImageModelProperty, outputImageLensModelProperty, outputImageExposureTimeProperty, outputImagePhotographicSensitivityProperty, outputImageApertureValueProperty, outputImageBitsPerSampleProperty)
                }
            }
        }
    }

    private fun updateImageView(
        selectedFile: File?,
        imageView: ImageView,
        imageHistogramCanvas: Canvas,
        imageWidthProperty: SimpleIntegerProperty,
        imageHeightProperty: SimpleIntegerProperty,
        imageModelProperty: SimpleStringProperty,
        imageLensModelProperty: SimpleStringProperty,
        imageExposureTimeProperty: SimpleStringProperty,
        imagePhotographicSensitivityProperty: SimpleStringProperty,
        imageApertureValueProperty: SimpleStringProperty,
        imageBitsPerSampleProperty: SimpleStringProperty
    ) {
        if (selectedFile == null) {
            if (imageView == inputImageView) {
                currentInputImage = null
            }
            imageView.image = dummyImage
            updateZoom()
        } else {
            runWithProgressDialog("Load Image", "Loading image $selectedFile", 200) {
                try {
                    val image = ImageReader.read(selectedFile)
                    if (imageView == inputImageView) {
                        currentInputImage = image
                    }
                    loadImageMetadata(selectedFile, imageModelProperty, imageLensModelProperty, imageExposureTimeProperty, imagePhotographicSensitivityProperty, imageApertureValueProperty, imageBitsPerSampleProperty)
                    image.let {
                        imageWidthProperty.set(it.width)
                        imageHeightProperty.set(it.height)
                        val writableImage = JavaFXImageUtil.toWritableImage(it)

                        Platform.runLater {
                            imageView.image = writableImage
                            updateZoom()
                        }
                        Platform.runLater {
                            updateHistogram(writableImage, imageHistogramCanvas)
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    // ignore
                }
            }
        }
    }

    private fun updateHistogram(image: WritableImage, histogramCanvas: Canvas) {
        val histogram = Histogram(histogramCanvas.width.toInt())
        for (y in 0 until image.height.toInt()) {
            for (x in 0 until image.width.toInt()) {
                var rgbInput = image.pixelReader.getColor(x, y)
                histogram.add(rgbInput)
            }
        }
        drawHistogram(histogram, histogramCanvas)
    }

    private fun loadImageMetadata(
        file: File,
        imageModelProperty: SimpleStringProperty,
        imageLensModelProperty: SimpleStringProperty,
        imageExposureTimeProperty: SimpleStringProperty,
        imagePhotographicSensitivityProperty: SimpleStringProperty,
        imageApertureValueProperty: SimpleStringProperty,
        imageBitsPerSampleProperty: SimpleStringProperty
    ) {
        val items = mutableMapOf<String, String>()

        try {
            val metadata: ImageMetadata? = Imaging.getMetadata(file)
            if (metadata is GenericImageMetadata) {
                for (item in metadata.items) {
                    if (item is GenericImageMetadata.GenericImageMetadataItem) {
                        items[item.keyword] = item.text
                    }
                }
            } else if (metadata is JpegImageMetadata) {
                for (item in metadata.exif.items) {
                    if (item is GenericImageMetadata.GenericImageMetadataItem) {
                        items[item.keyword] = item.text
                    }
                }
            }

            imageModelProperty.set(items["Model"]?.trim('\''))
            imageLensModelProperty.set(items["LensModel"]?.trim('\''))
            imageExposureTimeProperty.set(items["ExposureTime"])
            imagePhotographicSensitivityProperty.set(items["PhotographicSensitivity"])
            imageApertureValueProperty.set(items["ApertureValue"])
            imageBitsPerSampleProperty.set(items["BitsPerSample"])
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun showCommandEditor(command: String, initialArgumentValues: Map<String, Any>) {
        scriptNameProperty.set(command)
        commandArgumentEditor.children.clear()
        argumentsProperty.clear()

        val verboseModeProperty = SimpleBooleanProperty(false)
        val debugModeProperty = SimpleBooleanProperty(false)

        node(commandArgumentEditor) {
            runWithProgressDialog("Compiling Script", "Compiling script $command", 200) {
                val script = KImageManager.script(command)

                Platform.runLater {
                    if (script is ScriptV0_1) {
                        val docu = script.documentation(false)
                        docuTextArea.text = docu

                        docuWebView.engine.loadContent(markdownToHtml(docu))

                        codeTextArea.text = script.code

                        infoTabPane.selectionModel.select(infoTabDocu)

                        children += label(script.title) {
                            styleClass += "header2"
                        }

                        children += scrollpane {
                            content = gridpane {
                                padding = Insets(SPACING)
                                hgap = SPACING
                                vgap = SPACING

                                for (argument in script.scriptArguments.arguments) {
                                    setupArgumentEditor(argument, initialArgumentValues[argument.name])
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

                        children += hbox(SPACING) {
                            children += label("Mask:")
                            children += textfield {
                                isEditable = false
                                text = maskFileProperty.value?.name
                                maskFileProperty.addListener { _, _, value ->
                                    this.text = value.name
                                }
                            }
                            children += button(FontIcon()) {
                                id = "file-icon"
                                tooltip = Tooltip("Select the mask image file.")
                                onAction = EventHandler {
                                    val file = openFile(File(inputDirectoryProperty.get()))
                                    file?.let {
                                        maskFileProperty.value = it
                                        runWithProgressDialog("Load Mask Image", "Loading mask image $file", 200) {
                                            try {
                                                val image = ImageReader.read(file)
                                                val matrix = image[Channel.Gray]
                                                maskMatrixProperty.value = matrix
                                                maskInvertedMatrixProperty.value = DoubleMatrix(matrix.width, matrix.height) { x, y ->
                                                    1.0 - matrix[x, y]
                                                }
                                                maskActiveProperty.value = true
                                            } catch (ex: Exception) {
                                                ex.printStackTrace()
                                                // ignore
                                            }
                                        }
                                    }
                                }
                            }
                            children += checkbox(maskActiveProperty) {
                                text = "Active"
                            }
                            children += checkbox(maskShowProperty) {
                                text = "Show"
                            }
                            children += checkbox(maskInvertProperty) {
                                text = "Invert"
                            }
                        }

                        children += button(FontIcon()) {
                            id = "play-icon"
                            tooltip = Tooltip("Run the ${script.name} script.")
                            onAction = EventHandler {
                                if (autoCreateOutputDirectoryProperty.get()) {
                                    val parentDir = if (useInputDirectoryAsOutputDirectoryProperty.get()) {
                                        inputDirectoryProperty.get()
                                    } else {
                                        outputDirectoryProperty.get()
                                    }
                                    val dir = createWorkingDir(parentDir, script.name)
                                    outputDirectoryProperty.set(dir.path)
                                }

                                updateOutputDirectoryFiles(outputHideOldFilesProperty.get())

                                val runInputFiles = if (runOnlySelectedModeProperty.get()) {
                                    selectedInputFiles
                                } else {
                                    inputFiles
                                }

                                infoTabPane.selectionModel.select(infoTabLog)

                                runWithProgressDialog("Running ${script.name}", script.title, 0) { progress ->
                                    val systemOut = System.out
                                    try {
                                        logTextArea.clear()
                                        System.setOut(PrintStream(LogOutputStream(logTextArea)))

                                        KImageManager.executeScript(
                                            script,
                                            argumentsProperty,
                                            runInputFiles,
                                            false,
                                            verboseModeProperty.get(),
                                            debugModeProperty.get(),
                                            getMaskMatrix(),
                                            command,
                                            outputDirectoryProperty.get(),
                                            progress
                                        )
                                    } catch (ex: Exception) {
                                        println()
                                        ex.printStackTrace(System.out)
                                    } finally {
                                        System.setOut(systemOut)
                                        Platform.runLater {
                                            updateOutputDirectoryFiles(false)
                                        }
                                    }
                                }
                            }
                        }
                        children += checkbox(runOnlySelectedModeProperty) {
                            tooltip = Tooltip("Process only the selected input files.")
                            text = "Process only selected input"
                        }
                        children += checkbox(previewModeProperty) {
                            tooltip = Tooltip("Preview the output of the ${script.name} script.")
                            text = "Preview Mode"
                            previewModeProperty.addListener { _, _, value ->
                                if (value) {
                                    previewScript = script
                                    previewCommand = command
                                } else {
                                    previewScript = null
                                    previewCommand = null
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getMaskMatrix(): Matrix? {
        return if (maskActiveProperty.get()) {
            if (maskInvertProperty.value) {
                maskInvertedMatrixProperty.value
            } else {
                maskMatrixProperty.value
            }
        } else {
            null
        }
    }

    private fun createWorkingDir(parent: String, name: String): File {
        var index = 1
        var dir: File
        do {
            dir = File(parent, "${name}_$index")
            index++
        } while (dir.exists())

        Files.createDirectory(dir.toPath())

        return dir
    }

    private fun previewImage(
        image: Image,
        script: ScriptV0_1,
        command: String
    ) {
        val croppedInputFiles = if (script.isSingle()) {
            val croppedInputFile = cropInputFile(image)
            listOf(croppedInputFile)
        } else {
            listOf()
        }

        runWithProgressDialog("Previewing ${script.name}", script.title, 100) {
            //val systemOut = System.out
            try {
                System.setOut(PrintStream(OutputStream.nullOutputStream()))

                KImageManager.executeScript(
                    script,
                    argumentsProperty,
                    croppedInputFiles,
                    false,
                    false,
                    false,
                    getMaskMatrix(),
                    command,
                    tempOutputDirectory.path
                ) { _, output ->
                    if (output is Image) {
                        val writableImage = JavaFXImageUtil.toWritableImage(output)
                        Platform.runLater {
                            updateZoom(outputCenterX = ZOOM_WIDTH / 2, outputCenterY = ZOOM_HEIGHT / 2, outputImage = writableImage)
                        }
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace(System.out)
            } finally {
                //System.setOut(systemOut)
            }
        }
    }

    private fun cropInputFile(image: Image): File {
        val croppedInputFile = File(tempInputDirectory, "input.png")
        val croppedInputImage = image.crop(
            zoomCenterXProperty.get() - ZOOM_WIDTH / 2,
            zoomCenterYProperty.get() - ZOOM_HEIGHT / 2,
            ZOOM_WIDTH,
            ZOOM_HEIGHT
        )
        ImageWriter.write(croppedInputImage, croppedInputFile)
        return croppedInputFile
    }

    private fun GridPaneContext.setupArgumentEditor(
        argument: ScriptArg,
        initialValue: Any?
    ) {
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

                            toBooleanValue(initialValue, argument.default)?.let {
                                isSelected = it
                            }
                            selectedProperty().addListener { _, _, value ->
                                argumentsProperty[argument.name] = value
                            }
                        }
                    }
                    is ScriptIntArg -> {
                        hbox(SPACING) {
                            val argProperty = remember(SimpleStringProperty())
                            argProperty.addListener { _, _, value ->
                                argumentsProperty[argument.name] = value
                            }
                            children += textfield {
                                tooltip = Tooltip(argument.tooltip())
                                textFormatter = TextFormatter(IntegerStringConverter(), argument.default) { change ->
                                    filter(change, Regex("-?[0-9]*"))
                                }
                                textProperty().bindBidirectional(argProperty)
                                setupValidation(this, textProperty(), argument, argProperty)
                                setupEnabledWhen(argument, this.disableProperty())
                            }
                            argument.unit?.let {
                                children += label(it)
                            }
                            setupHints(argument, argProperty)
                            toIntValue(initialValue, argument.default)?.let {
                                argProperty.set(it.toString())
                            }
                        }
                    }
                    is ScriptDoubleArg -> {
                        hbox(SPACING) {
                            val argProperty = remember(SimpleStringProperty())
                            argProperty.addListener { _, _, value ->
                                argumentsProperty[argument.name] = value
                            }
                            children += textfield {
                                tooltip = Tooltip(argument.tooltip())
                                textFormatter = textFormatter(argument.default, argument.min, argument.max)
                                textProperty().bindBidirectional(argProperty)
                                setupValidation(this, textProperty(), argument, argProperty)
                                setupEnabledWhen(argument, this.disableProperty())
                            }
                            argument.unit?.let {
                                children += label(it)
                            }
                            setupHints(argument, argProperty)
                            toDoubleValue(initialValue, argument.default)?.let {
                                argProperty.set(it.toString())
                            }
                        }
                    }
                    is ScriptStringArg -> {
                        val argProperty = remember(SimpleStringProperty())
                        argProperty.addListener { _, _, value ->
                            argumentsProperty[argument.name] = value
                        }
                        if (argument.allowed.isNotEmpty()) {
                            val allowed2 = mutableListOf<String>()
                            if (argument is ScriptOptionalStringArg) {
                                allowed2.add("")
                            }
                            allowed2.addAll(argument.allowed)
                            hbox(SPACING) {
                                children += combobox(allowed2) {
                                    tooltip = Tooltip(argument.tooltip())
                                    valueProperty().bindBidirectional(argProperty)
                                    setupEnabledWhen(argument, this.disableProperty())
                                    toStringValue(initialValue, argument.default)?.let {
                                        argProperty.set(it)
                                    }
                                }
                                argument.unit?.let {
                                    children += label(it)
                                }
                            }
                        } else {
                            hbox(SPACING) {
                                children += textfield {
                                    tooltip = Tooltip(argument.tooltip())
                                    argument.regex?.let {
                                        textFormatter = TextFormatter(
                                            TextFormatter.IDENTITY_STRING_CONVERTER,
                                            argument.default
                                        ) { change ->
                                            filter(change, Regex(it))
                                        }
                                    }
                                    textProperty().bindBidirectional(argProperty)
                                    setupValidation(this, textProperty(), argument, argProperty)
                                    setupEnabledWhen(argument, this.disableProperty())
                                }
                                argument.unit?.let {
                                    children += label(it)
                                }
                                setupHints(argument, argProperty)
                                toStringValue(initialValue, argument.default)?.let {
                                    argProperty.set(it)
                                }
                            }
                        }
                    }
                    is ScriptFileArg -> {
                        hbox(SPACING) {
                            val argProperty = remember(SimpleStringProperty())
                            argProperty.addListener { _, _, value ->
                                if (value.isNullOrEmpty()) {
                                    argumentsProperty.remove(argument.name)
                                } else {
                                    //val file = File(inputDirectoryProperty.get(), value)
                                    val file = File(value)
                                    argumentsProperty[argument.name] = file
                                }
                            }
                            children += textfield(argProperty) {
                                // TODO relative to input or output dir?
                                tooltip = Tooltip(argument.tooltip())
                                toStringValue(initialValue, argument.default)?.let {
                                    text = it
                                }
                                setupValidation(this, textProperty(), argument, argProperty)
                                setupEnabledWhen(argument, this.disableProperty())
                            }
                            children += button(FontIcon()) {
                                if (argument.isDirectory == true) {
                                    id = "folder-icon"
                                    tooltip = Tooltip("Select the directory for ${argument.name}.")
                                } else {
                                    id = "file-icon"
                                    tooltip = Tooltip("Select the file for ${argument.name}.")
                                }
                                onAction = EventHandler {
                                    if (argument.isDirectory == true) {
                                        val file = openDir(File(inputDirectoryProperty.get()))
                                        file?.let {
                                            //argProperty.set(it.toRelativeString(File(inputDirectoryProperty.get())))
                                            argProperty.set(file.toString())
                                        }
                                    } else {
                                        val file = openFile(File(inputDirectoryProperty.get()))
                                        file?.let {
                                            //argProperty.set(it.toRelativeString(File(inputDirectoryProperty.get())))
                                            argProperty.set(file.toString())
                                        }
                                    }
                                }
                                setupHints(argument, argProperty)
                                toStringValue(initialValue, argument.default)?.let {
                                    argProperty.set(it)
                                }
                            }
                            argument.unit?.let {
                                children += label(it)
                            }
                        }
                    }
                    is ScriptImageArg -> {
                        hbox(SPACING) {
                            val argProperty = remember(SimpleStringProperty())
                            argProperty.addListener { _, _, value ->
                                if (value.isNullOrEmpty()) {
                                    argumentsProperty.remove(argument.name)
                                } else {
                                    val file = File(inputDirectoryProperty.get(), value)
                                    argumentsProperty[argument.name] = file
                                }
                            }
                            children += textfield(argProperty) {
                                tooltip = Tooltip(argument.tooltip())
                                toStringValue(initialValue, argument.default)?.let {
                                    text = it
                                }
                                setupValidation(this, textProperty(), argument, argProperty)
                                setupEnabledWhen(argument, this.disableProperty())
                            }
                            children += button(FontIcon()) {
                                id = "file-icon"
                                tooltip = Tooltip("Select the image file for ${argument.name}.")
                                onAction = EventHandler {
                                    val file = openFile(File(inputDirectoryProperty.get()))
                                    file?.let {
                                        //argProperty.set(it.toRelativeString(File(inputDirectoryProperty.get())))
                                        argProperty.set(file.toString())
                                    }
                                }
                            }
                            children += button("View") {
                                id = "file-icon"
                                tooltip = Tooltip("View image in explorer.")
                                onAction = EventHandler {
                                    argProperty.value?.let {
                                        val file = File(it)
                                        if (file.exists()) {
                                            Desktop.getDesktop().open(file)
                                        }
                                    }
                                }
                                argProperty.addListener { _, _, value ->
                                    this.visibleProperty().set(!value.isNullOrEmpty())
                                }
                                this.isVisible = false
                            }
                            argument.unit?.let {
                                children += label(it)
                            }
                            setupHints(argument, argProperty)
                            toStringValue(initialValue, argument.default)?.let {
                                argProperty.set(it)
                            }
                        }
                    }
                    is ScriptPointArg -> {
                        hbox(SPACING) {
                            val argProperty = remember(SimpleStringProperty())
                            val argXProperty = remember(SimpleStringProperty())
                            val argYProperty = remember(SimpleStringProperty())
                            val xyListener = {
                                val xString = argXProperty.valueSafe
                                val yString = argYProperty.valueSafe
                                if (xString == "" && yString == "") {
                                    argProperty.set("");
                                } else {
                                    val x = if (xString == "") 0.0 else xString.toDouble()
                                    val y = if (yString == "") 0.0 else yString.toDouble()
                                    argProperty.set("$x, $y");
                                }
                            }
                            argXProperty.addListener { _, _, _ -> xyListener() }
                            argYProperty.addListener { _, _, _ -> xyListener() }
                            argProperty.addListener { _, _, value ->
                                argumentsProperty[argument.name] = value
                                if (value == null || value == "") {
                                    argXProperty.set("")
                                    argYProperty.set("")
                                } else {
                                    val point = toPointValue(value)
                                    argXProperty.set(point?.x.toString())
                                    argYProperty.set(point?.y.toString())
                                }
                            }
                            children += textfield {
                                tooltip = Tooltip(argument.tooltip())
                                prefWidth = 100.0
                                textFormatter = textFormatter(argument.default?.x, argument.min?.x, argument.max?.x)
                                textProperty().bindBidirectional(argXProperty)
                                //setupValidation(this, textProperty(), argument, argProperty)
                                setupEnabledWhen(argument, this.disableProperty())
                            }
                            children += textfield {
                                tooltip = Tooltip(argument.tooltip())
                                prefWidth = 100.0
                                textFormatter = textFormatter(argument.default?.y, argument.min?.y, argument.max?.y)
                                textProperty().bindBidirectional(argYProperty)
                                //setupValidation(this, textProperty(), argument, argProperty)
                                setupEnabledWhen(argument, this.disableProperty())
                            }
                            argument.unit?.let {
                                children += label(it)
                            }
                            setupHints(argument, argProperty)
                            toPointValue(initialValue, argument.default)?.let {
                                argProperty.set(it.toString())
                            }
                        }
                    }
                    is ScriptListArg -> {
                        hbox {
//                            if (argument.hint == Hint.Curve) {
//                                children += createCurveEditor(argument, argumentsProperty)
//                            } else {
                                val argProperty = remember(SimpleStringProperty())
                                argProperty.addListener { _, _, value ->
                                    argumentsProperty[argument.name] = value
                                }
                                children += textfield(argProperty) {
                                    tooltip = Tooltip(argument.tooltip())
                                    textProperty().bindBidirectional(argProperty)
                                    setupValidation(this, textProperty(), argument, argProperty)
                                    setupEnabledWhen(argument, this.disableProperty())
                                }
                                argument.unit?.let {
                                    children += label(it)
                                }
                                setupHints(argument, argProperty)
                                toStringValue(initialValue, argument.default?.toString())?.let {
                                    argProperty.set(it)
                                }
//                            }
                        }
                    }
                    else -> {
                        hbox {
                            val argProperty = remember(SimpleStringProperty())
                            argProperty.addListener { _, _, value ->
                                argumentsProperty[argument.name] = value
                            }
                            children += textfield(argProperty) {
                                tooltip = Tooltip(argument.tooltip())
                                textProperty().bindBidirectional(argProperty)
                                setupValidation(this, textProperty(), argument, argProperty)
                                setupEnabledWhen(argument, this.disableProperty())
                            }
                            argument.unit?.let {
                                children += label(it)
                            }
                            setupHints(argument, argProperty)
                            toStringValue(initialValue, argument)?.let {
                                argProperty.set(it)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createCurveEditor(argument: ScriptListArg, argProperty: StringProperty): Node {
        val canvas = Canvas(200.0, 200.0)

        argProperty.addListener { _, _, value ->
            val curve = argument.toListValue(value) as List<Point>
            drawCurve(canvas, curve)
        }

        return canvas
    }

    private fun drawCurve(canvas: Canvas, curve: List<Point>) {
        val gc = canvas.graphicsContext2D

        val background = Color.WHITE
        gc.fill = background
        gc.fillRect(0.0, 0.0, canvas.width, canvas.height)
        gc.lineWidth = 1.0

        val curvePointsX = mutableListOf<Double>()
        val curvePointsY = mutableListOf<Double>()

        for (point in curve) {
            curvePointsX.add(point.x)
            curvePointsY.add(point.y)
        }

        val spline: SplineInterpolator = SplineInterpolator.createMonotoneCubicSpline(curvePointsX, curvePointsY)
        var lastPixelX = 0.0
        var lastPixelY = 0.0
        for (pixelX in 0 .. canvas.width.toInt()) {
            val x = pixelX / canvas.width
            val y = spline.interpolate(x)
            val pixelY = y * canvas.height

            gc.strokeLine(lastPixelX, canvas.height - lastPixelY, pixelX.toDouble(), canvas.height - pixelY)

            lastPixelX = pixelX.toDouble()
            lastPixelY = pixelY
        }
    }

    private fun drawFunction(canvas: Canvas, func: (Double) -> Double, xStart: Double = 0.0, xEnd: Double = 1.0, yStart: Double = 0.0, yEnd: Double = 0.0) {
        val gc = canvas.graphicsContext2D

        val background = Color.WHITE
        gc.fill = background
        gc.fillRect(0.0, 0.0, canvas.width, canvas.height)
        gc.lineWidth = 1.0

        var firstPixel = true
        var lastPixelX = 0.0
        var lastPixelY = 0.0
        var pixelX = 0.0
        while (pixelX < canvas.width) {
            val x = pixelX * (xEnd - xStart) + xStart
            val y = func(x)
            val pixelY = y * (yEnd - yStart) + yStart

            if (!firstPixel) {
                gc.strokeLine(lastPixelX, canvas.height - lastPixelY, pixelX, canvas.height - pixelY)
            }

            lastPixelX = pixelX
            lastPixelY = pixelY

            firstPixel = false
            pixelX += 1.0;
        }
    }

    private fun toStringValue(vararg values: Any?): String? {
        for (value in values) {
            if (value != null) {
                return value.toString()
            }
        }
        return null
    }

    private fun toBooleanValue(vararg values: Any?): Boolean? {
        return toStringValue(*values)?.toBoolean()
    }

    private fun toIntValue(vararg values: Any?): Int? {
        return toStringValue(*values)?.toInt()
    }

    private fun toDoubleValue(vararg values: Any?): Double? {
        return toStringValue(*values)?.toDouble()
    }

    private fun toPointValue(vararg values: Any?): Point? {
        val string = toStringValue(*values)
        return if (string != null) {
            val parts = string.split(",")
            Point(parts[0].toDouble(), parts[1].toDouble())
        } else {
            null;
        }
    }

    private fun <T> remember(obj: T): T {
        // prevent instances from being garbage collected
        rememberedObjects.add(obj!!)
        return obj
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
                    setupEnabledWhen(argument, this.disableProperty())
                }
            }
            Hint.ImageY -> {
                children += button("Take Y") {
                    onAction = EventHandler {
                        argProperty.set(zoomCenterYProperty.get().toString())
                    }
                    setupEnabledWhen(argument, this.disableProperty())
                }
            }
            Hint.ImageXY -> {
                children += button("Take XY") {
                    onAction = EventHandler {
                        argProperty.set("${zoomCenterXProperty.get()}, ${zoomCenterYProperty.get()}")
                    }
                    setupEnabledWhen(argument, this.disableProperty())
                }
            }
            Hint.ImageDeltaX -> {
                children += button("Take X") {
                    onAction = EventHandler {
                        argProperty.set(zoomCenterXProperty.get().toString())
                    }
                    setupEnabledWhen(argument, this.disableProperty())
                }
                children += button("Take DeltaX") {
                    onAction = EventHandler {
                        val delta = zoomCenterXProperty.get() - argProperty.get().toDouble()
                        argProperty.set(delta.toString())
                    }
                    setupEnabledWhen(argument, this.disableProperty())
                }
            }
            Hint.ImageDeltaY -> {
                children += button("Take Y") {
                    onAction = EventHandler {
                        argProperty.set(zoomCenterYProperty.get().toString())
                    }
                    setupEnabledWhen(argument, this.disableProperty())
                }
                children += button("Take DeltaY") {
                    onAction = EventHandler {
                        val delta = zoomCenterYProperty.get() - argProperty.get().toDouble()
                        argProperty.set(delta.toString())
                    }
                    setupEnabledWhen(argument, this.disableProperty())
                }
            }
            Hint.Curve -> {
                children += createCurveEditor(argument as ScriptListArg, argProperty)
            }
            else -> {
                // do nothing
            }
        }
    }

    private fun <T> setupValidation(node: Node, property: Property<T>, argument: ScriptArg, argProperty: StringProperty) {
        property.addListener { _, _, value ->
            node.pseudoClassStateChanged(INVALID, !argument.isValid(value?.toString()?:""))
        }
        node.pseudoClassStateChanged(INVALID, !argument.isValid(argProperty.get()?:""))
    }

    private fun Reference.evaluate(value: Any): Boolean {
        return when (this) {
            is ReferenceIsEqual -> value in values
            else -> false
        }
    }

    private fun setupEnabledWhen(argument: ScriptArg, disableProperty: BooleanProperty) {
        val enabledWhen = argument.enabledWhen ?: return

        argumentsProperty.addListener(MapChangeListener { change ->
            if (change.key == enabledWhen.name) {
                if (change.wasAdded()) {
                    disableProperty.set(!enabledWhen.evaluate(change.valueAdded))
                }
            }
        })

        val value = argumentsProperty[enabledWhen.name]
        if (value != null) {
            disableProperty.set(!enabledWhen.evaluate(value))
        }
    }

    private fun markdownToHtml(markdown: String): String {
        val options = MutableDataSet()
        val parser: Parser = Parser.builder(options).build()
        val renderer = HtmlRenderer.builder(options).build()

        return renderer.render(parser.parse(markdown))
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

        val files = dir.listFiles().toMutableList()
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
        imageView.onMouseReleased = EventHandler {
            updateFinalZoom()
        }
    }

    private fun setZoom(x: Int, y: Int) {
        zoomCenterXProperty.set(x)
        zoomCenterYProperty.set(y)
        infoTabPane.selectionModel.select(infoTabZoom)
        updateZoom(zoomX = x, zoomY = y)
    }

    private fun setMouseDragEvents(node: Node, handler: EventHandler<in MouseEvent>) {
        node.onMouseClicked = handler
        node.onMouseDragged = handler
    }

    private var zoomDragX: Double? = null
    private var zoomDragY: Double? = null
    private fun setupZoomDragEvents(imageView: ImageView) {
        imageView.onMousePressed = EventHandler { event: MouseEvent ->
            zoomDragX = event.x
            zoomDragY = event.y
        }
        imageView.onMouseDragged = EventHandler { event: MouseEvent ->
            val deltaX = ((zoomDragX!! - event.x) / zoomFactorProperty.get() + 0.5).toInt()
            val deltaY = ((zoomDragY!! - event.y) / zoomFactorProperty.get() + 0.5).toInt()
            if (deltaX != 0 || deltaY != 0) {
                val zoomX = zoomCenterXProperty.get() + deltaX
                val zoomY = zoomCenterYProperty.get() + deltaY
                zoomDragX = event.x
                zoomDragY = event.y
                zoomCenterXProperty.set(zoomX)
                zoomCenterYProperty.set(zoomY)
                updateZoom(zoomX = zoomX, zoomY = zoomY)
            }
        }
        imageView.onMouseReleased = EventHandler {
            updateFinalZoom()
            zoomDragX = null
            zoomDragY = null
        }
    }

    private fun updateZoom(
        zoomX: Int = zoomCenterXProperty.get(),
        zoomY: Int = zoomCenterYProperty.get(),
    ) {
        updateZoom(
            inputCenterX = zoomX,
            inputCenterY = zoomY,
            outputCenterX = zoomX,
            outputCenterY = zoomY)
    }

    private fun updateZoom(
        inputCenterX: Int = zoomCenterXProperty.get(),
        inputCenterY: Int = zoomCenterYProperty.get(),
        inputImage: javafx.scene.image.Image = inputImageView.image,
        outputCenterX: Int = zoomCenterXProperty.get(),
        outputCenterY: Int = zoomCenterYProperty.get(),
        outputImage: javafx.scene.image.Image = outputImageView.image,
        zoomFactor: Int = zoomFactorProperty.get()
    ) {
        val inputImageWidth = inputImage.width.toInt() - 1
        val inputImageHeight = inputImage.height.toInt() - 1

        val outputImageWidth = outputImage.width.toInt() - 1
        val outputImageHeight = outputImage.height.toInt() - 1

        val zoomWidthHalf = ZOOM_WIDTH / 2 / zoomFactor
        val zoomHeightHalf = ZOOM_HEIGHT / 2 / zoomFactor

        val deltaFactor = zoomDeltaFactorProperty.get()

        val inputZoomHistogram = Histogram(256)
        val outputZoomHistogram = Histogram(256)

        val maskMatrix = getMaskMatrix()

        for (y in 0 until ZOOM_HEIGHT) {
            for (x in 0 until ZOOM_WIDTH) {
                val xInput = clamp(x/zoomFactor + inputCenterX - zoomWidthHalf, 0, inputImageWidth)
                val yInput = clamp(y/zoomFactor + inputCenterY - zoomHeightHalf, 0, inputImageHeight)
                var rgbInput = inputImage.pixelReader.getColor(xInput, yInput)
                if (maskShowProperty.value && maskMatrix != null) {
                    val maskValue = maskMatrix[xInput, yInput]
                    if (maskValue > 0) {
                        rgbInput = Color(maskValue + (1.0-maskValue) * rgbInput.red, (1.0-maskValue) * rgbInput.green, (1.0-maskValue) * rgbInput.green, 1.0)
                    }
                }
                inputZoomImage.pixelWriter.setColor(x, y, rgbInput)
                inputZoomHistogram.add(rgbInput)

                val xOutput = clamp(x/zoomFactor + outputCenterX - zoomWidthHalf, 0, outputImageWidth)
                val yOutput = clamp(y/zoomFactor + outputCenterY - zoomHeightHalf, 0, outputImageHeight)
                val rgbOutput = outputImage.pixelReader.getColor(xOutput, yOutput)
                outputZoomImage.pixelWriter.setColor(x, y, rgbOutput)
                outputZoomHistogram.add(rgbOutput)

                val rgbDelta = calculateDeltaColor(rgbInput, rgbOutput, deltaFactor)
                deltaZoomImage.pixelWriter.setColor(x, y, rgbDelta)
            }
        }

        zoomInputMinRedProperty.value = inputZoomHistogram.minRed()
        zoomInputMinGreenProperty.value = inputZoomHistogram.minGreen()
        zoomInputMinBlueProperty.value = inputZoomHistogram.minBlue()
        zoomInputMaxRedProperty.value = inputZoomHistogram.maxRed()
        zoomInputMaxGreenProperty.value = inputZoomHistogram.maxGreen()
        zoomInputMaxBlueProperty.value = inputZoomHistogram.maxBlue()
        zoomInputMedianRedProperty.value = inputZoomHistogram.estimateMedianRed()
        zoomInputMedianGreenProperty.value = inputZoomHistogram.estimateMedianGreen()
        zoomInputMedianBlueProperty.value = inputZoomHistogram.estimateMedianBlue()
        zoomInputStddevRedProperty.value = inputZoomHistogram.estimateStddevRed()
        zoomInputStddevGreenProperty.value = inputZoomHistogram.estimateStddevGreen()
        zoomInputStddevBlueProperty.value = inputZoomHistogram.estimateStddevBlue()

        zoomOutputMinRedProperty.value = outputZoomHistogram.minRed()
        zoomOutputMinGreenProperty.value = outputZoomHistogram.minGreen()
        zoomOutputMinBlueProperty.value = outputZoomHistogram.minBlue()
        zoomOutputMaxRedProperty.value = outputZoomHistogram.maxRed()
        zoomOutputMaxGreenProperty.value = outputZoomHistogram.maxGreen()
        zoomOutputMaxBlueProperty.value = outputZoomHistogram.maxBlue()
        zoomOutputMedianRedProperty.value = outputZoomHistogram.estimateMedianRed()
        zoomOutputMedianGreenProperty.value = outputZoomHistogram.estimateMedianGreen()
        zoomOutputMedianBlueProperty.value = outputZoomHistogram.estimateMedianBlue()
        zoomOutputStddevRedProperty.value = outputZoomHistogram.estimateStddevRed()
        zoomOutputStddevGreenProperty.value = outputZoomHistogram.estimateStddevGreen()
        zoomOutputStddevBlueProperty.value = outputZoomHistogram.estimateStddevBlue()

        drawHistogram(inputZoomHistogram, inputZoomHistogramCanvas)
        drawHistogram(outputZoomHistogram, outputZoomHistogramCanvas)
    }

    private fun drawHistogram(histogram: Histogram, histogramCanvas: Canvas) {
        histogramCanvas.userData = histogram
        val gc = histogramCanvas.graphicsContext2D

        val background = Color.WHITE
        gc.fill = background
        gc.fillRect(0.0, 0.0, histogramCanvas.width, histogramCanvas.height)
        gc.lineWidth = 1.0

        val func: (Double) -> Double = { x -> if (x == 0.0) -1.0 else log10(x) }

        val max = func(histogram.maxCount().toDouble())

        val h = histogramCanvas.height

        var rY1 = h - (func(histogram.red(0).toDouble()) * h / max)
        var gY1 = h - (func(histogram.green(0).toDouble()) * h / max)
        var bY1 = h - (func(histogram.blue(0).toDouble()) * h / max)

        for (x in 1 until histogram.n) {
            val rY = h - (func(histogram.red(x).toDouble()) * h / max)
            val gY = h - (func(histogram.green(x).toDouble()) * h / max)
            val bY = h - (func(histogram.blue(x).toDouble()) * h / max)

            gc.stroke = red
            gc.strokeLine(x.toDouble()-1, rY1, x.toDouble(), rY)

            gc.stroke = green
            gc.strokeLine(x.toDouble()-1, gY1, x.toDouble(), gY)

            gc.stroke = blue
            gc.strokeLine(x.toDouble()-1, bY1, x.toDouble(), bY)

            rY1 = rY
            gY1 = gY
            bY1 = bY
        }
}

    class Histogram(val n: Int = 256) {
        private val countR = IntArray(n)
        private val countG = IntArray(n)
        private val countB = IntArray(n)
        private var entryCount = 0

        private var rMin = Double.MAX_VALUE
        private var gMin = Double.MAX_VALUE
        private var bMin = Double.MAX_VALUE

        private var rMax = -Double.MAX_VALUE
        private var gMax = -Double.MAX_VALUE
        private var bMax = -Double.MAX_VALUE

        fun add(color: Color) {
            rMin = min(rMin, color.red)
            gMin = min(gMin, color.green)
            bMin = min(bMin, color.blue)
            rMax = max(rMax, color.red)
            gMax = max(gMax, color.green)
            bMax = max(bMax, color.blue)
            val r = (color.red * (n-1)).toInt()
            val g = (color.green * (n-1)).toInt()
            val b = (color.blue * (n-1)).toInt()
            countR[r]++
            countG[g]++
            countB[b]++
            entryCount++
        }

        fun entries(): Int {
            return entryCount
        }

        fun maxCount(): Int {
            return max(max(countR.maxOrNull()!!, countG.maxOrNull()!!), countB.maxOrNull()!!)
        }

        fun red(index: Int): Int = countR[index]
        fun green(index: Int): Int = countG[index]
        fun blue(index: Int): Int = countB[index]

        fun minRed(): Double = rMin
        fun minGreen(): Double = gMin
        fun minBlue(): Double = bMin

        fun maxRed(): Double = rMax
        fun maxGreen(): Double = gMax
        fun maxBlue(): Double = bMax

        fun estimateMeanRed(): Double = estimateMean(countR)
        fun estimateMeanGreen(): Double = estimateMean(countG)
        fun estimateMeanBlue(): Double = estimateMean(countB)

        fun estimateMedianRed(): Double = estimatePercentile(0.5, countR)
        fun estimateMedianGreen(): Double = estimatePercentile(0.5, countG)
        fun estimateMedianBlue(): Double = estimatePercentile(0.5, countB)

        fun estimateStddevRed(): Double = estimateStddev(countR)
        fun estimateStddevGreen(): Double = estimateStddev(countG)
        fun estimateStddevBlue(): Double = estimateStddev(countB)

        private fun estimatePercentile(percentile: Double, arr: IntArray): Double {
            val nPercentile = (entryCount * percentile).toInt()
            var cumulativeN = 0
            for (i in arr.indices) {
                if (cumulativeN + arr[i] >= nPercentile) {
                    val lowerLimit = i.toDouble() / (arr.size - 1).toDouble()
                    val width = 1.0 / (arr.size - 1).toDouble()
                    return if (arr[i] == 0) {
                        0.0
                    } else {
                        lowerLimit + (nPercentile - cumulativeN) / arr[i].toDouble() * width
                    }
                }
                cumulativeN += arr[i]
            }
            return 0.0
        }

        private fun estimateMean(arr: IntArray): Double {
            var sum = 0.0
            val width = 1.0 / (arr.size - 1).toDouble()
            for (i in countR.indices) {
                val mid = (i.toDouble() + 0.5) / width
                sum += arr[i] * mid
            }
            return sum / entryCount
        }

        private fun estimateStddev(arr: IntArray): Double {
            val mean = estimateMean(arr)
            val nMinus1 = (arr.size - 1).toDouble()
            val width = 1.0 / nMinus1
            var sum = 0.0
            for (i in countR.indices) {
                val mid = (i.toDouble() + 0.5) / width
                val diff = mid - mean
                sum += arr[i] * diff*diff / (entryCount-1)
            }
            return sqrt(sum)
        }
    }

    private fun updateFinalZoom() {
        if (previewModeProperty.get()) {
            val script = previewScript
            val command = previewCommand
            val image = currentInputImage
            if (image != null && script != null && command != null) {
                previewImage(image, script, command)
            }
        }
    }

    private fun withZoomRectangle(imageView: ImageView, zoomRectanglePane: Pane): Node {
        val rectangle = Rectangle()
        rectangle.isMouseTransparent = true
        rectangle.strokeProperty().bind(crosshairColorProperty)
        rectangle.fill = Color.TRANSPARENT

        zoomCenterXProperty.addListener { _, _, _ -> updateZoomRectangle(imageView, rectangle) }
        zoomCenterYProperty.addListener { _, _, _ -> updateZoomRectangle(imageView, rectangle) }
        zoomFactorProperty.addListener { _, _, _ -> updateZoomRectangle(imageView, rectangle) }
        imageView.imageProperty().addListener { _, _, _ -> updateZoomRectangle(imageView, rectangle) }

        return Pane(imageView, rectangle, zoomRectanglePane)
    }

    private fun updateZoomRectangle(imageView: ImageView, rectangle: Rectangle) {
        val width = ZOOM_WIDTH / zoomFactorProperty.get() / imageView.image.width * imageView.boundsInLocal.width
        val height = ZOOM_HEIGHT / zoomFactorProperty.get()  / imageView.image.height * imageView.boundsInLocal.height
        val x = zoomCenterXProperty.get() / imageView.image.width * imageView.boundsInLocal.width
        val y = zoomCenterYProperty.get() / imageView.image.height * imageView.boundsInLocal.height

        rectangle.x = x - width / 2
        rectangle.y = y - height / 2
        rectangle.width = width
        rectangle.height = height

        if (rectangle.x < 0) {
            rectangle.width += rectangle.x
            rectangle.x = 0.0
        }
        if (rectangle.y < 0) {
            rectangle.height += rectangle.y
            rectangle.y = 0.0
        }
        if (rectangle.x + rectangle.width > imageView.boundsInLocal.width) {
            rectangle.width -= rectangle.x + rectangle.width - imageView.boundsInLocal.width
        }
        if (rectangle.y + rectangle.height > imageView.boundsInLocal.height) {
            rectangle.height -= rectangle.y + rectangle.height - imageView.boundsInLocal.height
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
        fileChooser.initialDirectory = ensureDirectoryExists(initialDirectory)
        fileChooser.title = title
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Image", "*.tif", "*.tiff", "*.png", "*.jpg", "*.jpeg", "*.fits", "*.fit", "*.rwz", "*.rw2", "*.cr2", "*.cr3", "*.nrw", "*.eip", "*.raf", "*.erf", "*.arw", "*.k25", "*.dng", "*.srf", "*.dcr", "*.raw", "*.crf", "*.bay"))
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Bitmap Image", "*.tif", "*.tiff", "*.png", "*.jpg", "*.jpeg", "*.fits", "*.fit"))
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("RAW Image", "*.rwz", "*.rw2", "*.cr2", "*.cr3", "*.nrw", "*.eip", "*.raf", "*.erf", "*.arw", "*.k25", "*.dng", "*.srf", "*.dcr", "*.raw", "*.crf", "*.bay"))

        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("All", "*"))
        return fileChooser.showOpenMultipleDialog(primaryStage)
    }

    fun openFile(initialDirectory: File, title: String = "Select File"): File? {
        val fileChooser = FileChooser()
        fileChooser.initialDirectory = ensureDirectoryExists(initialDirectory)
        fileChooser.title = title
        return fileChooser.showOpenDialog(primaryStage)
    }

    private fun ensureDirectoryExists(initialDirectory: File): File {
        var directory = initialDirectory
        while (!directory.isDirectory) {
            val parent = directory.parent ?: return File(".")
            directory = File(parent)
        }
        return directory
    }

    fun openDir(initialDirectory: File, title: String = "Select Directory"): File? {
        val directoryChooser = DirectoryChooser()
        directoryChooser.initialDirectory = ensureDirectoryExists(initialDirectory)
        directoryChooser.title = title
        return directoryChooser.showDialog(primaryStage)
    }

    companion object {
        private const val SPACING = 2.0

        private const val IMAGE_WIDTH = 400
        private const val IMAGE_HEIGHT = 400

        private const val ZOOM_WIDTH = 256
        private const val ZOOM_HEIGHT = 256

        private const val HISTOGRAM_WIDTH = 256
        private const val HISTOGRAM_HEIGHT = 80

        private const val FILE_TABLE_WIDTH = 400
        private const val FILE_TABLE_HEIGHT = 200

        private const val ARGUMENT_EDITOR_WIDTH = 400
        private const val ARGUMENT_EDITOR_HEIGHT = 400

        val INTEGER_FORMAT = DecimalFormat("##0")
        val DOUBLE_FORMAT = DecimalFormat("##0.000")
        val PERCENT_FORMAT = DecimalFormat("##0.000%")
        val PERCENT_HIGH_FORMAT = DecimalFormat("##0.000000%")

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