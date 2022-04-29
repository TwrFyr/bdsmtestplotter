package persistence

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import model.Kink

/**
 * Used for loading and managing all kinks. Meant to reduce the number of [Kink] instances.
 */
object KinkRepository {
    private val kinks: List<Kink> = loadKinksFromResource()

    fun getKinkById(id: Long): Kink = kinks.first { it.id == id }

    fun getKinkByName(name: String): Kink = kinks.first { it.name == name }

    private fun loadKinksFromResource(): List<Kink> {
        val json: String = this.javaClass.getResource("/kinks.json")?.readText() ?: ""
        assert(json.isNotBlank())
        return Json.decodeFromString(json)
    }
}