package com.cantina.pagamentos.core.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cantina.pagamentos.presentation.theme.CoresPastel
import com.cantina.pagamentos.presentation.theme.CoresTexto

/**
 * Barra de navegação inferior do aplicativo
 * Mostra as 4 abas principais: Todos, Positivo, Negativo e Zerado
 * Configurações foi movida para o topo da tela
 */
@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = CoresPastel.AzulSage,
        contentColor = CoresTexto.Principal
    ) {
        // Aba Todos - Lista todos os clientes
        NavigationBarItem(
            icon = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👥", style = MaterialTheme.typography.headlineMedium)
                    Text("Todos", style = MaterialTheme.typography.labelSmall)
                }
            },
            selected = currentRoute == "todos",
            onClick = {
                navController.navigate("todos") {
                    popUpTo("todos") { inclusive = true }
                }
            }
        )

        // Aba Saldo Positivo - Filtra clientes com saldo > 0
        NavigationBarItem(
            icon = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💰", style = MaterialTheme.typography.headlineMedium)
                    Text("Positivo", style = MaterialTheme.typography.labelSmall)
                }
            },
            selected = currentRoute == "positivo",
            onClick = {
                navController.navigate("positivo") {
                    popUpTo("todos")
                }
            }
        )

        // Aba Saldo Negativo - Filtra clientes com saldo < 0
        NavigationBarItem(
            icon = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💸", style = MaterialTheme.typography.headlineMedium)
                    Text("Negativo", style = MaterialTheme.typography.labelSmall)
                }
            },
            selected = currentRoute == "negativo",
            onClick = {
                navController.navigate("negativo") {
                    popUpTo("todos")
                }
            }
        )

        // Aba Saldo Zerado - Filtra clientes com saldo = 0
        NavigationBarItem(
            icon = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚪", style = MaterialTheme.typography.headlineMedium)
                    Text("Zerado", style = MaterialTheme.typography.labelSmall)
                }
            },
            selected = currentRoute == "zerado",
            onClick = {
                navController.navigate("zerado") {
                    popUpTo("todos")
                }
            }
        )
    }
}
