package app.core

import com.beust.klaxon.Klaxon
import java.security.MessageDigest
import java.sql.Timestamp
import java.util.*

object Util {
    fun hash(data: String) = MessageDigest
            .getInstance("SHA-256")
            .digest(data.toByteArray())
            .fold("") { result, value -> result + "%02x".format(value) }

    fun timestamp() = System.currentTimeMillis()

    fun date(timestamp: Long) = Date(Timestamp(timestamp).time)

    fun toJson(value: Any) = Klaxon().toJsonString(value)

    inline fun <reified T> fromJson(json: String) = Klaxon().parse<T>(json)!!

    fun getResourceAsStream(path: String) = Thread.currentThread().contextClassLoader.getResourceAsStream(path)!!

    fun getResourceAsString(path: String) = getResourceAsStream(path).bufferedReader().use { it.readText() }
}
