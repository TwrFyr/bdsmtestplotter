/**
 * Used for loading and managing all kinks. Meant to reduce the number of [Kink] instances.
 */
object KinkRepository {
    private val kinkMap: MutableMap<String, Kink> = mutableMapOf()

    init {
        loadKinks().forEach { kink ->
            kinkMap[kink.name] = kink
        }
    }

    fun getKinkByName(name: String): Kink {
        return kinkMap[name] ?: throw IllegalArgumentException("No kink with name '$name'.")
    }
}