package com.cantina.pagamentos

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.cantina.pagamentos.viewmodel.CantinaFirebaseViewModel
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.cantina.pagamentos.ui.theme.CantinaPastelTheme
import com.cantina.pagamentos.ui.theme.CoresSaldo
import com.cantina.pagamentos.ui.theme.CoresBadges
import com.cantina.pagamentos.ui.theme.CoresPastel
import com.cantina.pagamentos.ui.theme.CoresTexto
import com.cantina.pagamentos.ui.theme.getTextColor

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
    val anoAtual = Calendar.getInstance().get(Calendar.YEAR)
    if (ano < 1900 || ano > anoAtual) return false

    // Verifica se a data não é futura
    if (ano == anoAtual) {
        val mesAtual = Calendar.getInstance().get(Calendar.MONTH) + 1
        val diaAtual = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

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
        placeholder = { Text(placeholder) }
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
        containerColor = CoresPastel.VerdeSage,  // Fundo verde sage
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
                TelaConfiguracoesFirebase(navController, viewModel)
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
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val estadoAuth by viewModel.estadoAutenticacao.collectAsState()
    val isCarregando by viewModel.isCarregando.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Sistema Cantina - Login")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CoresPastel.VerdeMenta,
                    titleContentColor = CoresTexto.Principal,
                    actionIconContentColor = CoresTexto.Principal
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Card principal com formulário de login
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
                    // Logo
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

                    Spacer(modifier = Modifier.height(24.dp))

                    // Campo Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim() },
                        label = { Text("Email") },
                        placeholder = { Text("funcionario@cantina.com") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isCarregando,
                        leadingIcon = {
                            Text("📧", style = MaterialTheme.typography.titleLarge)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo Senha
                    OutlinedTextField(
                        value = senha,
                        onValueChange = { senha = it },
                        label = { Text("Senha") },
                        placeholder = { Text("Digite sua senha") },
                        visualTransformation = if (senhaVisivel) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isCarregando,
                        leadingIcon = {
                            Text("🔒", style = MaterialTheme.typography.titleLarge)
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { senhaVisivel = !senhaVisivel }
                            ) {
                                Text(
                                    if (senhaVisivel) "👁️" else "👁️‍🗨️",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botão Login
                    Button(
                        onClick = {
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
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isCarregando && email.isNotEmpty() && senha.isNotEmpty()
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card de informações
            Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CoresPastel.PessegoPastel  // Pêssego para avisos
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
    }

    // Observa mudanças no estado de autenticação
    LaunchedEffect(estadoAuth) {
        when (estadoAuth) {
            is EstadoAutenticacao.Autenticado -> {
                onLoginSucesso()
            }
            else -> {}
        }
    }
}

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
    var busca by remember { mutableStateOf("") }
    val clientes by viewModel.clientes.collectAsState()
    val isCarregando by viewModel.isCarregando.collectAsState()
    val context = LocalContext.current

    var forceUpdate by remember { mutableIntStateOf(0) }
    LaunchedEffect(clientes) {
        forceUpdate++
    }

    // Filtra clientes baseado na busca e no filtro selecionado
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(titulo)
                        Text(
                            text = "Funcionário: ${viewModel.getNomeFuncionario()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0), // remove o padding interno do TopAppBar
                actions = {
                    if (isCarregando) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }

                    // Botão exportar PDF - APENAS PARA ADMINS
                    if (viewModel.isadmin()) {
                        IconButton(onClick = {
                            val uri = viewModel.gerarPdfListaClientes(context, filtro)
                            if (uri != null) {
                                viewModel.compartilharPdf(context, uri, "Relatório Cantina - $filtro")
                            }
                        }) {
                            Text("📄", style = MaterialTheme.typography.headlineMedium)
                        }
                    }

                    // Botão adicionar cliente (continua disponível para todos)
                    IconButton(onClick = { navController.navigate("cadastro") }) {
                        Text("➕", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isCarregando && clientes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFAEC3B0))
                    .padding(paddingValues)
                    .padding(6.dp)
            ) {
                // Campo de busca
                OutlinedTextField(
                    value = busca,
                    onValueChange = { busca = it },
                    label = { Text("Buscar por nome") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Contador e badge de admin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${clientesFiltrados.size} cliente(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoresTexto.Secundario  // Cor de texto secundário
                    )

                    if (viewModel.isadmin()) {
                        Badge(
                            containerColor = CoresBadges.Admin  // Amarelo vanilla
                        ) {
                            Text(
                                "ADMIN",
                                color = CoresTexto.Principal  // Texto escuro
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Lista de clientes
                LazyColumn {
                    items(
                        items = clientesFiltrados,
                        key = { it.id }
                    ) { cliente ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = { navController.navigate("cliente/${cliente.id}") },
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    cliente.saldo > 0 -> CoresSaldo.Positivo     // Verde menta
                                    cliente.saldo == 0.0 -> CoresSaldo.Zerado    // Verde sage
                                    else -> CoresSaldo.Negativo                  // Rosa blush
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp)
                            ) {
                                Text(
                                    text = cliente.nomeCompleto,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Saldo: R$ %.2f".format(cliente.saldo),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = CoresTexto.Principal  // Sempre texto escuro em fundos pastéis
                                    )


                                    // Calcula quanto falta para o limite
                                    val faltaParaLimite = cliente.saldo - cliente.limiteNegativo

                                    // Aviso de proximidade do limite
                                    if (cliente.saldo < 0 && faltaParaLimite <= 10.0 && faltaParaLimite > 0) {
                                        Text(
                                            text = "⚠️ Faltam R$ %.2f para o limite".format(faltaParaLimite),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

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
    // Estados dos campos
    var nome by remember { mutableStateOf("") }
    var dataNascimento by remember { mutableStateOf("") }
    var dataErro by remember { mutableStateOf(false) }
    var telefone by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    var nomeErro by remember { mutableStateOf(false) }

    // Estado de carregamento e navegação
    val isCarregando by viewModel.isCarregando.collectAsState()
    var cadastroRealizado by remember { mutableStateOf(false) }

    // Snackbar para mensagens
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Observa mensagens do ViewModel
    LaunchedEffect(Unit) {
        viewModel.mensagem.collect { mensagem ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(mensagem)
            }
        }
    }

    // Navega de volta após cadastro bem-sucedido
    LaunchedEffect(cadastroRealizado) {
        if (cadastroRealizado) {
            delay(1000)
            navController.popBackStack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Cadastrar Cliente") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!isCarregando) {
                                navController.popBackStack()
                            }
                        },
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Card informativo
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = CoresPastel.AzulCeuPastel  // Azul céu para informações
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

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Nome Completo
            OutlinedTextField(
                value = nome,
                onValueChange = {
                    nome = it
                    // Valida se tem pelo menos 2 palavras
                    val nomePartes = it.trim().split(" ").filter { parte -> parte.isNotEmpty() }
                    nomeErro = it.isNotEmpty() && nomePartes.size < 2
                },
                label = { Text("Nome Completo") },
                placeholder = { Text("Ex: João da Silva") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isCarregando,
                isError = nomeErro,
                supportingText = {
                    if (nomeErro) {
                        Text(MSG_NOME_SOBRENOME, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text(MSG_NOME_SOBRENOME)
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Campo Data de Nascimento
            OutlinedTextField(
                value = dataNascimento,
                onValueChange = { novoTexto ->
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
                label = { Text("Data de Nascimento") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (!focusState.isFocused && dataNascimento.length == 6) {
                            val dia = dataNascimento.substring(0, 2)
                            val mes = dataNascimento.substring(2, 4)
                            val ano = corrigirAno(dataNascimento.substring(4, 6))
                            dataNascimento = "$dia$mes$ano"
                        }
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                placeholder = { Text("DD/MM/AAAA") },
                visualTransformation = DateVisualTransformation(),
                isError = dataErro,
                enabled = !isCarregando,
                supportingText = if (dataErro) {
                    {
                        val hoje = Calendar.getInstance()
                        val diaHoje = hoje[Calendar.DAY_OF_MONTH]
                        val mesHoje = hoje[Calendar.MONTH] + 1
                        val anoHoje = hoje[Calendar.YEAR]

                        if (dataNascimento.length >= 8) {
                            val ano = dataNascimento.substring(4, 8).toIntOrNull() ?: 0
                            val mes = dataNascimento.substring(2, 4).toIntOrNull() ?: 0
                            val dia = dataNascimento.substring(0, 2).toIntOrNull() ?: 0

                            if (ano > anoHoje || (ano == anoHoje && mes > mesHoje) ||
                                (ano == anoHoje && mes == mesHoje && dia > diaHoje)) {
                                Text("Data não pode ser futura", color = MaterialTheme.colorScheme.error)
                            } else {
                                Text(MSG_DATA_INVALIDA, color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Text(MSG_DATA_INVALIDA, color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else if (dataNascimento.length == 6) {
                    {
                        Text(
                            "Digite o ano com 4 dígitos ou toque fora para completar",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else null
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Campo Telefone
            var telefoneEditando by remember { mutableStateOf("") }
            var telefoneFocado by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = if (telefoneFocado) telefoneEditando else {
                    when (telefoneEditando.length) {
                        0 -> ""
                        in 1..2 -> "($telefoneEditando"
                        in 3..6 -> "(${telefoneEditando.substring(0, 2)}) ${telefoneEditando.substring(2)}"
                        in 7..10 -> "(${telefoneEditando.substring(0, 2)}) ${telefoneEditando.substring(2, 6)}-${telefoneEditando.substring(6)}"
                        11 -> "(${telefoneEditando.substring(0, 2)}) ${telefoneEditando.substring(2, 7)}-${telefoneEditando.substring(7)}"
                        else -> telefoneEditando
                    }
                },
                onValueChange = { novoTexto ->
                    telefoneEditando = novoTexto.filter { it.isDigit() }.take(11)
                    telefone = telefoneEditando
                },
                label = { Text("Telefone") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        telefoneFocado = focusState.isFocused
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                placeholder = { Text(if (telefoneFocado) "11999999999" else "(11) 99999-9999") },
                enabled = !isCarregando,
                supportingText = {
                    when (telefoneEditando.length) {
                        10 -> Text("Telefone fixo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        11 -> Text("Celular", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else -> if (!telefoneFocado && telefoneEditando.isNotEmpty() && telefoneEditando.length < 10) {
                            Text("Telefone incompleto", color = MaterialTheme.colorScheme.error)
                        } else null
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Botão Cadastrar
            Button(
                onClick = {
                    val nomePartes = nome.trim().split(" ").filter { it.isNotEmpty() }

                    when {
                        nome.isEmpty() || dataNascimento.isEmpty() || telefone.isEmpty() -> {
                            // Campos vazios - botão já está desabilitado
                        }
                        nomePartes.size < 2 -> {
                            // Nome deve ter pelo menos 2 partes - já validado visualmente
                        }
                        dataNascimento.length != 6 && dataNascimento.length != 8 -> {
                            // Data deve ter 6 ou 8 dígitos
                        }
                        telefone.length < 10 -> {
                            // Telefone deve ter pelo menos 10 dígitos
                        }
                        else -> {
                            // Completa o ano se necessário
                            val dataCompleta = if (dataNascimento.length == 6) {
                                val dia = dataNascimento.substring(0, 2)
                                val mes = dataNascimento.substring(2, 4)
                                val ano = corrigirAno(dataNascimento.substring(4, 6))
                                "$dia$mes$ano"
                            } else {
                                dataNascimento
                            }

                            if (validarData(dataCompleta)) {
                                // Formata os dados
                                val dataFormatada = "${dataCompleta.substring(0,2)}/${dataCompleta.substring(2,4)}/${dataCompleta.substring(4,8)}"
                                val telefoneFormatado = when (telefone.length) {
                                    11 -> "(${telefone.substring(0,2)}) ${telefone.substring(2,7)}-${telefone.substring(7,11)}"
                                    10 -> "(${telefone.substring(0,2)}) ${telefone.substring(2,6)}-${telefone.substring(6,10)}"
                                    else -> telefone
                                }

                                // Cria o cliente no Firebase
                                viewModel.criarCliente(
                                    nomeCompleto = nome.trim(),
                                    dataNascimento = dataFormatada,
                                    telefone = telefoneFormatado
                                )

                                cadastroRealizado = true
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCarregando && nome.isNotEmpty() && dataNascimento.isNotEmpty() &&
                        telefone.isNotEmpty() && !nomeErro
            ) {
                if (isCarregando) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

            Spacer(modifier = Modifier.height(16.dp))

            // Card de ajuda
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
    }
}

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
    // Estados locais
    var cliente by remember { mutableStateOf<ClienteFirebase?>(null) }
    var valorOperacao by remember { mutableStateOf("") }
    var showCreditoDialog by remember { mutableStateOf(false) }
    var showDebitoDialog by remember { mutableStateOf(false) }
    var showAddCreditDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showLimiteDialog by remember { mutableStateOf(false) }
    var showLimiteExcedidoDialog by remember { mutableStateOf(false) }
    var mensagemLimiteExcedido by remember { mutableStateOf("") }

    val context = LocalContext.current
    val isCarregando by viewModel.isCarregando.collectAsState()
    val clientes by viewModel.clientes.collectAsState()
    val transacoes by viewModel.transacoesCliente.collectAsState()

    // Busca o cliente na lista
    LaunchedEffect(clienteId, clientes) {
        cliente = clientes.find { it.id == clienteId }
        if (cliente != null) {
            viewModel.carregarTransacoes(clienteId)
        }
    }

    // Se não encontrou o cliente, volta
    if (cliente == null && !isCarregando) {
        LaunchedEffect(clienteId) {
            delay(300)
            navController.popBackStack()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        cliente?.nomeCompleto ?: "Carregando...",
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
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

                    // Botão exportar PDF
                    IconButton(
                        onClick = {
                            val uri = viewModel.gerarPdfExtratoCliente(context, clienteId)
                            if (uri != null) {
                                viewModel.compartilharPdf(context, uri, "Extrato - ${cliente?.nomeCompleto}")
                            }
                        },
                        enabled = !isCarregando && cliente != null
                    ) {
                        Text("📄", style = MaterialTheme.typography.headlineSmall)
                    }

                    // Botão adicionar crédito (só admin)
                    if (viewModel.isadmin()) {
                        Button(
                            onClick = { showAddCreditDialog = true },
                            enabled = !isCarregando,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CoresPastel.VerdeMenta,
                                contentColor = CoresTexto.Principal
                            )
                        ) {
                            Text("+ Crédito")
                        }
                    }

                    // Botão configurações (só admin)
                    if (viewModel.isadmin()) {
                        IconButton(
                            onClick = { showLimiteDialog = true },
                            enabled = !isCarregando
                        ) {
                            Text("⚙", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (cliente != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Card do Saldo
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                cliente!!.saldo > 0 -> CoresSaldo.Positivo
                                cliente!!.saldo == 0.0 -> CoresSaldo.Zerado
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
                                color = CoresTexto.Principal  // Sempre texto escuro
                            )
                            Text(
                                text = "R$ %.2f".format(cliente!!.saldo),
                                style = MaterialTheme.typography.headlineLarge,
                                color = CoresTexto.Principal  // Sempre texto escuro
                            )
                            //Spacer(modifier = Modifier.height(4.dp))
                            Text("Data de Nascimento: ${cliente!!.dataNascimento}")
                            Text("Telefone: ${cliente!!.telefone}")
                            Text(
                                text = "Limite: R$ %.2f".format(cliente!!.limiteNegativo),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Card Realizar Compra
                stickyHeader {
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
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            CampoMonetario(
                                valor = valorOperacao,
                                onValueChange = { valorOperacao = it },
                                label = "Valor da Compra R$",
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Button(
                                onClick = {
                                    val valorEmCentavos = valorOperacao.toLongOrNull() ?: 0L
                                    val valor = valorEmCentavos / 100.0
                                    if (valor > 0) {
                                        // Verifica localmente primeiro se vai exceder o limite
                                        cliente?.let { clienteAtual ->
                                            val saldoAposCompra = clienteAtual.saldo - valor

                                            if (saldoAposCompra < clienteAtual.limiteNegativo) {
                                                // Mostra dialog de erro imediatamente
                                                mensagemLimiteExcedido = "Compra não autorizada!\n\n" +
                                                        "Valor da compra: R$ %.2f\n".format(valor) +
                                                        "Saldo atual: R$ %.2f\n".format(clienteAtual.saldo) +
                                                        "Saldo após compra: R$ %.2f\n".format(saldoAposCompra) +
                                                        "Limite permitido: R$ %.2f\n\n".format(clienteAtual.limiteNegativo) +
                                                        "O cliente não possui saldo suficiente para esta compra."
                                                showLimiteExcedidoDialog = true
                                                valorOperacao = ""
                                            } else {
                                                // Se não vai exceder, tenta realizar a compra
                                                viewModel.realizarCompra(clienteId, valor) { sucesso, mensagem ->
                                                    if (sucesso) {
                                                        showDebitoDialog = true
                                                    } else {
                                                        mensagemLimiteExcedido = mensagem
                                                        showLimiteExcedidoDialog = true
                                                    }
                                                    valorOperacao = ""
                                                }
                                            }
                                        }
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

                            // Indicação visual do saldo disponível
                            val saldoDisponivel = cliente!!.saldo - cliente!!.limiteNegativo
                            Column {
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

                                // Aviso de proximidade do limite
                                if (saldoDisponivel > 0 && saldoDisponivel <= 10.0) {
                                    Text(
                                        text = "⚠️ Atenção: Cliente próximo do limite",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Cabeçalho do Extrato
                item {
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

                        if (viewModel.isadmin()) {
                            TextButton(
                                onClick = { showRemoveDialog = true },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Remover Cliente")
                            }
                        }
                    }
                }

                // Lista de transações
                if (transacoes.isEmpty()) {
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
                    items(transacoes.size) { index ->
                        val transacao = transacoes[index]
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
                }

                // Espaço extra no final
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // ===== DIALOGS =====

    // Dialog para adicionar crédito (ADMIN)
    if (showAddCreditDialog) {
        DialogAdicionarCredito(
            onDismiss = { showAddCreditDialog = false },
            onConfirm = { valor ->
                viewModel.adicionarCredito(clienteId, valor)
                showAddCreditDialog = false
                showCreditoDialog = true
            }
        )
    }

    // Dialog para alterar limite (ADMIN)
    if (showLimiteDialog) {
        DialogAlterarLimite(
            limiteAtual = cliente?.limiteNegativo ?: -50.0,
            onDismiss = { showLimiteDialog = false },
            onConfirm = { novoLimite ->
                viewModel.atualizarLimiteNegativo(clienteId, novoLimite)
                showLimiteDialog = false
            }
        )
    }

    // Dialog para remover cliente (ADMIN)
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Confirmar Exclusão") },
            text = { Text(AppConstants.MSG_CONFIRMAR_EXCLUSAO) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removerCliente(clienteId)
                        showRemoveDialog = false
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
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog de sucesso - Crédito
    if (showCreditoDialog) {
        AlertDialog(
            onDismissRequest = { showCreditoDialog = false },
            title = { Text(AppConstants.MSG_SUCESSO) },
            text = { Text(AppConstants.MSG_CREDITO_ADICIONADO) },
            confirmButton = {
                TextButton(onClick = { showCreditoDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Dialog de sucesso - Débito
    if (showDebitoDialog) {
        AlertDialog(
            onDismissRequest = { showDebitoDialog = false },
            title = { Text(AppConstants.MSG_SUCESSO) },
            text = { Text(AppConstants.MSG_COMPRA_REALIZADA) },
            confirmButton = {
                TextButton(onClick = { showDebitoDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Dialog de erro - Limite excedido
    if (showLimiteExcedidoDialog) {
        DialogLimiteExcedido(
            mensagem = mensagemLimiteExcedido,
            isAdmin = viewModel.isadmin(),
            onDismiss = { showLimiteExcedidoDialog = false },
            onAdicionarCredito = {
                showLimiteExcedidoDialog = false
                showAddCreditDialog = true
            }
        )
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
    navController: NavHostController,
    viewModel: CantinaFirebaseViewModel,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAddFuncionarioDialog by remember { mutableStateOf(false) }

    val isCarregando by viewModel.isCarregando.collectAsState()
    val estatisticas by viewModel.estatisticas.collectAsState()

    val context = LocalContext.current

    // Carrega estatísticas ao abrir a tela
    LaunchedEffect(Unit) {
        viewModel.carregarEstatisticas()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
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
    ) { paddingValues ->
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

            // Card de estatísticas
            if (viewModel.isadmin()) {
                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    CardEstatisticas(estatisticas)
                }
            }

            // Seção de Administração (apenas para admins)
            if (viewModel.isadmin()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "⚙️ Administração",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Botão adicionar funcionário
                item {
                    OutlinedButton(
                        onClick = { showAddFuncionarioDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CoresTexto.Principal
                                ),
                        border = BorderStroke(1.dp, CoresPastel.VerdeMenta)
                    )
                     {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adicionar Funcionário")
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Botão gerar relatório
                item {
                    OutlinedButton(
                        onClick = {
                            val uri = viewModel.gerarRelatorioCompleto(context)
                            if (uri != null) {
                                viewModel.compartilharPdf(context, uri, "Relatório Completo - Sistema Cantina")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📄", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gerar Relatório Completo")
                    }
                }
            }

            // Seção Conta
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "🔐 Conta",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Botão fazer logout
            item {
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoresPastel.VerdeMenta,
                        contentColor = CoresTexto.Principal
                    )
                ) {
                    Text("Sair da Conta")
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Card sobre
            item {
                CardSobre()
            }

            // Card de ajuda
            item {
                Spacer(modifier = Modifier.height(16.dp))
                CardAjuda(viewModel.isadmin())
            }
        }
    }

    // ===== DIALOGS =====

    // Dialog de confirmação de logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { /* ação */ },
            containerColor = CoresPastel.CinzaPerola,  // Fundo cinza pérola
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
    if (showAddFuncionarioDialog) {
        DialogAdicionarFuncionario(
            onDismiss = { showAddFuncionarioDialog = false },
            onConfirm = { email, senha, nome, isAdminNovo ->
                viewModel.criarFuncionario(email, senha, nome, isAdminNovo)
                showAddFuncionarioDialog = false
            }
        )
    }
}

/**
 * Card que exibe informações do usuário atual
 */
@Composable
fun CardInformacoesUsuario(viewModel: CantinaFirebaseViewModel) {
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
                    containerColor = if (viewModel.isadmin())
                        CoresBadges.Admin        // Amarelo vanilla
                    else
                        CoresBadges.Funcionario  // Azul céu
                ) {
                    Text(
                        text = if (viewModel.isadmin()) "ADMINISTRADOR" else "FUNCIONÁRIO",
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
/**
 * Card que exibe estatísticas do sistema - VERSÃO CORRIGIDA
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
