/**
 * Class containing all data necessary for user authentication.
 */
@kotlinx.serialization.Serializable
data class UserAuth(
    val uid: String,
    val salt: String,
    val authsig: String,
)