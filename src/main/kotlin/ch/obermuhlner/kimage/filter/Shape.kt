package ch.obermuhlner.kimage.filter

enum class Shape {
    Square {
        override fun horizontalRadius(radius: Int) = radius
        override fun verticalRadius(radius: Int) = radius
        override fun isInside(x: Int, y: Int, radius: Int) = true
    },
    Circle {
        override fun horizontalRadius(radius: Int) = radius
        override fun verticalRadius(radius: Int) = radius
        override fun isInside(x: Int, y: Int, radius: Int) = x*x + y*y <= radius*radius
    },
    Horizontal {
        override fun horizontalRadius(radius: Int) = radius
        override fun verticalRadius(radius: Int) = 0
        override fun isInside(x: Int, y: Int, radius: Int) = true
    },
    Vertical {
        override fun horizontalRadius(radius: Int) = 0
        override fun verticalRadius(radius: Int) = radius
        override fun isInside(x: Int, y: Int, radius: Int) = true
    },
    Cross {
        override fun horizontalRadius(radius: Int) = radius
        override fun verticalRadius(radius: Int) = radius
        override fun isInside(x: Int, y: Int, radius: Int) = true
    },
    DiagonalCross {
        override fun horizontalRadius(radius: Int) = radius
        override fun verticalRadius(radius: Int) = radius
        override fun isInside(x: Int, y: Int, radius: Int) = true
    },
    Star {
        override fun horizontalRadius(radius: Int) = radius
        override fun verticalRadius(radius: Int) = radius
        override fun isInside(x: Int, y: Int, radius: Int) = true
    };

    abstract fun horizontalRadius(radius: Int): Int
    abstract fun verticalRadius(radius: Int): Int
    abstract fun isInside(x: Int, y: Int, radius: Int): Boolean
}