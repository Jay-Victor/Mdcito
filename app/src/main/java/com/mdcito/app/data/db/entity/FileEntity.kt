package com.mdcito.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "files",
    foreignKeys = [
        ForeignKey(
            entity = FileEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["parent_id"]),
        Index(value = ["name"]),
        Index(value = ["updated_at"]),
        Index(value = ["is_pinned"]),
    ]
)
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val path: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "size") val size: Long = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "parent_id") val parentId: Long? = null,
    @ColumnInfo(name = "is_pinned") val isPinned: Boolean = false,
    val tags: String = "[]",
    val metadata: String = "{}",
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    val content: String = "",
    @ColumnInfo(name = "is_imported") val isImported: Boolean = false,
)
