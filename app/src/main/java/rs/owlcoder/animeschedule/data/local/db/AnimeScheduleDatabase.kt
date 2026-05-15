package rs.owlcoder.animeschedule.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        AiringEpisodeEntity::class,
        AnimeDetailEntity::class,
        MalListEntryEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AnimeScheduleDatabase : RoomDatabase() {
    abstract fun airingEpisodeDao(): AiringEpisodeDao
    abstract fun animeDetailDao(): AnimeDetailDao
    abstract fun malListEntryDao(): MalListEntryDao
}
