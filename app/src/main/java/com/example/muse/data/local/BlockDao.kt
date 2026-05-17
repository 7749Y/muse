package com.example.muse.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockDao {

    @Query("SELECT * FROM blocks WHERE documentId = :docId ORDER BY sequence")
    fun getBlocksForDocument(docId: Long): Flow<List<BlockEntity>>

    @Query("SELECT * FROM blocks WHERE documentId = :docId ORDER BY sequence")
    suspend fun getBlocksForDocumentOnce(docId: Long): List<BlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocks(blocks: List<BlockEntity>)

    @Query("DELETE FROM blocks WHERE documentId = :docId")
    suspend fun deleteBlocksForDocument(docId: Long)
}
