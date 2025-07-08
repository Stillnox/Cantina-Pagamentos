package com.cantina.pagamentos.presentation.screens.configuracoes

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.cantina.pagamentos.presentation.theme.CoresBadges
import com.cantina.pagamentos.presentation.theme.CoresPastel
import com.cantina.pagamentos.presentation.theme.CoresTexto
import com.cantina.pagamentos.presentation.viewmodels.CantinaFirebaseViewModel

/**
 * Tela de configurações do sistema
 * Mostra informações do usuário, estatísticas (para admins) e opções de gerenciamento
 */
@Composable
fun ConfiguracoesScreen(
    viewModel: CantinaFirebaseViewModel,
    onBackClick: () -> Unit = {}
) {
    val configState = rememberConfigState(viewModel)
    val context = LocalContext.current

    // Carrega estatísticas ao abrir a tela
    LaunchedEffect(Unit) {
        viewModel.carregarEstatisticas()
    }

    Scaffold(
        topBar = {
            ConfigTopBar(
                isCarregando = configState.isCarregando,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        ConfigContent(
            paddingValues = paddingValues,
            configState = configState,
            viewModel = viewModel,
            context = context
        )
    }

    // Dialogs
    ConfigDialogs(
        configState = configState,
        viewModel = viewModel
    )
}

/**
 * Estado da tela de configurações
 */
@Composable
private fun rememberConfigState(viewModel: CantinaFirebaseViewModel): ConfigState {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAddFuncionarioDialog by remember { mutableStateOf(false) }

    val isCarregando by viewModel.isCarregando.collectAsState()
    val estatisticas by viewModel.estatisticas.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()

    return ConfigState(
        showLogoutDialog = showLogoutDialog,
        onShowLogoutDialogChange = { showLogoutDialog = it },
        showAddFuncionarioDialog = showAddFuncionarioDialog,
        onShowAddFuncionarioDialogChange = { showAddFuncionarioDialog = it },
        isCarregando = isCarregando,
        estatisticas = estatisticas,
        isAdmin = isAdmin
    )
}

/**
 * TopBar da tela de configurações
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigTopBar(
    isCarregando: Boolean,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = { Text("Configurações") },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Text("←", style = MaterialTheme.typography.headlineMedium,
                    color = CoresPastel.AzulCeuPastel)
            }
        },
        actions = {
            if (isCarregando) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp),
                    strokeWidth = 2.dp,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CoresPastel.AzulSage,
            titleContentColor = CoresPastel.AzulCeuPastel
        ),
        windowInsets = WindowInsets(0, 0, 0, 0)
    )
}

/**
 * Conteúdo principal da tela de configurações
 */
@Composable
private fun ConfigContent(
    paddingValues: PaddingValues,
    configState: ConfigState,
    viewModel: CantinaFirebaseViewModel,
    context: Context
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card de informações do usuário
        item {
            CardInformacoesUsuario(viewModel)
        }

        // Card de estatísticas (apenas para admins)
        if (configState.isAdmin) {
            item {
                CardEstatisticas(configState.estatisticas)
            }
        }

        // Seção de Administração (apenas para admins)
        if (configState.isAdmin) {
            item {
                ConfigAdminSection(
                    onAddFuncionarioClick = { configState.onShowAddFuncionarioDialogChange(true) },
                    onGerarRelatorioClick = {
                        handleGerarRelatorioClick(viewModel, context)
                    }
                )
            }
        }

        // Seção Conta
        item {
            ConfigContaSection(
                onLogoutClick = { configState.onShowLogoutDialogChange(true) }
            )
        }

        // Card sobre
        item {
            CardSobre()
        }

        // Card de ajuda
        item {
            CardAjuda(configState.isAdmin)
        }
    }
}

/**
 * Seção de administração - SEM DUPLICAÇÃO
 */
@Composable
private fun ConfigAdminSection(
    onAddFuncionarioClick: () -> Unit,
    onGerarRelatorioClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "⚙️ Administração",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Botão para adicionar funcionário
            OutlinedButton(
                onClick = onAddFuncionarioClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = CoresPastel.VerdeMenta
                ),
                border = BorderStroke(1.dp, CoresPastel.VerdeMenta)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Adicionar Funcionário")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botão para gerar relatório
            OutlinedButton(
                onClick = onGerarRelatorioClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = CoresPastel.VerdeMenta
                ),
                border = BorderStroke(1.dp, CoresPastel.VerdeMenta)
            ) {
                Text("📄", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gerar Relatório Completo")
            }
        }
    }
}

