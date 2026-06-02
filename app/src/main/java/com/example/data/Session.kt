package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("clientId")]
)
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val startTime: Long,
    val endTime: Long? = null,
    val description: String = ""
)
