package com.nish.reflect.ui.screens.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nish.reflect.ReflectViewModel
import com.nish.reflect.ui.components.PillButton
import com.nish.reflect.ui.components.ProgressStrip
import com.nish.reflect.ui.components.SentimentPill
import com.nish.reflect.ui.components.ThemePill
import com.nish.reflect.ui.theme.DigestHeadlineStyle
import com.nish.reflect.ui.theme.JournalBodyStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: ReflectViewModel,
    onOpenList: () -> Unit,
    onOpenSettings: () -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp)
) {
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val aiEnabled by viewModel.aiEnabled.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val error by viewModel.error.collectAsState()

    var entryText by remember { mutableStateOf("") }
    var showMoodSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Find today's entry if it exists
    val todayStart = run {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    val todayEntry = entries.firstOrNull { it.createdAt >= todayStart }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today") },
                actions = {
                    IconButton(onClick = onOpenList) {
                        Icon(Icons.Default.Edit, contentDescription = "Journal")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showMoodSheet = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Mood", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(contentPadding)
                .padding(horizontal = 16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // If today's entry exists, show it; otherwise show editor
            if (todayEntry != null && entryText.isBlank()) {
                // Show today's entry with AI pills
                Text(
                    text = todayEntry.content,
                    style = JournalBodyStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // AI pills
                todayEntry.aiEmotion?.let { emotion ->
                    val isCorrected = todayEntry.userCorrectedEmotion != null
                    SentimentPill(
                        emotion = todayEntry.userCorrectedEmotion ?: emotion,
                        isCorrected = isCorrected,
                        onLongClick = { /* TODO: correction dialog */ }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                todayEntry.aiThemes?.split(",")?.forEach { theme ->
                    ThemePill(label = theme.trim(), count = 1)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                todayEntry.aiReflectionPrompt?.let { prompt ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = prompt,
                        style = DigestHeadlineStyle,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                // Editor
                OutlinedTextField(
                    value = entryText,
                    onValueChange = { entryText = it },
                    placeholder = {
                        Text(
                            "What's on your mind today?",
                            style = JournalBodyStyle.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    textStyle = JournalBodyStyle.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    minLines = 5
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isAnalyzing) {
                ProgressStrip()
            }

            error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save button
            if (entryText.isNotBlank()) {
                PillButton(
                    text = if (aiEnabled) "Save & Reflect" else "Save",
                    onClick = {
                        viewModel.saveEntry(entryText)
                        entryText = ""
                    }
                )
                if (aiEnabled) {
                    Text(
                        text = "AI will extract themes and mood from your entry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Mood check-in bottom sheet
    if (showMoodSheet) {
        MoodBottomSheet(
            sheetState = sheetState,
            onDismiss = { showMoodSheet = false },
            onMoodSelected = { mood, valence, energy, tags ->
                viewModel.logMood(mood, valence, energy, tags)
                showMoodSheet = false
            }
        )
    }
}