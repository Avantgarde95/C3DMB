// Kotlin version of my previous project: https://github.com/Avantgarde95/JOGLObjTest/

package app.ui

import app.core.Model
import com.jogamp.opengl.*
import com.jogamp.opengl.awt.GLJPanel
import com.jogamp.opengl.fixedfunc.GLLightingFunc
import com.jogamp.opengl.fixedfunc.GLMatrixFunc
import com.jogamp.opengl.util.FPSAnimator
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.KeyStroke
import kotlin.math.sqrt


class Viewer : GLJPanel(GLCapabilities(GLProfile.get(GLProfile.GL3bc))), GLEventListener {
    private var haveNewModel = true
    private var showWireframe = false
    private var modelDisplayList = 0
    private var wireframeDisplayList = 0
    private var model = Model(faces = emptyList())

    private val position = doubleArrayOf(0.0, 0.0, 0.0)
    private val angle = doubleArrayOf(0.0, 0.0, 0.0)
    private val scale = doubleArrayOf(0.0)
    private val dPosition = 0.1
    private val dAngle = 2.0
    private val dScale = 0.01

    init {
        addGLEventListener(this)
        autoSwapBufferMode = true
    }

    fun start() {
        FPSAnimator(this, 60, true).start()
    }

    fun setModel(model: Model) {
        this.model = model
        resetStates()
    }

    override fun init(drawable: GLAutoDrawable?) {
        val gl = drawable.getMyGL()
        initGL(gl)
        initControls()
    }

    override fun dispose(drawable: GLAutoDrawable?) {
        // Do nothing.
    }

    override fun display(drawable: GLAutoDrawable?) {
        val gl = drawable.getMyGL()

        if (haveNewModel) {
            haveNewModel = false
            resetModelDisplayList(gl)
            resetWireframeDisplayList(gl)
        }

        gl.glClear(GL.GL_COLOR_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT)

        gl.glPushMatrix()
        gl.glLoadIdentity()
        gl.glTranslated(position[0], position[1], position[2])
        gl.glRotated(angle[0], 1.0, 0.0, 0.0)
        gl.glRotated(angle[1], 0.0, 1.0, 0.0)
        gl.glRotated(angle[2], 0.0, 0.0, 1.0)
        gl.glScaled(scale[0], scale[0], scale[0])
        gl.glCallList(if (showWireframe) wireframeDisplayList else modelDisplayList)
        gl.glPopMatrix()

        gl.glFlush()
    }

    override fun reshape(drawable: GLAutoDrawable?, x: Int, y: Int, width: Int, height: Int) {
        val gl = drawable.getMyGL()
        val aspect = width / height.toDouble()

        gl.glViewport(0, 0, width, height)

        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION)
        gl.glLoadIdentity()

