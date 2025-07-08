package com.cantina.pagamentos.presentation.screens.clientes.cadastro

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.cantina.pagamentos.core.utils.Constants as Const
import com.cantina.pagamentos.core.utils.corrigirAno
import com.cantina.pagamentos.core.utils.formatarTelefone
import com.cantina.pagamentos.core.utils.validarData
import com.cantina.pagamentos.presentation.components.common.DateVisualTransformation
import com.cantina.pagamentos.presentation.theme.CoresPastel
import com.cantina.pagamentos.presentation.theme.CoresTexto
import com.cantina.pagamentos.presentation.viewmodels.CantinaFirebaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar


/**
 * Tela para cadastrar um novo cliente
 * Valida nome completo, data de nascimento e telefone
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaCadastroFirebase(navController: NavHostController, viewModel: CantinaFirebaseViewModel) {
    val cadastroState = rememberCadastroState(viewModel)


    // Observa mensagens do ViewModel
    LaunchedEffect(Unit) {
        viewModel.mensagem.collect { mensagem ->
            cadastroState.coroutineScope.launch {
                cadastroState.snackbarHostState.showSnackbar(mensagem)
            }
        }
    }

    // Navega de volta após cadastro bem-sucedido
    LaunchedEffect(cadastroState.cadastroRealizado) {
        if (cadastroState.cadastroRealizado) {
            delay(1000)
            navController.popBackStack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(cadastroState.snackbarHostState) },
        topBar = {
            CadastroTopBar(
                isCarregando = cadastroState.isCarregando,
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        CadastroContent(
            paddingValues = paddingValues,
            cadastroState = cadastroState,
            viewModel = viewModel
        )
    }
}

/**
 * Estado da tela de cadastro
 */
@Composable
private fun rememberCadastroState(viewModel: CantinaFirebaseViewModel): CadastroState {
    var nome by remember { mutableStateOf("") }
    var dataNascimento by remember { mutableStateOf("") }
    var dataErro by remember { mutableStateOf(false) }
    var telefone by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    var nomeErro by remember { mutableStateOf(false) }
    var telefoneEditando by remember { mutableStateOf("") }
    var telefoneFocado by remember { mutableStateOf(false) }
    var cadastroRealizado by remember { mutableStateOf(false) }

    val isCarregando by viewModel.isCarregando.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    return CadastroState(
        nome = nome,
        onNomeChange = {
            nome = it
            val nomePartes = it.trim().split(" ").filter { parte -> parte.isNotEmpty() }
            nomeErro = it.isNotEmpty() && nomePartes.size < 2
        },
        nomeErro = nomeErro,
        dataNascimento = dataNascimento,
        onDataNascimentoChange = { novoTexto ->
            val numerosFiltrados = novoTexto.filter { it.isDigit() }.take(8)
            if (numerosFiltrados.length == 6 && !isFocused) {
                val dia = numerosFiltrados.substring(0, 2)
                val mes = numerosFiltrados.substring(2, 4)
                val ano = corrigirAno(numerosFiltrados.substring(4, 6))
                dataNascimento = "$dia$mes$ano"
            } else {
                dataNascimento = numerosFiltrados
            }
            dataErro = when (dataNascimento.length) {
                6 -> {
                    val anoCorrigido = corrigirAno(dataNascimento.substring(4, 6))
                    !validarData(dataNascimento.substring(0, 4) + anoCorrigido)
                }
                8 -> !validarData(dataNascimento)
                else -> false
            }
        },
        dataErro = dataErro,
        isFocused = isFocused,
        onFocusChange = { isFocused = it },
        telefone = telefone,
        telefoneEditando = telefoneEditando,
        onTelefoneChange = { novoTexto ->
            telefoneEditando = novoTexto.filter { it.isDigit() }.take(11)
            telefone = telefoneEditando
        },
        telefoneFocado = telefoneFocado,
        onTelefoneFocusChange = { telefoneFocado = it },
        isCarregando = isCarregando,
        cadastroRealizado = cadastroRealizado,
        onCadastroRealizado = { cadastroRealizado = it },
        snackbarHostState = snackbarHostState,
        coroutineScope = coroutineScope
    )
}


