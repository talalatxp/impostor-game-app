package com.talalatxp.impostorgame.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.viewModel
import com.talalatxp.impostorgame.domain.model.Category
import com.talalatxp.impostorgame.domain.model.GameSettings
import com.talalatxp.impostorgame.domain.model.Player
import com.talalatxp.impostorgame.domain.model.PlayerRole

@Composable
fun ImpostorApp(factory: ViewModelProvider.Factory) {
    val viewModel: GameViewModel = viewModel(factory = factory)
    val state = viewModel.uiState
    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    when (state.screen) {
        Screen.HOME -> HomeScreen(state, viewModel)
        Screen.SETUP -> SetupScreen(state, viewModel)
        Screen.CATEGORIES -> CategoriesScreen(state.categories, viewModel)
        Screen.ROLES -> RolesScreen(state, viewModel)
        Screen.ROUND -> RoundScreen(state, viewModel)
        Screen.RESULTS -> ResultsScreen(state, viewModel)
    }
}

@Composable private fun AppScaffold(title: String, onBack: (() -> Unit)? = null, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = { CenterAlignedTopAppBar(
            title = { Text(title, fontWeight = FontWeight.Bold) },
            navigationIcon = { if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Volver") } },
        ) },
        content = content,
    )
}

@Composable private fun HomeScreen(state: GameUiState, vm: GameViewModel) = AppScaffold("El Impostor") { padding ->
    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("¿Quién está fingiendo?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text("Un juego de pistas, sospechas y faroleo para compartir un solo móvil.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { vm.navigate(Screen.SETUP) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Empezar partida")
        }
        OutlinedButton(onClick = { vm.navigate(Screen.CATEGORIES) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Outlined.Category, null); Spacer(Modifier.width(8.dp)); Text("Ver categorías")
        }
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat("Inocentes", state.stats.innocentWins, Modifier.weight(1f))
                Stat("Impostores", state.stats.impostorWins, Modifier.weight(1f))
                Stat("Jugadores", state.players.size, Modifier.weight(1f))
            }
        }
        Text("Pasa el teléfono, no mires las cartas de los demás y trata de no levantar sospechas.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun Stat(label: String, value: Int, modifier: Modifier) = Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Text(value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
}

@Composable private fun SetupScreen(state: GameUiState, vm: GameViewModel) = AppScaffold("Configurar partida", { vm.navigate(Screen.HOME) }) { padding ->
    var playerName by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
        item { Text("Jugadores", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(playerName, { playerName = it }, Modifier.weight(1f), label = { Text("Nombre") }, singleLine = true)
                Spacer(Modifier.width(8.dp)); FilledIconButton(onClick = { vm.addPlayer(playerName); playerName = "" }) { Icon(Icons.Outlined.Add, "Añadir") }
            }
        }
        items(state.players, key = { it.id }) { player -> PlayerRow(player, { vm.removePlayer(player) }) }
        item { HorizontalDivider(Modifier.padding(vertical = 6.dp)); Text("Ajustes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { ImpostorCount(state.players.size, state.settings, vm) }
        item { TimerChooser(state.settings, vm) }
        item { SwitchRow("El impostor recibe una pista sutil", state.settings.impostorGetsClue) { checked -> vm.updateSettings { it.copy(impostorGetsClue = checked) } } }
        item { CategoryPicker(state.categories, state.settings, vm) }
        item { state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
        item { Button(onClick = vm::startGame, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = state.players.size >= 3) { Text("Repartir roles") } }
    }
}

@Composable private fun PlayerRow(player: Player, onRemove: () -> Unit) = Card(Modifier.fillMaxWidth()) {
    Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Person, null); Spacer(Modifier.width(12.dp)); Text(player.name, Modifier.weight(1f), fontWeight = FontWeight.Medium)
        IconButton(onClick = onRemove) { Icon(Icons.Outlined.Close, "Eliminar ${player.name}") }
    }
}

@Composable private fun ImpostorCount(playerCount: Int, settings: GameSettings, vm: GameViewModel) = Card(Modifier.fillMaxWidth()) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text("Impostores", fontWeight = FontWeight.Medium); Text("Máximo ${maxOf(1, playerCount - 1)}", style = MaterialTheme.typography.bodySmall) }
        IconButton(onClick = { vm.updateSettings { it.copy(impostorsCount = it.impostorsCount - 1) } }) { Icon(Icons.Outlined.Remove, "Menos") }
        Text(settings.impostorsCount.toString(), style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = { vm.updateSettings { it.copy(impostorsCount = it.impostorsCount + 1) } }) { Icon(Icons.Outlined.Add, "Más") }
    }
}

