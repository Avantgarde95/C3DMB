package com.github.avantgarde95.c3dmb.ui

import com.github.avantgarde95.c3dmb.core.*
import com.github.avantgarde95.c3dmb.core.Renderer
import com.jogamp.opengl.GL
import com.jogamp.opengl.GL3
import glm_.glm
import glm_.mat4x4.Mat4
import glm_.pow
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import javax.swing.*

class Viewer : JPanel() {
    private val vertexShaderCode = """
        |#version 330 core
        |
        |struct Vertex {
        |    vec3 position;
        |    vec3 normal;
        |};
        |
        |struct Matrix {
        |    mat4 model;
        |    mat4 view;
        |    mat4 projection;
        |};
        |
        |uniform Matrix u_matrix;
        |uniform Matrix u_normalMatrix;
        |
        |layout(location = 0) in vec3 v_vertexPosition;
        |layout(location = 1) in vec3 v_vertexNormal;
        |
        |out Vertex f_vertex;
        |
        |void main() {
        |    Vertex vertex = Vertex(v_vertexPosition, v_vertexNormal);
        |
        |    Vertex worldVertex = Vertex(
        |        (u_matrix.model * vec4(vertex.position, 1.0)).xyz,
        |        (u_normalMatrix.model * vec4(vertex.normal, 1.0)).xyz
        |    );
        |
        |    Vertex eyeVertex = Vertex(
        |        (u_matrix.view * vec4(worldVertex.position, 1.0)).xyz,
        |        (u_normalMatrix.view * vec4(worldVertex.normal, 1.0)).xyz
        |    );
        |
        |    // Vertex shader -> GPU.
        |    gl_Position = u_matrix.projection * vec4(eyeVertex.position, 1.0);
        |
        |    // Vertex shader -> Fragment shader.
        |    f_vertex.position = worldVertex.position.xyz;
        |    f_vertex.normal = worldVertex.normal.xyz;
        |}
        |""".trimMargin()

    private val fragmentShaderCode = """
        |#version 330 core
        |
        |struct Vertex {
        |    vec3 position;
        |    vec3 normal;
        |};
        |
        |struct Eye {
        |    vec3 position;
        |};
        |
        |struct Light {
        |    vec3 position;
        |    float attenuation;
        |    vec3 diffuse;
        |    vec3 specular;
        |};
        |
        |uniform Eye u_eye;
        |uniform Light u_light;
        |uniform bool u_isWireframeMode;
        |
        |in Vertex f_vertex;
        |
        |layout(location = 0) out vec3 glFragColor;
        |
        |vec3 getDiffuse(Vertex vertex, Light light) {
        |    vec3 N = vertex.normal;
        |    vec3 L = normalize(light.position - vertex.position);
        |    float NdotL = dot(N, L);
        |
        |    if (NdotL < 0.0) {
        |        return vec3(0.0);
        |    }
        |
        |    return light.diffuse * NdotL;
        |}
        |
        |vec3 getSpecular(Vertex vertex, Eye eye, Light light, float smoothness) {
        |    vec3 N = vertex.normal;
        |    vec3 L = normalize(light.position - vertex.position);
        |    float NdotL = dot(N, L);
        |
        |    if (NdotL < 0.0) {
        |        return vec3(0.0);
        |    }
        |
        |    vec3 V = normalize(eye.position - vertex.position);
        |    vec3 H = normalize(L + V);
        |    float NdotH = dot(N, H);
        |    float angle = acos(max(NdotH, 0.0));
        |
        |    return light.specular * exp(-pow(angle / smoothness, 2.0));
        |}
        |
        |float getAttenuation(Vertex vertex, Light light) {
        |    return 1.0 / (1.0 + pow(distance(light.position, vertex.position), 1.0) * light.attenuation);
        |}
        |
        |void main() {
        |    // Fragment shader -> GPU.
        |    if (u_isWireframeMode) {
        |        glFragColor = vec3(1.0);
        |    }
        |    else {
        |        glFragColor = vec3(0.0);
        |        glFragColor += getDiffuse(f_vertex, u_light);
        |        glFragColor *= getAttenuation(f_vertex, u_light);
        |    }
        |}
        |""".trimMargin()

    // Axis-aligned bounding box.
    private class AABB(vertices: List<Vec3>) {
        val min = vertices.reduce { result, v -> glm.min(result, v) }
        val max = vertices.reduce { result, v -> glm.max(result, v) }
        val center = (min + max) * 0.5f
        val extent = max - min
        val maxExtent = maxOf(extent.x, extent.y, extent.z)
        val minExtent = minOf(extent.x, extent.y, extent.z)
    }

