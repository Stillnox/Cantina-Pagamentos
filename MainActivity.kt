package com.cantina.pagamentos

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomAppBarDefaults.windowInsets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cantina.pagamentos.AppConstants.DATE_FORMAT_FULL
import com.cantina.pagamentos.AppConstants.MSG_DATA_INVALIDA
import com.cantina.pagamentos.AppConstants.MSG_NOME_SOBRENOME
import com.cantina.pagamentos.models.ClienteFirebase
import com.cantina.pagamentos.models.EstadoAutenticacao
import com.cantina.pagamentos.models.TransacaoFirebase
import com.cantina.pagamentos.ui.theme.CantinaPastelTheme
import com.cantina.pagamentos.ui.theme.CoresBadges
import com.cantina.pagamentos.ui.theme.CoresPastel
import com.cantina.pagamentos.ui.theme.CoresSaldo
import com.cantina.pagamentos.ui.theme.CoresTexto
import com.cantina.pagamentos.viewmodel.CantinaFirebaseViewModel
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ===========================================================================================
// SEÇÃO 1: CONSTANTES E CONFIGURAÇÕES
// ===========================================================================================

/**
 * Objeto que contém todas as constantes do aplicativo
 * Facilita manutenção e tradução de mensagens
 */
object AppConstants {
    // Formatos de data/hora
    const val DATE_FORMAT_FULL = "dd/MM/yyyy HH:mm"

    // Mensagens de sucesso
    const val MSG_SUCESSO = "Sucesso!"
    const val MSG_CLIENTE_CADASTRADO = "Cliente cadastrado com sucesso."
    const val MSG_CREDITO_ADICIONADO = "Crédito adicionado com sucesso."
    const val MSG_COMPRA_REALIZADA = "Compra realizada com sucesso."
    const val MSG_LIMITE_ATUALIZADO = "Limite atualizado!"

    // Mensagens de erro/validação
    const val MSG_PREENCHA_CAMPOS = "Preencha todos os campos"
    const val MSG_NOME_SOBRENOME = "Digite nome e sobrenome"
    const val MSG_DATA_INVALIDA = "Data inválida"
    const val MSG_DATA_INCOMPLETA = "Data de nascimento incompleta"
    const val MSG_DATA_FUTURA = "Data não pode ser futura"
    const val MSG_TELEFONE_INCOMPLETO = "Telefone deve ter pelo menos 10 dígitos"
    const val MSG_LIMITE_EXCEDIDO = "Limite Excedido"
    const val MSG_ERRO_PDF = "Erro ao gerar PDF"
    const val MSG_CONFIRMAR_EXCLUSAO = "Tem certeza que deseja remover este cliente?\n\nTodos os dados serão perdidos!"
}

// ===========================================================================================
// SEÇÃO 2: FUNÇÕES AUXILIARES
// ===========================================================================================

/**
 * Valida se uma data no formato DDMMAAAA é válida
 * Verifica mês válido, dia válido para o mês, ano entre 1900 e atual, e se não é futura
 * @param data String com 8 dígitos representando a data
 * @return true se a data é válida, false caso contrário
 */
fun validarData(data: String): Boolean {
    if (data.length != 8) return false

    val dia = data.substring(0, 2).toIntOrNull() ?: return false
    val mes = data.substring(2, 4).toIntOrNull() ?: return false
    val ano = data.substring(4, 8).toIntOrNull() ?: return false


    // Valida mês
    if (mes < 1 || mes > 12) return false

    // Calcula dias no mês considerando anos bissextos
    val diasNoMes = when (mes) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (ano % 4 == 0 && (ano % 100 != 0 || ano % 400 == 0)) 29 else 28
        else -> return false
    }

    // Valida dia
    if (dia < 1 || dia > diasNoMes) return false

    // Valida ano
    val anoAtual = Calendar.getInstance()[Calendar.YEAR]
    if (ano < 1900 || ano > anoAtual) return false

    // Verifica se a data não é futura
    if (ano == anoAtual) {
        val mesAtual = Calendar.getInstance()[Calendar.MONTH] + 1
        val diaAtual = Calendar.getInstance()[Calendar.DAY_OF_MONTH]

        if (mes > mesAtual) return false
        if (mes == mesAtual && dia > diaAtual) return false
    }

    return true
}

/**
 * Converte ano de 2 dígitos para 4 dígitos
 * Anos > anoAtual+5 são considerados do século passado
 * @param ano String com 2 ou 4 dígitos
 * @return String com 4 dígitos do ano
 */
