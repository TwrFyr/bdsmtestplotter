/**
 * Class representing a specific kink from `bsdmtest.org`.
 */
@kotlinx.serialization.Serializable
data class Kink(
    val id: Long,
    val name: String,
    val pairDescription: String,
    val description: String,
)