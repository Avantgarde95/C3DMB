@file:Suppress("ObsoleteExperimentalCoroutines", "DEPRECATION", "CanBeParameter")

package com.github.avantgarde95.c3dmb.core

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

class Server(
        private val me: Node,
        private val chain: Chain,
        private val onReceiveBlockFromPeer: (block: Block) -> Unit,
        private val onReceiveModelFromTool: (model: Model) -> Unit,
        private val onReceiveTransactionFromPeer: (transaction: Transaction) -> Unit,
        private val onLog: (message: String) -> Unit
) {
    private val instance = embeddedServer(Netty, host = me.host, port = me.port) {
        routing {
            post("/block") {
                onLog("Server: Received a block from one of the peers!\n")
                call.respond(HttpStatusCode.OK, "")
                onReceiveBlockFromPeer(Util.fromJson(call.receiveText()))
            }

            post("/model") {
                onLog("Server: Received a model from one of the modeling tools!\n")
                call.respond(HttpStatusCode.OK, "")
                onReceiveModelFromTool(Util.fromJson(call.receiveText()))
            }

            post("/transaction") {
                onLog("Server: Received a transaction from one of the peers!\n")
                call.respond(HttpStatusCode.OK, "")
                onReceiveTransactionFromPeer(Util.fromJson(call.receiveText()))
            }

            post("/download") {
                onLog("Server: Got a download request from one of the peers!\n")
                call.respondText(Util.toJson(BlocksWrapper(chain.blocks)), status = HttpStatusCode.OK)
            }
        }
    }

    init {
        instance.start(wait = false)
        onLog("Server: Running...\n")
    }
}
