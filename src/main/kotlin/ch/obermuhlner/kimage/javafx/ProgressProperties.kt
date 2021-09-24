package ch.obermuhlner.kimage.javafx

import ch.obermuhlner.kimage.Progress
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty

class ProgressProperties : Progress {
    val totalProperty = SimpleIntegerProperty(0)
    val totalMessage = SimpleStringProperty("")

    val stepProperty = SimpleIntegerProperty(0)
    val stepMessage = SimpleStringProperty("")

    override fun addTotal(totalStepCount: Int, message: String) {
        totalProperty.set(totalProperty.get() + totalStepCount)
        totalMessage.set(message)
    }

    override fun step(stepCount: Int, message: String) {
        stepProperty.set(stepProperty.get() + stepCount)
        stepMessage.set(message)
    }

    fun progressPercent() = stepProperty.get().toDouble() / totalProperty.get()
}