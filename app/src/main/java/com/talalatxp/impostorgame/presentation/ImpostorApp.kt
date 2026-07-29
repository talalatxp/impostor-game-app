@file:OptIn(ExperimentalMaterial3Api::class)
package com.talalatxp.impostorgame.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.talalatxp.impostorgame.R
import com.talalatxp.impostorgame.domain.model.Category
import com.talalatxp.impostorgame.domain.model.GameSettings
import com.talalatxp.impostorgame.domain.model.Player
import com.talalatxp.impostorgame.domain.model.PlayerRole
import com.talalatxp.impostorgame.domain.model.PlayerScore

@Composable
fun ImpostorApp(factory: ViewModelProvider.Factory) {
    val viewModel: GameViewModel = viewModel(factory = factory)
    val state = viewModel.uiState
    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF491A1A)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.app_icon_impostor),
                contentDescription = null,
                modifier = Modifier.size(240.dp),
                contentScale = ContentScale.Crop,
            )
        }
        return
    }
    androidx.activity.compose.BackHandler(
        enabled = state.screen == Screen.SETUP || state.screen == Screen.CATEGORIES || state.screen == Screen.ROLES,
        onBack = viewModel::goBack,
    )
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
            navigationIcon = { if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") } },
        ) },
        content = content,
    )
}

private enum class RankingView { GENERAL, SESSION }

@Composable private fun HomeScreen(state: GameUiState, vm: GameViewModel) = AppScaffold("El Impostor") { padding ->
    var rankingView by remember { mutableStateOf(RankingView.GENERAL) }
    val ranking = when (rankingView) {
        RankingView.GENERAL -> state.generalRanking
        RankingView.SESSION -> state.sessionRanking
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Image(
                painter = painterResource(R.drawable.home_man_glasses),
                contentDescription = "Hombre con gafas",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                contentScale = ContentScale.Fit,
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            Button(onClick = { vm.navigate(Screen.SETUP) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Empezar partida")
            }
        }
        item {
            OutlinedButton(onClick = { vm.navigate(Screen.CATEGORIES) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Outlined.Category, null); Spacer(Modifier.width(8.dp)); Text("Ver categorías")
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            Text("Ranking", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        }
        item {
            RankingSelector(
                selected = rankingView,
                onSelected = { rankingView = it },
            )
        }
        if (rankingView == RankingView.SESSION) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = vm::clearSessionRanking,
                        enabled = state.sessionRanking.isNotEmpty(),
                    ) {
                        Icon(Icons.Outlined.DeleteSweep, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Limpiar resultados")
                    }
                }
            }
        }
        item { RankingCard(ranking) }
    }
}

@Composable
private fun RankingSelector(
    selected: RankingView,
    onSelected: (RankingView) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (selected == RankingView.GENERAL) {
            Button(onClick = { onSelected(RankingView.GENERAL) }, modifier = Modifier.weight(1f)) { Text("General") }
        } else {
            OutlinedButton(onClick = { onSelected(RankingView.GENERAL) }, modifier = Modifier.weight(1f)) { Text("General") }
        }
        if (selected == RankingView.SESSION) {
            Button(onClick = { onSelected(RankingView.SESSION) }, modifier = Modifier.weight(1f)) { Text("Sesión") }
        } else {
            OutlinedButton(onClick = { onSelected(RankingView.SESSION) }, modifier = Modifier.weight(1f)) { Text("Sesión") }
        }
    }
}

