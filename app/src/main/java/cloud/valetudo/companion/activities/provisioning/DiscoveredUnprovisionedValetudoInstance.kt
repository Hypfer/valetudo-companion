package cloud.valetudo.companion.activities.provisioning

data class DiscoveredUnprovisionedValetudoInstance(
    val model: String,
    val manufacturer: String,
    val valetudoVersion: String,
    val host: String,
) {
    override fun toString(): String = "$manufacturer $model ($host)"
}
