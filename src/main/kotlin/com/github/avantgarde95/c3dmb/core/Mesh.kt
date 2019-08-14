package com.github.avantgarde95.c3dmb.core

import glm_.glm
import glm_.vec3.Vec3
import glm_.vec3.Vec3i

class Mesh(val vertices: List<Vec3>, val indices: List<Vec3i>) {
    companion object {
        fun fromModel(model: Model): Mesh {
            val vertexSet = mutableSetOf<Vec3>()

            model.faces.forEach { face ->
                face.forEach { vertex ->
                    vertexSet.add(Vec3(vertex.x, vertex.y, vertex.z))
                }
            }

            val vertices = vertexSet.toList()
            val indices = mutableListOf<Vec3i>()

            model.faces.forEach { face ->
                val f = face.map { vertex ->
                    vertices.indexOf(Vec3(vertex.x, vertex.y, vertex.z))
                }

                for (i in 1..(f.size - 2)) {
                    indices.add(Vec3i(f[0], f[i], f[i + 1]))
                }
            }

            return Mesh(vertices, indices)
        }
    }

    fun computeNormals(): List<Vec3> {
        val normals = vertices.map { Vec3(0.0f) }.toMutableList()

        indices.forEach {
            val v0 = vertices[it.x]
            val v1 = vertices[it.y]
            val v2 = vertices[it.z]
            val n = glm.normalize(glm.cross(v1 - v0, v2 - v0))

            normals[it.x] = normals[it.x] + n
            normals[it.y] = normals[it.y] + n
            normals[it.z] = normals[it.z] + n
        }

        for (i in 0..normals.lastIndex) {
            normals[i] = glm.normalize(normals[i])
        }

        return normals
    }
}
