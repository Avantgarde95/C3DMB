@file:Suppress("ObsoleteExperimentalCoroutines", "DEPRECATION", "CanBeParameter")

package app.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.url
import kotlinx.coroutines.experimental.launch

class Client(
        private val peers: List<Node>,
        private val tools: List<Node>,
        private val onLog: (message: String) -> Unit
) {
    private val instance = HttpClient(CIO)

    init {
        onLog("Client: Running...\n")
    }

    fun broadcastTransactionToPeers(transaction: Transaction) {
        onLog("Client: Broadcasting the transaction to the peers...\n")

        peers.forEach { node ->
            launch {
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
            launch {
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
            launch {
                instance.post<Unit> {
                    url("http://${node.host}:${node.port}/block")
                    body = Util.toJson(block)
                }
            }
        }
    }
}
