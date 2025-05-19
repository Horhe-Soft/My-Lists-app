package com.test.myapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.test.myapp.ui.theme.MyApplicationTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.navigation.NavController
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.room.ForeignKey
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                NavigationApp()
            }
        }
    }
}

//-----SYSTEM-------------------------------------------------------------------

@Composable
fun NavigationApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "mainScreen") {
        composable("mainScreen") {
            MainScreen(navController = navController)
        }
        composable("historyScreen") {
            HistoryScreen(navController = navController)
        }
        composable("settingsScreen") {
            SettingsScreen(navController = navController)
        }
    }
}

//-----SCREENS------------------------------------------------------------------

@Composable
fun MainScreen(navController: NavController,
               viewModel: CheckListViewModel = viewModel()) {

    val coroutineScope = rememberCoroutineScope()

    var currentTableId by remember { mutableIntStateOf(1) }
    var currentScreenPos by remember { mutableStateOf(1) }

    var nextId by remember { mutableIntStateOf(1) }
    var prevId by remember { mutableIntStateOf(1) }
    var firstId by remember { mutableIntStateOf(1) }
    var lastId by remember { mutableIntStateOf(1) }

    LaunchedEffect(currentTableId) {
        viewModel.setTableId(currentTableId)
        coroutineScope.launch {
            currentScreenPos = viewModel.getTitlePos()
        }
    }

    LaunchedEffect(Unit) {
        firstId = viewModel.getFirstId()
        lastId = viewModel.getLastId()
        currentTableId = firstId
    }

    val checkListItems by viewModel.activeChecklistItems.collectAsState()
    val checkListItemsSorted by viewModel.activeChecklistItemsSorted.collectAsState()
    var itemsForList by remember(checkListItems) { mutableStateOf(checkListItems) }

    val checkedNotArchived by viewModel.checkedChecklistItems.collectAsState()
    val listTitle by viewModel.title.collectAsState()
    val maxScreenPos by viewModel.titleSize.collectAsState()

    val isSortingByCheck by viewModel.sortByChecked.collectAsState()
    val isAutoHidingChecked by viewModel.autoHideChecked.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var showTitleDialog by remember { mutableStateOf(false) }
    var newItemText by remember { mutableStateOf("") }
    var titleText by remember(listTitle) { mutableStateOf(listTitle ?: "Список") }
    var canDelete by remember { mutableStateOf(true) }

    var dragAmount by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 50.dp

    canDelete = maxScreenPos > 1

    if (showDialog) {
        AddItemDialog(
            currentText = newItemText,
            onTextChange = { newItemText = it },
            onDismiss = {
                showDialog = false
                newItemText = ""
            },
            onConfirm = {
                viewModel.addItem(newItemText)
                showDialog = false
                newItemText = ""
            }
        )
    }

    if (showTitleDialog) {
        ChangeTitleDialog(
            currentText = titleText,
            onTextChange = { titleText = it },
            onDismiss = {
                showTitleDialog = false
            },
            onConfirm = {
                viewModel.setTitle(titleText)
                showTitleDialog = false
            },
            onDeleteClick = {
                showTitleDialog = false
                coroutineScope.launch {
                    currentTableId = viewModel.deleteAndSelect()
                }
            },
            canDelete = canDelete
        )
    }

    itemsForList = if (isSortingByCheck) {
        checkListItemsSorted
    } else {
        checkListItems
    }

    Scaffold {
        innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SettingsButton(onClick = { navController.navigate("SettingsScreen") })
                ListName(onClick = {
                    showTitleDialog = true
                    titleText = listTitle?: "Список"
                                   },
                    listTitle?: "Список",
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(horizontal = 4.dp)
                )
                AddListButton(
                    onClick = {
                        coroutineScope.launch {
                            currentTableId = viewModel.createTitle()
                            println("curr $currentTableId")
                        }
                    }
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Text("${ currentScreenPos + 1 } из $maxScreenPos")

                Spacer(modifier = Modifier.weight(1f))
            }
            ListBlock(items = itemsForList,
                viewModel::updateChecked,
                modifier = Modifier
                    .padding(innerPadding)
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragAmount = 0f
                            },
                            onHorizontalDrag = { change, dragAmountDelta ->
                                dragAmount += dragAmountDelta
                                change.consume()
                            },
                            onDragEnd = {
                                val swipeThresholdPx = swipeThreshold.toPx()

                                coroutineScope.launch {
                                    nextId = viewModel.getNextTitleId(currentTableId)
                                    prevId = viewModel.getPrevTitleId(currentTableId)
                                    lastId = viewModel.getLastId()

                                    println("n$nextId p$prevId")

                                    if(abs(dragAmount) > swipeThresholdPx) {
                                        if(dragAmount < 0) {
                                            if(currentTableId < lastId) {
                                                currentTableId = nextId
                                            }
                                        } else {
                                            if(currentTableId > firstId) {
                                                currentTableId = prevId
                                            }
                                        }
                                    }
                                    dragAmount = 0f
                                }
                            },
                            onDragCancel = {
                                dragAmount = 0f
                            }
                        )
                    },
                viewModel = viewModel,
                isAutoHiding = isAutoHidingChecked
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ToHistoryButton(onClick = { viewModel.archiveCheckedItems() }, isEnabled = checkedNotArchived.isNotEmpty())
                HistoryButton(onClick = { navController.navigate("historyScreen") })
                AddButton(onClick = { showDialog = true })
            }
        }
    }
}