        gl.glFrustum(
                -aspect, // Left.
                aspect,  // Right.
                -1.0,    // Bottom.
                1.0,     // Top.
                1.0,     // Near.
                100.0    // Far.
        )

        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW)
        gl.glLoadIdentity()
    }

    private fun initGL(gl: GL2) {
        gl.glEnable(GL.GL_DEPTH_TEST)
        gl.glDepthFunc(GL.GL_LEQUAL)

        gl.glEnable(GL.GL_CULL_FACE)
        gl.glCullFace(GL.GL_BACK)

        gl.glEnable(GLLightingFunc.GL_LIGHTING)
        gl.glEnable(GLLightingFunc.GL_LIGHT0)
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_POSITION, floatArrayOf(-1.0f, -1.0f, -1.0f, 0.0f), 0)
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_AMBIENT, floatArrayOf(1.0f, 1.0f, 1.0f), 0)
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_DIFFUSE, floatArrayOf(1.0f, 1.0f, 1.0f), 0)
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_SPECULAR, floatArrayOf(1.0f, 1.0f, 1.0f), 0)
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_SHININESS, floatArrayOf(1.0f, 1.0f, 1.0f), 0)

        gl.glShadeModel(GLLightingFunc.GL_SMOOTH)
        gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_AMBIENT, floatArrayOf(0.2f, 0.2f, 0.2f, 1.0f), 0)
        gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_DIFFUSE, floatArrayOf(0.8f, 0.8f, 0.8f, 1.0f), 0)
        gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_SPECULAR, floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f), 0)
        gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_SHININESS, floatArrayOf(100.0f), 0)

        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
    }

    private fun initControls() {
        bindKey('r') { showWireframe = !showWireframe }
        bindKey('+') { scale[0] += dScale }
        bindKey('-') { scale[0] -= dScale }
        bindKey('d') { position[0] += dPosition }
        bindKey('a') { position[0] -= dPosition }
        bindKey('w') { position[1] += dPosition }
        bindKey('s') { position[1] -= dPosition }
        bindKey('q') { position[2] += dPosition }
        bindKey('e') { position[2] -= dPosition }
        bindKey('i') { angle[0] += dAngle }
        bindKey('k') { angle[0] -= dAngle }
        bindKey('l') { angle[1] += dAngle }
        bindKey('j') { angle[1] -= dAngle }
        bindKey('u') { angle[2] += dAngle }
        bindKey('o') { angle[2] -= dAngle }
    }

    private fun resetStates() {
        haveNewModel = true
        showWireframe = false
        position[0] = 0.0
        position[1] = 0.0
        position[2] = -3.0
        angle[0] = 0.0
        angle[1] = 0.0
        angle[2] = 0.0
        scale[0] = 1.0
    }

    private fun resetModelDisplayList(gl: GL2) {
        gl.glDeleteLists(modelDisplayList, 1)
        modelDisplayList = gl.glGenLists(1)
        gl.glNewList(modelDisplayList, GL2.GL_COMPILE)

        model.faces.forEach { face ->
            if (face.size >= 3) {
                val vertices = face.map { doubleArrayOf(it.x, it.y, it.z) }
                val normal = getNormal(vertices[0], vertices[1], vertices[2])

                gl.glBegin(GL.GL_TRIANGLE_FAN)

                vertices.forEach {
                    gl.glVertex3d(it[0], it[1], it[2])
                    gl.glNormal3d(normal[0], normal[1], normal[2])
                }

                gl.glEnd()
            }
        }

        gl.glEndList()
    }

    private fun resetWireframeDisplayList(gl: GL2) {
        gl.glDeleteLists(wireframeDisplayList, 1)
        wireframeDisplayList = gl.glGenLists(1)
        gl.glNewList(wireframeDisplayList, GL2.GL_COMPILE)

        model.faces.forEach { face ->
            if (face.size >= 3) {
                val vertices = face.map { doubleArrayOf(it.x, it.y, it.z) }

                for (i in 0..(face.size - 3)) {
                    val triangleVertices = arrayOf(vertices[0], vertices[i + 1], vertices[i + 2])
                    val normal = getNormal(vertices[0], vertices[1], vertices[2])

                    gl.glBegin(GL.GL_LINE_LOOP)

                    triangleVertices.forEach {
                        gl.glVertex3d(it[0], it[1], it[2])
                        gl.glNormal3d(normal[0], normal[1], normal[2])
                    }

                    gl.glEnd()
                }
            }
        }

        gl.glEndList()
    }

    private fun GLAutoDrawable?.getMyGL() = this!!.gl.gL2

    private fun bindKey(key: Char, action: () -> Unit) {
        "Action$key".apply {
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), this)
            actionMap.put(this, object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent?) {
                    action()
                }
            })
        }
    }

    private fun getNormal(v1: DoubleArray, v2: DoubleArray, v3: DoubleArray): DoubleArray {
        val v12 = doubleArrayOf(0.0, 0.0, 0.0)
        val v13 = doubleArrayOf(0.0, 0.0, 0.0)
        val n = doubleArrayOf(0.0, 0.0, 0.0)

        subVec3(v12, v2, v1)
        subVec3(v13, v3, v1)
        crossVec3(n, v12, v13)

        val nNorm = sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2])

        return when (nNorm) {
            0.0 -> n
            else -> doubleArrayOf(n[0] / nNorm, n[1] / nNorm, n[2] / nNorm)
        }
    }

    // Double version of VectorUtil.subVec3 of JOGL.
    private fun subVec3(result: DoubleArray, v1: DoubleArray, v2: DoubleArray): DoubleArray {
        result[0] = v1[0] - v2[0]
        result[1] = v1[1] - v2[1]
        result[2] = v1[2] - v2[2]

        return result
    }

    // Double version of VectorUtil.crossVec3 of JOGL.
    private fun crossVec3(result: DoubleArray, v1: DoubleArray, v2: DoubleArray): DoubleArray {
        result[0] = v1[1] * v2[2] - v1[2] * v2[1]
        result[1] = v1[2] * v2[0] - v1[0] * v2[2]
        result[2] = v1[0] * v2[1] - v1[1] * v2[0]

        return result
    }
}