fun corrigirAno(ano: String): String {
    return when (ano.length) {
        2 -> {
            val anoInt = ano.toIntOrNull() ?: 0
            val anoAtual = Calendar.getInstance().get(Calendar.YEAR)
            val anoAtualDoisDigitos = anoAtual % 100

            if (anoInt > anoAtualDoisDigitos + 5) {
                "19$ano"
            } else {
                "20$ano"
            }
        }
        4 -> ano
        else -> ano
    }
}

// ===========================================================================================
// SEÇÃO 3: COMPONENTES UI REUTILIZÁVEIS
// ===========================================================================================

/**
 * Visual Transformation para formatar data enquanto o usuário digita
 * Transforma entrada "31122024" em exibição "31/12/2024"
 */
class DateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Remove não-dígitos e limita a 8 caracteres
        val trimmed = text.text.filter { it.isDigit() }.take(8)

        // Constrói a string formatada com barras
        val output = buildString {
            for (i in trimmed.indices) {
                append(trimmed[i])
                // Adiciona barras após dia (posição 1) e mês (posição 3)
                if (i == 1 || i == 3) append("/")
            }
        }

        // Mapeia as posições para a transformação
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 4) return offset + 1
                if (offset <= 8) return offset + 2
                return output.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset - 1
                if (offset <= 10) return offset - 2
                return trimmed.length
            }
        }

        return TransformedText(AnnotatedString(output), offsetMapping)
    }
}

/**
 * Campo de entrada formatado para valores monetários
 * Converte automaticamente entrada em centavos para formato R$ X,XX
 * @param valor Valor em centavos como String
 * @param onValueChange Callback quando o valor muda
 * @param label Label do campo
 * @param modifier Modificadores do Compose
 * @param placeholder Placeholder quando vazio
 */
@SuppressLint("DefaultLocale")
@Composable
fun CampoMonetario(
    valor: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "0,00",
) {
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = if (valor.isEmpty()) "" else {
                    val valorEmCentavos = valor.toLongOrNull() ?: 0L
                    val reais = valorEmCentavos / 100
                    val centavos = valorEmCentavos % 100
                    String.format("%d,%02d", reais, centavos)
                },
                selection = TextRange(valor.length)
            )
        )
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            // Remove tudo que não é dígito
            val apenasNumeros = newValue.text.filter { it.isDigit() }
            onValueChange(apenasNumeros)

            // Formata o texto para exibição
            val novoTextoFormatado = if (apenasNumeros.isEmpty()) "" else {
                val valorEmCentavos = apenasNumeros.toLongOrNull() ?: 0L
                val reais = valorEmCentavos / 100
                val centavos = valorEmCentavos % 100
                String.format("%d,%02d", reais, centavos)
            }

            // Atualiza o campo com cursor no final
            textFieldValue = TextFieldValue(
                text = novoTextoFormatado,
                selection = TextRange(novoTextoFormatado.length)
            )
        },
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        placeholder = { Text(placeholder) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedLabelColor = CoresTexto.Principal,
            unfocusedLabelColor = CoresTexto.Secundario      // Usa o parâmetro da função
        )
    )
}

/**
 * Barra de navegação inferior do aplicativo
 * Mostra as 5 abas principais: Todos, Positivo, Negativo, Zerado e Configurações
 */
@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = CoresPastel.AzulSage,  // Fundo verde sage
        contentColor = CoresTexto.Principal      // Ícones e texto escuros
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

        // Aba Configurações - Estatísticas e gerenciamento
        NavigationBarItem(
            icon = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚙️", style = MaterialTheme.typography.headlineMedium)
                    Text("Config", style = MaterialTheme.typography.labelSmall)
                }
            },
            selected = currentRoute == "configuracoes",
            onClick = {
                navController.navigate("configuracoes") {
                    popUpTo("todos")
                }
            }
        )
    }
}

// ===========================================================================================
// SEÇÃO 4: ACTIVITY PRINCIPAL
// ===========================================================================================

/**
 * Activity principal do aplicativo
 * Responsável por:
 * - Verificar Google Play Services
 * - Verificar Firebase
 * - Configurar o tema
 * - Iniciar a navegação
 */
class MainActivity : ComponentActivity() {
    private var googlePlayServicesAvailable = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verifica se Google Play Services está disponível
        verificarGooglePlayServices()

        // Verifica se Firebase está inicializado
        verificarFirebase()

