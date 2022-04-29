package persistence.record

import model.TestResult
import persistence.KinkRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Used for persisting a test result.
 */
@kotlinx.serialization.Serializable
class TestResultRecord(
    val resultId: String,
    val date: String,
    val scores: List<KinkScoreRecord>,
) {
    companion object {
        /**
         * Returns a record built from a [TestResult].
         */
        fun from(testResult: TestResult) = TestResultRecord(
            resultId = testResult.resultId,
            date = testResult.date.format(DateTimeFormatter.ISO_DATE),
            scores = testResult.kinkMap.map { (k, v) -> KinkScoreRecord(k.id, v) },
        )

        /**
         * Returns a [TestResult] built from a [TestResultRecord].
         */
        fun parse(record: TestResultRecord) = TestResult(
            resultId = record.resultId,
            date = LocalDate.parse(record.date, DateTimeFormatter.ISO_DATE),
            kinkMap = buildMap { record.scores.forEach { this[KinkRepository.getKinkById(it.kinkId)] = it.score } }
        )
    }
}