package persistence.record

/**
 * Used for persisting a kink score. This does not include [model.Kink] data, but a reference to it.
 */
@kotlinx.serialization.Serializable
data class KinkScoreRecord(
    val kinkId: Long,
    val score: Int,
)