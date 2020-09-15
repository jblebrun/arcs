package arcs.core.host

import arcs.core.storage.StorageKey


/**
 * Describe the strategy for registering and unregistering for resurrection.
 */
interface ResurrectionManager {
    fun requestResurrection(targetId: String, keys: List<StorageKey>)
    fun cancelResurrectionRequest(targetId: String)
}
