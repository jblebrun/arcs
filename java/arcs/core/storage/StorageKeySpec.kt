package arcs.core.storage

/**
 * This describes the metadata related to a [StorageKey] type. The [companion] object of a
 * [StorageKey] implementation should implement this interface.
 */
interface StorageKeySpec<T : StorageKey> : StorageKeyParser<T>
