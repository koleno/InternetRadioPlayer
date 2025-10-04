package xyz.koleno.internetradioplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity()
data class Station(
    @PrimaryKey(autoGenerate = true) val uid: Int,
    val name: String,
    val uri: String,
    val text: String = "",
    val position: Int = 0,
    val lastPlayed: Long = 0
)