    // Eye (Camera).
    private class Eye(
            var position: Vec3,
            var center: Vec3,
            var up: Vec3
    ) {
        fun moveRight(value: Float) {
            val move = glm.normalize(glm.cross(center - position, up)) * value

            position = position + move
            center = center + move
        }

        fun moveUp(value: Float) {
            val move = glm.normalize(up) * value

            position = position + move
            center = center + move
        }

        fun moveFront(value: Float) {
            val move = glm.normalize(center - position) * value

            position = position + move
            center = center + move
        }

        fun rotateRight(value: Float) {
            val matrix = glm.rotate(Mat4(1.0f), -value, up)

            center = (matrix * Vec4(center - position)).toVec3() + position
        }

        fun rotateUp(value: Float) {
            val matrix = glm.rotate(Mat4(1.0f), value, glm.cross(center - position, up))

            center = (matrix * Vec4(center - position)).toVec3() + position
            up = (matrix * Vec4(up)).toVec3()
        }

        fun rotateFront(value: Float) {
            val matrix = glm.rotate(Mat4(1.0f), -value, center - position)

            up = (matrix * Vec4(up)).toVec3()
        }
    }

    // =====================================

    // Variables.
    private var mesh = Mesh(emptyList(), emptyList())
    private var isMeshChanged = false
    private var aabb = AABB(listOf(Vec3(0)))
    private var windowSize = Vec2(0, 0)
    private var eye = Eye(Vec3(0), Vec3(0), Vec3(0))
    private var showMode = 2

    // GL objects.
    private lateinit var vertexShader: Shader
    private lateinit var fragmentShader: Shader
    private lateinit var program: Program
    private lateinit var vao: VAO
    private lateinit var vbo: VBO
    private lateinit var ibo: IBO

    // OpenGL renderer.
    private val renderer = object : Renderer() {
        override fun onStart(gl: GL3) {
            initGL(gl)
            initGLObjects(gl)
        }

        override fun onFrame(gl: GL3) {
            if (isMeshChanged) {
                updateVariables()
                updateGLObjects(gl)
                isMeshChanged = false
            }

            drawMesh(gl)
        }

        override fun onResize(gl: GL3, width: Int, height: Int) {
            gl.glViewport(0, 0, width, height)
            windowSize = Vec2(width, height)
        }

        private fun initGL(gl: GL3) {
            gl.glEnable(GL.GL_DEPTH_TEST)
            gl.glDepthFunc(GL.GL_LESS)
            gl.glEnable(GL.GL_CULL_FACE)
            gl.glCullFace(GL.GL_BACK)
            gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        }

        private fun initGLObjects(gl: GL3) {
            vertexShader = Shader(gl, GL3.GL_VERTEX_SHADER, vertexShaderCode)
            fragmentShader = Shader(gl, GL3.GL_FRAGMENT_SHADER, fragmentShaderCode)
            program = Program(gl, vertexShader, fragmentShader)
            vao = VAO(gl)
            vbo = VBO(gl)
            ibo = IBO(gl)

            vao.run {
                bind(gl)

                vbo.run {
                    bind(gl)
                    setLayout(gl, listOf(3, 3))
                    setData(gl, emptyList())
                    unbind(gl)
                }

                ibo.run {
                    bind(gl)
                    setData(gl, emptyList())
                }

                unbind(gl)
            }
        }

        private fun updateVariables() {
            aabb = AABB(mesh.vertices)

            eye = Eye(
                    position = aabb.center + Vec3(0, 0, aabb.maxExtent),
                    center = aabb.center,
                    up = Vec3(0, 1, 0)
            )
        }

        private fun updateGLObjects(gl: GL3) {
            vao.run {
                bind(gl)

                vbo.run {
                    bind(gl)
                    val normals = mesh.computeNormals()
                    val vertices = mutableListOf<Vec3>()

                    for (i in 0 until mesh.vertices.size) {
                        vertices.add(mesh.vertices[i])
                        vertices.add(normals[i])
                    }

                    setData(gl, vertices)
                    unbind(gl)
                }

                ibo.run {
                    bind(gl)
                    setData(gl, mesh.indices)
                }

                unbind(gl)
            }
        }

        private fun drawMesh(gl: GL3) {
            // Clear the screen.
            gl.glClear(GL.GL_COLOR_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT)

            program.run {
                use(gl)

                Mat4(1.0f).let {
                    setUniform(gl, "u_matrix.model", it)
                    setUniform(gl, "u_normalMatrix.model", glm.inverseTranspose(it))
                }

                glm.lookAt(eye.position, eye.center, eye.up).let {
                    setUniform(gl, "u_matrix.view", it)
                    setUniform(gl, "u_normalMatrix.view", glm.inverseTranspose(it))
                }

                setUniform(gl, "u_matrix.projection", glm.perspective(
                        glm.radians(90.0f),
                        windowSize.x / windowSize.y,
                        aabb.minExtent * 0.1f,
                        aabb.maxExtent * 100.0f
                ))

                setUniform(gl, "u_eye.position", eye.position)
                setUniform(gl, "u_light.position", eye.position)
                setUniform(gl, "u_light.attenuation", 0.1f / aabb.maxExtent.pow(1.0f))
                setUniform(gl, "u_light.diffuse", Vec3(0.4f, 0.8f, 0.8f))
                setUniform(gl, "u_light.specular", Vec3(0.5f))

                vao.run {
                    bind(gl)

                    ibo.run {
                        bind(gl)

                        when (showMode) {
                            0 -> {
                                // Draw the surface.
                                gl.glEnable(GL.GL_CULL_FACE)
                                gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL3.GL_FILL)
                                setUniform(gl, "u_isWireframeMode", false)
                                draw(gl)
                            }
                            1 -> {
                                // Draw the wireframe.
                                gl.glDisable(GL.GL_CULL_FACE)
                                gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL3.GL_LINE)
                                setUniform(gl, "u_isWireframeMode", true)
                                draw(gl)
                            }
                            else -> {
                                gl.glEnable(GL.GL_CULL_FACE)

                                // Let the wireframe be in front of the surface.
                                gl.glPolygonOffset(1.0f, 1.0f)

                                // Draw the surface.
                                gl.glEnable(GL.GL_POLYGON_OFFSET_FILL)
                                gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL3.GL_FILL)
                                setUniform(gl, "u_isWireframeMode", false)
                                draw(gl)

                                // Draw the wireframe.
                                gl.glDisable(GL.GL_POLYGON_OFFSET_FILL)
                                gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL3.GL_LINE)
                                setUniform(gl, "u_isWireframeMode", true)
                                draw(gl)
                            }
                        }
                    }

