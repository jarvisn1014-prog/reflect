package com.nish.reflect.ui.screens.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nish.reflect.ReflectViewModel
import com.nish.reflect.engine.MoodAggregator
import com.nish.reflect.ui.components.EmptyState
import com.nish.reflect.ui.components.PillButton
import com.nish.reflect.ui.components.ThemePill
import com.nish.reflect.ui.components.WeeklyMoodChart
import com.nish.reflect.ui.theme.DigestHeadlineStyle

@Composable
fun InsightsScreen(
    viewModel: ReflectViewModel
) {
    val stats by viewModel.weeklyStats.collectAsState()
    val headline by viewModel.digestHeadline.collectAsState()
    val isGenerating by viewModel.isGeneratingDigest.collectAsState()
    val error by viewModel.error.collectAsState()
    val entries by viewModel.entries.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (stats == null && headline == null) {
                // Initial state
                if (entries.size < 3) {
                    EmptyState(
                        title = "Write a few entries this week",
                        subtitle = "Your weekly reflection will appear here. One more and you'll have your first insight."
                    )
                } else {
                    EmptyState(
                        title = "Generate your weekly reflection",
                        subtitle = "Tap below to see patterns from your week"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PillButton(
                        text = "Generate Weekly Reflection",
                        onClick = { viewModel.generateWeeklyDigest() }
                    )
                }
            } else {
                stats?.let { s ->
                    // Zone 1: Headline
                    headline?.let { h ->
                        Text(
                            text = h,
                            style = DigestHeadlineStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // Zone 2: 7-bar Canvas chart
                    WeeklyMoodChart(
                        dayLabels = s.dayLabels,
                        dayValences = s.dayValences
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Zone 3: Theme pills
                    if (s.topThemes.isNotEmpty()) {
                        Text(
                            text = "Themes this week",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            s.topThemes.forEach { (theme, count) ->
                                ThemePill(label = theme, count = count)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    PillButton(
                        text = if (isGenerating) "Generating..." else "Refresh Reflection",
                        onClick = { viewModel.generateWeeklyDigest() },
                        enabled = !isGenerating
                    )
                }
            }

            error?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}