package com.example.muse.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "document_secondary_tag",
    primaryKeys = ["documentId", "secondaryTagId"],
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SecondaryTagEntity::class,
            parentColumns = ["id"],
            childColumns = ["secondaryTagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("secondaryTagId")],
)
data class DocumentSecondaryTagCrossRef(
    val documentId: Long,
    val secondaryTagId: Long,
)
