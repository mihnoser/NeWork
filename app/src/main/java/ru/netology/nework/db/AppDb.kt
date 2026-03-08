package ru.netology.nework.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.netology.nework.dao.*


@Database(entities = [PostEntity::class], version = 1)
abstract class AppDbRoom : RoomDatabase() {
    abstract fun postDaoRoom(): PostDao

}