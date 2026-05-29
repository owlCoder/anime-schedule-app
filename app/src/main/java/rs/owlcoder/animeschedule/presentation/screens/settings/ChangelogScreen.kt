package rs.owlcoder.animeschedule.presentation.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class ChangelogEntry(
    val version: String,
    val date: String,
    val changes: List<String>
)

private val changelog = listOf(
    ChangelogEntry(
        version = "1.1",
        date = "Maj 2026",
        changes = listOf(
            "Novi ekran za podešavanje teme i boje akcenta",
            "AMOLED crna tema za bolju uštedu baterije",
            "Navbar pill sa blur efektom i aktivnim stanjem ikonica",
            "Ekran istorije promena",
            "Redizajn ekrana O aplikaciji",
            "Popravka vizuelnih grešaka u podešavanjima"
        )
    ),
    ChangelogEntry(
        version = "1.0.4",
        date = "April 2026",
        changes = listOf(
            "Popravka avatara koji se nije osvežavao nakon ponovne prijave",
            "Uklonjen neiskorišćen kod u prezentacionom sloju",
            "Stabilizacija OAuth toka za MAL prijavu"
        )
    ),
    ChangelogEntry(
        version = "1.0.3",
        date = "Mart 2026",
        changes = listOf(
            "Dodato edge-to-edge prikazivanje sadržaja",
            "Popravka ID rezolucije za anime detalje (AniList ↔ MAL)",
            "Reaktivni tok za MAL listu — ažuriranja odmah vidljiva"
        )
    ),
    ChangelogEntry(
        version = "1.0.2",
        date = "Februar 2026",
        changes = listOf(
            "Podrška za vremensku zonu u podešavanjima",
            "Prikaz rasporeda u lokalnoj vremenskoj zoni korisnika",
            "Dodati prazni ekrani za sve tabove"
        )
    ),
    ChangelogEntry(
        version = "1.0.1",
        date = "Januar 2026",
        changes = listOf(
            "Inicijalna podrška za MyAnimeList OAuth 2.0 + PKCE",
            "Pretraga anime-a putem AniList GraphQL API-ja",
            "Osnovna navigacija sa 5 tabova"
        )
    ),
    ChangelogEntry(
        version = "1.0.0",
        date = "Decembar 2025",
        changes = listOf(
            "Prvo javno izdanje",
            "Raspored emitovanja anime serija za danas, sutra i nedelju",
            "Prikaz u vremenskoj zoni uređaja"
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(onBack: () -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Istorija promena", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            item { Spacer(Modifier.height(12.dp)) }
            itemsIndexed(changelog) { index, entry ->
                ChangelogCard(entry = entry, expandedByDefault = index == 0)
                Spacer(Modifier.height(10.dp))
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ChangelogCard(entry: ChangelogEntry, expandedByDefault: Boolean) {
    var expanded by remember { mutableStateOf(expandedByDefault) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Verzija ${entry.version}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        entry.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                    entry.changes.forEach { change ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(16.dp)
                            )
                            Text(
                                change,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
