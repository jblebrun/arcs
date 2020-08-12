package arcs.core.host

interface HandleManagerProvider {
    fun create(arcId: String, hostId: String): HandleManager
    fun close()
}
