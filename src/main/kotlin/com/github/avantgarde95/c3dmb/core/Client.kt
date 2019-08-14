@file:Suppress("ObsoleteExperimentalCoroutines", "DEPRECATION", "CanBeParameter", "EXPERIMENTAL_API_USAGE")

package com.github.avantgarde95.c3dmb.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.url
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class Client(
        private val peers: List<Node>,
        private val tools: List<Node>,
        private val onReceiveBlockFromPeer: (block: Block) -> Unit,
        private val onLog: (message: String) -> Unit
) {
    private val instance = HttpClient(CIO)

    init {
        onLog("Client: Running...\n")
    }

    fun broadcastTransactionToPeers(transaction: Transaction) {
        onLog("Client: Broadcasting the transaction to the peers...\n")

        peers.forEach { node ->
            GlobalScope.launch {
                instance.post<Unit> {
                    url("http://${node.host}:${node.port}/transaction")
                    body = Util.toJson(transaction)
                }
            }
        }
    }

    fun broadcastModelToTools(model: Model) {
        onLog("Client: Broadcasting the snapshot to the modeling tools...\n")

        tools.forEach { node ->
            GlobalScope.launch {
                instance.post<Unit> {
                    url("http://${node.host}:${node.port}/model")
                    body = Util.toJson(model)
                }
            }
        }
    }

    fun broadcastBlockToPeers(block: Block) {
        onLog("Client: Broadcasting the block to the peers...\n")

        peers.forEach { node ->
            GlobalScope.launch {
                instance.post<Unit> {
                    url("http://${node.host}:${node.port}/block")
                    body = Util.toJson(block)
                }
            }
        }
    }

    fun requestChainToOneOfPeers() {
        peers.forEachIndexed { index, node ->
            try {
                onLog("Client: Trying to download the chain from ${index}th node...\n")

                GlobalScope.launch {
                    val data = instance.post<String> {
                        url("http://${node.host}:${node.port}/download")
                        body = ""
                    }

                    Util.fromJson<BlocksWrapper>(data).blocks.forEach { block ->
                        onReceiveBlockFromPeer(block)
                    }
                }

                return
            } catch (e: Exception) {
                // Do nothing.
            }
        }
    }
}