@Composable
fun HistoryScreen(navController: NavController,
                  viewModel: CheckListViewModel = viewModel(),
                  listName: String = "История записей") {

    val checkListItems by viewModel.archivedChecklistItems.collectAsState()

    Scaffold {
        innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BackButton(onClick = { navController.popBackStack() })
                Text(listName, fontSize = 24.sp)
                ClearAllHistoryButton(onClick = { viewModel.clearHistory() }, isEnabled = checkListItems.isNotEmpty())
            }
            ListBlock(items = checkListItems,
                viewModel::updateChecked,
                modifier = Modifier
                    .padding(innerPadding)
                    .weight(1f),
                viewModel = viewModel,
                isAutoHiding = false
            )
            Row {
                FromHistoryButton(onClick = { viewModel.unarchiveItems() }, isEnabled = checkListItems.isNotEmpty())
                Spacer(modifier = Modifier.weight(1f))
                ClearHistoryButton(onClick = { viewModel.deleteSelected() }, isEnabled = checkListItems.isNotEmpty())
            }
        }
    }
}

@Composable
fun SettingsScreen(navController: NavController,
                   viewModel: CheckListViewModel = viewModel()) {

    val isSortingByCheck by viewModel.sortByChecked.collectAsState()
    val isAutoHidingChecked by viewModel.autoHideChecked.collectAsState()

    Scaffold {
        innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BackButton(onClick = { navController.popBackStack() })
                Text("Настройки", fontSize = 24.sp)
                ClearSettingsButton(
                    onClick = {
                        viewModel.setState(name = "sortByChecked", newState = false)
                        viewModel.setState(name = "autoHideChecked", newState = false)
                    }
                )
            }
            HorizontalDivider(thickness = 2.dp)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                SortSettings(isEnabled = true,
                    isTurnedOn = isSortingByCheck,
                    onCheckedChange = {
                        viewModel.setState(name = "sortByChecked", newState = !isSortingByCheck)
                    }
                )
                AutoHideSettings(isEnabled = true,
                    isTurnedOn = isAutoHidingChecked,
                    onCheckedChange = {
                        viewModel.setState(name = "autoHideChecked", newState = !isAutoHidingChecked)
                    }
                )
            }
        }
    }
}

//-----DATA-CLASSES-------------------------------------------------------------

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = CheckListTitleData::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CheckListItemData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val isChecked: Boolean = false,
    val isArchived: Boolean = false,
    val tableId: Int = 0
)

@Entity(tableName = "checklist_title")
data class CheckListTitleData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String
)

@Entity(tableName = "settings_info")
data class SettingsInfo(
    @PrimaryKey val name: String, //суть настройки
    val state: Boolean
)

//-----DIALOGS-AND-MENUS--------------------------------------------------------

@Composable
fun AddItemDialog(currentText: String,
                  onTextChange: (String) -> Unit,
                  onDismiss: () -> Unit,
                  onConfirm: () -> Unit) {
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый пункт") },
        text = {
            OutlinedTextField(
                value = currentText,
                onValueChange = onTextChange,
                label = { Text("Текст пункта") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onDismiss
                ) {
                    Text("Отменить")
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onConfirm,
                    enabled = currentText.isNotBlank()
                ) {
                    Text("Добавить")
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun ChangeTextDialog(currentText: String,
                     onTextChange: (String) -> Unit,
                     onDismiss: () -> Unit,
                     onConfirm: () -> Unit) {
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Изменение текста") },
        text = {
            OutlinedTextField(
                value = currentText,
                onValueChange = onTextChange,
                label = { Text("Текст пункта") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onDismiss
                ) {
                    Text("Отменить")
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onConfirm,
                    enabled = currentText.isNotBlank()
                ) {
                    Text("Готово")
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun ChangeTitleDialog(currentText: String,
                      onTextChange: (String) -> Unit,
                      onDismiss: () -> Unit,
                      onConfirm: () -> Unit,
                      onDeleteClick: () -> Unit,
                      canDelete: Boolean) {

    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Название списка")
                DeleteListButton(
                    onClick = onDeleteClick,
                    isEnabled = canDelete
                )
            }
        },
        text = {
            OutlinedTextField(
                value = currentText,
                onValueChange = onTextChange,
                label = { Text("Ваш заголовок") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onDismiss
                ) {
                    Text("Отменить")
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onConfirm,
                    enabled = currentText.isNotBlank()
                ) {
                    Text("Готово")
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

//-----MODULES------------------------------------------------------------------

@Composable
fun SettingsButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = "Menu"
        )
    }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back"
        )
    }
}

@Composable
fun HistoryButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.DateRange,
            contentDescription = "History"
        )
    }
}

@Composable
fun AddListButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Add list"
        )
    }
}

