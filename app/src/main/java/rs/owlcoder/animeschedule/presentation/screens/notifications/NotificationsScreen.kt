package rs.owlcoder.animeschedule.presentation.screens.notifications

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import rs.owlcoder.animeschedule.presentation.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen() {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Obaveštenja", style = MaterialTheme.typography.titleLarge) },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        EmptyState(
            icon = Icons.Default.Notifications,
            title = "Nema obaveštenja",
            subtitle = "Ovde će se pojavljivati obaveštenja o novim epizodama",
            modifier = Modifier.padding(innerPadding)
        )
    }
}
