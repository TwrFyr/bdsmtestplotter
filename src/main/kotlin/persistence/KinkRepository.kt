package persistence

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import model.Kink

/**
 * Used for loading and managing all kinks. Meant to reduce the number of [Kink] instances.
 */
object KinkRepository {
    private val kinkMap: MutableMap<String, Kink> = mutableMapOf()

    init {
        loadKinksFromResource().forEach { kink ->
            kinkMap[kink.name] = kink
        }
    }

    fun getKinkByName(name: String): Kink {
        return kinkMap[name] ?: throw IllegalArgumentException("No kink with name '$name'.")
    }

    private fun loadKinksFromResource(): List<Kink> {
        val json: String = this.javaClass.getResource("/kinks.json")?.readText() ?: ""
        assert(json.isNotBlank())
        return Json.decodeFromString(json)
    }
}