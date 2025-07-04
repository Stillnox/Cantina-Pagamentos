package com.cantina.pagamentos.presentation.screens.clientes.lista

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cantina.pagamentos.data.models.ClienteFirebase
import com.cantina.pagamentos.presentation.theme.CoresBadges
import com.cantina.pagamentos.presentation.theme.CoresPastel
import com.cantina.pagamentos.presentation.theme.CoresTexto
import com.cantina.pagamentos.presentation.viewmodels.CantinaFirebaseViewModel
import com.cantina.pagamentos.presentation.components.cards.ClienteCard


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
    val context = LocalContext.current

    Scaffold(
        topBar = {
            ListaTopBar(
                titulo = listaState.titulo,
                viewModel = viewModel,
                filtro = filtro,
                context = context,
                onAddCliente = { navController.navigate("cadastro") }
            )
        }
    ) { paddingValues ->
        ListaContent(
            paddingValues = paddingValues,
            listaState = listaState,
            navController = navController
        )
    }
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
 * TopBar da tela de lista
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListaTopBar(
    titulo: String,
    viewModel: CantinaFirebaseViewModel,
    filtro: String,
    context: Context,
    onAddCliente: () -> Unit,
) {
    val isCarregando by viewModel.isCarregando.collectAsState()
    val saldoVisivel by viewModel.saldoVisivel.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()

    TopAppBar(
        title = {
            Column {
                Text(titulo)
                Text(
                    text = "Funcionário: ${viewModel.getNomeFuncionario()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoresPastel.CinzaPerola
                )
            }
        },
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CoresPastel.AzulSage,
            titleContentColor = CoresPastel.CinzaPerola,
            actionIconContentColor = CoresPastel.CinzaPerola
        ),
        actions = {
            ListaTopBarActions(
                isCarregando = isCarregando,
                saldoVisivel = saldoVisivel,
                isAdmin = isAdmin,
                viewModel = viewModel,
                filtro = filtro,
                context = context,
                onAddCliente = onAddCliente
            )
        }
    )
}

/**
 * Ações da TopBar da lista
 */
@Composable
private fun ListaTopBarActions(
    isCarregando: Boolean,
    saldoVisivel: Boolean,
    isAdmin: Boolean,
    viewModel: CantinaFirebaseViewModel,
    filtro: String,
    context: Context,
    onAddCliente: () -> Unit,
) {
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
        onClick = {
            println("🔥 [UI - Lista] Botão clicado! Estado atual: $saldoVisivel")
            viewModel.alternarVisibilidadeSaldo()
        }
    ) {
        Text(
            if (saldoVisivel) "👁️" else "👁️‍🗨️",
            style = MaterialTheme.typography.headlineMedium
        )
    }

    // Botão exportar PDF - APENAS PARA ADMINS
    if (isAdmin) {
        IconButton(onClick = {
            val uri = viewModel.gerarPdfListaClientes(context, filtro)
            if (uri != null) {
                viewModel.compartilharPdf(context, uri, "Relatório Cantina - $filtro")
            }
        }) {
            Text("📄", style = MaterialTheme.typography.headlineMedium)
        }
    }

    // Botão adicionar cliente
    IconButton(onClick = onAddCliente) {
        Text("➕", style = MaterialTheme.typography.headlineMedium)
    }
}

/**
 * Conteúdo principal da tela de lista
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
        ListaMainContent(
            paddingValues = paddingValues,
            listaState = listaState,
            navController = navController
        )
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
    BasicTextField(
        value = listaState.busca,
        onValueChange = listaState.onBuscaChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .onFocusChanged { focusState ->
                println("BasicTextField focused: ${focusState.isFocused}")
            },
        singleLine = true,
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = listaState.busca,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = remember { MutableInteractionSource() },
                label = { Text("Buscar por nome") },
                contentPadding = PaddingValues(top = 2.dp, bottom = 2.dp, start = 6.dp, end = 2.dp), // SEU PADDING DESEJADO
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CoresPastel.AzulSage,
                    unfocusedBorderColor = CoresPastel.VerdeMenta,
                    focusedLabelColor = CoresPastel.AzulSage,
                    unfocusedLabelColor = CoresPastel.VerdeMenta,
                    focusedContainerColor = CoresPastel.VerdeMenta,
                    focusedTextColor = CoresPastel.VerdeMenta
                    //unfocusedContainerColor = CoresPastel.VerdeMenta // Supondo que você tenha essa cor
                ),
            )
        }
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
