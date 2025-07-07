package com.cantina.pagamentos.presentation.screens.clientes.lista

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cantina.pagamentos.data.models.ClienteFirebase
import com.cantina.pagamentos.presentation.components.cards.ClienteCard
import com.cantina.pagamentos.presentation.theme.CoresBadges
import com.cantina.pagamentos.presentation.theme.CoresPastel
import com.cantina.pagamentos.presentation.theme.CoresTexto
import com.cantina.pagamentos.presentation.viewmodels.CantinaFirebaseViewModel

/**
 * Tela que exibe a lista de clientes filtrada
 * @param filtro "todos", "positivo", "negativo" ou "zerado"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaListaClientesFirebase(
    navController: NavHostController,
    viewModel: CantinaFirebaseViewModel,
    filtro: String,
) {
    val listaState = rememberListaState(viewModel, filtro)

    Scaffold(
        topBar = {
            ListaTopBar(
                filtro = filtro,
                viewModel = viewModel,
                navController = navController
            )
        },
        // FAB para adicionar cliente
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("cadastro") },
                containerColor = CoresPastel.VerdeMenta,
                contentColor = CoresTexto.Principal,
                modifier = Modifier.size(52.dp)
            ) {
                Text(
                    "✚",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        ListaContent(
            paddingValues = paddingValues,
            listaState = listaState,
            navController = navController
        )
    }
}


/**
 * TopBar com dropdown integrado
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListaTopBar(
    filtro: String,
    viewModel: CantinaFirebaseViewModel,
    navController: NavHostController,
) {
    val isCarregando by viewModel.isCarregando.collectAsState()
    val saldoVisivel by viewModel.saldoVisivel.collectAsState()
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Define o título e emoji baseado no filtro
    val (titulo, emoji) = when (filtro) {
        "todos" -> "Todos os Clientes" to "👥"
        "positivo" -> "Saldo Positivo" to "💰"
        "negativo" -> "Saldo Negativo" to "💸"
        "zerado" -> "Saldo Zerado" to "⚪"
        else -> "Clientes" to "👥"
    }

    TopAppBar(
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(emoji)
                    Text(
                        text = titulo,
                        color = CoresTexto.Suave)

                }
                Text(
                    text = viewModel.getNomeFuncionario(),
                    style = MaterialTheme.typography.bodySmall,
                    color = CoresTexto.Suave
                )
            }
        },
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CoresPastel.AzulSage,
            titleContentColor = CoresPastel.CinzaPerola,
            actionIconContentColor = CoresPastel.CinzaPerola
        ),
        navigationIcon = {
            // Ícone do menu dropdown
            Box {
                IconButton(onClick = { dropdownExpanded = true }) {
                    Text("☰", style = MaterialTheme.typography.headlineMedium, color = CoresTexto.Suave)
                }

                // Menu dropdown
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface),

                    ) {
                    // Opção: Todos
                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("👥", style = MaterialTheme.typography.titleLarge)
                                Text("Todos os Clientes", color = if (filtro == "todos") Color(0xFF1976D2) else CoresTexto.Suave)
                            }
                        },
                        onClick = {
                            navController.navigate("todos") {
                                popUpTo("todos") { inclusive = true }
                            }
                            dropdownExpanded = false
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = if (filtro == "todos")
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
                                Text("💰", style = MaterialTheme.typography.titleLarge)
                                Text("Saldo Positivo", color = if (filtro == "positivo") Color(0xFF1976D2) else CoresTexto.Suave)
                            }
                        },
                        onClick = {
                            navController.navigate("positivo") {
                                popUpTo("todos")
                            }
                            dropdownExpanded = false
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = if (filtro == "positivo")
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
                                Text("💸", style = MaterialTheme.typography.titleLarge)
                                Text("Saldo Negativo", color = if (filtro == "negativo") Color(0xFF1976D2) else CoresTexto.Suave)
                            }
                        },
                        onClick = {
                            navController.navigate("negativo") {
                                popUpTo("todos")
                            }
                            dropdownExpanded = false
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = if (filtro == "negativo")
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
                                Text("⚪", style = MaterialTheme.typography.titleLarge)
                                Text("Saldo Zerado", color = if (filtro == "zerado") Color(0xFF1976D2) else CoresTexto.Suave)
                            }
                        },
                        onClick = {
                            navController.navigate("zerado") {
                                popUpTo("todos")
                            }
                            dropdownExpanded = false
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = if (filtro == "zerado")
                                MaterialTheme.colorScheme.primary
                            else
                                CoresTexto.Principal
                        )
                    )
                }
            }
        },
        actions = {
            // Loading indicator
            if (isCarregando) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp),
                    strokeWidth = 2.dp
                )
            }

            // Botão de visualizar/ocultar saldo
            IconButton(
                onClick = { viewModel.alternarVisibilidadeSaldo() }
            ) {
                Text(
                    if (saldoVisivel) "👁️" else "👁️‍🗨️",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Botão de configurações
            IconButton(
                onClick = { navController.navigate("configuracoes") }
            ) {
                Text("⚙️", style = MaterialTheme.typography.headlineMedium)
            }
        }
    )
}

/**
 * Estado da tela de lista de clientes
 */
