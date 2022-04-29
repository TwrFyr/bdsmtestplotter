package persistence

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.TestResult
import persistence.record.TestResultRecord
import java.io.File

/**
 * Used for persisting and managing results, also eradicates duplicates.
 */
object ResultRepository {

    private const val DEFAULT_FILENAME = "test_results.json"

    private val results = mutableListOf<TestResult>()

    init {
        results.addAll(loadResultsFromFile())
        results.sortBy { it.resultId }
    }

    fun getResult(id: String): TestResult {
        return _getResult(id) ?: throw NoSuchElementException("result with id \"$id\" not found")
    }

    private fun _getResult(id: String): TestResult? {
        var lowerBound = 0
        var upperBound = results.size

        while (upperBound - lowerBound > 10) {
            val index = (upperBound - lowerBound) / 2
            val probe = results[index]
            if (probe.resultId == id) {
                return probe
            }
            if (probe.resultId > id) {
                lowerBound = index
            } else {
                upperBound = index
            }
        }
        for (i in lowerBound until upperBound) {
            val probe = results[i]
            if (probe.resultId == id) {
                return probe
            }
        }
        return null
    }

    fun getAllResults(): List<TestResult> = results

    /**
     * Adds the given result into the sorted results if it is not already in the list.
     */
    fun add(result: TestResult) {
        if (_getResult(result.resultId) != null) {
            return
        }

        var lowerBound = 0
        var upperBound = results.size

        while (upperBound != lowerBound) {
            val index = (upperBound - lowerBound) / 2
            val probe = results[index]
            if (probe.resultId > result.resultId) {
                lowerBound = index
            } else {
                upperBound = index
            }
        }
        results.add(lowerBound, result)
    }

    fun addAll(results: Collection<TestResult>) {
        results.forEach { add(it) }
    }

    private fun loadResultsFromFile(filename: String = DEFAULT_FILENAME): List<TestResult> {
        val file = File(filename)
        if (!file.exists()) {
            file.createNewFile()
        }

        val content = file.readText(Charsets.UTF_8)
        if (content.isEmpty()) {
            return emptyList()
        }
        val testResultRecords = Json.decodeFromString<List<TestResultRecord>>(content)
        return testResultRecords.map { TestResultRecord.parse(it) }
    }

    private fun writeResultsToFile(filename: String = DEFAULT_FILENAME, results: List<TestResult>) {
        val testResultRecords = results.map { TestResultRecord.from(it) }
        val content = Json.encodeToString(testResultRecords)
        File(filename).writeText(content)
    }
}