/**
 * TopBar da tela de cadastro
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CadastroTopBar(
    isCarregando: Boolean,
    onBackClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                "Cadastrar Cliente",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp), // Diminuído em 2dp (28sp - 2sp = 26sp)
                color = CoresPastel.AzulCeuPastel // Mudada cor para azul céu pastel
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                enabled = !isCarregando
            ) {
                Text("←", style = MaterialTheme.typography.headlineMedium, color = CoresPastel.AzulCeuPastel)
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
        },
        windowInsets = WindowInsets(0, 0, 0, 0) // Remove espaço superior
    )
}

/**
 * Conteúdo principal da tela de cadastro
 */
@Composable
private fun CadastroContent(
    paddingValues: PaddingValues,
    cadastroState: CadastroState,
    viewModel: CantinaFirebaseViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        CadastroInfoCard()
        Spacer(modifier = Modifier.height(16.dp))
        CadastroForm(
            cadastroState = cadastroState,
            viewModel = viewModel
        )
        Spacer(modifier = Modifier.height(16.dp))
        CadastroHelpCard()
    }
}

/**
 * Card informativo do cadastro
 */
@Composable
private fun CadastroInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CoresPastel.AzulCeuPastel
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ℹ️", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "O cliente será criado com saldo inicial R$ 0,00",
                style = MaterialTheme.typography.bodyMedium,
                color = CoresTexto.Principal
            )
        }
    }
}

/**
 * Formulário de cadastro
 */
@Composable
private fun CadastroForm(
    cadastroState: CadastroState,
    viewModel: CantinaFirebaseViewModel,
) {
    CadastroNomeField(cadastroState = cadastroState)
    Spacer(modifier = Modifier.height(8.dp))
    CadastroDataField(cadastroState = cadastroState)
    Spacer(modifier = Modifier.height(8.dp))
    CadastroTelefoneField(cadastroState = cadastroState)
    Spacer(modifier = Modifier.height(24.dp))
    CadastroButton(
        cadastroState = cadastroState,
        viewModel = viewModel
    )
}

/**
 * Campo de nome completo
 */
