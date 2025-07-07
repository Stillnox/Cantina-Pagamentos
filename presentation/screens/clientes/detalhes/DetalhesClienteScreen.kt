package com.cantina.pagamentos.presentation.screens.clientes.detalhes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.cantina.pagamentos.core.utils.Constants as Const
import com.cantina.pagamentos.data.models.ClienteFirebase
import com.cantina.pagamentos.data.models.TransacaoFirebase
import com.cantina.pagamentos.presentation.components.common.CampoMonetario
import com.cantina.pagamentos.presentation.components.dialogs.CreditoDialog
import com.cantina.pagamentos.presentation.components.dialogs.DialogAlterarLimite
import com.cantina.pagamentos.presentation.components.dialogs.DialogLimiteExcedido
import com.cantina.pagamentos.presentation.theme.CoresPastel
import com.cantina.pagamentos.presentation.theme.CoresSaldo
import com.cantina.pagamentos.presentation.theme.CoresTexto
import com.cantina.pagamentos.presentation.viewmodels.CantinaFirebaseViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale


/**
 * Tela que exibe detalhes de um cliente específico
 * Permite realizar operações de crédito/débito e visualizar histórico
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaClienteFirebase(
    navController: NavHostController,
    viewModel: CantinaFirebaseViewModel,
    clienteId: String,
    isDualPane: Boolean = false,
) {
    val clienteState = rememberClienteState(viewModel)
    val context = LocalContext.current

    // Carrega o cliente e transações
    LaunchedEffect(clienteId, clienteState.clientes) {
        val clienteEncontrado = clienteState.clientes.find { it.id == clienteId }
        clienteState.onClienteChange(clienteEncontrado)

        if (clienteEncontrado != null) {
            viewModel.carregarTransacoes(clienteId)
        }
    }

    // Se não encontrou o cliente, volta
    if (clienteState.cliente == null && !clienteState.isCarregando) {
        LaunchedEffect(clienteId) {
            delay(300)
            if (!isDualPane) navController.popBackStack()
        }
        return
    }

    Scaffold(
        topBar = {
            ClienteTopBar(
                cliente = clienteState.cliente,
                isCarregando = clienteState.isCarregando,
                isAdmin = clienteState.isAdmin,
                onBackClick = {
                    if (!isDualPane) navController.popBackStack()
                },
                onExportClick = {
                    val uri = viewModel.gerarPdfExtratoCliente(context, clienteId)
                    if (uri != null) {
                        viewModel.compartilharPdf(context, uri, "Extrato - ${clienteState.cliente?.nomeCompleto}")
                    }
                },
                onAddCreditClick = { clienteState.onShowAddCreditDialogChange(true) },
                onConfigClick = { clienteState.onShowLimiteDialogChange(true) }
            )
        }
    ) { paddingValues ->
        if (clienteState.cliente != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Saldo do cliente
                item {
                    ClienteSaldoCard(
                        cliente = clienteState.cliente,
                        saldoVisivel = clienteState.saldoVisivel
                    )
                }

                // Área de compra
                stickyHeader {
                    ClienteCompraCard(
                        cliente = clienteState.cliente,
                        valorOperacao = clienteState.valorOperacao,
                        onValorOperacaoChange = clienteState.onValorOperacaoChange,
                        saldoVisivel = clienteState.saldoVisivel,
                        isCarregando = clienteState.isCarregando,
                        onConfirmCompra = { valor ->
                            handleCompra(
                                clienteState = clienteState,
                                viewModel = viewModel,
                                clienteId = clienteId,
                                valor = valor
                            )
                        }
                    )
                }

                // Cabeçalho do extrato
                item {
                    ClienteExtratoHeader(
                        isAdmin = clienteState.isAdmin,
                        onRemoveClick = { clienteState.onShowRemoveDialogChange(true) }
                    )
                }

                // Lista de transações
                if (clienteState.transacoes.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📝", style = MaterialTheme.typography.headlineMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Nenhuma transação realizada ainda",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "As compras e créditos aparecerão aqui",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = clienteState.transacoes,
                        key = { transacao -> transacao.id }
                    ) { transacao ->
                        ClienteTransacaoItem(transacao = transacao)
                    }
                }

                // Espaço no final para navegação
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    ClienteDialogs(
        clienteState = clienteState,
        viewModel = viewModel,
        clienteId = clienteId,
        navController = navController,
        isDualPane = isDualPane
    )
}

/**
 * Versão simplificada para Dual Pane - sem navegação
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaClienteFirebaseDual(
    viewModel: CantinaFirebaseViewModel,
    clienteId: String,
    onClose: () -> Unit,
) {
    val clienteState = rememberClienteState(viewModel)
    val context = LocalContext.current

    // Carrega o cliente e transações
    LaunchedEffect(clienteId, clienteState.clientes) {
        val clienteEncontrado = clienteState.clientes.find { it.id == clienteId }
        clienteState.onClienteChange(clienteEncontrado)

        if (clienteEncontrado != null) {
            viewModel.carregarTransacoes(clienteId)
        }
    }

    // Se não encontrou o cliente, limpa a seleção
    if (clienteState.cliente == null && !clienteState.isCarregando) {
        LaunchedEffect(clienteId) {
            delay(300)
            onClose()
        }
        return
    }

    Scaffold(
        topBar = {
            ClienteTopBarDual(
                cliente = clienteState.cliente,
                isCarregando = clienteState.isCarregando,
                isAdmin = clienteState.isAdmin,
                onExportClick = {
                    val uri = viewModel.gerarPdfExtratoCliente(context, clienteId)
                    if (uri != null) {
                        viewModel.compartilharPdf(context, uri, "Extrato - ${clienteState.cliente?.nomeCompleto}")
                    }
                },
                onAddCreditClick = { clienteState.onShowAddCreditDialogChange(true) },
                onConfigClick = { clienteState.onShowLimiteDialogChange(true) }
            )
        }
    ) { paddingValues ->
        if (clienteState.cliente != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Saldo do cliente
                item {
                    ClienteSaldoCard(
                        cliente = clienteState.cliente,
                        saldoVisivel = clienteState.saldoVisivel
                    )
                }

                // Área de compra
                stickyHeader {
                    ClienteCompraCard(
                        cliente = clienteState.cliente,
                        valorOperacao = clienteState.valorOperacao,
                        onValorOperacaoChange = clienteState.onValorOperacaoChange,
                        saldoVisivel = clienteState.saldoVisivel,
                        isCarregando = clienteState.isCarregando,
                        onConfirmCompra = { valor ->
                            handleCompra(
                                clienteState = clienteState,
                                viewModel = viewModel,
                                clienteId = clienteId,
                                valor = valor
                            )
                        }
                    )
                }

                // Cabeçalho do extrato
                item {
                    ClienteExtratoHeader(
                        isAdmin = clienteState.isAdmin,
                        onRemoveClick = { clienteState.onShowRemoveDialogChange(true) }
                    )
                }

                // Lista de transações
                if (clienteState.transacoes.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📝", style = MaterialTheme.typography.headlineMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Nenhuma transação realizada ainda",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = clienteState.transacoes,
                        key = { transacao -> transacao.id }
                    ) { transacao ->
                        ClienteTransacaoItem(transacao = transacao)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Dialogs com onClose ao invés de navController
    ClienteDialogsDual(
        clienteState = clienteState,
        viewModel = viewModel,
        clienteId = clienteId,
        onClose = onClose
    )
}

@Composable
private fun rememberClienteState(viewModel: CantinaFirebaseViewModel): ClienteState {
    var cliente by remember { mutableStateOf<ClienteFirebase?>(null) }
    var valorOperacao by remember { mutableStateOf("") }
    var showCreditoDialog by remember { mutableStateOf(false) }
    var showDebitoDialog by remember { mutableStateOf(false) }
    var showAddCreditDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showLimiteDialog by remember { mutableStateOf(false) }
    var showLimiteExcedidoDialog by remember { mutableStateOf(false) }
    var mensagemLimiteExcedido by remember { mutableStateOf("") }

    val isCarregando by viewModel.isCarregando.collectAsState()
    val clientes by viewModel.clientes.collectAsState()
    val transacoes by viewModel.transacoesCliente.collectAsState()
    val saldoVisivel by viewModel.saldoVisivel.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()

    return ClienteState(
        cliente = cliente,
        onClienteChange = { cliente = it },
        valorOperacao = valorOperacao,
        onValorOperacaoChange = { valorOperacao = it },
        showCreditoDialog = showCreditoDialog,
        onShowCreditoDialogChange = { showCreditoDialog = it },
        showDebitoDialog = showDebitoDialog,
        onShowDebitoDialogChange = { showDebitoDialog = it },
        showAddCreditDialog = showAddCreditDialog,
        onShowAddCreditDialogChange = { showAddCreditDialog = it },
        showRemoveDialog = showRemoveDialog,
        onShowRemoveDialogChange = { showRemoveDialog = it },
        showLimiteDialog = showLimiteDialog,
        onShowLimiteDialogChange = { showLimiteDialog = it },
        showLimiteExcedidoDialog = showLimiteExcedidoDialog,
        onShowLimiteExcedidoDialogChange = { showLimiteExcedidoDialog = it },
        mensagemLimiteExcedido = mensagemLimiteExcedido,
        onMensagemLimiteExcedidoChange = { mensagemLimiteExcedido = it },
        isCarregando = isCarregando,
        clientes = clientes,
        transacoes = transacoes,
        saldoVisivel = saldoVisivel,
        isAdmin = isAdmin
    )
}

private data class ClienteState(
    val cliente: ClienteFirebase?,
    val onClienteChange: (ClienteFirebase?) -> Unit,
    val valorOperacao: String,
    val onValorOperacaoChange: (String) -> Unit,
    val showCreditoDialog: Boolean,
    val onShowCreditoDialogChange: (Boolean) -> Unit,
    val showDebitoDialog: Boolean,
    val onShowDebitoDialogChange: (Boolean) -> Unit,
    val showAddCreditDialog: Boolean,
    val onShowAddCreditDialogChange: (Boolean) -> Unit,
    val showRemoveDialog: Boolean,
    val onShowRemoveDialogChange: (Boolean) -> Unit,
    val showLimiteDialog: Boolean,
    val onShowLimiteDialogChange: (Boolean) -> Unit,
    val showLimiteExcedidoDialog: Boolean,
    val onShowLimiteExcedidoDialogChange: (Boolean) -> Unit,
    val mensagemLimiteExcedido: String,
    val onMensagemLimiteExcedidoChange: (String) -> Unit,
    val isCarregando: Boolean,
    val clientes: List<ClienteFirebase>,
    val transacoes: List<TransacaoFirebase>,
    val saldoVisivel: Boolean,
    val isAdmin: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClienteTopBar(
    cliente: ClienteFirebase?,
    isCarregando: Boolean,
    isAdmin: Boolean,
    onBackClick: () -> Unit,
    onExportClick: () -> Unit,
    onAddCreditClick: () -> Unit,
    onConfigClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                cliente?.nomeCompleto ?: "Carregando...",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Text("←", style = MaterialTheme.typography.headlineMedium)
            }
        },
        actions = {
            if (isCarregando) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp),
                    strokeWidth = 2.dp
                )
            }
            IconButton(onClick = onExportClick, enabled = !isCarregando && cliente != null) {
                Text("📄", style = MaterialTheme.typography.headlineSmall)
            }
            if (isAdmin) {
                Button(
                    onClick = onAddCreditClick,
                    enabled = !isCarregando,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoresPastel.VerdeMenta,
                        contentColor = CoresTexto.Principal
                    )
                ) {
                    Text("+ Crédito")
                }
                IconButton(
                    onClick = onConfigClick,
                    enabled = !isCarregando
                ) {
                    Text("⚙", style = MaterialTheme.typography.headlineMedium)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CoresPastel.AzulSage,
        ),
        windowInsets = WindowInsets(0, 0, 0, 0)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClienteTopBarDual(
    cliente: ClienteFirebase?,
    isCarregando: Boolean,
    isAdmin: Boolean,
    onExportClick: () -> Unit,
    onAddCreditClick: () -> Unit,
    onConfigClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                cliente?.nomeCompleto ?: "Carregando...",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        actions = {
            if (isCarregando) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp),
                    strokeWidth = 2.dp
                )
            }
            IconButton(onClick = onExportClick, enabled = !isCarregando && cliente != null) {
                Text("📄", style = MaterialTheme.typography.headlineSmall)
            }
            if (isAdmin) {
                Button(
                    onClick = onAddCreditClick,
                    enabled = !isCarregando,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoresPastel.VerdeMenta,
                        contentColor = CoresTexto.Principal
                    )
                ) {
                    Text("+ Crédito")
                }
                IconButton(
                    onClick = onConfigClick,
                    enabled = !isCarregando
                ) {
                    Text("⚙", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
    )
}

@Composable
private fun ClienteSaldoCard(cliente: ClienteFirebase, saldoVisivel: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                cliente.saldo > 0 -> CoresSaldo.Positivo
                cliente.saldo == 0.0 -> CoresSaldo.Zerado
                else -> CoresSaldo.Negativo
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Saldo Atual",
                style = MaterialTheme.typography.titleMedium,
                color = CoresTexto.Principal
            )
            Text(
                text = if (saldoVisivel) "R$ %.2f".format(cliente.saldo) else "R$ ****",
                style = MaterialTheme.typography.headlineLarge,
                color = CoresTexto.Principal
            )
            Text("Data de Nascimento: ${cliente.dataNascimento}")
            Text("Telefone: ${cliente.telefone}")
            Text(
                text = if (saldoVisivel) "Limite: R$ %.2f".format(cliente.limiteNegativo) else "Limite: ****",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ClienteCompraCard(
    cliente: ClienteFirebase,
    valorOperacao: String,
    onValorOperacaoChange: (String) -> Unit,
    saldoVisivel: Boolean,
    isCarregando: Boolean,
    onConfirmCompra: (Double) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = CoresPastel.AzulSage
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Realizar Compra",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                modifier = Modifier.padding(bottom = 6.dp),
                color = CoresTexto.Principal
            )
            CampoMonetario(
                valor = valorOperacao,
                onValueChange = onValorOperacaoChange,
                label = "Valor da Compra R$",
                modifier = Modifier.fillMaxWidth()

            )
            Spacer(modifier = Modifier.height(6.dp))
            ClienteCompraButton(
                valorOperacao = valorOperacao,
                isCarregando = isCarregando,
                onConfirmCompra = onConfirmCompra
            )
            ClienteCompraAvisos(
                cliente = cliente,
                saldoVisivel = saldoVisivel
            )
        }
    }
}

@Composable
private fun ClienteCompraButton(
    valorOperacao: String,
    isCarregando: Boolean,
    onConfirmCompra: (Double) -> Unit,
) {
    Button(
        onClick = {
            val valorEmCentavos = valorOperacao.toLongOrNull() ?: 0L
            val valor = valorEmCentavos / 100.0
            if (valor > 0) {
                onConfirmCompra(valor)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isCarregando && valorOperacao.isNotEmpty(),
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = CoresPastel.CinzaPerola,
            disabledContentColor = CoresTexto.Secundario,
            containerColor = CoresPastel.VerdeMenta,
            contentColor = CoresTexto.Principal
        )
    ) {
        if (isCarregando) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text("Confirmar Compra", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ClienteCompraAvisos(
    cliente: ClienteFirebase,
    saldoVisivel: Boolean,
) {
    val saldoDisponivel = cliente.saldo - cliente.limiteNegativo
    Column {
        if (saldoVisivel) {
            if (saldoDisponivel > 0) {
                Text(
                    text = "Disponível para compras: R$ %.2f".format(saldoDisponivel),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Text(
                    text = "⚠️ Cliente atingiu o limite de crédito",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (saldoDisponivel > 0 && saldoDisponivel <= 10.0) {
                Text(
                    text = "⚠️ Atenção: Cliente próximo do limite",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        } else {
            Text(
                text = "Disponível para compras: ****",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ClienteExtratoHeader(
    isAdmin: Boolean,
    onRemoveClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Histórico de Transações",
            style = MaterialTheme.typography.titleLarge
        )
        if (isAdmin) {
            TextButton(
                onClick = onRemoveClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Remover Cliente")
            }
        }
    }
}

@Composable
private fun ClienteTransacaoItem(transacao: TransacaoFirebase) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = CoresPastel.PessegoPastel)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = SimpleDateFormat(
                        Const.DATE_FORMAT_FULL,
                        Locale.getDefault()
                    ).format(transacao.data.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = CoresTexto.Principal
                )
                Text(
                    text = "${if (transacao.tipo == "CREDITO") "+" else "-"} R$ %.2f".format(transacao.valor),
                    color = if (transacao.tipo == "CREDITO")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = "Por: ${transacao.funcionarioNome}",
                style = MaterialTheme.typography.bodySmall,
                color = CoresTexto.Principal
            )
        }
    }
}

@Composable
private fun ClienteDialogs(
    clienteState: ClienteState,
    viewModel: CantinaFirebaseViewModel,
    clienteId: String,
    navController: NavHostController,
    isDualPane: Boolean = false,
) {
    // Dialog para adicionar crédito (ADMIN)
    if (clienteState.showAddCreditDialog) {
        CreditoDialog(
            onDismiss = { clienteState.onShowAddCreditDialogChange(false) },
            onConfirm = { valor ->
                viewModel.adicionarCredito(clienteId, valor)
                clienteState.onShowAddCreditDialogChange(false)
                clienteState.onShowCreditoDialogChange(true)
            }
        )
    }
    // Dialog para alterar limite (ADMIN)
    if (clienteState.showLimiteDialog) {
        DialogAlterarLimite(
            limiteAtual = clienteState.cliente?.limiteNegativo ?: -50.0,
            onDismiss = { clienteState.onShowLimiteDialogChange(false) },
            onConfirm = { novoLimite ->
                viewModel.atualizarLimiteNegativo(clienteId, novoLimite)
                clienteState.onShowLimiteDialogChange(false)
            }
        )
    }
    // Dialog para remover cliente (ADMIN)
    if (clienteState.showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { clienteState.onShowRemoveDialogChange(false) },
            title = { Text("Confirmar Exclusão") },
            text = { Text(Const.MSG_CONFIRMAR_EXCLUSAO) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removerCliente(clienteId)
                        clienteState.onShowRemoveDialogChange(false)
                        if (!isDualPane) navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoresPastel.CoralSuave,
                        contentColor = CoresTexto.Principal
                    )
                ) {
                    Text("Remover")
                }
            },
            dismissButton = {
                TextButton(onClick = { clienteState.onShowRemoveDialogChange(false) }) {
                    Text("Cancelar")
                }
            }
        )
    }
    // Dialog de sucesso - Crédito
    if (clienteState.showCreditoDialog) {
        AlertDialog(
            onDismissRequest = { clienteState.onShowCreditoDialogChange(false) },
            title = { Text(Const.MSG_SUCESSO) },
            text = { Text(Const.MSG_CREDITO_ADICIONADO) },
            confirmButton = {
                TextButton(onClick = { clienteState.onShowCreditoDialogChange(false) }) {
                    Text("OK")
                }
            }
        )
    }
    // Dialog de sucesso - Débito
    if (clienteState.showDebitoDialog) {
        AlertDialog(
            onDismissRequest = { clienteState.onShowDebitoDialogChange(false) },
            title = { Text(Const.MSG_SUCESSO) },
            text = { Text(Const.MSG_COMPRA_REALIZADA) },
            confirmButton = {
                TextButton(onClick = { clienteState.onShowDebitoDialogChange(false) }) {
                    Text("OK")
                }
            }
        )
    }
    // Dialog de erro - Limite excedido
    if (clienteState.showLimiteExcedidoDialog) {
        DialogLimiteExcedido(
            mensagem = clienteState.mensagemLimiteExcedido,
            isAdmin = clienteState.isAdmin,
            onDismiss = { clienteState.onShowLimiteExcedidoDialogChange(false) },
            onAdicionarCredito = {
                clienteState.onShowLimiteExcedidoDialogChange(false)
                clienteState.onShowAddCreditDialogChange(true)
            }
        )
    }
}

@Composable
private fun ClienteDialogsDual(
    clienteState: ClienteState,
    viewModel: CantinaFirebaseViewModel,
    clienteId: String,
    onClose: () -> Unit,
) {
    // Dialog para adicionar crédito (ADMIN)
    if (clienteState.showAddCreditDialog) {
        CreditoDialog(
            onDismiss = { clienteState.onShowAddCreditDialogChange(false) },
            onConfirm = { valor ->
                viewModel.adicionarCredito(clienteId, valor)
                clienteState.onShowAddCreditDialogChange(false)
                clienteState.onShowCreditoDialogChange(true)
            }
        )
    }

    // Dialog para alterar limite (ADMIN)
    if (clienteState.showLimiteDialog) {
        DialogAlterarLimite(
            limiteAtual = clienteState.cliente?.limiteNegativo ?: -50.0,
            onDismiss = { clienteState.onShowLimiteDialogChange(false) },
            onConfirm = { novoLimite ->
                viewModel.atualizarLimiteNegativo(clienteId, novoLimite)
                clienteState.onShowLimiteDialogChange(false)
            }
        )
    }

    // Dialog para remover cliente (ADMIN)
    if (clienteState.showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { clienteState.onShowRemoveDialogChange(false) },
            title = { Text("Confirmar Exclusão") },
            text = { Text(Const.MSG_CONFIRMAR_EXCLUSAO) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removerCliente(clienteId)
                        clienteState.onShowRemoveDialogChange(false)
                        onClose() // ao invés de navController.popBackStack()
                    }
                ) {
                    Text("Remover")
                }
            },
            dismissButton = {
                TextButton(onClick = { clienteState.onShowRemoveDialogChange(false) }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog de sucesso - Crédito
    if (clienteState.showCreditoDialog) {
        AlertDialog(
            onDismissRequest = { clienteState.onShowCreditoDialogChange(false) },
            title = { Text(Const.MSG_SUCESSO) },
            text = { Text(Const.MSG_CREDITO_ADICIONADO) },
            confirmButton = {
                TextButton(onClick = { clienteState.onShowCreditoDialogChange(false) }) {
                    Text("OK")
                }
            }
        )
    }

    // Dialog de sucesso - Débito
    if (clienteState.showDebitoDialog) {
        AlertDialog(
            onDismissRequest = { clienteState.onShowDebitoDialogChange(false) },
            title = { Text(Const.MSG_SUCESSO) },
            text = { Text(Const.MSG_COMPRA_REALIZADA) },
            confirmButton = {
                TextButton(onClick = { clienteState.onShowDebitoDialogChange(false) }) {
                    Text("OK")
                }
            }
        )
    }

    // Dialog de erro - Limite excedido
    if (clienteState.showLimiteExcedidoDialog) {
        DialogLimiteExcedido(
            mensagem = clienteState.mensagemLimiteExcedido,
            isAdmin = clienteState.isAdmin,
            onDismiss = { clienteState.onShowLimiteExcedidoDialogChange(false) },
            onAdicionarCredito = {
                clienteState.onShowLimiteExcedidoDialogChange(false)
                clienteState.onShowAddCreditDialogChange(true)
            }
        )
    }
}

private fun handleCompra(
    clienteState: ClienteState,
    viewModel: CantinaFirebaseViewModel,
    clienteId: String,
    valor: Double,
) {
    clienteState.cliente?.let { clienteAtual ->
        val saldoAposCompra = clienteAtual.saldo - valor
        if (saldoAposCompra < clienteAtual.limiteNegativo) {
            clienteState.onMensagemLimiteExcedidoChange(
                "Compra não autorizada!\n\n" +
                        "Valor da compra: R$ %.2f\n".format(valor) +
                        "Saldo atual: R$ %.2f\n".format(clienteAtual.saldo) +
                        "Saldo após compra: R$ %.2f\n".format(saldoAposCompra) +
                        "Limite permitido: R$ %.2f\n\n".format(clienteAtual.limiteNegativo) +
                        "O cliente não possui saldo suficiente para esta compra."
            )
            clienteState.onShowLimiteExcedidoDialogChange(true)
            clienteState.onValorOperacaoChange("")
        } else {
            viewModel.realizarCompra(clienteId, valor) { sucesso, mensagem ->
                if (sucesso) {
                    clienteState.onShowDebitoDialogChange(true)
                } else {
                    clienteState.onMensagemLimiteExcedidoChange(mensagem)
                    clienteState.onShowLimiteExcedidoDialogChange(true)
                }
                clienteState.onValorOperacaoChange("")
            }
        }
    }
}