@Composable
private fun rememberListaState(
    viewModel: CantinaFirebaseViewModel,
    filtro: String,
): ListaState {
    var busca by remember { mutableStateOf("") }
    val clientes by viewModel.clientes.collectAsState()
    val isCarregando by viewModel.isCarregando.collectAsState()
    val saldoVisivel by viewModel.saldoVisivel.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()

    val clientesFiltrados = viewModel.buscarClientesPorNome(busca).filter { cliente ->
        when (filtro) {
            "positivo" -> cliente.saldo > 0
            "negativo" -> cliente.saldo < 0
            "zerado" -> cliente.saldo == 0.0
            else -> true
        }
    }.sortedBy { it.nomeCompleto.lowercase() }

    val titulo = when (filtro) {
        "todos" -> "Todos os Clientes"
        "positivo" -> "Saldo Positivo"
        "negativo" -> "Saldo Negativo"
        "zerado" -> "Saldo Zerado"
        else -> "Clientes"
    }

    return ListaState(
        busca = busca,
        onBuscaChange = { busca = it },
        clientes = clientes,
        clientesFiltrados = clientesFiltrados,
        isCarregando = isCarregando,
        saldoVisivel = saldoVisivel,
        isAdmin = isAdmin,
        titulo = titulo
    )
}

/**
 * Conteúdo principal da lista
 */
@Composable
private fun ListaContent(
    paddingValues: PaddingValues,
    listaState: ListaState,
    navController: NavHostController,
) {
    if (listaState.isCarregando && listaState.clientes.isEmpty()) {
        ListaLoadingScreen()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CoresPastel.CinzaPerola)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Campo de busca
            ListaSearchField(listaState = listaState)

            Spacer(modifier = Modifier.height(8.dp))

            // Contador e badge
            ListaHeader(listaState = listaState)

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de clientes
            ListaClientes(
                clientes = listaState.clientesFiltrados,
                saldoVisivel = listaState.saldoVisivel,
                navController = navController
            )
        }
    }
}

/**
 * Tela de carregamento da lista
 */
@Composable
private fun ListaLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Conteúdo principal da lista
 */
@Composable
private fun ListaMainContent(
    paddingValues: PaddingValues,
    listaState: ListaState,
    navController: NavHostController,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CoresPastel.CinzaPerola)
            .padding(paddingValues)
            .padding(6.dp)
    ) {
        ListaSearchField(listaState = listaState)
        Spacer(modifier = Modifier.height(8.dp))
        ListaHeader(listaState = listaState)
        Spacer(modifier = Modifier.height(16.dp))
        ListaClientes(
            clientes = listaState.clientesFiltrados,
            saldoVisivel = listaState.saldoVisivel,
            navController = navController
        )
    }
}

/** Campo de busca da lista **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListaSearchField(listaState: ListaState) {
    OutlinedTextField(
        value = listaState.busca,
        onValueChange = listaState.onBuscaChange,
        label = { Text("Buscar por nome") },  // Voltando para label
        modifier = Modifier
            .fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            // Borda
            focusedBorderColor = CoresPastel.AzulSage,
            unfocusedBorderColor = CoresPastel.VerdeMenta,

            // Label (texto "Buscar por nome")
            focusedLabelColor = CoresPastel.AzulSage,
            unfocusedLabelColor = CoresPastel.VerdeMenta,

            // FUNDO - cores bem contrastantes! 🎯
            // Verde bem claro quando focado (digitando)
            focusedContainerColor = Color(0xFFE8F6F0),
            // Transparente quando não focado
            unfocusedContainerColor = Color.Transparent,

            // Texto que você digita - PRETO FORTE para contrastar
            focusedTextColor = Color(0xFF000000),  // Preto puro
            unfocusedTextColor = Color(0xFF000000),  // Preto puro

            // Cursor - PRETO FORTE
            cursorColor = Color(0xFF000000)  // Preto puro
        )
    )
}
/** Cabeçalho da lista com contador e badge **/
@Composable
private fun ListaHeader(listaState: ListaState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${listaState.clientesFiltrados.size} cliente(s)",
            style = MaterialTheme.typography.bodyMedium,
            color = CoresTexto.Secundario
        )

        if (listaState.isAdmin) {
            Badge(containerColor = CoresBadges.Admin) {
                Text(
                    "ADMIN",
                    color = CoresTexto.Principal
                )
            }
        }
    }
}

// presentation/screens/clientes/lista/ListaClientesScreen.kt

// ... outras funções ...

/**
 * Lista de clientes
 */
@Composable
private fun ListaClientes(
    clientes: List<ClienteFirebase>,
    saldoVisivel: Boolean,
    navController: NavHostController
) {
    LazyColumn {
        items(
            items = clientes,
            key = { it.id }
        ) { cliente ->
            ClienteCard(
                cliente = cliente,
                saldoVisivel = saldoVisivel,
                onClick = { navController.navigate("cliente/${cliente.id}") }
            )
        }
    }
}

/**
 * Classe para gerenciar o estado da lista
 */
private data class ListaState(
    val busca: String,
    val onBuscaChange: (String) -> Unit,
    val clientes: List<ClienteFirebase>,
    val clientesFiltrados: List<ClienteFirebase>,
    val isCarregando: Boolean,
    val saldoVisivel: Boolean,
    val isAdmin: Boolean,
    val titulo: String,
)
