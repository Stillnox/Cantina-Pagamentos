package com.cantina.pagamentos.presentation.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cantina.pagamentos.presentation.theme.CoresPastel
import com.cantina.pagamentos.presentation.theme.CoresTexto

/**
 * Menu dropdown para selecionar o filtro de clientes
 * Substitui a navegação por abas inferior
 */
@Composable
fun DropdownFilterMenu(
    filtroAtual: String,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // Mapeia o filtro para o texto e emoji correspondente
    val (titulo, emoji) = when (filtroAtual) {
        "todos" -> "Todos os Clientes" to "👥"
        "positivo" -> "Saldo Positivo" to "💰"
        "negativo" -> "Saldo Negativo" to "💸"
        "zerado" -> "Saldo Zerado" to "⚪"
        else -> "Clientes" to "👥"
    }

    Box(modifier = modifier) {
        // Botão que abre o dropdown
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            colors = CardDefaults.cardColors(
                containerColor = CoresPastel.VerdeMenta
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium,
                        color = CoresTexto.Principal
                    )
                }

                // Ícone de seta
                Text(
                    text = if (expanded) "⬆️" else "⬇️",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // Menu dropdown
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Opção: Todos
            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👥", style = MaterialTheme.typography.headlineSmall)
                        Column {
                            Text(
                                "Todos os Clientes",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Ver todos os clientes cadastrados",
                                style = MaterialTheme.typography.bodySmall,
                                color = CoresTexto.Secundario
                            )
                        }
                    }
                },
                onClick = {
                    navController.navigate("todos") {
                        popUpTo("todos") { inclusive = true }
                    }
                    expanded = false
                },
                colors = MenuDefaults.itemColors(
                    textColor = if (filtroAtual == "todos")
                        MaterialTheme.colorScheme.primary
                    else
                        CoresTexto.Principal
                )
            )

            HorizontalDivider()

            // Opção: Positivo
            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💰", style = MaterialTheme.typography.headlineSmall)
                        Column {
                            Text(
                                "Saldo Positivo",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Clientes com crédito disponível",
                                style = MaterialTheme.typography.bodySmall,
                                color = CoresTexto.Secundario
                            )
                        }
                    }
                },
                onClick = {
                    navController.navigate("positivo") {
                        popUpTo("todos")
                    }
                    expanded = false
                },
                colors = MenuDefaults.itemColors(
                    textColor = if (filtroAtual == "positivo")
                        MaterialTheme.colorScheme.primary
                    else
                        CoresTexto.Principal
                )
            )

            HorizontalDivider()

            // Opção: Negativo
            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💸", style = MaterialTheme.typography.headlineSmall)
                        Column {
                            Text(
                                "Saldo Negativo",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Clientes em débito",
                                style = MaterialTheme.typography.bodySmall,
                                color = CoresTexto.Secundario
                            )
                        }
                    }
                },
                onClick = {
                    navController.navigate("negativo") {
                        popUpTo("todos")
                    }
                    expanded = false
                },
                colors = MenuDefaults.itemColors(
                    textColor = if (filtroAtual == "negativo")
                        MaterialTheme.colorScheme.error
                    else
                        CoresTexto.Principal
                )
            )

            HorizontalDivider()

            // Opção: Zerado
            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚪", style = MaterialTheme.typography.headlineSmall)
                        Column {
                            Text(
                                "Saldo Zerado",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Clientes sem saldo",
                                style = MaterialTheme.typography.bodySmall,
                                color = CoresTexto.Secundario
                            )
                        }
                    }
                },
                onClick = {
                    navController.navigate("zerado") {
                        popUpTo("todos")
                    }
                    expanded = false
                },
                colors = MenuDefaults.itemColors(
                    textColor = if (filtroAtual == "zerado")
                        MaterialTheme.colorScheme.primary
                    else
                        CoresTexto.Principal
                )
            )
        }
    }
}
