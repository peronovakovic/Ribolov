package ba.ribolov.android.core.data

import java.util.UUID

/** No local store is activated until encrypted persistence and account isolation are implemented. */
data class PendingInspection(val id: UUID, val payload: String)

interface InspectionOutbox {
    suspend fun enqueue(inspection: PendingInspection)
    suspend fun pending(): List<PendingInspection>
    suspend fun acknowledge(id: UUID)
}

interface FieldSync {
    /** A successful upload does not refresh the validity of the downloaded permit package. */
    suspend fun uploadPending()
    /** Activate a new package only after validating every required part. */
    suspend fun refreshPackage()
}