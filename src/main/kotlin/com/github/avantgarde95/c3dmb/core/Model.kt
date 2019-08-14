package com.github.avantgarde95.c3dmb.core

import com.beust.klaxon.Json

class Model(
        val faces: List<List<Vertex>>
) {
    @Json(ignored = true)
    val hash = Util.hash(toString())

    override fun toString() = faces.joinToString { "[${it.joinToString()}]" }
}
