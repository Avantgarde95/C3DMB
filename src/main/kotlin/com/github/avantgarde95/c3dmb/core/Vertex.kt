package com.github.avantgarde95.c3dmb.core

data class Vertex(
        val x: Double,
        val y: Double,
        val z: Double
) {
    override fun toString() = "($x, $y, $z)"
}
