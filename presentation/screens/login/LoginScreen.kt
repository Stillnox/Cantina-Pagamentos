package com.cantina.pagamentos.presentation.screens.login

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cantina.pagamentos.data.models.EstadoAutenticacao
import com.cantina.pagamentos.presentation.theme.CoresPastel
import com.cantina.pagamentos.presentation.theme.CoresTexto
import com.cantina.pagamentos.presentation.viewmodels.CantinaFirebaseViewModel


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
    onLoginClick: (String, String) -> Unit,
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
    onLoginClick: (String, String) -> Unit,
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
        color = CoresPastel.AzulCeuPastel
    )
}

/**
 * Campos do formulário de login
 */
@Composable
private fun LoginFormFields(
    loginState: LoginState,
    isCarregando: Boolean,
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
    isCarregando: Boolean,
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
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = CoresPastel.VerdeMenta,
            unfocusedTextColor = CoresPastel.VerdeMenta,
            focusedLabelColor = CoresPastel.AzulCeuPastel,
            unfocusedLabelColor = CoresPastel.AzulCeuPastel,
        )
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
    isCarregando: Boolean,
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
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = CoresPastel.VerdeMenta,
            unfocusedTextColor = CoresPastel.VerdeMenta,
            focusedLabelColor = CoresPastel.AzulCeuPastel,
            unfocusedLabelColor = CoresPastel.AzulCeuPastel,
        )
    )
}

/**
 * Botão de login
 */
@Composable
private fun LoginButton(
    loginState: LoginState,
    isCarregando: Boolean,
    onLoginClick: (String, String) -> Unit,
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
                style = MaterialTheme.typography.titleMedium,
                color = CoresPastel.VerdeMenta
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
    viewModel: CantinaFirebaseViewModel,
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
    val onSenhaVisivelChange: (Boolean) -> Unit,
)
