package com.myhebnu.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myhebnu.ui.home.components.HomeCardPanel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Greeting section (top ~45% of screen)
        Spacer(Modifier.weight(0.20f))
        GreetingSection(name = uiState.studentName)
        Spacer(Modifier.weight(0.35f))

        // Card panel (bottom ~45%)
        HomeCardPanel(
            uiState = uiState,
            onNavigate = onNavigate
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GreetingSection(name: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // "早上好" part
        val greetingWord = extractGreeting(name)

        // Split: greeting word in onSurface, name in primary
        if (name.isNotEmpty() && greetingWord.isNotEmpty()) {
            Text(
                text = buildString {
                    append(greetingWord)
                    append("，")
                },
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = name,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        } else {
            Text(
                text = greetingWord.ifEmpty { "你好" },
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun extractGreeting(full: String): String {
    // If the full text is like "早上好，刘泳", extract "早上好"
    // Otherwise just return the full text
    val separators = listOf("，", ",")
    for (sep in separators) {
        val idx = full.indexOf(sep)
        if (idx > 0) return full.substring(0, idx)
    }
    return full
}
