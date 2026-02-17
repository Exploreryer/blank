package com.example.blank.ui.sidebar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.blank.data.local.NoteEntity
import com.example.blank.domain.model.SyncState
import com.example.blank.ui.DrawerTab
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryDrawer(
    tab: DrawerTab,
    currentNotes: List<NoteEntity>,
    archivedNotes: List<NoteEntity>,
    onTabChange: (DrawerTab) -> Unit,
    onSelectNote: (NoteEntity) -> Unit,
    onToggleStar: (String) -> Unit,
    onSetArchived: (id: String, archived: Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(top = 8.dp)
    ) {
        Surface(
            color = Color(0xFFEFF2F6),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DrawerTabChip(
                    text = "当前",
                    selected = tab == DrawerTab.CURRENT,
                    modifier = Modifier.weight(1f),
                    onClick = { onTabChange(DrawerTab.CURRENT) }
                )
                DrawerTabChip(
                    text = "归档",
                    selected = tab == DrawerTab.ARCHIVED,
                    modifier = Modifier.weight(1f),
                    onClick = { onTabChange(DrawerTab.ARCHIVED) }
                )
            }
        }
        HorizontalDivider(color = Color(0xFFEDEDED))

        val activeNotes = if (tab == DrawerTab.CURRENT) currentNotes else archivedNotes
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(activeNotes, key = { it.id }) { note ->
                NoteItemRow(
                    note = note,
                    onSelect = { onSelectNote(note) },
                    onToggleStar = { onToggleStar(note.id) },
                    onArchive = { onSetArchived(note.id, true) },
                    onUnarchive = { onSetArchived(note.id, false) },
                    onDelete = { onDelete(note.id) },
                    modifier = Modifier.animateItem()
                )
            }
        }

        HorizontalDivider(color = Color(0xFFEDEDED))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = "设置", tint = Color(0xFF475467))
            }
        }
    }
}

@Composable
private fun DrawerTabChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .background(if (selected) Color(0xFFDDE4EE) else Color.Transparent)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color(0xFF0F172A) else Color(0xFF667085),
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteItemRow(
    note: NoteEntity,
    onSelect: () -> Unit,
    onToggleStar: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleLine = note.content.lineSequence().firstOrNull().orEmpty().ifBlank { "空白记录" }
    val time = remember(note.updatedAt) {
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(note.updatedAt))
    }
    val meta = remember(note.updatedAt, note.syncState, note.isStarred, note.isArchived) {
        buildString {
            append(time)
            if (note.syncState == SyncState.CONFLICT) append("  · 冲突副本")
            if (note.isStarred) append("  · 星标")
            if (note.isArchived) append("  · 已归档")
        }
    }
    var menuExpanded by remember(note.id) { mutableStateOf(false) }
    var showDeleteConfirm by remember(note.id) { mutableStateOf(false) }

    Surface(
        color = Color.White,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFFE4E7EC),
                shape = MaterialTheme.shapes.large
            )
            .combinedClickable(
                onClick = onSelect,
                onLongClick = { menuExpanded = true }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleLine,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (note.syncState == SyncState.CONFLICT) Color(0xFF7A2323) else Color(0xFF667085)
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = "更多操作",
                        tint = Color(0xFF475467)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = Color(0xFFF8FAFC),
                    tonalElevation = 0.dp,
                    shadowElevation = 4.dp
                ) {
                    DropdownMenuItem(
                        text = { Text(if (note.isArchived) "取消归档" else "归档") },
                        onClick = {
                            menuExpanded = false
                            if (note.isArchived) onUnarchive() else onArchive()
                        },
                        leadingIcon = { Icon(Icons.Outlined.Archive, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (note.isStarred) "取消星标" else "加星标") },
                        onClick = {
                            menuExpanded = false
                            onToggleStar()
                        },
                        leadingIcon = {
                            Icon(
                                if (note.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = null
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = Color(0xFF7A2323)) },
                        onClick = {
                            menuExpanded = false
                            showDeleteConfirm = true
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFF7A2323))
                        }
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color(0xFFF8FAFC),
            title = { Text("删除记录") },
            text = { Text("删除后不可恢复") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) { Text("删除", color = Color(0xFFB42318)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消", color = Color(0xFF475467)) }
            }
        )
    }
}
