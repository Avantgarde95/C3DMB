package app.core

class Settings(
        val name: String,
        val me: Node,
        val peers: List<Node>,
        val tools: List<Node>
)
