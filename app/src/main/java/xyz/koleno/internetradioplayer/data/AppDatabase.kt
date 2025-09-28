package xyz.koleno.internetradioplayer.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@androidx.room.Database(entities = [Station::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao

    companion object {
        private val stations = listOf(
            Station(
                uid = 0,
                name = "SRO 1",
                uri = "https://icecast.stv.livebox.sk/slovensko_128.mp3",
                position = 0
            ),
            Station(
                uid = 0,
                name = "The MIX",
                uri = "https://playerservices.streamtheworld.com/api/livestream-redirect/WTMXFM.mp3?dist=hubbard&source=hubbard-web&ttag=web&gdpr=0",
                position = 1
            ),
            Station(
                uid = 0,
                name = "KISS FM",
                uri = "https://ample.revma.ihrhls.com/zc849/37_aptz1k8i4i7p02/playlist.m3u8",
                position = 2
            ),
            Station(
                uid = 0,
                name = "Funradio",
                uri = "https://stream.funradio.sk:18443/fun128.mp3",
                position = 3
            ),
            Station(
                uid = 0,
                name = "The Drive",
                uri = "https://playerservices.streamtheworld.com/api/livestream-redirect/WDRVFM.mp3?dist=hubbard&source=hubbard-web&ttag=web&gdpr=0",
                position = 4
            ),
            Station(
                uid = 0,
                name = "Oldies 93",
                uri = "https://ice23.securenetsystems.net/WNBY?playSessionID=066445A9-D7C7-7A9E-821E532B78D3C55C",
                position = 5
            ),
            Station(
                uid = 0,
                name = "Radio Expres",
                uri = "https://stream.bauermedia.sk/96.mp3",
                position = 6
            ),
            Station(
                uid = 0,
                name = "WBEZ",
                uri = "https://stream.wbez.org/wbez64-web.aac",
                position = 7
            ),
            Station(
                uid = 0,
                name = "B96",
                uri = "https://prod-3-86-140-94.amperwave.net/audacy-wbbmfmaac-hlsc.m3u8",
                position = 8
            )
        )

        // For Singleton instantiation
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        // Create and pre-populate the database. See this article for more details:
        // https://medium.com/google-developers/7-pro-tips-for-room-fbadea4bfbd1#4785
        @OptIn(DelicateCoroutinesApi::class)
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "StationDatabase")
                .addCallback(
                    object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)

                            GlobalScope.launch(Dispatchers.IO) {
                                getInstance(context).stationDao().insertAll(stations)
                            }
                        }
                    }
                )
                .build()
        }
    }
}