        // Configura a janela para edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Define o conteúdo da UI
        setContent {
            CantinaPastelTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom)),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (googlePlayServicesAvailable) {
                        AppCantinaFirebase()
                    } else {
                        TelaErroGooglePlayServices()
                    }
                }
            }
        }
    }

    /**
     * Verifica disponibilidade do Google Play Services
     * Necessário para o Firebase funcionar corretamente
     */
    private fun verificarGooglePlayServices() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)

        when (resultCode) {
            com.google.android.gms.common.ConnectionResult.SUCCESS -> {
                googlePlayServicesAvailable = true
                println("🔥 [MainActivity] Google Play Services disponível")
            }
            else -> {
                googlePlayServicesAvailable = false
                println("🔥 [MainActivity] Google Play Services não disponível: ${googleApiAvailability.getErrorString(resultCode)}")

                if (googleApiAvailability.isUserResolvableError(resultCode)) {
                    googleApiAvailability.getErrorDialog(this, resultCode, 9000)?.show()
                }
            }
        }
    }

    /**
     * Verifica se o Firebase está inicializado corretamente
     */
    private fun verificarFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                println("🔥 [MainActivity] Firebase não inicializado!")
            } else {
                println("🔥 [MainActivity] Firebase inicializado com sucesso")
            }
        } catch (e: Exception) {
            println("🔥 [MainActivity] ERRO ao verificar Firebase: ${e.message}")
            e.printStackTrace()
        }
    }
}

// ===========================================================================================
// SEÇÃO 5: COMPOSABLE PRINCIPAL E NAVEGAÇÃO
// ===========================================================================================

/**
 * Composable principal do aplicativo com Firebase
 * Gerencia:
 * - Estado de autenticação (login/logout)
 * - Navegação entre telas
 * - Mensagens de feedback
 */
@Composable
fun AppCantinaFirebase() {
    val viewModel: CantinaFirebaseViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val navController: NavHostController = rememberNavController()
    val context = LocalContext.current

    val estadoAuth by viewModel.estadoAutenticacao.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Observa mensagens do ViewModel para mostrar Toast
    LaunchedEffect(Unit) {
        viewModel.mensagem.collectLatest { mensagem ->
            Toast.makeText(context, mensagem, Toast.LENGTH_LONG).show()
        }
    }

    // Renderiza UI baseado no estado de autenticação
    when (estadoAuth) {
        is EstadoAutenticacao.Carregando -> {
            TelaCarregamento()
        }
        is EstadoAutenticacao.NaoAutenticado -> {
            TelaLogin(
                viewModel = viewModel,
                onLoginSucesso = {
                    println("🔥 [AppCantinaFirebase] Login sucesso callback chamado")
                }
            )
        }
        is EstadoAutenticacao.Autenticado -> {
            TelaPrincipal(navController, viewModel, snackbarHostState)
        }
    }
}

/**
 * Tela de carregamento exibida enquanto verifica autenticação
 */
@Composable
fun TelaCarregamento() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Verificando autenticação...")
        }
    }
}

/**
 * Tela principal após login bem-sucedido
 * Contém a navegação e as telas do aplicativo
 */
@Composable
fun TelaPrincipal(
    navController: NavHostController,
    viewModel: CantinaFirebaseViewModel,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "todos",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("todos") {
                TelaListaClientesFirebase(navController, viewModel, "todos")
            }

            composable("positivo") {
                TelaListaClientesFirebase(navController, viewModel, "positivo")
            }

            composable("negativo") {
                TelaListaClientesFirebase(navController, viewModel, "negativo")
            }

            composable("zerado") {
                TelaListaClientesFirebase(navController, viewModel, "zerado")
            }

            composable("configuracoes") {
                TelaConfiguracoesFirebase(viewModel)
            }

            composable("cadastro") {
                TelaCadastroFirebase(navController, viewModel)
            }

            composable("cliente/{clienteId}") { backStackEntry ->
                val clienteId = backStackEntry.arguments?.getString("clienteId") ?: ""
                TelaClienteFirebase(navController, viewModel, clienteId)
            }
        }
    }
}

/**
 * Tela de erro para quando Google Play Services não está disponível
 * Oferece opções para atualizar ou abrir configurações
 */
@Composable
fun TelaErroGooglePlayServices() {
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Google Play Services Necessário",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Este aplicativo precisa do Google Play Services para funcionar corretamente.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = android.net.Uri.fromParts("package", "com.google.android.gms", null)
                    context.startActivity(intent)
                }
            ) {
                Text("Abrir Configurações")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = "market://details?id=com.google.android.gms".toUri()
                    context.startActivity(intent)
                }
            ) {
                Text("Atualizar Google Play Services")
            }
        }
    }
}

