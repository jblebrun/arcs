package arcs.core.common

/** Base interface for types that can be stored in a [Handle] (see [Entity] and [Reference]). */
interface Storable {
    /** Creation timestamp (in millis) on the Referencable object. */
    val creationTimestamp: Long
        get() = TODO("not implemented")

    /** Expiration timestamp (in millis) on the Referencable object. */
    val expirationTimestamp: Long
        get() = TODO("not implemented")
}