/**
 * Seção de conta
 */
@Composable
private fun ConfigContaSection(onLogoutClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🔐 Conta",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Button(
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoresPastel.VerdeMenta,
                    contentColor = CoresTexto.Principal
                )
            ) {
                Text("Sair da Conta")
            }
        }
    }
}

/**
 * Dialogs da tela de configurações
 */
@Composable
private fun ConfigDialogs(
    configState: ConfigState,
    viewModel: CantinaFirebaseViewModel,
) {
    // Dialog de confirmação de logout
    if (configState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { configState.onShowLogoutDialogChange(false) },
            title = {
                Text(
                    "Confirmar Logout",
                    color = CoresPastel.CoralSuave
                )
            },
            text = {
                Text(
                    "Tem certeza que deseja sair da sua conta?",
                    color = CoresPastel.AzulCeuPastel
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.fazerLogout()
                        configState.onShowLogoutDialogChange(false)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Sair")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { configState.onShowLogoutDialogChange(false) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = CoresPastel.AzulCeuPastel
                    )
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog para adicionar funcionário (ADMIN)
    if (configState.showAddFuncionarioDialog) {
        DialogAdicionarFuncionario(
            onDismiss = { configState.onShowAddFuncionarioDialogChange(false) },
            onConfirm = { email, senha, nome, isAdminNovo ->
                viewModel.criarFuncionario(email, senha, nome, isAdminNovo)
                configState.onShowAddFuncionarioDialogChange(false)
            }
        )
    }
}

/**
 * Função para lidar com o clique no botão de gerar relatório
 */
private fun handleGerarRelatorioClick(viewModel: CantinaFirebaseViewModel, context: Context) {
    val uri = viewModel.gerarRelatorioCompleto(context)
    if (uri != null) {
        viewModel.compartilharPdf(context, uri, "Relatório Completo - Sistema Cantina")
    }
}

/**
 * Card que exibe informações do usuário atual
 */
@Composable
private fun CardInformacoesUsuario(viewModel: CantinaFirebaseViewModel) {
    val isAdmin by viewModel.isAdmin.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "👤 Usuário Atual",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Nome: ${viewModel.getNomeFuncionario()}",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tipo: ",
                    style = MaterialTheme.typography.bodyLarge
                )
                Badge(
                    containerColor = if (isAdmin)
                        CoresBadges.Admin        // Amarelo vanilla
                    else
                        CoresBadges.Funcionario  // Azul céu
                ) {
                    Text(
                        text = if (isAdmin) "ADMINISTRADOR" else "FUNCIONÁRIO",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = CoresTexto.Principal  // Sempre texto escuro
                    )
                }
            }
        }
    }
}

/**
 * Card que exibe estatísticas do sistema
 */
