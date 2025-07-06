package com.cantina.pagamentos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cantina.pagamentos.data.models.EstadoAutenticacao
import com.cantina.pagamentos.presentation.screens.clientes.cadastro.TelaCadastroFirebase
import com.cantina.pagamentos.presentation.screens.clientes.detalhes.TelaClienteFirebase
import com.cantina.pagamentos.presentation.screens.clientes.lista.TelaListaClientesFirebase
import com.cantina.pagamentos.presentation.screens.configuracoes.ConfiguracoesScreen
import com.cantina.pagamentos.presentation.screens.login.TelaLogin
import com.cantina.pagamentos.presentation.theme.CantinaPastelTheme
import com.cantina.pagamentos.presentation.viewmodels.CantinaFirebaseViewModel
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.flow.collectLatest

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
 */
@Composable
fun TelaPrincipal(
    navController: NavHostController,
    viewModel: CantinaFirebaseViewModel,
    snackbarHostState: SnackbarHostState,
) {
    Scaffold(
        // Sem bottomBar
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "todos",
            modifier = Modifier.padding(paddingValues)
        ) {
            // Todas as rotas continuam iguais
            composable("todos") {
                TelaListaClientesFirebase(navController, viewModel, "todos")
            }
            // Rotas permanecem as mesmas
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
                ConfiguracoesScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
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
