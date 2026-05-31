package com.digitalwallet.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class NotificationType { RECEIVED, SENT, PURCHASED, REDEEMED, SUBSCRIPTION, SYSTEM }

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val type: NotificationType,
    val isRead: Boolean = false,
    val relatedId: Long = 0,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