@Composable
private fun RankingCard(ranking: List<PlayerScore>) {
    Card(Modifier.fillMaxWidth()) {
        if (ranking.isEmpty()) {
            Text(
                "Todavía no hay resultados.",
                modifier = Modifier.padding(20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(Modifier.fillMaxWidth()) {
                ranking.forEachIndexed { index, score ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (index == 0) "👑" else "${index + 1}.",
                            modifier = Modifier.width(42.dp),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        Text(score.playerName, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text(
                            "${score.points} ${if (score.points == 1) "punto" else "puntos"}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (index != ranking.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

@Composable private fun SetupScreen(state: GameUiState, vm: GameViewModel) = AppScaffold("Configurar partida", vm::goBack) { padding ->
    var playerName by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp)) {
        item { Text("Jugadores", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(playerName, { playerName = it }, Modifier.weight(1f), label = { Text("Nombre") }, singleLine = true)
                Spacer(Modifier.width(8.dp)); FilledIconButton(onClick = { vm.addPlayer(playerName); playerName = "" }) { Icon(Icons.Outlined.Add, "Añadir") }
            }
        }
        items(state.players, key = { it.id }) { player -> PlayerRow(player, { vm.removePlayer(player) }) }
        item {
            HorizontalDivider(Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(14.dp))
            Text("Ajustes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item { ImpostorCount(state.players.size, state.settings, vm) }
        item { TimerChooser(state.settings, vm) }
        item { SwitchRow("El impostor recibe pista", state.settings.impostorGetsClue) { checked -> vm.updateSettings { it.copy(impostorGetsClue = checked) } } }
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
            listOf(120 to "2 min", 180 to "3 min", 0 to "Sin límite").forEach { (seconds, label) ->
                FilterChip(
                    selected = settings.timerDurationSeconds == seconds,
                    onClick = { vm.updateSettings { it.copy(timerDurationSeconds = seconds) } },
                    modifier = Modifier.height(40.dp),
                    label = { Text(label, maxLines = 1, softWrap = false) },
                )
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
    AppScaffold("Reparto", vm::goBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Pásale el teléfono a", style = MaterialTheme.typography.titleMedium)
                Text(role.player.name, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(30.dp))
                RoleRevealCard(role, state.session.word.word, state.roleIndex)
            }
            Button(onClick = vm::nextRole, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(56.dp)) {
                Text(if (state.roleIndex == state.session.roles.lastIndex) "Empezar juego" else "Siguiente jugador")
            }
        }
    }
}

@Composable
private fun RoleRevealCard(role: PlayerRole, secretWord: String, roleIndex: Int) {
    val coverHeight = 270.dp
    val maxRevealPx = with(LocalDensity.current) { coverHeight.toPx() }
    var coverTarget by remember(roleIndex) { mutableFloatStateOf(0f) }
    var isDragging by remember(roleIndex) { mutableStateOf(false) }
    val coverOffset by animateFloatAsState(
        targetValue = coverTarget,
        animationSpec = if (isDragging) {
            tween(durationMillis = 16, easing = LinearEasing)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "Desplazamiento de la cubierta secreta",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .height(coverHeight)
            .clip(MaterialTheme.shapes.extraLarge),
    ) {
        RoleCard(role, secretWord)
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { translationY = coverOffset }
                .background(Color(0xFF743B37))
                .pointerInput(roleIndex, maxRevealPx) {
                    detectVerticalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            coverTarget = 0f
                        },
                        onDragCancel = {
                            isDragging = false
                            coverTarget = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            coverTarget = (coverTarget + dragAmount).coerceIn(-maxRevealPx, 0f)
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Lock, null, Modifier.size(42.dp))
                Spacer(Modifier.height(12.dp))
                Text("Desliza hacia arriba para revelar", textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable private fun RoleCard(role: PlayerRole, secretWord: String) = Card(Modifier.fillMaxWidth().height(270.dp)) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (role.isImpostor) {
            Text("¡ERES EL IMPOSTOR!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp)); Text(role.clue?.let { "Tu pista: $it" } ?: "No tienes pista. Improvisa.", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        } else {
            Text("Eres inocente", color = Color(0xFFB8F5C2), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp))
            Text(secretWord, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        }
    }
}

@Composable private fun RoundScreen(state: GameUiState, vm: GameViewModel) = AppScaffold("Ronda de pistas") { padding ->
    val session = state.session!!
    val hasTimer = session.settings.timerDurationSeconds > 0
    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(
            text = if (hasTimer) formatTime(state.remainingSeconds) else "Sin límite de tiempo",
            style = if (hasTimer) MaterialTheme.typography.displayLarge else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
        )
        if (hasTimer) {
            LinearProgressIndicator(progress = { state.remainingSeconds.toFloat() / session.settings.timerDurationSeconds }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = vm::toggleTimer) { Icon(if (state.isTimerRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text(if (state.isTimerRunning) "Pausar" else "Iniciar") }
                OutlinedButton(onClick = { vm.addTime() }) { Icon(Icons.Outlined.Add, null); Text("30 s") }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Empieza", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(session.roles.first().player.name, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f)); Button(onClick = vm::finishRound, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Ir a resultados") }
    }
}

@Composable private fun ResultsScreen(state: GameUiState, vm: GameViewModel) = AppScaffold("Resultados") { padding ->
    val session = state.session!!
    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("La palabra era", style = MaterialTheme.typography.titleMedium)
        Text(session.word.word, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        Text("Pista: ${session.word.clue}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Text("Impostor${if (session.roles.count { it.isImpostor } > 1) "es" else ""}", style = MaterialTheme.typography.titleLarge)
        session.roles.filter { it.isImpostor }.forEach { Text(it.player.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error) }
        Text("¿Quién ganó?", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { vm.recordWinner(Winner.INNOCENTS) },
                enabled = state.winner == null,
            ) { Text("Inocentes") }
            Button(
                onClick = { vm.recordWinner(Winner.IMPOSTORS) },
                enabled = state.winner == null,
            ) { Text("Impostor") }
        }
        state.winner?.let { Text("Resultado guardado", color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.weight(1f))
        Button(onClick = vm::startGame, modifier = Modifier.fillMaxWidth()) { Text("Nueva ronda") }
        TextButton(onClick = { vm.navigate(Screen.HOME) }) { Text("Volver al inicio") }
    }
}

@Composable private fun CategoriesScreen(categories: List<Category>, vm: GameViewModel) = AppScaffold("Categorías", vm::goBack) { padding ->
    var newCategory by remember { mutableStateOf(false) }
    var confirmGenerate by remember { mutableStateOf(false) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)) {
            item {
                Text("Banco de palabras", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Pulsa una categoría para ver sus palabras, pistas y las ya usadas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { confirmGenerate = true }, modifier = Modifier.fillMaxWidth()) { Text("Generar 10 categorías nuevas") }
            }
            items(categories, key = { it.id }) { category -> CategoryItem(category, { selectedCategoryId = category.id }) }
            item { TextButton(onClick = vm::resetCategories) { Icon(Icons.Outlined.RestartAlt, null); Spacer(Modifier.width(6.dp)); Text("Restablecer categorías predeterminadas") } }
        }
        FloatingActionButton(onClick = { newCategory = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) { Icon(Icons.Outlined.Add, "Nueva categoría") }
    }
    if (newCategory) CategoryDialog(onDismiss = { newCategory = false }, onSave = { name, description -> vm.addCategory(name, description); newCategory = false })
    if (confirmGenerate) AlertDialog(
        onDismissRequest = { confirmGenerate = false },
        title = { Text("¿Generar categorías?") },
        text = { Text("Se reemplazarán las categorías actuales por 10 categorías aleatorias del banco offline. También se perderán las marcas de palabras usadas.") },
        confirmButton = { TextButton(onClick = { vm.generateTenCategories(); confirmGenerate = false }) { Text("Generar") } },
        dismissButton = { TextButton(onClick = { confirmGenerate = false }) { Text("Cancelar") } },
    )
    categories.firstOrNull { it.id == selectedCategoryId }?.let { category -> CategoryDetailDialog(category, onDismiss = { selectedCategoryId = null }, onAddWord = { word, clue -> vm.addWord(category.id, word, clue) }, onUpdateWord = { id, word, clue -> vm.updateWord(category.id, id, word, clue) }, onDeleteWord = { vm.deleteWord(category.id, it) }, onSetWordUsed = { id, used -> vm.setWordUsed(category.id, id, used) }, onRename = { vm.renameCategory(category.id, it) }, onDelete = { vm.deleteCategory(category.id); selectedCategoryId = null }) }
}

@Composable private fun CategoryItem(category: Category, onClick: () -> Unit) = Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Folder, null); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(category.name, fontWeight = FontWeight.Bold); Text("${category.words.size} palabras · ${category.words.count { it.isUsed }} usadas", style = MaterialTheme.typography.bodySmall); Text(category.description, style = MaterialTheme.typography.bodySmall) }; Icon(Icons.Outlined.ChevronRight, null) }
}

@Composable private fun CategoryDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nueva categoría") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(name, { name = it }, label = { Text("Nombre") }); OutlinedTextField(description, { description = it }, label = { Text("Descripción") }) } }, confirmButton = { TextButton(onClick = { onSave(name, description) }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable private fun CategoryDetailDialog(category: Category, onDismiss: () -> Unit, onAddWord: (String, String) -> Unit, onUpdateWord: (String, String, String) -> Unit, onDeleteWord: (String) -> Unit, onSetWordUsed: (String, Boolean) -> Unit, onRename: (String) -> Unit, onDelete: () -> Unit) {
    var word by remember { mutableStateOf("") }; var clue by remember { mutableStateOf("") }; var editingWordId by remember { mutableStateOf<String?>(null) }; var editingName by remember { mutableStateOf(false) }; var name by remember(category.id) { mutableStateOf(category.name) }
    AlertDialog(onDismissRequest = onDismiss, title = { if (editingName) OutlinedTextField(name, { name = it }, label = { Text("Nombre") }) else Text(category.name) }, text = {
        Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            category.words.forEach { item -> Row(Modifier.fillMaxWidth().clickable { editingWordId = item.id; word = item.word; clue = item.clue }, verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = item.isUsed, onCheckedChange = { onSetWordUsed(item.id, it) }); Column(Modifier.weight(1f)) { Text("${item.word} — ${item.clue}"); Text(if (item.isUsed) "Usada (desmarca para reactivarla)" else "Disponible", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; IconButton(onClick = { onDeleteWord(item.id) }) { Icon(Icons.Outlined.Delete, "Eliminar ${item.word}") } } }
            HorizontalDivider(); Text(if (editingWordId == null) "Añadir palabra" else "Editar palabra", fontWeight = FontWeight.Bold)
            OutlinedTextField(word, { word = it }, label = { Text("Palabra secreta") }); OutlinedTextField(clue, { clue = it }, label = { Text("Pista") })
        }
    }, confirmButton = { Row { if (editingName) TextButton(onClick = { onRename(name); editingName = false }) { Text("Renombrar") } else TextButton(onClick = { editingName = true }) { Text("Renombrar") }; TextButton(onClick = { val id = editingWordId; if (id == null) onAddWord(word, clue) else onUpdateWord(id, word, clue); word = ""; clue = ""; editingWordId = null }) { Text(if (editingWordId == null) "Añadir" else "Guardar") } } }, dismissButton = { Row { if (category.isCustom) TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Eliminar") }; TextButton(onClick = onDismiss) { Text("Cerrar") } } })
}

private fun formatTime(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)