// ===========================================================================================
// SEÇÃO 6: TELA DE LOGIN
// ===========================================================================================

/**
 * Tela de login do sistema
 * Permite que funcionários façam login com email e senha
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaLogin(
    viewModel: CantinaFirebaseViewModel,
    onLoginSucesso: () -> Unit,
) {
    val loginState = rememberLoginState()
    val context = LocalContext.current
    val estadoAuth by viewModel.estadoAutenticacao.collectAsState()
    val isCarregando by viewModel.isCarregando.collectAsState()

    // Observa mudanças no estado de autenticação
    LaunchedEffect(estadoAuth) {
        if (estadoAuth is EstadoAutenticacao.Autenticado) {
            onLoginSucesso()
        }
    }

    Scaffold(
        topBar = { LoginTopBar() }
    ) { paddingValues ->
        LoginContent(
            paddingValues = paddingValues,
            loginState = loginState,
            isCarregando = isCarregando,
            onLoginClick = { email, senha ->
                handleLoginClick(context, email, senha, viewModel)
            }
        )
    }
}

/**
 * Estado do formulário de login
 */
@Composable
private fun rememberLoginState(): LoginState {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }

    return LoginState(
        email = email,
        onEmailChange = { email = it.trim() },
        senha = senha,
        onSenhaChange = { senha = it },
        senhaVisivel = senhaVisivel,
        onSenhaVisivelChange = { senhaVisivel = it }
    )
}

/**
 * TopBar da tela de login
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginTopBar() {
    TopAppBar(
        title = { Text("Sistema Cantina - Login") },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CoresPastel.AzulSage,
            titleContentColor = CoresTexto.Principal,
            actionIconContentColor = CoresTexto.Principal
        )
    )
}

/**
 * Conteúdo principal da tela de login
 */
@Composable
private fun LoginContent(
    paddingValues: PaddingValues,
    loginState: LoginState,
    isCarregando: Boolean,
    onLoginClick: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LoginFormCard(
            loginState = loginState,
            isCarregando = isCarregando,
            onLoginClick = onLoginClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        LoginInfoCard()
    }
}

/**
 * Card principal com formulário de login
 */
@Composable
private fun LoginFormCard(
    loginState: LoginState,
    isCarregando: Boolean,
    onLoginClick: (String, String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LoginHeader()
            Spacer(modifier = Modifier.height(24.dp))
            LoginFormFields(
                loginState = loginState,
                isCarregando = isCarregando
            )
            Spacer(modifier = Modifier.height(24.dp))
            LoginButton(
                loginState = loginState,
                isCarregando = isCarregando,
                onLoginClick = onLoginClick
            )
        }
    }
}

/**
 * Cabeçalho do formulário de login
 */