@Composable
fun ClearSettingsButton(onClick: () -> Unit, isEnabled: Boolean = true) {
    IconButton(onClick = onClick, enabled = isEnabled) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "CLear"
        )
    }
}

@Composable
fun DeleteListButton(onClick: () -> Unit, isEnabled: Boolean = true) {
    IconButton(onClick = onClick, enabled = isEnabled) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = "Delete List"
        )
    }
}

@Composable
fun ListBlock(items: List<CheckListItemData>,
              onItemCheckedChange: (Int, Boolean) -> Unit,
              isAutoHiding: Boolean,
              viewModel: CheckListViewModel,
              modifier: Modifier = Modifier) {
    ElevatedCard(
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier
            .padding(8.dp)
            .verticalScroll(rememberScrollState())) {
            items.forEach { item ->
                key(item.id) {
                    CheckListStruct(
                        itemData = item,
                        onCheckedChange = { isChecked ->
                            onItemCheckedChange(item.id, isChecked)
                            if(isAutoHiding && isChecked) {
                                viewModel.archiveItem(item.id)
                            }
                        },
                        viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun CheckListStruct(itemData: CheckListItemData,
                    onCheckedChange: (Boolean) -> Unit,
                    viewModel: CheckListViewModel) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = itemData.isChecked,
            onCheckedChange = onCheckedChange
        )
        CheckboxTextButton(
            itemData.text,
            /*itemData.id,*/
            itemData.tableId,
            viewModel
        )
    }
}

@Composable
fun CheckboxTextButton(text: String, id: Int, viewModel: CheckListViewModel) {
    var showChangeTextDialog by remember { mutableStateOf(false) }
    var newText by remember(text) { mutableStateOf(text) }

    TextButton(
        onClick = {
            newText = text
            showChangeTextDialog = true
        },
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(text, fontSize = 16.sp)
        }
    }

    if(showChangeTextDialog) {
        ChangeTextDialog(
            currentText = newText,
            onTextChange = { newText = it },
            onDismiss = { showChangeTextDialog = false },
            onConfirm = {
                showChangeTextDialog = false
                viewModel.updateText(id, newText)
            }
        )
    }
}

@Composable
fun AddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text("Добавить")
    }
}

@Composable
fun ToHistoryButton(onClick: () -> Unit, isEnabled: Boolean, modifier: Modifier = Modifier) {
    FilledTonalButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier
    ) {
        Text("Скрыть")
    }
}

@Composable
fun FromHistoryButton(onClick: () -> Unit, isEnabled: Boolean, modifier: Modifier = Modifier) {
    FilledTonalButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier
    ) {
        Text("Восстановить")
    }
}

@Composable
fun ClearHistoryButton(onClick: () -> Unit, isEnabled: Boolean, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier
    ) {
        Text("Удалить")
    }
}

@Composable
fun ClearAllHistoryButton(onClick: () -> Unit, isEnabled: Boolean, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = "Очистить всю историю"
        )
    }
}

@Composable
fun ListName(onClick: () -> Unit, text: String, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text,
            fontSize = 28.sp,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
    }
}

//--SETTINGS--------------------------------------------------------------------

@Composable
fun SortSettings(isEnabled: Boolean = true, onCheckedChange: (Boolean) -> Unit, isTurnedOn: Boolean) {
    var isChecked by remember(isTurnedOn) { mutableStateOf(isTurnedOn) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Отмеченные пункты\nв конце списка", fontSize = 18.sp)
        Spacer(modifier = Modifier.weight(0.1f))
        Switch(
            checked = isChecked,
            onCheckedChange = {
                isChecked = it
                onCheckedChange(it)
                              },
            enabled = isEnabled
        )
    }
}

@Composable
fun AutoHideSettings(isEnabled: Boolean = true, onCheckedChange: (Boolean) -> Unit, isTurnedOn: Boolean) {
    var isChecked by remember(isTurnedOn) { mutableStateOf(isTurnedOn) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Автоматически скрывать\nотмеченные пункты", fontSize = 18.sp)
        Spacer(modifier = Modifier.weight(0.1f))
        Switch(
            checked = isChecked,
            onCheckedChange = {
                isChecked = it
                onCheckedChange(it)
                              },
            enabled = isEnabled
        )
    }
}

//------------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    NavigationApp()
}