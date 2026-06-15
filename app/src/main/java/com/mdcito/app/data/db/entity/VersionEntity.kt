package com.mdcito.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "versions",
    foreignKeys = [
        ForeignKey(
            entity = FileEntity::class,
            parentColumns = ["id"],
            childColumns = ["file_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["file_id", "created_at"]),
        Index(value = ["created_at"]),
    ]
)
data class VersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "file_id") val fileId: Long,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    val size: Long = 0,
    val summary: String = "",
    val content: String = "",
)
