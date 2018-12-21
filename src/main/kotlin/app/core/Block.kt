package app.core

import com.beust.klaxon.Json

class Block(
        val timestamp: Long,
        val previousHash: String,
        val transactions: List<Transaction>,
        val nonce: Int
) {
    @Json(ignored = true)
    val hash = Util.hash("$timestamp$previousHash${transactions.joinToString { it.hash }}$nonce")

    companion object {
        fun genesisBlock() = Block(
                timestamp = Util.timestamp(),
                transactions = listOf(
                        Transaction(
                                author = "",
                                timestamp = 0,
                                previousHash = "",
                                addedModel = Model(emptyList()),
                                removedModel = Model(emptyList())
                        )
                ),
                nonce = 0,
                previousHash = ""
        )
    }

    fun findTransaction(hash: String): Transaction? {
        for (transaction in transactions) {
            if (transaction.hash == hash) {
                return transaction
            }
        }

        return null
    }

    fun mineNextBlock(transactions: List<Transaction>): Block {
        var nextNonce = 0

        while (!validateNonce(nextNonce)) {
            nextNonce++
        }

        return Block(
                timestamp = Util.timestamp(),
                previousHash = hash,
                transactions = transactions,
                nonce = nextNonce
        )
    }

    private fun validateNonce(nextNonce: Int) =
            Util.hash("$nonce$nextNonce$hash").substring(0, 2) == "00"

    override fun toString() = """
        |Block $hash
        |-- Timestamp: $timestamp
        |-- Previous block: $previousHash
        |-- Nonce: $nonce
        |-- Transactions:
        |${transactions.joinToString("\n\n")}""".trimMargin()
}
