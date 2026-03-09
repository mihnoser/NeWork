package ru.netology.nework.db

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.netology.nework.dao.*
import ru.netology.nework.entity.EventsEntity
import ru.netology.nework.entity.UserEntity


@Database(entities = [PostEntity::class, UserEntity::class, EventsEntity::class, JobEntity::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun postDaoRoom(): DaoRoom

}