                    unbind(gl)
                }
            }
        }
    }

    init {
        initLayout()
        initControls()

        renderer.start()
    }

    fun setMesh(mesh: Mesh) {
        this.mesh = mesh
        isMeshChanged = true
    }

    private fun initLayout() {
        layout = BorderLayout()

        add(JLabel("Move: WSADQE / Rotate: IKJLUO"), BorderLayout.NORTH)
        add(renderer, BorderLayout.CENTER)

        /*
        add(JPanel().apply {
            layout = OverlayLayout(this)

            add(JLabel("Move: WSADQE / Rotate: IKJLUO").apply {
                alignmentX = 0.0f
                alignmentY = 0.0f
                isOpaque = true
                background = Color.BLACK
                foreground = Color.WHITE
            })

            add(JPanel().apply {
                alignmentX = 0.0f
                alignmentY = 0.0f
                layout = BorderLayout()

                add(renderer, BorderLayout.CENTER)
            })
        }, BorderLayout.CENTER)
         */

        add(JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            val group = ButtonGroup()

            add(JRadioButton("Show surface", true).apply {
                group.add(this)
                addActionListener { showMode = 0 }
            })

            add(JRadioButton("Show wireframe").apply {
                group.add(this)
                addActionListener { showMode = 1 }
            })

            add(JRadioButton("Show all").apply {
                group.add(this)
                addActionListener { showMode = 2 }
                isSelected = true
            })
        }, BorderLayout.SOUTH)
    }

    private fun initControls() {
        val moveSpeed = 0.05f
        val rotateSpeed = glm.radians(3.0f)

        bindKey('w') { eye.moveFront(aabb.minExtent * moveSpeed) }
        bindKey('s') { eye.moveFront(aabb.minExtent * -moveSpeed) }
        bindKey('a') { eye.moveRight(-aabb.minExtent * moveSpeed) }
        bindKey('d') { eye.moveRight(aabb.minExtent * moveSpeed) }
        bindKey('q') { eye.moveUp(aabb.minExtent * moveSpeed) }
        bindKey('e') { eye.moveUp(-aabb.minExtent * moveSpeed) }

        bindKey('j') { eye.rotateRight(-rotateSpeed) }
        bindKey('l') { eye.rotateRight(rotateSpeed) }
        bindKey('i') { eye.rotateUp(rotateSpeed) }
        bindKey('k') { eye.rotateUp(-rotateSpeed) }
        bindKey('u') { eye.rotateFront(rotateSpeed) }
        bindKey('o') { eye.rotateFront(-rotateSpeed) }
    }

    private fun bindKey(key: Char, action: () -> Unit) {
        val name = "Action$key"
        val possibleKeys = listOf(key.toLowerCase(), key.toUpperCase())

        possibleKeys.forEach {
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(it), name)
        }

        actionMap.put(name, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                action()
            }
        })
    }
}