@Composable private fun TimerChooser(settings: GameSettings, vm: GameViewModel) = Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp)) {
        Text("Tiempo de debate", fontWeight = FontWeight.Medium); Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(60 to "1 min", 120 to "2 min", 180 to "3 min", 0 to "Sin límite").forEach { (seconds, label) ->
                FilterChip(selected = settings.timerDurationSeconds == seconds, onClick = { vm.updateSettings { it.copy(timerDurationSeconds = seconds) } }, label = { Text(label) })
            }
        }
    }
}

@Composable private fun SwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) = Card(Modifier.fillMaxWidth()) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(checked, onCheckedChange = onChecked) }
}

@Composable private fun CategoryPicker(categories: List<Category>, settings: GameSettings, vm: GameViewModel) = Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp)) {
        Text("Categorías activas", fontWeight = FontWeight.Medium)
        Text("Sin selección equivale a usar todas.", style = MaterialTheme.typography.bodySmall)
        categories.forEach { category ->
            Row(Modifier.fillMaxWidth().clickable { vm.toggleCategory(category.id) }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(category.id in settings.selectedCategoryIds, { vm.toggleCategory(category.id) }); Text(category.name)
            }
        }
    }
}

@Composable private fun RolesScreen(state: GameUiState, vm: GameViewModel) {
    val role = state.session!!.roles[state.roleIndex]
    var revealed by remember(state.roleIndex) { mutableStateOf(false) }
    val coverOffset by animateFloatAsState(if (revealed) 0f else 1f, label = "secret cover")
    AppScaffold("Reparto secreto") { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Pásale el teléfono a", style = MaterialTheme.typography.titleMedium)
                Text(role.player.name, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(30.dp))
                RoleCard(role, state.session.word.word, revealed)
            }
            Button(onClick = vm::nextRole, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(56.dp)) {
                Text(if (state.roleIndex == state.session.roles.lastIndex) "Empezar juego" else "Siguiente jugador")
            }
            if (!revealed) Box(
                Modifier.align(Alignment.Center).fillMaxWidth().height(270.dp).clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .pointerInput(state.roleIndex) { detectDragGesturesAfterLongPress(onDragStart = { revealed = true }, onDragEnd = { revealed = false }, onDragCancel = { revealed = false }, onDrag = { _, _ -> }) },
                contentAlignment = Alignment.Center,
            ) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Outlined.Lock, null, Modifier.size(42.dp)); Spacer(Modifier.height(12.dp)); Text("Mantén pulsado y desliza para revelar", textAlign = TextAlign.Center) } }
        }
    }
}

@Composable private fun RoleCard(role: PlayerRole, secretWord: String, revealed: Boolean) = Card(Modifier.fillMaxWidth().height(270.dp)) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (role.isImpostor) {
            Text("¡ERES EL IMPOSTOR!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp)); Text(role.clue?.let { "Tu pista: $it" } ?: "No tienes pista. Improvisa.", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        } else {
            Text("Eres inocente", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(14.dp)); Text(secretWord, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black); Spacer(Modifier.height(8.dp)); Text("Pista: ${role.clue ?: ""}")
        }
        if (!revealed) Text("", color = Color.Transparent)
    }
}

