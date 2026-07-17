package com.nish.reflect.ui.screens.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nish.reflect.ui.theme.Accent

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MoodBottomSheet(
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
    onMoodSelected: (mood: String, valence: Float, energy: Float, tags: String?) -> Unit
) {
    var selectedMood by remember { mutableStateOf<String?>(null) }
    var selectedValence by remember { mutableStateOf(0f) }
    var showTags by remember { mutableStateOf(false) }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }

    val moods = listOf("Rough" to -1.0f, "Low" to -0.5f, "Okay" to 0f, "Good" to 0.5f, "Great" to 1.0f)
    val tagOptions = listOf("anxious", "calm", "grateful", "frustrated", "lonely", "motivated", "sad", "content", "overwhelmed", "hopeful", "tired", "joyful")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("How are you feeling?", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // Valence scale — 5 tappable circles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                moods.forEach { (label, valence) ->
                    val isSelected = selectedMood == label
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) Accent else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(56.dp)
                            .clickable {
                                selectedMood = label
                                selectedValence = valence
                            }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Optional tags — progressive disclosure
            if (selectedMood != null) {
                Text(
                    text = if (showTags) "Hide details ▲" else "Add detail ▼",
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent,
                    modifier = Modifier.clickable { showTags = !showTags }
                )

                if (showTags) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tagOptions.forEach { tag ->
                            val isSelected = tag in selectedTags
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    selectedTags = if (isSelected) selectedTags - tag else selectedTags + tag
                                }
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Done button — saves mood with tags
                Surface(
                    shape = RoundedCornerShape(25.dp),
                    color = Accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable {
                            val tags = if (selectedTags.isEmpty()) null else selectedTags.joinToString(",")
                            onMoodSelected(selectedMood!!, selectedValence, 0.5f, tags)
                        }
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}