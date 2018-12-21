package app

import app.core.Util
import app.ui.App
import java.awt.Font
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.WindowConstants

fun main(args: Array<String>) {
    Font
            .createFont(
                    Font.TRUETYPE_FONT,
                    Util.getResourceAsStream("Roboto-Bold.ttf")
            )
            .deriveFont(16f)
            .apply {
                UIManager.put("Button.font", this)
                UIManager.put("TabbedPane.font", this)
                UIManager.put("TitledBorder.font", this)
                UIManager.put("List.font", this)
                UIManager.put("TextArea.font", this)
            }

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