@Composable
private fun LoginHeader() {
    Text(
        text = "🍔",
        style = MaterialTheme.typography.displayLarge
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Faça login para continuar",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Campos do formulário de login
 */
@Composable
private fun LoginFormFields(
    loginState: LoginState,
    isCarregando: Boolean
) {
    LoginEmailField(
        email = loginState.email,
        onEmailChange = loginState.onEmailChange,
        isCarregando = isCarregando
    )

    Spacer(modifier = Modifier.height(16.dp))

    LoginPasswordField(
        senha = loginState.senha,
        onSenhaChange = loginState.onSenhaChange,
        senhaVisivel = loginState.senhaVisivel,
        onSenhaVisivelChange = loginState.onSenhaVisivelChange,
        isCarregando = isCarregando
    )
}

/**
 * Campo de email
 */
@Composable
private fun LoginEmailField(
    email: String,
    onEmailChange: (String) -> Unit,
    isCarregando: Boolean
) {
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text("Email") },
        placeholder = { Text("funcionario@cantina.com") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isCarregando,
        leadingIcon = {
            Text("📧", style = MaterialTheme.typography.titleLarge)
        }
    )
}

/**
 * Campo de senha
 */
@Composable
private fun LoginPasswordField(
    senha: String,
    onSenhaChange: (String) -> Unit,
    senhaVisivel: Boolean,
    onSenhaVisivelChange: (Boolean) -> Unit,
    isCarregando: Boolean
) {
    OutlinedTextField(
        value = senha,
        onValueChange = onSenhaChange,
        label = { Text("Senha") },
        placeholder = { Text("Digite sua senha") },
        visualTransformation = if (senhaVisivel) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isCarregando,
        leadingIcon = {
            Text("🔒", style = MaterialTheme.typography.titleLarge)
        },
        trailingIcon = {
            IconButton(onClick = { onSenhaVisivelChange(!senhaVisivel) }) {
                Text(
                    if (senhaVisivel) "👁️" else "👁️‍🗨️",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    )
}

/**
 * Botão de login
 */
@Composable
private fun LoginButton(
    loginState: LoginState,
    isCarregando: Boolean,
    onLoginClick: (String, String) -> Unit
) {
    val isButtonEnabled = !isCarregando && loginState.email.isNotEmpty() && loginState.senha.isNotEmpty()

    Button(
        onClick = { onLoginClick(loginState.email, loginState.senha) },
        modifier = Modifier.fillMaxWidth(),
        enabled = isButtonEnabled
    ) {
        if (isCarregando) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                text = "Entrar",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * Card de informações de acesso
 */
@Composable
private fun LoginInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CoresPastel.PessegoPastel
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "ℹ️ Informações de Acesso",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "• Use seu email corporativo\n" +
                        "• A senha deve ter pelo menos 6 caracteres\n" +
                        "• Em caso de problemas, contate o administrador",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Função para lidar com o clique no botão de login
 */
private fun handleLoginClick(
    context: Context,
    email: String,
    senha: String,
    viewModel: CantinaFirebaseViewModel
) {
    when {
        email.isEmpty() -> {
            Toast.makeText(context, "Digite o email", Toast.LENGTH_SHORT).show()
        }
        senha.isEmpty() -> {
            Toast.makeText(context, "Digite a senha", Toast.LENGTH_SHORT).show()
        }
        else -> {
            viewModel.fazerLogin(email, senha)
        }
    }
}

/**
 * Classe para gerenciar o estado do formulário de login
 */
private data class LoginState(
    val email: String,
    val onEmailChange: (String) -> Unit,
    val senha: String,
    val onSenhaChange: (String) -> Unit,
    val senhaVisivel: Boolean,
    val onSenhaVisivelChange: (Boolean) -> Unit
)

// ===========================================================================================
// SEÇÃO 7: TELA DE LISTA DE CLIENTES
// ===========================================================================================

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
    filtro: String
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
    onAddCliente: () -> Unit
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
    onAddCliente: () -> Unit
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
    navController: NavHostController
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
    navController: NavHostController
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
@Composable
private fun ListaSearchField(listaState: ListaState) {
    OutlinedTextField(
        value = listaState.busca,
        onValueChange = listaState.onBuscaChange,
        label = { Text("Buscar por nome") },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CoresPastel.AzulSage,
            unfocusedBorderColor = CoresPastel.VerdeMenta,
            focusedLabelColor = CoresPastel.AzulSage,
            unfocusedLabelColor = CoresPastel.VerdeMenta
        )
    )
}

/**
 * Cabeçalho da lista com contador e badge
 */
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

/** Card de um cliente individual **/
@Composable
private fun ClienteCard(
    cliente: ClienteFirebase,
    saldoVisivel: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = getClienteBorderColor(cliente.saldo)
        ),
        border = BorderStroke(
            width = 2.dp,
            color = getClienteBorderColor(cliente.saldo)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            ClienteHeader(cliente = cliente)
            ClienteSaldo(cliente = cliente, saldoVisivel = saldoVisivel)
        }
    }
}

/**
 * Cabeçalho do card do cliente
 */
@Composable
private fun ClienteHeader(cliente: ClienteFirebase) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = cliente.nomeCompleto,
            style = MaterialTheme.typography.titleMedium
        )

        ClienteSaldoBadge(saldo = cliente.saldo)
    }
}

/**
 * Badge indicador do tipo de saldo
 */
