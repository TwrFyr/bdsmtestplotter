package kink

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.Kink
import java.io.File

private fun main() {
    sortKinksJsonById()
}

/**
 * Sorts manually created json of kinks.
 */
private fun sortKinksJsonById() {
    val jsonString = object {}.javaClass.getResource("/kinks_unsorted.json")?.readText() ?: ""
    val kinks = Json.decodeFromString<List<Kink>>(jsonString)
    val sortedString = Json.encodeToString(kinks.sortedBy { it.id })
    File("kinks.json").writeText(sortedString)
}