@Composable private fun RoundScreen(state: GameUiState, vm: GameViewModel) = AppScaffold("Ronda de pistas") { padding ->
    val session = state.session!!
    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(if (session.settings.timerDurationSeconds == 0) "Sin límite de tiempo" else formatTime(state.remainingSeconds), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
        if (session.settings.timerDurationSeconds > 0) LinearProgressIndicator(progress = state.remainingSeconds.toFloat() / session.settings.timerDurationSeconds, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = vm::toggleTimer) { Icon(if (state.isTimerRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text(if (state.isTimerRunning) "Pausar" else "Iniciar") }
            OutlinedButton(onClick = { vm.addTime() }) { Icon(Icons.Outlined.Add, null); Text("30 s") }
        }
        Text("Orden de turnos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        session.roles.forEachIndexed { index, role -> Text("${index + 1}. ${role.player.name}", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyLarge) }
        Spacer(Modifier.weight(1f)); Button(onClick = vm::finishRound, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Ir a resultados") }
    }
}

@Composable private fun ResultsScreen(state: GameUiState, vm: GameViewModel) = AppScaffold("Resultados", { vm.navigate(Screen.HOME) }) { padding ->
    val session = state.session!!
    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("La palabra era", style = MaterialTheme.typography.titleMedium)
        Text(session.word.word, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        Text("Pista sutil: ${session.word.clue}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Text("Impostor${if (session.roles.count { it.isImpostor } > 1) "es" else ""}", style = MaterialTheme.typography.titleLarge)
        session.roles.filter { it.isImpostor }.forEach { Text(it.player.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error) }
        Text("¿Quién ganó?", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { vm.recordWinner(Winner.INNOCENTS) }) { Text("Inocentes") }
            Button(onClick = { vm.recordWinner(Winner.IMPOSTORS) }) { Text("Impostor") }
        }
        state.winner?.let { Text("Resultado guardado", color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.weight(1f))
        Button(onClick = vm::startGame, modifier = Modifier.fillMaxWidth()) { Text("Nueva ronda") }
        TextButton(onClick = { vm.navigate(Screen.HOME) }) { Text("Volver al inicio") }
    }
}

@Composable private fun CategoriesScreen(categories: List<Category>, vm: GameViewModel) = AppScaffold("Categorías", { vm.navigate(Screen.HOME) }) { padding ->
    var newCategory by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Category?>(null) }
    Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)) {
            item { Text("Banco de palabras", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Pulsa una categoría para ver sus palabras y pistas.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(categories, key = { it.id }) { category -> CategoryItem(category, { selected = category }) }
            item { TextButton(onClick = vm::resetCategories) { Icon(Icons.Outlined.RestartAlt, null); Spacer(Modifier.width(6.dp)); Text("Restablecer categorías predeterminadas") } }
        }
        FloatingActionButton(onClick = { newCategory = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) { Icon(Icons.Outlined.Add, "Nueva categoría") }
    }
    if (newCategory) CategoryDialog(onDismiss = { newCategory = false }, onSave = { name, description -> vm.addCategory(name, description); newCategory = false })
    selected?.let { category -> CategoryDetailDialog(category, onDismiss = { selected = null }, onAddWord = { word, clue -> vm.addWord(category.id, word, clue) }, onUpdateWord = { id, word, clue -> vm.updateWord(category.id, id, word, clue) }, onDeleteWord = { vm.deleteWord(category.id, it) }, onRename = { vm.renameCategory(category.id, it) }, onDelete = { vm.deleteCategory(category.id); selected = null }) }
}

@Composable private fun CategoryItem(category: Category, onClick: () -> Unit) = Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Folder, null); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(category.name, fontWeight = FontWeight.Bold); Text("${category.words.size} palabras · ${category.description}", style = MaterialTheme.typography.bodySmall) }; Icon(Icons.Outlined.ChevronRight, null) }
}

@Composable private fun CategoryDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nueva categoría") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(name, { name = it }, label = { Text("Nombre") }); OutlinedTextField(description, { description = it }, label = { Text("Descripción") }) } }, confirmButton = { TextButton(onClick = { onSave(name, description) }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable private fun CategoryDetailDialog(category: Category, onDismiss: () -> Unit, onAddWord: (String, String) -> Unit, onUpdateWord: (String, String, String) -> Unit, onDeleteWord: (String) -> Unit, onRename: (String) -> Unit, onDelete: () -> Unit) {
    var word by remember { mutableStateOf("") }; var clue by remember { mutableStateOf("") }; var editingWordId by remember { mutableStateOf<String?>(null) }; var editingName by remember { mutableStateOf(false) }; var name by remember(category.id) { mutableStateOf(category.name) }
    AlertDialog(onDismissRequest = onDismiss, title = { if (editingName) OutlinedTextField(name, { name = it }, label = { Text("Nombre") }) else Text(category.name) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            category.words.forEach { item -> Row(Modifier.fillMaxWidth().clickable { editingWordId = item.id; word = item.word; clue = item.clue }, verticalAlignment = Alignment.CenterVertically) { Text("${item.word} — ${item.clue}", Modifier.weight(1f)); IconButton(onClick = { onDeleteWord(item.id) }) { Icon(Icons.Outlined.Delete, "Eliminar ${item.word}") } } }
            HorizontalDivider(); Text(if (editingWordId == null) "Añadir palabra" else "Editar palabra", fontWeight = FontWeight.Bold)
            OutlinedTextField(word, { word = it }, label = { Text("Palabra secreta") }); OutlinedTextField(clue, { clue = it }, label = { Text("Pista sutil") })
        }
    }, confirmButton = { Row { if (editingName) TextButton(onClick = { onRename(name); editingName = false }) { Text("Renombrar") } else TextButton(onClick = { editingName = true }) { Text("Renombrar") }; TextButton(onClick = { val id = editingWordId; if (id == null) onAddWord(word, clue) else onUpdateWord(id, word, clue); word = ""; clue = ""; editingWordId = null }) { Text(if (editingWordId == null) "Añadir" else "Guardar") } } }, dismissButton = { Row { if (category.isCustom) TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Eliminar") }; TextButton(onClick = onDismiss) { Text("Cerrar") } } })
}

private fun formatTime(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)
