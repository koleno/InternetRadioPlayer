package xyz.koleno.internetradioplayer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {

    @Query("SELECT * FROM station ORDER BY position")
    fun getAll(): Flow<List<Station>>

    @Query("SELECT * FROM station ORDER BY lastPlayed DESC LIMIT 1")
    fun getLatestPlayed(): Station

    @Insert
    fun insert(station: Station)

    @Query("SELECT count(*) FROM station")
    fun getCount(): Int

    @Insert
    fun insertAll(stations: List<Station>)

    @Update
    fun update(stations: List<Station>)

    @Update
    fun update(station: Station)

    @Query("DELETE FROM station WHERE uid = :uid")
    fun delete(uid: Int)

    @Query("DELETE FROM station")
    fun deleteAll()
}