package com.example.muse.data.local

import com.example.muse.ui.screen.edit.SavedModule

data class DocumentWithModules(
    val document: DocumentEntity,
    val modules: List<SavedModule>,
    val secondaryTags: List<SecondaryTagEntity> = emptyList(),
)

class DocumentRepository(private val db: MuseDatabase) {

    private val documentDao = db.documentDao()
    private val blockDao = db.blockDao()
    private val tagDao = db.tagDao()

    suspend fun saveDocument(
        name: String,
        modules: List<SavedModule>,
        documentId: Long? = null,
        primaryTagId: Long? = null,
    ): Long {
        val now = System.currentTimeMillis()
        val docId: Long

        if (documentId != null && documentId > 0) {
            docId = documentId
            documentDao.insert(
                DocumentEntity(
                    id = docId,
                    name = name,
                    primaryTagId = primaryTagId,
                    updatedAt = now,
                )
            )
        } else {
            docId = documentDao.insert(
                DocumentEntity(
                    name = name,
                    primaryTagId = primaryTagId,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }

        // 替换 blocks
        blockDao.deleteBlocksForDocument(docId)
        val blocks = ModuleConverter.toBlocks(modules, docId)
        if (blocks.isNotEmpty()) {
            blockDao.insertBlocks(blocks)
        }

        return docId
    }

    suspend fun saveDocumentSecondaryTags(documentId: Long, secondaryTagIds: List<Long>) {
        tagDao.deleteSecondaryTagsForDocument(documentId)
        if (secondaryTagIds.isNotEmpty()) {
            tagDao.insertDocumentSecondaryTags(
                secondaryTagIds.map { DocumentSecondaryTagCrossRef(documentId, it) }
            )
        }
    }

    suspend fun loadDocument(id: Long): DocumentWithModules? {
        val doc = documentDao.getDocumentByIdOnce(id) ?: return null
        val blocks = blockDao.getBlocksForDocumentOnce(id)
        val modules = ModuleConverter.toModules(blocks)
        val secondaryTags = tagDao.getSecondaryTagsForDocument(id)
        return DocumentWithModules(
            document = doc,
            modules = modules,
            secondaryTags = secondaryTags,
        )
    }

    suspend fun deleteDocument(id: Long) {
        documentDao.deleteById(id)
    }

    suspend fun getOrCreateDefaultDocument(): DocumentWithModules {
        var doc = documentDao.getDocumentByIdOnce(1)
        if (doc == null) {
            val id = documentDao.insert(
                DocumentEntity(name = "default", starred = false)
            )
            doc = documentDao.getDocumentByIdOnce(id)!!
        }
        val blocks = blockDao.getBlocksForDocumentOnce(doc.id)
        val modules = ModuleConverter.toModules(blocks)
        val secondaryTags = tagDao.getSecondaryTagsForDocument(doc.id)
        return DocumentWithModules(
            document = doc,
            modules = modules,
            secondaryTags = secondaryTags,
        )
    }
}