@Composable
private fun ClienteSaldoBadge(saldo: Double) {
    Badge(
        containerColor = getClienteBorderColor(saldo)
    ) {
        Text(
            text = getClienteSaldoEmoji(saldo),
            color = CoresTexto.Principal,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * Seção de saldo do cliente
 */
@Composable
private fun ClienteSaldo(
    cliente: ClienteFirebase,
    saldoVisivel: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (saldoVisivel) "Saldo: R$ %.2f".format(cliente.saldo) else "Saldo: ****",
            style = MaterialTheme.typography.bodyLarge,
            color = CoresTexto.Principal
        )

        ClienteLimiteAviso(cliente = cliente, saldoVisivel = saldoVisivel)
    }
}

/**
 * Aviso de proximidade do limite
 */
@Composable
private fun ClienteLimiteAviso(
    cliente: ClienteFirebase,
    saldoVisivel: Boolean
) {
    if (saldoVisivel && cliente.saldo < 0) {
        val faltaParaLimite = cliente.saldo - cliente.limiteNegativo
        if (faltaParaLimite <= 10.0 && faltaParaLimite > 0) {
            Text(
                text = "⚠️ Faltam R$ %.2f para o limite".format(faltaParaLimite),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Função auxiliar para obter cor da borda baseada no saldo
 */
private fun getClienteBorderColor(saldo: Double): Color {
    return when {
        saldo > 0 -> CoresPastel.VerdeMenta
        saldo == 0.0 -> CoresPastel.PessegoPastel
        else -> CoresPastel.CoralSuave
    }
}

/**
 * Função auxiliar para obter emoji baseado no saldo
 */
private fun getClienteSaldoEmoji(saldo: Double): String {
    return when {
        saldo > 0 -> "💰"
        saldo == 0.0 -> "⚪"
        else -> "⚠️"
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
    val titulo: String
)

// ===========================================================================================
// SEÇÃO 8: TELA DE CADASTRO DE CLIENTE
// ===========================================================================================

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
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = { Text("Cadastrar Cliente") },
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                enabled = !isCarregando
            ) {
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
        }
    )
}

/**
 * Conteúdo principal da tela de cadastro
 */
@Composable
private fun CadastroContent(
    paddingValues: PaddingValues,
    cadastroState: CadastroState,
    viewModel: CantinaFirebaseViewModel
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
    viewModel: CantinaFirebaseViewModel
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
                Text(MSG_NOME_SOBRENOME, color = MaterialTheme.colorScheme.error)
            } else {
                Text(MSG_NOME_SOBRENOME)
            }
        }
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
        }
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
                Text(MSG_DATA_INVALIDA, color = MaterialTheme.colorScheme.error)
            }
        } else {
            Text(MSG_DATA_INVALIDA, color = MaterialTheme.colorScheme.error)
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
        }
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
    viewModel: CantinaFirebaseViewModel
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
    viewModel: CantinaFirebaseViewModel
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
 * Formata o telefone para exibição
 */
private fun formatarTelefone(telefone: String): String {
    return when (telefone.length) {
        0 -> ""
        in 1..2 -> "($telefone"
        in 3..6 -> "(${telefone.substring(0, 2)}) ${telefone.substring(2)}"
        in 7..10 -> "(${telefone.substring(0, 2)}) ${telefone.substring(2, 6)}-${telefone.substring(6)}"
        11 -> "(${telefone.substring(0, 2)}) ${telefone.substring(2, 7)}-${telefone.substring(7)}"
        else -> telefone
    }
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
    val coroutineScope: CoroutineScope
)

