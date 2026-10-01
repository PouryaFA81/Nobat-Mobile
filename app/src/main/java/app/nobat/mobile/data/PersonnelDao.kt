package app.nobat.mobile.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonnelDao {
    @Query(
        "SELECT * FROM personnel WHERE accountId = :accountId ORDER BY name COLLATE NOCASE ASC"
    )
    fun observeForAccount(accountId: Long): Flow<List<Personnel>>

    @Query(
        "SELECT * FROM personnel WHERE accountId = :accountId ORDER BY name COLLATE NOCASE ASC"
    )
    suspend fun listForAccount(accountId: Long): List<Personnel>

    @Query("SELECT * FROM personnel WHERE id = :id AND accountId = :accountId LIMIT 1")
    suspend fun get(accountId: Long, id: Long): Personnel?

    @Query("SELECT COUNT(*) FROM personnel WHERE accountId = :accountId")
    suspend fun countForAccount(accountId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(person: Personnel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Personnel>)

    @Update
    suspend fun update(person: Personnel)

    @Query("DELETE FROM personnel WHERE id = :id AND accountId = :accountId")
    suspend fun delete(accountId: Long, id: Long)

    @Query("DELETE FROM personnel WHERE accountId = :accountId")
    suspend fun deleteAllForAccount(accountId: Long)
}
