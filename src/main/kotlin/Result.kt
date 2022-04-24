import kink.Kink
import java.time.LocalDate

/**
 * Class representing a dated test result from `bsdmtest.org`.
 */
data class Result(
    val id: String,
    val date: LocalDate,
    val kinkMap: Map<Kink, Int>,
)
