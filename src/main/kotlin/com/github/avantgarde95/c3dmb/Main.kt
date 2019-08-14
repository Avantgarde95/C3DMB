package com.github.avantgarde95.c3dmb

import com.github.avantgarde95.c3dmb.core.Util
import com.github.avantgarde95.c3dmb.ui.App
import java.awt.Font
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.WindowConstants

fun main(args: Array<String>) {
    val normalFont = Font.createFont(
            Font.TRUETYPE_FONT,
            Util.getResourceAsStream("Roboto-Bold.ttf")
    ).deriveFont(16f)

    val monoFont = Font.createFont(
            Font.TRUETYPE_FONT,
            Util.getResourceAsStream("RobotoMono-Bold.ttf")
    ).deriveFont(16f)

    arrayOf(
            "Button",
            "TabbedPane",
            "TitledBorder",
            "List",
            "Tree",
            "Label",
            "RadioButton"
    ).forEach { UIManager.put("$it.font", normalFont) }

    arrayOf(
            "TextArea",
            "TextField",
            "Tree"
    ).forEach { UIManager.put("$it.font", monoFont) }

    SwingUtilities.invokeLater {
        App(
                "Blockchain client",
                when {
                    args.isEmpty() -> null
                    else -> Util.getResourceAsString(args[0])
                }
        ).run {
            defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            isVisible = true
            isResizable = true
            pack()
        }
    }
}
