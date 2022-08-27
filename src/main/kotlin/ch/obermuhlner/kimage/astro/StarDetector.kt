package ch.obermuhlner.kimage.astro

import ch.obermuhlner.kimage.gaussianBlurFilter
import ch.obermuhlner.kimage.math.max
import ch.obermuhlner.kimage.math.median
import ch.obermuhlner.kimage.matrix.DoubleMatrix
import ch.obermuhlner.kimage.matrix.Matrix
import org.apache.commons.math3.fitting.GaussianCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoints

data class Star(
    val x: Int,
    val y: Int,
    val radius: Double,
    val value: Double
)

data class AnalyzedStar(
    val x: Double,
    val y: Double,
    val radius: Double,
    val peakValue: Double,
    val valid: Boolean
)

class StarDetector(val originalMatrix: Matrix) {
    fun detectPotentialStars(minValue: Double = 0.0001, stackCount: Int = 4, startRadius: Int = 1, limitFactor: Double = 0.3): List<Star> {
        val originalMedian = originalMatrix.median()
        val background = DoubleMatrix(originalMatrix.width, originalMatrix.height) { _, _ ->
            originalMedian
        }
        val baseMatrix = originalMatrix - background

        val potentialStars = mutableListOf<Star>()

        var blurRadius = startRadius
        var lastBlurMatrix = baseMatrix
        for (stackIndex in 0 until stackCount) {
            val blurMatrix = baseMatrix.gaussianBlurFilter(blurRadius)
            val diffMatrix = lastBlurMatrix - blurMatrix

            potentialStars += detectPotentialStars(diffMatrix, blurRadius, minValue, limitFactor)
            blurRadius *= 2
            lastBlurMatrix = blurMatrix
        }

//        potentialStars.sortBy { -it.radius }
//        potentialStars.sortBy { -it.value }
        potentialStars.sortWith(compareBy({ -it.radius }, { -it.value }))

        val goodStars = mutableListOf<Star>()
        while (potentialStars.isNotEmpty()) {
            val star = potentialStars[0]
            //println("GOOD $blob")
            goodStars += star

            val squareRadius = star.radius * star.radius
            potentialStars.removeIf {
                val dx = it.x - star.x
                val dy = it.y - star.y
                val distSquare = dx*dx + dy*dy
                distSquare <= squareRadius
            }
        }

        return goodStars
    }

    fun detectPotentialStars(matrix: Matrix, radius: Int, minValue: Double, limitFactor: Double): List<Star> {
        val stars = mutableListOf<Star>()

        matrix.forEach { x, y, value ->
            if (value > minValue) {
                val correctedRadius = 1+ radius + radius*radius*0.01
                val blob = Star(x, y, correctedRadius, value)
                stars += blob
            }
        }

        val maxValue = stars.map { it.value }.max()
        val limitValue = maxValue * limitFactor
        stars.removeIf { it.value < limitValue }

        return stars
    }

    fun analyzePotentialStars(stars: List<Star>): List<AnalyzedStar> {
        return stars.map { analyzePotentialStar(it) }.filterNotNull()
    }

    fun analyzePotentialStar(star: Star, kappa: Double = 0.5, radiusMinFactor: Double = 0.5, radiusMaxFactor: Double = 2.0): AnalyzedStar? {
        val radius = star.radius * 2

        var sumAllValues = 0.0
        var sumAllXValues = 0.0
        var sumAllYValues = 0.0
        val points = WeightedObservedPoints()
        val dy = 0
        for (dx in -radius.toInt() .. radius.toInt()) {
            val x = star.x + dx
            val y = star.y + dy
            sumAllValues += originalMatrix[x, y]
            sumAllXValues += dx * originalMatrix[x, y]
            sumAllYValues += dy * originalMatrix[x, y]
            points.add(dx.toDouble(), originalMatrix[x, y])
        }

        val bestX = sumAllXValues / sumAllValues
        val bestY = sumAllYValues / sumAllValues

        try {
            val (normalized, mean, sigma) = GaussianCurveFitter.create().fit(points.toList())
            println("ANALYZED $star normalized=$normalized mean=$mean sigma=$sigma")

            val valid = if (sigma * kappa > star.radius * radiusMaxFactor) {
                println("REJECT sigma $sigma")
                false
            } else if (sigma *kappa < star.radius * radiusMinFactor) {
                println("REJECT sigma $sigma")
                false
            } else {
                true
            }

            val analyzedRadius = (sigma*kappa*0.5) + 1.0
            return AnalyzedStar(star.x+bestX, star.y+bestY, analyzedRadius, normalized, valid)
        } catch(ex: Exception) {
            return null
        }
    }

    // https://bitbucket.org/assetmax/assetmax-desktop-app/pull-requests/723/added-new-fields?link_source=email
//        val n = radius*2+1
//        val starData = Array<DoubleArray>(n) { y -> DoubleArray(n) { x -> originalMatrix[x, y] } }
//        val gaussian2DFitter = MultivariateNormalMixtureExpectationMaximization(starData)
//        val initialFit = MultivariateNormalMixtureExpectationMaximization.estimate(starData, 2)
//        gaussian2DFitter.fit(initialFit)
//        val fittedModel = gaussian2DFitter.fittedModel
//        println(" FITTED dimension = ${fittedModel.dimension}")
//        println(" FITTED components = ${fittedModel.components}")

}