// ===========================================================================================
// SEÇÃO 9: TELA DE DETALHES DO CLIENTE
// ===========================================================================================

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
) {
    val clienteState = rememberClienteState(viewModel)
    val context = LocalContext.current

    // Busca o cliente na lista e carrega transações
    LaunchedEffect(clienteId, clienteState.clientes) {
        clienteState.onClienteChange(clienteState.clientes.find { it.id == clienteId })
        if (clienteState.cliente != null) {
            viewModel.carregarTransacoes(clienteId)
        }
    }

    // Se não encontrou o cliente, volta
    if (clienteState.cliente == null && !clienteState.isCarregando) {
        LaunchedEffect(clienteId) {
            delay(300)
            navController.popBackStack()
        }
        return
    }

    Scaffold(
        topBar = {
            ClienteTopBar(
                cliente = clienteState.cliente,
                isCarregando = clienteState.isCarregando,
                isAdmin = clienteState.isAdmin,
                onBackClick = { navController.popBackStack() },
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
                item {
                    ClienteSaldoCard(
                        cliente = clienteState.cliente,
                        saldoVisivel = clienteState.saldoVisivel
                    )
                }
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
                item {
                    ClienteExtratoHeader(
                        isAdmin = clienteState.isAdmin,
                        onRemoveClick = { clienteState.onShowRemoveDialogChange(true) }
                    )
                }
                if (clienteState.transacoes.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "Nenhuma transação realizada",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(clienteState.transacoes.size) { index ->
                        ClienteTransacaoItem(transacao = clienteState.transacoes[index])
                    }
                }
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
        navController = navController
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
    val isAdmin: Boolean
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
    onConfigClick: () -> Unit
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
            containerColor = CoresPastel.AzulSage,),
        windowInsets = WindowInsets(0, 0, 0, 0)
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
    onConfirmCompra: (Double) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
    onConfirmCompra: (Double) -> Unit
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
    saldoVisivel: Boolean
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
    onRemoveClick: () -> Unit
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
            .padding(horizontal = 16.dp, vertical = 4.dp)
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
                        DATE_FORMAT_FULL,
                        Locale.getDefault()
                    ).format(transacao.data.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ClienteDialogs(
    clienteState: ClienteState,
    viewModel: CantinaFirebaseViewModel,
    clienteId: String,
    navController: NavHostController
) {
    // Dialog para adicionar crédito (ADMIN)
    if (clienteState.showAddCreditDialog) {
        DialogAdicionarCredito(
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
            text = { Text(AppConstants.MSG_CONFIRMAR_EXCLUSAO) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removerCliente(clienteId)
                        clienteState.onShowRemoveDialogChange(false)
                        navController.popBackStack()
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
            title = { Text(AppConstants.MSG_SUCESSO) },
            text = { Text(AppConstants.MSG_CREDITO_ADICIONADO) },
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
            title = { Text(AppConstants.MSG_SUCESSO) },
            text = { Text(AppConstants.MSG_COMPRA_REALIZADA) },
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
    valor: Double
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

// ===========================================================================================
// SEÇÃO 10: DIALOGS CUSTOMIZADOS
// ===========================================================================================

/**
 * Dialog para adicionar crédito ao cliente
 */
@Composable
fun DialogAdicionarCredito(
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var valorCredito by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Crédito") },
        text = {
            Column {
                Text("Digite o valor a ser adicionado:")
                Spacer(modifier = Modifier.height(16.dp))
                CampoMonetario(
                    valor = valorCredito,
                    onValueChange = { valorCredito = it },
                    label = "Valor R$"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val valorEmCentavos = valorCredito.toLongOrNull() ?: 0L
                    val valor = valorEmCentavos / 100.0
                    if (valor > 0) {
                        onConfirm(valor)
                    }
                }
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Dialog para alterar limite negativo do cliente
 */
@Composable
fun DialogAlterarLimite(
    limiteAtual: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var novoLimite by remember {
        mutableStateOf(
            (kotlin.math.abs(limiteAtual) * 100).toLong().toString()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurações") },
        text = {
            Column {
                Text("Limite negativo atual: R$ %.2f".format(limiteAtual))
                Spacer(modifier = Modifier.height(16.dp))
                CampoMonetario(
                    valor = novoLimite,
                    onValueChange = { novoLimite = it },
                    label = "Novo limite"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val valorEmCentavos = novoLimite.toLongOrNull() ?: 0L
                    val valor = valorEmCentavos / 100.0
                    if (valor >= 0) {
                        onConfirm(-valor)
                    }
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Dialog de erro quando limite é excedido
 */
@Composable
fun DialogLimiteExcedido(
    mensagem: String,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onAdicionarCredito: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("❌ ", style = MaterialTheme.typography.headlineMedium)
                Text("Compra Não Autorizada")
            }
        },
        text = {
            Column {
                Text(
                    text = mensagem,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Mostra sugestão para admin
                if (isAdmin) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 Dica: Como administrador, você pode adicionar crédito ao cliente ou aumentar o limite negativo.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendi")
            }
        },
        dismissButton = if (isAdmin) {
            {
                TextButton(onClick = onAdicionarCredito) {
                    Text("Adicionar Crédito")
                }
            }
        } else null
    )
}

// ===========================================================================================
// SEÇÃO 11: TELA DE CONFIGURAÇÕES
// ===========================================================================================

/**
 * Tela de configurações do sistema
 * Exibe estatísticas, permite gerenciar funcionários e fazer logout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaConfiguracoesFirebase(
    viewModel: CantinaFirebaseViewModel
) {
    val configState = rememberConfigState(viewModel)
    val context = LocalContext.current

    // Carrega estatísticas ao abrir a tela
    LaunchedEffect(Unit) {
        viewModel.carregarEstatisticas()
    }

    Scaffold(
        topBar = {
            ConfigTopBar(isCarregando = configState.isCarregando)
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
private fun ConfigTopBar(isCarregando: Boolean) {
    TopAppBar(
        title = { Text("Configurações") },
        actions = {
            if (isCarregando) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(6.dp)
                        .padding(end = 8.dp),
                    strokeWidth = 2.dp,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CoresPastel.AzulSage),
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
            .padding(16.dp)
    ) {
        // Card de informações do usuário
        item {
            CardInformacoesUsuario(viewModel)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Card de estatísticas (apenas para admins)
        if (configState.isAdmin) {
            item { Spacer(modifier = Modifier.height(16.dp)) }
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

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Card sobre
        item {
            CardSobre()
        }

        // Card de ajuda
        item {
            Spacer(modifier = Modifier.height(16.dp))
            CardAjuda(configState.isAdmin)
        }
    }
}

/**
 * Seção de administração
 */
@Composable
private fun ConfigAdminSection(
    onAddFuncionarioClick: () -> Unit,
    onGerarRelatorioClick: () -> Unit
) {
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "⚙️ Administração",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    OutlinedButton(
        onClick = onAddFuncionarioClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = CoresTexto.Principal
        ),
        border = BorderStroke(1.dp, CoresPastel.VerdeMenta)
    ) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Adicionar Funcionário")
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(
        onClick = onGerarRelatorioClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("📄", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Gerar Relatório Completo")
    }
}

/**
 * Seção de conta
 */
@Composable
private fun ConfigContaSection(onLogoutClick: () -> Unit) {
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "🔐 Conta",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp)
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

/**
 * Dialogs da tela de configurações
 */
@Composable
private fun ConfigDialogs(
    configState: ConfigState,
    viewModel: CantinaFirebaseViewModel
) {
    // Dialog de confirmação de logout
    if (configState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { /* ação */ },
            containerColor = CoresPastel.CinzaPerola,
            title = {
                Text(
                    "Título do Dialog",
                    color = CoresTexto.Principal
                )
            },
            text = {
                Text(
                    "Conteúdo do dialog",
                    color = CoresTexto.Secundario
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { /* ação */ },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = CoresTexto.Principal
                    )
                ) {
                    Text("Confirmar")
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

/**
 * Card que exibe informações do usuário atual
 */
@Composable
fun CardInformacoesUsuario(viewModel: CantinaFirebaseViewModel) {
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
fun CardEstatisticas(estatisticas: Map<String, Any>) {
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
            Spacer(modifier = Modifier.height(8.dp))

            val totalClientes = estatisticas["totalClientes"] as? Int ?: 0
            val clientesPositivos = estatisticas["clientesPositivos"] as? Int ?: 0
            val clientesNegativos = estatisticas["clientesNegativos"] as? Int ?: 0
            val clientesZerados = estatisticas["clientesZerados"] as? Int ?: 0
            val saldoTotal = estatisticas["saldoTotal"] as? Double ?: 0.0

            Text("Total de Clientes: $totalClientes")
            Text("Clientes com saldo positivo: $clientesPositivos")
            Text("Clientes com saldo negativo: $clientesNegativos")
            Text("Clientes com saldo zerado: $clientesZerados")

            // LINHA CORRIGIDA: Usar HorizontalDivider ao invés de Divider
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

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
fun CardSobre() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "ℹ️ Sobre",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Versão: 2.0 (Firebase)")
            Text("Sistema de gerenciamento de cantina")
            Text("Desenvolvido com Kotlin + Jetpack Compose + Firebase")
        }
    }
}

/**
 * Card com dicas de uso baseado no tipo de usuário
 */
@Composable
fun CardAjuda(isAdmin: Boolean) {
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
                text = "💡 Dicas de Uso",
                style = MaterialTheme.typography.titleSmall
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
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = "Como FUNCIONÁRIO você pode:\n" +
                            "• Cadastrar novos clientes\n" +
                            "• Realizar vendas (débitos)\n" +
                            "• Visualizar saldos e históricos\n" +
                            "• Buscar clientes por nome",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Dialog para adicionar novo funcionário
 */
@Composable
fun DialogAdicionarFuncionario(
    onDismiss: () -> Unit,
    onConfirm: (email: String, senha: String, nome: String, isAdmin: Boolean) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var nome by remember { mutableStateOf("") }
    var isAdminNovo by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Funcionário") },
        text = {
            Column {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome Completo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
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
                    singleLine = true
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
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isAdminNovo,
                        onCheckedChange = { isAdminNovo = it }
                    )
                    Text(
                        text = "Administrador",
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
                Text("Criar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
