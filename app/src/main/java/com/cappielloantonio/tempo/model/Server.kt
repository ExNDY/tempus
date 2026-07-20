package com.cappielloantonio.tempo.model

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Keep
@Entity(tableName = "server")
data class Server(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val serverId: String,

    @ColumnInfo(name = "server_name")
    val serverName: String,

    @ColumnInfo(name = "username")
    val username: String,

    @ColumnInfo(name = "password")
    val password: String,

    @ColumnInfo(name = "address")
    val address: String,

    @ColumnInfo(name = "local_address")
    val localAddress: String?,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "low_security", defaultValue = "false")
    val isLowSecurity: Boolean,

    @ColumnInfo(name = "client_cert")
    val clientCert: String?,
) : Serializable