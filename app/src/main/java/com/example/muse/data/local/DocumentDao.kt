package com.example.muse.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentEntity): Long

    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    fun getDocumentById(id: Long): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentByIdOnce(id: Long): DocumentEntity?

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE documents SET primary_tag_id = :primaryTagId WHERE id = :documentId")
    suspend fun updatePrimaryTag(documentId: Long, primaryTagId: Long?)
}
