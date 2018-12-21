package app.ui

import app.core.*
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.*

class App(title: String, settingsText: String?) : JFrame(title) {
    private val blockListModel = DefaultListModel<String>()
    private val transactionListModel = DefaultListModel<String>()

    // ===========================================================

    private val settingsInput = JTextArea().apply {
        isEditable = true

        text = settingsText ?: """
        |{
        |   "name": "SGVR Lab",
        |   "me": {"host": "127.0.0.1", "port": 8888},
        |   "peers": [
        |       {"host": "127.0.0.1", "port": 8878}
        |   ],
        |   "tools": [
        |       {"host": "127.0.0.1", "port": 8889}
        |   ]
        |}
        |""".trimMargin()
    }

    private val runButton = JButton("Run").apply {
        addActionListener {
            onClickRunButton()
        }
    }

    // ===========================================================

    private val poolText = JTextArea(4, 1).apply {
        isOpaque = true
        isEditable = false
    }

    // ===========================================================

    private val blockList = JList<String>().apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        layoutOrientation = JList.VERTICAL
        model = blockListModel

        addListSelectionListener { event ->
            if (!event.valueIsAdjusting && selectedValue != null) {
                onSelectBlockList(selectedValue)
            }
        }
    }

    private val mineButton = JButton("Mine a block").apply {
        addActionListener {
            onClickMineButton()
        }
    }

    // ===========================================================

    private val blockText = JTextArea(4, 1).apply {
        isOpaque = false
        isEditable = false
    }

    private val transactionList = JList<String>().apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        layoutOrientation = JList.VERTICAL
        model = transactionListModel

        addListSelectionListener { event ->
            if (!event.valueIsAdjusting && selectedValue != null) {
                onSelectTransactionList(selectedValue)
            }
        }
    }

    // ===========================================================

    private val transactionText = JTextArea(5, 1).apply {
        isOpaque = false
        isEditable = false
    }

    private val transactionViewer = Viewer().apply {
        isFocusable = true
        isRequestFocusEnabled = true

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                requestFocus()
            }
        })

        start()
    }

    private val applyButton = JButton("Checkout this version").apply {
        addActionListener {
            onClickApplyButton()
        }
    }

    // ===========================================================

    private val logText = JTextArea().apply {
        isEditable = false
    }

    // ===========================================================

    private lateinit var selectedBlock: Block
    private lateinit var selectedTransaction: Transaction

    private val chain = Chain(
            onMine = { block ->
                client.broadcastBlockToPeers(block)
            },
            onUpdateBlocks = { blocks ->
                setBlockList(blocks)
            },
            onUpdatePool = { transactions ->
                setPoolText(transactions)
            },
            onLog = { message ->
                appendLogText(message)
            }
    )

    private val settings by lazy {
        Util.fromJson<Settings>(getSettingsInput())
    }

    private val client by lazy {
        Client(
                peers = settings.peers,
                tools = settings.tools,
                onLog = { message ->
                    appendLogText(message)
                }
        )
    }

    private val server by lazy {
        Server(
                me = settings.me,
                onReceiveBlockFromPeer = { block ->
                    chain.addBlock(block)
                },
                onReceiveModelFromTool = { model ->
                    val lastTransaction = chain.getLastTransaction()
                    val lastModel = lastTransaction.getModel(chain)

                    Transaction(
                            author = settings.name,
                            timestamp = Util.timestamp(),
                            previousHash = lastTransaction.hash,
                            addedModel = Model(faces = model.faces - lastModel.faces),
                            removedModel = Model(faces = lastModel.faces - model.faces)
                    ).apply {
                        chain.addTransaction(this)
                        client.broadcastTransactionToPeers(this)
                    }
                },
                onReceiveTransactionFromPeer = { transaction ->
                    chain.addTransaction(transaction)
                },
                onLog = { message ->
                    appendLogText(message)
                }
        )
    }

    // ===========================================================

    init {
        initLayout()
        hookException()
    }

    private fun initLayout() {
        val poolPanel = JPanel().apply {
            border = BorderFactory.createTitledBorder("Mempool")
            layout = BorderLayout(0, 10)

            add(JScrollPane(poolText), BorderLayout.CENTER)
        }

        val chainPanel = JPanel().apply {
            border = BorderFactory.createTitledBorder("Chain")
            layout = BorderLayout(0, 10)

            add(JScrollPane(blockList), BorderLayout.CENTER)
            add(mineButton, BorderLayout.PAGE_END)
        }

        val blockPanel = JPanel().apply {
            border = BorderFactory.createTitledBorder("Selected block")
            layout = BorderLayout(0, 10)

            add(
                    JScrollPane(
                            blockText,
                            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                    ), BorderLayout.PAGE_START
            )
            add(JScrollPane(transactionList), BorderLayout.CENTER)
        }

        val transactionPanel = JPanel().apply {
            border = BorderFactory.createTitledBorder("Selected transaction")
            layout = BorderLayout(0, 10)

            add(
                    JScrollPane(
                            transactionText,
                            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                    ), BorderLayout.PAGE_START
            )
            add(transactionViewer, BorderLayout.CENTER)
            add(applyButton, BorderLayout.PAGE_END)
        }

        add(JPanel().apply {
            layout = BorderLayout()

            add(JTabbedPane().apply {
                addTab("Settings", JPanel().apply {
                    layout = BorderLayout()

                    add(JScrollPane(settingsInput), BorderLayout.CENTER)
                    add(runButton, BorderLayout.PAGE_END)
                })

                addTab(
                        "Chain", JSplitPane(
                        JSplitPane.HORIZONTAL_SPLIT,
                        JSplitPane(
                                JSplitPane.VERTICAL_SPLIT,
                                JSplitPane(
                                        JSplitPane.VERTICAL_SPLIT,
                                        poolPanel,
                                        chainPanel
                                ),
                                blockPanel
                        ),
                        transactionPanel
                )
                )

                addTab("Log", JPanel().apply {
                    layout = GridLayout(1, 1)

                    add(JScrollPane(logText))
                })

                preferredSize = Dimension(800, 600)
            }, BorderLayout.CENTER)
        })
    }

    private fun hookException() {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            e.printStackTrace()

            appendLogText(StringWriter().apply {
                e.printStackTrace(PrintWriter(this))
                append("\n")
            }.toString())
        }
    }

    private fun onSelectBlockList(value: String) {
        selectedBlock = chain.findBlock(value.substring("Block ".length))!!
        setTransactionList(selectedBlock.transactions)
        setBlockText(selectedBlock)
    }

    private fun onSelectTransactionList(value: String) {
        selectedTransaction = selectedBlock.findTransaction(value.substring("Transaction ".length))!!
        setTransactionText(selectedTransaction)
        setTransactionViewer(selectedTransaction)
    }

    private fun onClickRunButton() {
        try {
            settings
            client
            server

            settingsInput.isEnabled = false
            runButton.isEnabled = false
        } catch (e: Exception) {
            throw e
        }
    }

    private fun onClickMineButton() {
        chain.mineBlock()
    }

    private fun onClickApplyButton() {
        client.broadcastModelToTools(selectedTransaction.getModel(chain))
    }

    private fun getSettingsInput() = settingsInput.text

    private fun setPoolText(transactions: List<Transaction>) {
        poolText.text = transactions.joinToString("\n") { "Transaction ${it.hash}" }
        repaint()
    }

    private fun setBlockList(blocks: List<Block>) {
        blockListModel.clear()
        blocks.asReversed().forEach { blockListModel.addElement("Block ${it.hash}") }
    }

    private fun setTransactionList(transactions: List<Transaction>) {
        transactionListModel.clear()
        transactions.asReversed().forEach { transactionListModel.addElement("Transaction ${it.hash}") }
    }

    private fun setBlockText(block: Block) {
        blockText.text = """
            |Block ${block.hash}
            |- Timestamp: ${block.timestamp} (${Util.date(block.timestamp)})
            |- Previous block: ${block.previousHash}
            |- Nonce: ${block.nonce}""".trimMargin()

        repaint()
    }

    private fun setTransactionText(transaction: Transaction) {
        val addedCount = transaction.addedModel.faces.size
        val removedCount = transaction.removedModel.faces.size

        transactionText.text = """
            |Transaction ${transaction.hash}
            |- Author: ${transaction.author}
            |- Timestamp: ${transaction.timestamp} (${Util.date(transaction.timestamp)})
            |- Previous transaction: ${transaction.previousHash}
            |- Difference: Added $addedCount faces / Removed $removedCount faces""".trimMargin()

        repaint()
    }

    private fun setTransactionViewer(transaction: Transaction) {
        transactionViewer.setModel(transaction.getModel(chain))
    }

    private fun appendLogText(message: String) {
        logText.append(message)
    }
}
