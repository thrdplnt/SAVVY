package com.example.savvy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: LocalUser)

    @Query("SELECT * FROM local_user_profile WHERE uid = :uid")
    fun getUser(uid: String): Flow<LocalUser?>

    @Query("SELECT * FROM local_user_profile WHERE isSynced = 0 LIMIT 1")
    suspend fun getUnsyncedUser(): LocalUser?

    @Query("DELETE FROM local_user_profile WHERE uid = :uid")
    suspend fun deleteUser(uid: String)
}
