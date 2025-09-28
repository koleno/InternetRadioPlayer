package xyz.koleno.internetradioplayer.data

import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class StationRepository @Inject constructor(
    private val stationDao: StationDao
) {

    fun getAll(): Flow<List<Station>> = stationDao.getAll()

    fun getLatestPlayed(): Station = stationDao.getLatestPlayed()

    fun insert(station: Station) = stationDao.insert(station)

    fun getCount(): Int = stationDao.getCount()

    fun insertAll(stations: List<Station>) = stationDao.insertAll(stations)
    fun update(stations: List<Station>) = stationDao.update(stations)

    fun update(station: Station) = stationDao.update(station)

    fun delete(uid: Int) = stationDao.delete(uid)

}