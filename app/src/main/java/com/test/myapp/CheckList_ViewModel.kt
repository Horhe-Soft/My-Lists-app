package com.test.myapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


@OptIn(ExperimentalCoroutinesApi::class)
class CheckListViewModel(application: Application): AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)

    private val itemDao = database.checkListItemDao()
    private val titleDao = database.checkListTitleDao()
    private val settingsDao = database.settingsDao()

    //----------------------------------------------------------------------------------------------
    private val _currentTableId = MutableStateFlow<Int>(0)

    fun setTableId(tableId: Int) {
        _currentTableId.value = tableId
    }
    //----------------------------------------------------------------------------------------------

    val activeChecklistItems: StateFlow<List<CheckListItemData>> = _currentTableId
        .flatMapLatest { id ->
            itemDao.getActiveItems(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeChecklistItemsSorted: StateFlow<List<CheckListItemData>> = _currentTableId
        .flatMapLatest { id ->
            itemDao.getActiveItemsSorted(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val checkedChecklistItems: StateFlow<List<CheckListItemData>> = _currentTableId
        .flatMapLatest { id ->
            itemDao.getCheckedItems(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedChecklistItems: StateFlow<List<CheckListItemData>> = itemDao.getArchivedItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addItem(text: String) {
        if (text.isNotBlank()) {
            viewModelScope.launch {
                val newItem = CheckListItemData(text = text,
                    isChecked = false,
                    isArchived = false,
                    tableId = _currentTableId.value)
                itemDao.insertItem(newItem)
            }
        }
    }

    fun updateText(itemId: Int, text: String) {
        viewModelScope.launch {
            itemDao.updateText(itemId, text)
        }
    }

    fun updateChecked(itemId: Int, isChecked: Boolean) {
        viewModelScope.launch {
            itemDao.updateCheckedState(itemId, isChecked)
        }
    }

    fun archiveCheckedItems() {
        viewModelScope.launch {
            activeChecklistItems.value
                .filter { it.isChecked }
                .forEach { itemDao.archiveItem(it.id) }
        }
    }

    fun archiveItem(itemId: Int) {
        viewModelScope.launch {
            itemDao.archiveItem(itemId)
        }
    }

    fun unarchiveItems() {
        viewModelScope.launch {
            archivedChecklistItems.value
                .filter { it.isChecked }
                .forEach { itemDao.unarchiveItem(it.id) }
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            archivedChecklistItems.value
                .filter { it.isChecked }
                .forEach { itemDao.deleteItemById(it.id) }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            itemDao.clearHistory()
        }
    }

    //----------------------------------------------------------------------------------------------

    fun deleteList() {
        viewModelScope.launch {
            titleDao.deleteTitleById(_currentTableId.value)
        }
    }

    //----FOR-TITLE---------------------------------------------------------------------------------

    val title: StateFlow<String?> = _currentTableId.flatMapLatest { tableId ->
        val titleId = tableId/* + 1*/
        titleDao.getTitle(titleId)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Список")
    val titleSize: StateFlow<Int> = titleDao.getSize()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

    val allTitles: StateFlow<List<CheckListTitleData>> = titleDao.getAllTitles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /*val titlePos: StateFlow<Int> = titleDao.countTitlePos(_currentTableId.value)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)*/

    suspend fun createTitle(): Int {

        var lastId = getLastId() + 1
        val defaultTitle = CheckListTitleData(title = "Список $lastId")

        lastId = titleDao.createTitle(defaultTitle).toInt()

        return lastId
    }

    fun setTitle(text: String) {
        val tableId: Int = _currentTableId.value/* + 1*/
        viewModelScope.launch {
            titleDao.setTitle(text, tableId)
        }
    }

    suspend fun getFirstId(): Int {
        val allIdList = titleDao.allId()
        val firstId = allIdList.firstOrNull()

        if(firstId == null) {
            return 1
        } else {
            return firstId
        }
    }

    suspend fun getLastId(): Int {
        val allIdList = titleDao.allId()
        val lastId = allIdList.lastOrNull()

        if(lastId == null) {
            return 0
        } else {
            return lastId
        }
    }

    suspend fun getNextTitleId(currentId: Int): Int {
        val nextIdList = titleDao.allNextId(currentId)
        val prevIdList = titleDao.allPrevId(currentId)

        val nextId = nextIdList.firstOrNull()
        val prevId = prevIdList.firstOrNull()

        return if (nextId != null) {
            nextId
        } else if (prevId != null) {
            currentId
        } else {
            currentId
        }
    }

    suspend fun getPrevTitleId(currentId: Int): Int {
        val nextIdList = titleDao.allNextId(currentId)
        val prevIdList = titleDao.allPrevId(currentId)

        val nextId = nextIdList.firstOrNull()
        val prevId = prevIdList.firstOrNull()

        return if (prevId != null) {
            prevId
        } else if (nextId != null) {
            currentId
        } else {
            currentId
        }
    }

    suspend fun getTitlePos(): Int {
        return titleDao.countTitlePos(_currentTableId.value)
    }

    suspend fun deleteAndSelect(): Int {
        val nextId = getNextTitleId(_currentTableId.value)
        val prevId = getPrevTitleId(_currentTableId.value)
        val firstId = getFirstId()
        val lastId = getLastId()

        titleDao.deleteTitleById(_currentTableId.value)

        if(_currentTableId.value != lastId) {
            return nextId
        } else if(_currentTableId.value != firstId) {
            return prevId
        } else {
            return firstId
        }
    }

    //----FOR-SETTINGS------------------------------------------------------------------------------

    val settingsSize: StateFlow<Int> = settingsDao.getSize()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)
    val sortByChecked: StateFlow<Boolean> = settingsDao.getState("sortByChecked")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoHideChecked: StateFlow<Boolean> = settingsDao.getState("autoHideChecked")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoLineBreak: StateFlow<Boolean> = settingsDao.getState("autoLineBreak")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun createSetting(defaultSetting: SettingsInfo) {
        viewModelScope.launch {
            settingsDao.createSetting(defaultSetting)
        }
    }

    fun setState(name: String, newState: Boolean) {
        viewModelScope.launch {
            settingsDao.setSetting(newState, name)
        }
    }

    //----------------------------------------------------------------------------------------------
    val settingsCount: Int = 3 //----NUMBER-OF-OPTIONS-IN-SETTINGS----

    init {
        viewModelScope.launch {
            titleSize.filter { it != -1 }
                .take(1)
                .collect { actualSize ->
                    if(actualSize == 0) {
                        createTitle()
                    }
                }

            val defaultSortState = SettingsInfo(name = "sortByChecked", state = false)
            val defaultAutoHide = SettingsInfo(name = "autoHideChecked", state = false)
            val defaultAutoLineBreak = SettingsInfo(name = "autoLineBreak", state = false)

            settingsSize.filter { it != -1 }
                .take(1)
                .collect { actualSize ->
                    if(actualSize < settingsCount) {
                        createSetting(defaultSortState)
                        createSetting(defaultAutoHide)
                        createSetting(defaultAutoLineBreak)
                    }
                }
        }
    }
}