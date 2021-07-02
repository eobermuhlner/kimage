package ch.obermuhlner.kimage.math

import kotlin.math.hypot

/**
 * https://gist.github.com/lecho/7627739
 */
class SplineInterpolator private constructor(private val mX: List<Double>, private val mY: List<Double>, private val mM: DoubleArray) {
    /**
     * Interpolates the value of Y = f(X) for given X. Clamps X to the domain of the spline.
     *
     * @param x
     * The X value.
     * @return The interpolated Y = f(X) value.
     */
    fun interpolate(x: Double): Double {
        // Handle the boundary cases.
        val n: Int = mX.size
        if (java.lang.Double.isNaN(x)) {
            return x
        }
        if (x <= mX[0]) {
            return mY[0]
        }
        if (x >= mX[n - 1]) {
            return mY[n - 1]
        }

        // Find the index 'i' of the last point with smaller X.
        // We know this will be within the spline due to the boundary tests.
        var i: Int = 0
        while (x >= mX[i + 1]) {
            i += 1
            if (x == mX[i]) {
                return mY[i]
            }
        }

        // Perform cubic Hermite spline interpolation.
        val h: Double = mX[i + 1] - mX[i]
        val t: Double = (x - mX[i]) / h
        return ((mY[i] * (1 + 2 * t) + h * mM[i] * t) * (1 - t) * (1 - t)
                + (mY[i + 1] * (3 - 2 * t) + h * mM[i + 1] * (t - 1)) * t * t)
    }

    // For debugging.
    override fun toString(): String {
        val str: StringBuilder = StringBuilder()
        val n: Int = mX.size
        str.append("[")
        for (i in 0 until n) {
            if (i != 0) {
                str.append(", ")
            }
            str.append("(").append(mX[i])
            str.append(", ").append(mY[i])
            str.append(": ").append(mM[i]).append(")")
        }
        str.append("]")
        return str.toString()
    }

    companion object {
        /**
         * Creates a monotone cubic spline from a given set of control points.
         *
         * The spline is guaranteed to pass through each control point exactly. Moreover, assuming the control points are
         * monotonic (Y is non-decreasing or non-increasing) then the interpolated values will also be monotonic.
         *
         * This function uses the Fritsch-Carlson method for computing the spline parameters.
         * http://en.wikipedia.org/wiki/Monotone_cubic_interpolation
         *
         * @param x
         * The X component of the control points, strictly increasing.
         * @param y
         * The Y component of the control points
         * @return
         *
         * @throws IllegalArgumentException
         * if the X or Y arrays are null, have different lengths or have fewer than 2 values.
         */
        fun createMonotoneCubicSpline(x: List<Double>?, y: List<Double>?): SplineInterpolator {
            if ((x == null) || (y == null) || (x.size != y.size) || (x.size < 2)) {
                throw IllegalArgumentException(("There must be at least two control "
                        + "points and the arrays must be of equal length."))
            }
            val n: Int = x.size
            val d: DoubleArray = DoubleArray(n - 1) // could optimize this out
            val m: DoubleArray = DoubleArray(n)

            // Compute slopes of secant lines between successive points.
            for (i in 0 until n - 1) {
                val h: Double = x[i + 1] - x[i]
                if (h <= 0) {
                    throw IllegalArgumentException("The control points must all have strictly increasing X values.")
                }
                d[i] = (y[i + 1] - y[i]) / h
            }

            // Initialize the tangents as the average of the secants.
            m[0] = d[0]
            for (i in 1 until n - 1) {
                m[i] = (d[i - 1] + d[i]) * 0.5
            }
            m[n - 1] = d[n - 2]

            // Update the tangents to preserve monotonicity.
            for (i in 0 until n - 1) {
                if (d[i] == 0.0) { // successive Y values are equal
                    m[i] = 0.0
                    m[i + 1] = 0.0
                } else {
                    val a: Double = m[i] / d[i]
                    val b: Double = m[i + 1] / d[i]
                    val h: Double = hypot(a, b)
                    if (h > 3) {
                        val t: Double = 3 / h
                        m[i] = t * a * d[i]
                        m[i + 1] = t * b * d[i]
                    }
                }
            }
            return SplineInterpolator(x, y, m)
        }
    }
}