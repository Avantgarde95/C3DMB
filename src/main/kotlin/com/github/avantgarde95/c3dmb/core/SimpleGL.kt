@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.github.avantgarde95.c3dmb.core

import com.jogamp.common.nio.Buffers
import com.jogamp.opengl.*
import com.jogamp.opengl.awt.GLCanvas
import com.jogamp.opengl.util.FPSAnimator
import glm_.ToFloatBuffer
import glm_.mat4x4.Mat4
import glm_.toBuffer
import glm_.vec3.Vec3
import glm_.vec3.Vec3i
import java.nio.charset.StandardCharsets

class Shader(gl: GL3, type: Int, code: String) {
    val id = gl.glCreateShader(type)

    init {
        gl.glShaderSource(id, 1, arrayOf(code), null)
        gl.glCompileShader(id)

        val logLengthBuffer = Buffers.newDirectIntBuffer(1)
        gl.glGetShaderiv(id, GL3.GL_INFO_LOG_LENGTH, logLengthBuffer)
        val logLength = logLengthBuffer[0]

        if (logLength > 1) {
            val logBuffer = Buffers.newDirectByteBuffer(logLength)
            gl.glGetShaderInfoLog(id, logLength, null, logBuffer)
            val log = StandardCharsets.UTF_8.decode(logBuffer).toString()

            throw Exception("[com.github.avantgarde95.c3dmb.core.Shader] $log")
        }
    }
}

class Program(gl: GL3, vararg shaders: Shader) {
    val id = gl.glCreateProgram()

    init {
        shaders.forEach { gl.glAttachShader(id, it.id) }
        gl.glLinkProgram(id)
        shaders.forEach { gl.glDetachShader(id, it.id) }

        val logLengthBuffer = Buffers.newDirectIntBuffer(1)
        gl.glGetProgramiv(id, GL3.GL_INFO_LOG_LENGTH, logLengthBuffer)
        val logLength = logLengthBuffer[0]

        if (logLength > 1) {
            val logBuffer = Buffers.newDirectByteBuffer(logLength)
            gl.glGetProgramInfoLog(id, logLength, null, logBuffer)
            val log = StandardCharsets.UTF_8.decode(logBuffer).toString()

            throw Exception("[com.github.avantgarde95.c3dmb.core.Program] $log")
        }
    }

    fun use(gl: GL3) {
        gl.glUseProgram(id)
    }

    fun setUniform(gl: GL3, name: String, value: Int) {
        gl.glUniform1i(gl.glGetUniformLocation(id, name), value)
    }

    fun setUniform(gl: GL3, name: String, value: Boolean) {
        gl.glUniform1i(gl.glGetUniformLocation(id, name), if (value) 1 else 0)
    }

    fun setUniform(gl: GL3, name: String, value: Float) {
        gl.glUniform1f(gl.glGetUniformLocation(id, name), value)
    }

    fun setUniform(gl: GL3, name: String, value: Vec3) {
        gl.glUniform3fv(gl.glGetUniformLocation(id, name), 1, value.toFloatBuffer())
    }

    fun setUniform(gl: GL3, name: String, value: Mat4) {
        gl.glUniformMatrix4fv(gl.glGetUniformLocation(id, name), 1, false, value.toFloatBuffer())
    }
}

class VAO(gl: GL3) {
    val id: Int

    init {
        val idBuffer = Buffers.newDirectIntBuffer(1)
        gl.glGenVertexArrays(1, idBuffer)
        id = idBuffer[0]
    }

    fun bind(gl: GL3) {
        gl.glBindVertexArray(id)
    }

    fun unbind(gl: GL3) {
        gl.glBindVertexArray(0)
    }
}

class VBO(gl: GL3) {
    val id: Int

    init {
        val idBuffer = Buffers.newDirectIntBuffer(1)
        gl.glGenBuffers(1, idBuffer)
        id = idBuffer[0]
    }

    fun bind(gl: GL3) {
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, id)
    }

    fun unbind(gl: GL3) {
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0)
    }

    fun setLayout(gl: GL3, dimensions: List<Int>) {
        val stride = dimensions.sum() * Buffers.SIZEOF_FLOAT
        var offset = 0L

        dimensions.forEachIndexed { index, dimension ->
            gl.glEnableVertexAttribArray(index)
            gl.glVertexAttribPointer(index, dimension, GL.GL_FLOAT, false, stride, offset)

            offset += dimension * Buffers.SIZEOF_FLOAT
        }
    }

    fun setData(gl: GL3, values: List<ToFloatBuffer>) {
        val buffer = values.toBuffer(assumeConstSize = false)

        gl.glBufferData(GL3.GL_ARRAY_BUFFER, buffer.capacity().toLong(), buffer, GL.GL_STATIC_DRAW)
    }
}

class IBO(gl: GL3) {
    val id: Int

    private var count = 0
    private var mode = 0

    init {
        val idBuffer = Buffers.newDirectIntBuffer(1)
        gl.glGenBuffers(1, idBuffer)
        id = idBuffer[0]
    }

    fun bind(gl: GL3) {
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, id)
    }

    fun setData(gl: GL3, values: List<Vec3i>) {
        val buffer = values.toBuffer(assumeConstSize = true)
        count = values.size * 3
        mode = GL.GL_TRIANGLES

        gl.glBufferData(GL3.GL_ELEMENT_ARRAY_BUFFER, buffer.capacity().toLong(), buffer, GL.GL_STATIC_DRAW)
    }

    fun draw(gl: GL3) {
        gl.glDrawElements(mode, count, GL.GL_UNSIGNED_INT, 0)
    }
}

abstract class Renderer : GLCanvas(GLCapabilities(GLProfile.get(GLProfile.GL3))) {
    abstract fun onStart(gl: GL3)
    abstract fun onFrame(gl: GL3)
    abstract fun onResize(gl: GL3, width: Int, height: Int)

    init {
        this.addGLEventListener(object : GLEventListener {
            override fun init(drawable: GLAutoDrawable?) {
                drawable!!.gl = DebugGL3(drawable.getMyGL())
                onStart(drawable.getMyGL())
            }

            override fun display(drawable: GLAutoDrawable?) {
                onFrame(drawable.getMyGL())
            }

            override fun dispose(drawable: GLAutoDrawable?) {
            }

            override fun reshape(drawable: GLAutoDrawable?, x: Int, y: Int, width: Int, height: Int) {
                onResize(drawable.getMyGL(), width, height)
            }

            private fun GLAutoDrawable?.getMyGL() = this!!.gl.gL3
        })
    }

    fun start() {
        FPSAnimator(this, 60, true).start()
    }
}
