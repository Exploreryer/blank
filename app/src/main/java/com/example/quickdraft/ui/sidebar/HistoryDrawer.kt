package com.example.quickdraft.ui.sidebar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.quickdraft.data.local.NoteEntity
import com.example.quickdraft.domain.model.SyncState
import com.example.quickdraft.ui.DrawerTab
import com.example.quickdraft.ui.theme.BrandOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryDrawer(
    tab: DrawerTab,
    currentNotes: List<NoteEntity>,
    archivedNotes: List<NoteEntity>,
    onTabChange: (DrawerTab) -> Unit,
    onSelectNote: (NoteEntity) -> Unit,
    onToggleStar: (String) -> Unit,
    onArchiveToggle: (String) -> Unit,
    onSettings: () -> Unit
) {
    val notes = if (tab == DrawerTab.CURRENT) currentNotes else archivedNotes
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(top = 8.dp)
    ) {
        Text(
            text = "Blank",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 8.dp)
        )
        TabRow(
            selectedTabIndex = if (tab == DrawerTab.CURRENT) 0 else 1,
            containerColor = Color.White,
            divider = {},
            indicator = { tabPositions ->
                val index = if (tab == DrawerTab.CURRENT) 0 else 1
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[index])
                        .zIndex(1f),
                    color = Color(0xFF1F1F1F)
                )
            }
        ) {
            Tab(
                selected = tab == DrawerTab.CURRENT,
                onClick = { onTabChange(DrawerTab.CURRENT) },
                text = { Text("当前", color = Color(0xFF202020), fontWeight = if (tab == DrawerTab.CURRENT) FontWeight.Medium else FontWeight.Normal) },
                selectedContentColor = Color(0xFF202020),
                unselectedContentColor = Color(0xFF7A7A7A)
            )
            Tab(
                selected = tab == DrawerTab.ARCHIVED,
                onClick = { onTabChange(DrawerTab.ARCHIVED) },
                text = { Text("归档", color = Color(0xFF202020), fontWeight = if (tab == DrawerTab.ARCHIVED) FontWeight.Medium else FontWeight.Normal) },
                selectedContentColor = Color(0xFF202020),
                unselectedContentColor = Color(0xFF7A7A7A)
            )
        }
        HorizontalDivider(color = Color(0xFFEDEDED))
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(notes, key = { it.id }) { note ->
                NoteItemRow(
                    note = note,
                    onSelect = { onSelectNote(note) },
                    onToggleStar = { onToggleStar(note.id) },
                    onArchiveToggle = { onArchiveToggle(note.id) }
                )
                HorizontalDivider(
                    color = Color(0xFFF1F2F4),
                    thickness = 1.dp,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = "设置", tint = BrandOrange)
            }
        }
    }
}

@Composable
private fun NoteItemRow(
    note: NoteEntity,
    onSelect: () -> Unit,
    onToggleStar: () -> Unit,
    onArchiveToggle: () -> Unit
) {
    val titleLine = note.content.lineSequence().firstOrNull().orEmpty().ifBlank { "空白记录" }
    val time = remember(note.updatedAt) {
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(note.updatedAt))
    }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onToggleStar()
                    false
                }

                SwipeToDismissBoxValue.EndToStart -> {
                    onArchiveToggle()
                    false
                }

                SwipeToDismissBoxValue.Settled -> true
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (direction == SwipeToDismissBoxValue.StartToEnd) {
                            Icon(
                                imageVector = if (note.isStarred) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = "星标",
                                tint = BrandOrange
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        if (direction == SwipeToDismissBoxValue.EndToStart) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = "归档",
                                tint = Color(0xFF667085)
                            )
                        }
                    }
                }
            }
        }
    ) {
        NavigationDrawerItem(
            label = {
                Column {
                    Text(
                        text = titleLine,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (note.syncState == SyncState.CONFLICT) "$time  冲突副本" else time,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (note.syncState == SyncState.CONFLICT) BrandOrange else Color.Gray
                    )
                }
            },
            selected = false,
            onClick = onSelect,
            icon = null,
            badge = {
                Icon(
                    imageVector = if (note.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "星标状态",
                    tint = if (note.isStarred) BrandOrange else Color(0xFFBDBDBD)
                )
            },
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent,
                selectedContainerColor = Color.Transparent
            )
        )
    }
}
