package com.moviznow.admin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.moviznow.admin.data.model.AppCollection
import com.moviznow.admin.data.model.ContentItem
import com.moviznow.admin.data.model.LinkDef
import com.moviznow.admin.data.model.Season
import com.moviznow.admin.ui.theme.MovizColors
import com.moviznow.admin.ui.theme.StatusBadge
import com.moviznow.admin.ui.theme.statusColor
import com.moviznow.admin.ui.viewmodel.AdminUiState

@Composable
fun ContentScreen(
    state: AdminUiState,
    onSaveContent: (ContentItem, () -> Unit) -> Unit,
    onDeleteContent: (ContentItem) -> Unit,
    onSearchTmdb: (String) -> Unit,
    onClearTmdb: () -> Unit,
    onSaveGenre: (String?, String) -> Unit,
    onDeleteGenre: (String) -> Unit,
    onSaveLanguage: (String?, String) -> Unit,
    onDeleteLanguage: (String) -> Unit,
    onSaveQuality: (String?, String, String?) -> Unit,
    onDeleteQuality: (String) -> Unit,
    onSaveCollection: (AppCollection) -> Unit,
    onDeleteCollection: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("all") }
    var editingContent by remember { mutableStateOf<ContentItem?>(null) }

    val filteredCatalog = remember(state.contentList, searchQuery, typeFilter) {
        val q = searchQuery.trim().lowercase()
        state.contentList.filter { item ->
            val matchesFilter = when (typeFilter) {
                "movie" -> item.type == "movie"
                "series" -> item.type == "series"
                "published" -> item.status == "published"
                "draft" -> item.status == "draft"
                else -> true
            }
            val matchesQ = q.isEmpty() ||
                item.title.lowercase().contains(q) ||
                item.year.toString().contains(q)
            matchesFilter && matchesQ
        }
    }

    Scaffold(
        containerColor = MovizColors.BgPrimary,
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { editingContent = ContentItem() },
                    containerColor = MovizColors.Emerald,
                    contentColor = Color(0xFF022C22)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Content")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MovizColors.BgSurface,
                contentColor = MovizColors.Emerald,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MovizColors.Emerald
                    )
                }
            ) {
                listOf("Catalog (${state.contentList.size})", "Collections", "Taxonomy").forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == i) MovizColors.Emerald else MovizColors.TextSecondary
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search movies & series...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MovizColors.Emerald,
                            unfocusedBorderColor = MovizColors.Border,
                            focusedContainerColor = MovizColors.BgSurface,
                            unfocusedContainerColor = MovizColors.BgSurface,
                            focusedTextColor = MovizColors.TextPrimary,
                            unfocusedTextColor = MovizColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("all" to "All", "movie" to "Movies", "series" to "Series", "published" to "Published", "draft" to "Drafts")
                            .forEach { (key, label) ->
                                FilterChip(
                                    selected = typeFilter == key,
                                    onClick = { typeFilter = key },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MovizColors.Emerald.copy(alpha = 0.2f),
                                        selectedLabelColor = MovizColors.Emerald,
                                        containerColor = MovizColors.BgSurface,
                                        labelColor = MovizColors.TextSecondary
                                    )
                                )
                            }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredCatalog, key = { it.id }) { item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MovizColors.BgSurface,
                                border = BorderStroke(1.dp, MovizColors.Border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editingContent = item }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    AsyncImage(
                                        model = item.posterUrl,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .width(56.dp)
                                            .height(80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = item.title,
                                            color = MovizColors.TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.type.uppercase()} • ${item.year}${item.imdbRating?.let { " • ★ $it" } ?: ""}",
                                            color = MovizColors.TextSecondary,
                                            fontSize = 12.sp
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            StatusBadge(text = item.status.uppercase(), color = statusColor(item.status))
                                            val linksSummary = if (item.type == "series") "${item.seasons.size} season(s)" else "${item.movieLinks.size} link(s)"
                                            StatusBadge(text = linksSummary, color = MovizColors.Sky)
                                        }
                                    }
                                    Column {
                                        IconButton(onClick = { editingContent = item }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MovizColors.TextSecondary)
                                        }
                                        IconButton(onClick = { onDeleteContent(item) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MovizColors.Red)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> CollectionsSubTab(
                    collections = state.collections,
                    onSaveCollection = onSaveCollection,
                    onDeleteCollection = onDeleteCollection
                )

                2 -> TaxonomySubTab(
                    state = state,
                    onSaveGenre = onSaveGenre,
                    onDeleteGenre = onDeleteGenre,
                    onSaveLanguage = onSaveLanguage,
                    onDeleteLanguage = onDeleteLanguage,
                    onSaveQuality = onSaveQuality,
                    onDeleteQuality = onDeleteQuality
                )
            }
        }
    }

    editingContent?.let { target ->
        ContentEditorDialog(
            initial = target,
            state = state,
            onSearchTmdb = onSearchTmdb,
            onClearTmdb = onClearTmdb,
            onDismiss = {
                onClearTmdb()
                editingContent = null
            },
            onSave = { updated ->
                onSaveContent(updated) {
                    onClearTmdb()
                    editingContent = null
                }
            }
        )
    }
}

