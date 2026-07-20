package com.cappielloantonio.tempo.model

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Keep
@Entity(tableName = "recent_search")
data class RecentSearch(
    @PrimaryKey
    @ColumnInfo(name = "search")
    var search: String,

    @ColumnInfo(name = "timestamp", defaultValue = "0")
    var timestamp: Long
) : Serializable