@Composable
private fun CadastroNomeField(cadastroState: CadastroState) {
    OutlinedTextField(
        value = cadastroState.nome,
        onValueChange = cadastroState.onNomeChange,
        label = { Text("Nome Completo") },
        placeholder = { Text("Ex: João da Silva") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !cadastroState.isCarregando,
        isError = cadastroState.nomeErro,
        supportingText = {
            if (cadastroState.nomeErro) {
                Text(Const.MSG_NOME_SOBRENOME, color = MaterialTheme.colorScheme.error)
            } else {
                Text(Const.MSG_NOME_SOBRENOME)
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CoresPastel.VerdeMenta,
            unfocusedBorderColor = CoresPastel.VerdeMenta,
            focusedLabelColor = CoresPastel.VerdeMenta,
            unfocusedLabelColor = CoresPastel.VerdeMenta,
        )
    )
}

/**
 * Campo de data de nascimento
 */
@Composable
private fun CadastroDataField(cadastroState: CadastroState) {
    OutlinedTextField(
        value = cadastroState.dataNascimento,
        onValueChange = cadastroState.onDataNascimentoChange,
        label = { Text("Data de Nascimento") },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                cadastroState.onFocusChange(focusState.isFocused)
                if (!focusState.isFocused && cadastroState.dataNascimento.length == 6) {
                    val dia = cadastroState.dataNascimento.substring(0, 2)
                    val mes = cadastroState.dataNascimento.substring(2, 4)
                    val ano = corrigirAno(cadastroState.dataNascimento.substring(4, 6))
                    cadastroState.onDataNascimentoChange("$dia$mes$ano")
                }
            },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        placeholder = { Text("DD/MM/AAAA") },
        visualTransformation = DateVisualTransformation(),
        isError = cadastroState.dataErro,
        enabled = !cadastroState.isCarregando,
        supportingText = {
            CadastroDataSupportingText(cadastroState = cadastroState)
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CoresPastel.VerdeMenta,
            unfocusedBorderColor = CoresPastel.VerdeMenta,
            focusedLabelColor = CoresPastel.VerdeMenta,
            unfocusedLabelColor = CoresPastel.VerdeMenta,
        )
    )
}

/**
 * Texto de suporte do campo de data
 */
@Composable
private fun CadastroDataSupportingText(cadastroState: CadastroState) {
    if (cadastroState.dataErro) {
        val hoje = Calendar.getInstance()
        val diaHoje = hoje[Calendar.DAY_OF_MONTH]
        val mesHoje = hoje[Calendar.MONTH] + 1
        val anoHoje = hoje[Calendar.YEAR]

        if (cadastroState.dataNascimento.length >= 8) {
            val ano = cadastroState.dataNascimento.substring(4, 8).toIntOrNull() ?: 0
            val mes = cadastroState.dataNascimento.substring(2, 4).toIntOrNull() ?: 0
            val dia = cadastroState.dataNascimento.substring(0, 2).toIntOrNull() ?: 0

            if (ano > anoHoje || (ano == anoHoje && mes > mesHoje) ||
                (ano == anoHoje && mes == mesHoje && dia > diaHoje)) {
                Text("Data não pode ser futura", color = MaterialTheme.colorScheme.error)
            } else {
                Text(Const.MSG_DATA_INVALIDA, color = MaterialTheme.colorScheme.error)
            }
        } else {
            Text(Const.MSG_DATA_INVALIDA, color = MaterialTheme.colorScheme.error)
        }
    } else if (cadastroState.dataNascimento.length == 6) {
        Text(
            "Digite o ano com 4 dígitos ou toque fora para completar",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Campo de telefone
 */
@Composable
private fun CadastroTelefoneField(cadastroState: CadastroState) {
    OutlinedTextField(
        value = if (cadastroState.telefoneFocado) cadastroState.telefoneEditando else {
            formatarTelefone(cadastroState.telefoneEditando)
        },
        onValueChange = cadastroState.onTelefoneChange,
        label = { Text("Telefone") },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                cadastroState.onTelefoneFocusChange(focusState.isFocused)
            },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        placeholder = { Text(if (cadastroState.telefoneFocado) "11999999999" else "(11) 99999-9999") },
        enabled = !cadastroState.isCarregando,
        supportingText = {
            CadastroTelefoneSupportingText(cadastroState = cadastroState)
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CoresPastel.VerdeMenta,
            unfocusedBorderColor = CoresPastel.VerdeMenta,
            focusedLabelColor = CoresPastel.VerdeMenta,
            unfocusedLabelColor = CoresPastel.VerdeMenta,
        )
    )
}

/**
 * Texto de suporte do campo de telefone
 */
@Composable
private fun CadastroTelefoneSupportingText(cadastroState: CadastroState) {
    when (cadastroState.telefoneEditando.length) {
        10 -> Text("Telefone fixo", color = MaterialTheme.colorScheme.onSurfaceVariant)
        11 -> Text("Celular", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else -> if (!cadastroState.telefoneFocado && cadastroState.telefoneEditando.isNotEmpty() && cadastroState.telefoneEditando.length < 10) {
            Text("Telefone incompleto", color = MaterialTheme.colorScheme.error)
        } else null
    }
}

/**
 * Botão de cadastrar
 */
@Composable
private fun CadastroButton(
    cadastroState: CadastroState,
    viewModel: CantinaFirebaseViewModel,
) {
    val isButtonEnabled = !cadastroState.isCarregando &&
            cadastroState.nome.isNotEmpty() &&
            cadastroState.dataNascimento.isNotEmpty() &&
            cadastroState.telefone.isNotEmpty() &&
            !cadastroState.nomeErro

    Button(
        onClick = {
            handleCadastroClick(cadastroState, viewModel)
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = isButtonEnabled
    ) {
        if (cadastroState.isCarregando) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cadastrando...")
            }
        } else {
            Text("Cadastrar Cliente")
        }
    }
}

/**
 * Card de ajuda do cadastro
 */
@Composable
private fun CadastroHelpCard() {
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
                text = "💡 Dicas",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• O cliente poderá comprar até o limite configurado\n" +
                        "• O limite padrão é R$ -50,00\n" +
                        "• Apenas administradores podem adicionar créditos",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Função para lidar com o clique no botão de cadastro
 */
private fun handleCadastroClick(
    cadastroState: CadastroState,
    viewModel: CantinaFirebaseViewModel,
) {
    val nomePartes = cadastroState.nome.trim().split(" ").filter { it.isNotEmpty() }

    if (isValidCadastroData(cadastroState, nomePartes)) {
        val dataCompleta = completeDataIfNeeded(cadastroState.dataNascimento)

        if (validarData(dataCompleta)) {
            val dataFormatada = formatarData(dataCompleta)
            val telefoneFormatado = formatarTelefoneParaSalvar(cadastroState.telefone)

            viewModel.criarCliente(
                nomeCompleto = cadastroState.nome.trim(),
                dataNascimento = dataFormatada,
                telefone = telefoneFormatado
            )

            cadastroState.onCadastroRealizado(true)
        }
    }
}

/**
 * Valida se os dados do cadastro são válidos
 */
private fun isValidCadastroData(cadastroState: CadastroState, nomePartes: List<String>): Boolean {
    return cadastroState.nome.isNotEmpty() &&
            cadastroState.dataNascimento.isNotEmpty() &&
            cadastroState.telefone.isNotEmpty() &&
            nomePartes.size >= 2 &&
            (cadastroState.dataNascimento.length == 6 || cadastroState.dataNascimento.length == 8) &&
            cadastroState.telefone.length >= 10
}

/**
 * Completa a data se necessário
 */
private fun completeDataIfNeeded(dataNascimento: String): String {
    return if (dataNascimento.length == 6) {
        val dia = dataNascimento.substring(0, 2)
        val mes = dataNascimento.substring(2, 4)
        val ano = corrigirAno(dataNascimento.substring(4, 6))
        "$dia$mes$ano"
    } else {
        dataNascimento
    }
}

/**
 * Formata a data para exibição
 */
private fun formatarData(dataCompleta: String): String {
    return "${dataCompleta.substring(0,2)}/${dataCompleta.substring(2,4)}/${dataCompleta.substring(4,8)}"
}



/**
 * Formata o telefone para salvar
 */
private fun formatarTelefoneParaSalvar(telefone: String): String {
    return when (telefone.length) {
        11 -> "(${telefone.substring(0,2)}) ${telefone.substring(2,7)}-${telefone.substring(7,11)}"
        10 -> "(${telefone.substring(0,2)}) ${telefone.substring(2,6)}-${telefone.substring(6,10)}"
        else -> telefone
    }
}

/**
 * Classe para gerenciar o estado do cadastro
 */
private data class CadastroState(
    val nome: String,
    val onNomeChange: (String) -> Unit,
    val nomeErro: Boolean,
    val dataNascimento: String,
    val onDataNascimentoChange: (String) -> Unit,
    val dataErro: Boolean,
    val isFocused: Boolean,
    val onFocusChange: (Boolean) -> Unit,
    val telefone: String,
    val telefoneEditando: String,
    val onTelefoneChange: (String) -> Unit,
    val telefoneFocado: Boolean,
    val onTelefoneFocusChange: (Boolean) -> Unit,
    val isCarregando: Boolean,
    val cadastroRealizado: Boolean,
    val onCadastroRealizado: (Boolean) -> Unit,
    val snackbarHostState: SnackbarHostState,
    val coroutineScope: CoroutineScope,
)