@Composable
private fun ContentEditorDialog(
    initial: ContentItem,
    state: AdminUiState,
    onSearchTmdb: (String) -> Unit,
    onClearTmdb: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (ContentItem) -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var type by remember { mutableStateOf(initial.type) }
    var status by remember { mutableStateOf(initial.status) }
    var year by remember { mutableStateOf(initial.year.toString()) }
    var posterUrl by remember { mutableStateOf(initial.posterUrl) }
    var description by remember { mutableStateOf(initial.description) }
    var trailerUrl by remember { mutableStateOf(initial.trailerUrl) }
    var imdbRating by remember { mutableStateOf(initial.imdbRating.orEmpty()) }
    var runtime by remember { mutableStateOf(initial.runtime.orEmpty()) }
    var selectedGenres by remember { mutableStateOf(initial.genreIds.toSet()) }
    var selectedLangs by remember { mutableStateOf(initial.languageIds.toSet()) }
    var qualityId by remember { mutableStateOf(initial.qualityId.orEmpty()) }
    var movieLinks by remember { mutableStateOf(initial.movieLinks) }
    var newLinkName by remember { mutableStateOf("") }
    var newLinkUrl by remember { mutableStateOf("") }
    var newLinkSize by remember { mutableStateOf("") }
    var newSeasonNum by remember { mutableStateOf((initial.seasons.size + 1).toString()) }
    var seasons by remember { mutableStateOf(initial.seasons) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            shape = RoundedCornerShape(18.dp),
            color = MovizColors.BgSurface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initial.id.isBlank()) "Add Content" else "Edit ${initial.title}",
                        color = MovizColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = { onSearchTmdb(title) }) {
                        Icon(Icons.Default.TravelExplore, contentDescription = "TMDB", modifier = Modifier.size(16.dp))
                        Text(" TMDB", fontSize = 11.sp)
                    }
                }

                if (state.tmdbResults.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MovizColors.BgElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Tap a TMDB match to autofill:", color = MovizColors.Emerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            state.tmdbResults.take(4).forEach { match ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            title = match.title
                                            type = match.type
                                            year = match.year.toString()
                                            if (match.posterUrl.isNotBlank()) posterUrl = match.posterUrl
                                            if (match.overview.isNotBlank()) description = match.overview
                                            imdbRating = match.voteAverage
                                            onClearTmdb()
                                        }
                                        .padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${match.title} (${match.year}) • ${match.type}", color = MovizColors.TextPrimary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("movie", "series").forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.uppercase(), fontSize = 11.sp) })
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    listOf("published", "draft", "selected_content").forEach { s ->
                        FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s.replace("_", " "), fontSize = 11.sp) })
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Year") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = imdbRating, onValueChange = { imdbRating = it }, label = { Text("Rating") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = runtime, onValueChange = { runtime = it }, label = { Text("Runtime") }, singleLine = true, modifier = Modifier.weight(1f))
                }

                OutlinedTextField(value = posterUrl, onValueChange = { posterUrl = it }, label = { Text("Poster URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = trailerUrl, onValueChange = { trailerUrl = it }, label = { Text("Trailer URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())

                if (state.genres.isNotEmpty()) {
                    Text("Genres", color = MovizColors.TextSecondary, fontSize = 12.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.genres.forEach { g ->
                            FilterChip(
                                selected = selectedGenres.contains(g.id),
                                onClick = { selectedGenres = if (selectedGenres.contains(g.id)) selectedGenres - g.id else selectedGenres + g.id },
                                label = { Text(g.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                if (state.languages.isNotEmpty()) {
                    Text("Languages", color = MovizColors.TextSecondary, fontSize = 12.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.languages.forEach { l ->
                            FilterChip(
                                selected = selectedLangs.contains(l.id),
                                onClick = { selectedLangs = if (selectedLangs.contains(l.id)) selectedLangs - l.id else selectedLangs + l.id },
                                label = { Text(l.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                if (state.qualities.isNotEmpty()) {
                    Text("Quality", color = MovizColors.TextSecondary, fontSize = 12.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.qualities.forEach { q ->
                            FilterChip(
                                selected = qualityId == q.id,
                                onClick = { qualityId = q.id },
                                label = { Text(q.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                if (type == "movie") {
                    Text("Movie Links (${movieLinks.size})", color = MovizColors.Emerald, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    movieLinks.forEachIndexed { idx, l ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${l.name.ifBlank { "Link" }} • ${l.size}${l.unit}", color = MovizColors.TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { movieLinks = movieLinks.filterIndexed { i, _ -> i != idx } }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MovizColors.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = newLinkName, onValueChange = { newLinkName = it }, label = { Text("Label (1080p)") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = newLinkSize, onValueChange = { newLinkSize = it }, label = { Text("Size (1.4 GB)") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = newLinkUrl, onValueChange = { newLinkUrl = it }, label = { Text("Download URL") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = {
                            if (newLinkUrl.isNotBlank()) {
                                movieLinks = movieLinks + LinkDef(
                                    id = "lnk_${System.currentTimeMillis()}",
                                    name = newLinkName.ifBlank { "Download" },
                                    url = newLinkUrl.trim(),
                                    size = newLinkSize.trim(),
                                    unit = if (newLinkSize.contains("GB", true)) "GB" else "MB"
                                )
                                newLinkName = ""
                                newLinkUrl = ""
                                newLinkSize = ""
                            }
                        }) { Text("Add") }
                    }
                } else {
                    Text("Seasons (${seasons.size})", color = MovizColors.Emerald, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    seasons.forEach { s ->
                        Text("Season ${s.seasonNumber} • ${s.episodes.size} ep(s) • ${s.zipLinks.size} ZIP", color = MovizColors.TextSecondary, fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = newSeasonNum, onValueChange = { newSeasonNum = it }, label = { Text("Season #") }, singleLine = true, modifier = Modifier.width(110.dp))
                        OutlinedButton(onClick = {
                            val num = newSeasonNum.toIntOrNull() ?: (seasons.size + 1)
                            if (seasons.none { it.seasonNumber == num }) {
                                seasons = seasons + Season(id = "s$num", seasonNumber = num, title = "Season $num")
                                newSeasonNum = (num + 1).toString()
                            }
                        }) { Text("+ Add Season") }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = MovizColors.TextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isBlank()) return@Button
                            onSave(
                                initial.copy(
                                    title = title.trim(),
                                    type = type,
                                    status = status,
                                    year = year.toIntOrNull() ?: 2026,
                                    posterUrl = posterUrl.trim(),
                                    description = description.trim(),
                                    trailerUrl = trailerUrl.trim(),
                                    imdbRating = imdbRating.trim().ifBlank { null },
                                    runtime = runtime.trim().ifBlank { null },
                                    genreIds = selectedGenres.toList(),
                                    languageIds = selectedLangs.toList(),
                                    qualityId = qualityId.ifBlank { null },
                                    movieLinks = movieLinks,
                                    seasons = seasons
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MovizColors.Emerald, contentColor = Color(0xFF022C22))
                    ) {
                        Text("Save to Catalog", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionsSubTab(
    collections: List<AppCollection>,
    onSaveCollection: (AppCollection) -> Unit,
    onDeleteCollection: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("New Collection Title") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSaveCollection(AppCollection(title = title.trim(), order = collections.size + 1))
                            title = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MovizColors.Emerald, contentColor = Color(0xFF022C22))
                ) { Text("Add") }
            }
        }
        items(collections, key = { it.id }) { col ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MovizColors.BgSurface,
                border = BorderStroke(1.dp, MovizColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(col.title, color = MovizColors.TextPrimary, fontWeight = FontWeight.Bold)
                        Text("${col.contentIds.size} item(s)", color = MovizColors.TextMuted, fontSize = 12.sp)
                    }
                    IconButton(onClick = { onDeleteCollection(col.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MovizColors.Red)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaxonomySubTab(
    state: AdminUiState,
    onSaveGenre: (String?, String) -> Unit,
    onDeleteGenre: (String) -> Unit,
    onSaveLanguage: (String?, String) -> Unit,
    onDeleteLanguage: (String) -> Unit,
    onSaveQuality: (String?, String, String?) -> Unit,
    onDeleteQuality: (String) -> Unit
) {
    var genreInput by remember { mutableStateOf("") }
    var langInput by remember { mutableStateOf("") }
    var qualInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Genres (${state.genres.size})", color = MovizColors.TextPrimary, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = genreInput, onValueChange = { genreInput = it }, label = { Text("Add Genre") }, singleLine = true, modifier = Modifier.weight(1f))
            Button(onClick = { if (genreInput.isNotBlank()) { onSaveGenre(null, genreInput.trim()); genreInput = "" } }) { Text("Add") }
        }
        state.genres.forEach { g ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(g.name, color = MovizColors.TextSecondary)
                IconButton(onClick = { onDeleteGenre(g.id) }) { Icon(Icons.Default.Delete, contentDescription = null, tint = MovizColors.Red) }
            }
        }

        Text("Languages (${state.languages.size})", color = MovizColors.TextPrimary, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = langInput, onValueChange = { langInput = it }, label = { Text("Add Language") }, singleLine = true, modifier = Modifier.weight(1f))
            Button(onClick = { if (langInput.isNotBlank()) { onSaveLanguage(null, langInput.trim()); langInput = "" } }) { Text("Add") }
        }
        state.languages.forEach { l ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(l.name, color = MovizColors.TextSecondary)
                IconButton(onClick = { onDeleteLanguage(l.id) }) { Icon(Icons.Default.Delete, contentDescription = null, tint = MovizColors.Red) }
            }
        }

        Text("Qualities (${state.qualities.size})", color = MovizColors.TextPrimary, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = qualInput, onValueChange = { qualInput = it }, label = { Text("Add Quality (e.g. 4K HDR)") }, singleLine = true, modifier = Modifier.weight(1f))
            Button(onClick = { if (qualInput.isNotBlank()) { onSaveQuality(null, qualInput.trim(), "#10B981"); qualInput = "" } }) { Text("Add") }
        }
        state.qualities.forEach { q ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(q.name, color = MovizColors.TextSecondary)
                IconButton(onClick = { onDeleteQuality(q.id) }) { Icon(Icons.Default.Delete, contentDescription = null, tint = MovizColors.Red) }
            }
        }
    }
}
