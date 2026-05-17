package com.example.muse.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocks",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("documentId"), Index("documentId", "sequence", unique = true)],
)
data class BlockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val type: String,
    val content: String,
    val sequence: Int,
    val groupId: String? = null,
    val headingLevel: Int? = null,
    val metadataJson: String? = null,
)
