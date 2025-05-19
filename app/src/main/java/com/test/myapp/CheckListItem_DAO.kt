package com.test.myapp

import androidx.room.*
import kotlinx.coroutines.flow.Flow


@Dao
interface CheckListItemDao {

    //-----FOR--ACTUAL------------------------------------------------------------------------------

    @Query("SELECT * FROM checklist_items WHERE isArchived = 0 AND tableId = :tableId/* ORDER BY isChecked ASC, id DESC*/")
    fun getActiveItems(tableId: Int): Flow<List<CheckListItemData>>

    @Query("SELECT * FROM checklist_items WHERE isArchived = 0 AND tableId = :tableId ORDER BY isChecked ASC, id DESC")
    fun getActiveItemsSorted(tableId: Int): Flow<List<CheckListItemData>>

    @Query("SELECT text FROM checklist_items WHERE id = :id")
    fun getTextById(id: Int): Flow<String>

    @Query("UPDATE checklist_items SET text = :text WHERE id = :id")
    suspend fun updateText(id: Int, text: String)

    @Query("SELECT * FROM checklist_items WHERE isArchived = 0 AND isChecked = 1 AND tableId = :tableId ORDER BY isChecked ASC, id DESC")
    fun getCheckedItems(tableId: Int): Flow<List<CheckListItemData>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(item: CheckListItemData)

    @Update
    suspend fun updateItem(item: CheckListItemData)

    @Query("UPDATE checklist_items SET isChecked = :isChecked WHERE id = :id")
    suspend fun updateCheckedState(id: Int, isChecked: Boolean)

    @Query("UPDATE checklist_items SET isArchived = 1 WHERE id = :id")
    suspend fun archiveItem(id: Int)

    @Query("UPDATE checklist_items SET isArchived = 0 WHERE id = :id")
    suspend fun unarchiveItem(id: Int)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteItemById(id: Int)

    @Query("DELETE FROM checklist_items WHERE tableId = :tableId")
    suspend fun deleteItemByTableId(tableId: Int)

    @Query("UPDATE checklist_items SET tableId = tableId - 1 WHERE tableId > :tableId")
    suspend fun downgradeTableId(tableId: Int)

    //-----FOR--HISTORY-----------------------------------------------------------------------------

    @Query("SELECT * FROM checklist_items WHERE isArchived = 1 ORDER BY id DESC")
    fun getArchivedItems(): Flow<List<CheckListItemData>>

    @Query("DELETE FROM checklist_items WHERE isArchived = 1")
    suspend fun clearHistory()
}

@Dao
interface CheckListTitleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun createTitle(defaultTitle: CheckListTitleData): Long

    @Query("SELECT title FROM checklist_title WHERE id = :id")
    fun getTitle(id: Int): Flow<String?>

    @Query("UPDATE checklist_title SET title = :newTitle WHERE id = :tableId")
    suspend fun setTitle(newTitle: String, tableId: Int)

    @Query("SELECT COUNT(*) FROM checklist_title")
    fun getSize(): Flow<Int>

    @Query("SELECT * FROM checklist_title")
    fun getAllTitles(): Flow<List<CheckListTitleData>>

    @Query("DELETE FROM checklist_title WHERE id = :tableId")
    suspend fun deleteTitleById(tableId: Int)

    @Query("SELECT id FROM checklist_title WHERE id > :currentId ORDER BY id ASC")
    suspend fun allNextId(currentId: Int): List<Int>

    @Query("SELECT id FROM checklist_title WHERE id < :currentId ORDER BY id DESC")
    suspend fun allPrevId(currentId: Int): List<Int>

    @Query("SELECT id FROM checklist_title WHERE id != 0 ORDER BY id ASC")
    suspend fun allId(): List<Int>

    @Query("SELECT COUNT(*) FROM checklist_title WHERE id < :id")
    suspend fun countTitlePos(id: Int): Int
}

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun createSetting(defaultSetting: SettingsInfo)

    @Query("SELECT state FROM settings_info WHERE name = :name")
    fun getState(name: String): Flow<Boolean>

    @Query("UPDATE settings_info SET state = :newState WHERE name = :name")
    suspend fun setSetting(newState: Boolean, name: String)

    @Query("SELECT COUNT(*) FROM settings_info")
    fun getSize(): Flow<Int>
}