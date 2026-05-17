package com.example.muse.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    // ── Primary tags ──

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPrimaryTag(tag: PrimaryTagEntity): Long

    @Query("SELECT * FROM primary_tags ORDER BY name")
    fun getAllPrimaryTags(): Flow<List<PrimaryTagEntity>>

    @Query("SELECT * FROM primary_tags ORDER BY name")
    suspend fun getAllPrimaryTagsOnce(): List<PrimaryTagEntity>

    @Query("SELECT * FROM primary_tags WHERE id = :id")
    suspend fun getPrimaryTagById(id: Long): PrimaryTagEntity?

    @Query("DELETE FROM primary_tags WHERE id = :id")
    suspend fun deletePrimaryTag(id: Long)

    // ── Secondary tags ──

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSecondaryTag(tag: SecondaryTagEntity): Long

    @Query("SELECT * FROM secondary_tags ORDER BY name")
    fun getAllSecondaryTags(): Flow<List<SecondaryTagEntity>>

    @Query("SELECT * FROM secondary_tags ORDER BY name")
    suspend fun getAllSecondaryTagsOnce(): List<SecondaryTagEntity>

    @Query("SELECT * FROM secondary_tags WHERE id = :id")
    suspend fun getSecondaryTagById(id: Long): SecondaryTagEntity?

    @Query("DELETE FROM secondary_tags WHERE id = :id")
    suspend fun deleteSecondaryTag(id: Long)

    // ── Document ↔ SecondaryTag relations ──

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDocumentSecondaryTag(crossRef: DocumentSecondaryTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDocumentSecondaryTags(crossRefs: List<DocumentSecondaryTagCrossRef>)

    @Query("DELETE FROM document_secondary_tag WHERE documentId = :documentId")
    suspend fun deleteSecondaryTagsForDocument(documentId: Long)

    @Query("""
        SELECT st.* FROM secondary_tags st
        INNER JOIN document_secondary_tag dst ON st.id = dst.secondaryTagId
        WHERE dst.documentId = :documentId
        ORDER BY st.name
    """)
    suspend fun getSecondaryTagsForDocument(documentId: Long): List<SecondaryTagEntity>
}
