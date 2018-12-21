package app.core

class Chain(
        private val onMine: (block: Block) -> Unit,
        private val onUpdateBlocks: (blocks: List<Block>) -> Unit,
        private val onUpdatePool: (transactions: List<Transaction>) -> Unit,
        private val onLog: (message: String) -> Unit
) {
    private val blocks = mutableListOf(Block.genesisBlock())
    private val pool = mutableListOf<Transaction>()

    init {
        onUpdateBlocks(blocks)
    }

    fun getLastTransaction(): Transaction {
        for (block in blocks.asReversed()) {
            if (block.transactions.isNotEmpty()) {
                return block.transactions.last()
            }
        }

        return blocks.first().transactions.first()
    }

    fun findBlock(hash: String): Block? {
        for (block in blocks) {
            if (block.hash == hash) {
                return block
            }
        }

        return null
    }

    fun findTransaction(hash: String): Transaction? {
        for (block in blocks) {
            for (transaction in block.transactions) {
                if (transaction.hash == hash) {
                    return transaction
                }
            }
        }

        return null
    }

    fun mineBlock() {
        val block = blocks.last().mineNextBlock(pool.filter { checkDoubleSpending(it) })
        addBlock(block)

        pool.clear()
        onUpdatePool(pool)

        onLog("Chain: Mined a block! (Hash: ${block.hash})\n")
        onMine(block)
    }

    fun addBlock(block: Block) {
        blocks.add(block)
        onUpdateBlocks(blocks)
    }

    fun addTransaction(transaction: Transaction) {
        pool.add(transaction)
        onUpdatePool(pool)
    }

    private fun checkDoubleSpending(transaction: Transaction): Boolean {
        for (block in blocks) {
            for (existingTransaction in block.transactions) {
                if (transaction.hash == existingTransaction.hash) {
                    onLog("Chain: Transaction ${transaction.hash} already exists!\n")
                    return false
                }
            }
        }

        return true
    }

    override fun toString() = blocks.joinToString("\n\n")
}