@Composable
private fun CardEstatisticas(estatisticas: Map<String, Any>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 Estatísticas do Sistema",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(12.dp))

            val totalClientes = estatisticas["totalClientes"] as? Int ?: 0
            val clientesPositivos = estatisticas["clientesPositivos"] as? Int ?: 0
            val clientesNegativos = estatisticas["clientesNegativos"] as? Int ?: 0
            val clientesZerados = estatisticas["clientesZerados"] as? Int ?: 0
            val saldoTotal = estatisticas["saldoTotal"] as? Double ?: 0.0

            Text("Total de Clientes: $totalClientes", style = MaterialTheme.typography.bodyMedium)
            Text("• Com saldo positivo: $clientesPositivos", style = MaterialTheme.typography.bodyMedium)
            Text("• Com saldo negativo: $clientesNegativos", style = MaterialTheme.typography.bodyMedium)
            Text("• Com saldo zerado: $clientesZerados", style = MaterialTheme.typography.bodyMedium)

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = "Saldo Total: R$ %.2f".format(saldoTotal),
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    saldoTotal > 0 -> MaterialTheme.colorScheme.primary
                    saldoTotal < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

/**
 * Card com informações sobre o app
 */
@Composable
private fun CardSobre() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "ℹ️ Sobre",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Versão: 2.0 (Firebase)", style = MaterialTheme.typography.bodyMedium)
            Text("Sistema de gerenciamento de cantina", style = MaterialTheme.typography.bodyMedium)
            Text("Desenvolvido com Kotlin + Jetpack Compose + Firebase", style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * Card com dicas de uso baseado no tipo de usuário
 */
@Composable
private fun CardAjuda(isAdmin: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "💡 Dicas de Uso",
                style = MaterialTheme.typography.titleMedium,
                color = CoresPastel.AzulCeuPastel
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isAdmin) {
                Text(
                    text = "Como ADMINISTRADOR você pode:\n" +
                            "• Adicionar créditos aos clientes\n" +
                            "• Alterar limites de crédito\n" +
                            "• Remover clientes\n" +
                            "• Adicionar novos funcionários\n" +
                            "• Gerar relatórios completos",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoresPastel.AzulCeuPastel
                )
            } else {
                Text(
                    text = "Como FUNCIONÁRIO você pode:\n" +
                            "• Cadastrar novos clientes\n" +
                            "• Realizar vendas (débitos)\n" +
                            "• Visualizar saldos e históricos\n" +
                            "• Buscar clientes por nome",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoresTexto.Principal
                )
            }
        }
    }
}

/**
 * Dialog para adicionar novo funcionário
 */
@Composable
private fun DialogAdicionarFuncionario(
    onDismiss: () -> Unit,
    onConfirm: (email: String, senha: String, nome: String, isAdmin: Boolean) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var nome by remember { mutableStateOf("") }
    var isAdminNovo by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Funcionário",
            color = CoresPastel.VerdeMenta) },
        text = {
            Column {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome Completo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CoresPastel.VerdeMenta,
                        unfocusedBorderColor = CoresPastel.AzulCeuPastel,
                        focusedLabelColor = CoresPastel.VerdeMenta,
                        unfocusedLabelColor = CoresPastel.AzulCeuPastel,
                        focusedTextColor = CoresPastel.VerdeMenta,
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.lowercase() },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CoresPastel.VerdeMenta,
                        unfocusedBorderColor = CoresPastel.AzulCeuPastel,
                        focusedLabelColor = CoresPastel.VerdeMenta,
                        unfocusedLabelColor = CoresPastel.AzulCeuPastel,
                        focusedTextColor = CoresPastel.VerdeMenta,
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it },
                    label = { Text("Senha (mín. 6 caracteres)") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CoresPastel.VerdeMenta,
                        unfocusedBorderColor = CoresPastel.AzulCeuPastel,
                        focusedLabelColor = CoresPastel.VerdeMenta,
                        unfocusedLabelColor = CoresPastel.AzulCeuPastel,
                        focusedTextColor = CoresPastel.VerdeMenta,
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isAdminNovo,
                        onCheckedChange = { isAdminNovo = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = CoresPastel.AzulCeuPastel,
                            uncheckedColor = CoresPastel.AzulCeuPastel,
                            checkmarkColor = CoresTexto.Principal
                        )
                    )
                    Text(
                        text = "Administrador",
                        color = CoresPastel.AzulCeuPastel,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (isAdminNovo) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "⚠️ Administradores podem adicionar créditos e remover clientes!",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (nome.isNotEmpty() && email.isNotEmpty() && senha.length >= 6) {
                        onConfirm(email, senha, nome, isAdminNovo)
                    }
                },
                enabled = nome.isNotEmpty() && email.isNotEmpty() && senha.length >= 6
            ) {
                Text("Criar", color = CoresPastel.VerdeMenta)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = CoresPastel.CoralSuave)
            }
        }
    )
}

/**
 * Classe para gerenciar o estado das configurações
 */
private data class ConfigState(
    val showLogoutDialog: Boolean,
    val onShowLogoutDialogChange: (Boolean) -> Unit,
    val showAddFuncionarioDialog: Boolean,
    val onShowAddFuncionarioDialogChange: (Boolean) -> Unit,
    val isCarregando: Boolean,
    val estatisticas: Map<String, Any>,
    val isAdmin: Boolean
)
