package com.github.avantgarde95.c3dmb.core

import com.beust.klaxon.Json

class Transaction(
        val author: String,
        val timestamp: Long,
        val previousHash: String,
        val addedModel: Model,
        val removedModel: Model
) {
    @Json(ignored = true)
    val hash = Util.hash("$author$timestamp${addedModel.hash}${removedModel.hash}")

    fun getModel(chain: Chain): Model {
        val transactions = mutableListOf<Transaction>()
        var currentTransaction = this

        while (true) {
            transactions.add(0, currentTransaction)

            if (currentTransaction.previousHash.isEmpty()) {
                break
            }

            currentTransaction = chain.findTransaction(currentTransaction.previousHash)!!
        }

        val faceSet = mutableSetOf<List<Vertex>>()

        for (transaction in transactions) {
            faceSet += transaction.addedModel.faces
            faceSet -= transaction.removedModel.faces
        }

        return Model(faces = faceSet.toList())
    }

    override fun toString() = """
        |Transaction $hash
        |-- Author: $author
        |-- Time: ${Util.date(timestamp)}
        |-- Previous transaction: $previousHash
        |$addedModel
        |$removedModel""".trimMargin()
}
