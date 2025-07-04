// ServicoAutenticacao.kt
package com.cantina.pagamentos.data.services

import com.cantina.pagamentos.data.models.EstadoAutenticacao
import com.cantina.pagamentos.data.models.FuncionarioFirebase
import com.cantina.pagamentos.data.models.ResultadoFirebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Serviço responsável pela autenticação e gerenciamento de usuários
 *
 * Este serviço funciona como um "porteiro" do aplicativo:
 * - Verifica quem está tentando entrar (login)
 * - Mantém registro de quem está dentro (sessão)
 * - Controla o que cada pessoa pode fazer (permissões)
 */
class ServicoAutenticacao {

    // Instâncias do Firebase
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Estado atual da autenticação (observável)
    private val _estadoAutenticacao = MutableStateFlow<EstadoAutenticacao>(EstadoAutenticacao.Carregando)
    val estadoAutenticacao: StateFlow<EstadoAutenticacao> = _estadoAutenticacao

    // Funcionário atualmente logado
    private var funcionarioAtual: FuncionarioFirebase? = null

    init {
        // Quando o serviço inicia, verifica se já tem alguém logado
        verificarUsuarioLogado()
    }

    /**
     * Verifica se já existe um usuário logado
     * Isso é útil quando o app é aberto novamente
     */
    private fun verificarUsuarioLogado() {
        val usuarioFirebase = auth.currentUser

        if (usuarioFirebase != null) {
            // Se tem alguém logado, busca os dados completos
            CoroutineScope(Dispatchers.IO).launch {
                carregarDadosFuncionario(usuarioFirebase)
            }
        } else {
            // Se não tem ninguém logado
            _estadoAutenticacao.value = EstadoAutenticacao.NaoAutenticado
        }
    }

    /**
     * Realiza o login do funcionário
     *
     * @param email Email do funcionário
     * @param senha Senha do funcionário
     * @return ResultadoFirebase indicando sucesso ou erro
     */
    suspend fun fazerLogin(email: String, senha: String): ResultadoFirebase<FuncionarioFirebase> {
        return try {
            println("🔥 [ServicoAutenticacao] Iniciando login para: $email")
            
            // Verifica se o Firebase Auth está disponível
            if (auth == null) {
                println("🔥 [ServicoAutenticacao] ERRO: Firebase Auth não inicializado")
                return ResultadoFirebase.Erro("Firebase não inicializado")
            }
            
            // 1. Faz login no Firebase Auth
            val resultado = auth.signInWithEmailAndPassword(email, senha).await()
            val usuario = resultado.user

            println("🔥 [ServicoAutenticacao] Login Firebase Auth: ${if (usuario != null) "SUCESSO" else "FALHOU"}")

            if (usuario != null) {
                println("🔥 [ServicoAutenticacao] Usuário ID: ${usuario.uid}")
                
                // 2. Se login funcionou, busca dados completos do funcionário
                val funcionario = carregarDadosFuncionario(usuario)

                if (funcionario != null) {
                    println("🔥 [ServicoAutenticacao] Funcionário carregado: ${funcionario.nome}")
                    ResultadoFirebase.Sucesso(funcionario)
                } else {
                    println("🔥 [ServicoAutenticacao] ERRO: Funcionário não encontrado no Firestore")
                    // Se não encontrou dados do funcionário, faz logout
                    auth.signOut()
                    ResultadoFirebase.Erro("Funcionário não encontrado no sistema")
                }
            } else {
                println("🔥 [ServicoAutenticacao] ERRO: Usuário null após login")
                ResultadoFirebase.Erro("Erro ao fazer login")
            }

        } catch (e: Exception) {
            println("🔥 [ServicoAutenticacao] EXCEÇÃO: ${e.message}")
            e.printStackTrace()
            
            // Traduz erros comuns para português
            val mensagemErro = when {
                e.message?.contains("password") == true -> "Senha incorreta"
                e.message?.contains("no user") == true -> "Email não cadastrado"
                e.message?.contains("network") == true -> "Erro de conexão com internet"
                e.message?.contains("GoogleApiAvailability") == true -> "Google Play Services não disponível"
                e.message?.contains("SecurityException") == true -> "Erro de configuração do Firebase"
                else -> "Erro ao fazer login: ${e.message}"
            }
            ResultadoFirebase.Erro(mensagemErro)
        }
    }

    /**
     * Carrega os dados completos do funcionário do Firestore
     */
    private suspend fun carregarDadosFuncionario(usuario: FirebaseUser): FuncionarioFirebase? {
        return try {
            // Busca no Firestore usando o ID do usuário
            val documento = firestore.collection("funcionarios")
                .document(usuario.uid)
                .get()
                .await()

            if (documento.exists()) {
                val funcionario = documento.toObject(FuncionarioFirebase::class.java)

                if (funcionario != null && funcionario.ativo) {
                    // Salva o funcionário atual e atualiza o estado
                    funcionarioAtual = funcionario
                    _estadoAutenticacao.value = EstadoAutenticacao.Autenticado(funcionario)
                    funcionario
                } else {
                    // Se o funcionário está inativo
                    _estadoAutenticacao.value = EstadoAutenticacao.NaoAutenticado
                    null
                }
            } else {
                _estadoAutenticacao.value = EstadoAutenticacao.NaoAutenticado
                null
            }
        } catch (e: Exception) {
            _estadoAutenticacao.value = EstadoAutenticacao.NaoAutenticado
            null
        }
    }

    /**
     * Faz logout do funcionário
     */
    fun fazerLogout() {
        auth.signOut()
        funcionarioAtual = null
        _estadoAutenticacao.value = EstadoAutenticacao.NaoAutenticado
    }

    /**
     * Verifica se o funcionário atual é administrador
     */
    fun isAdmin(): Boolean {
        return funcionarioAtual?.isadmin ?: false
    }

    /**
     * Retorna o funcionário atualmente logado
     */
    fun getFuncionarioAtual(): FuncionarioFirebase? {
        return funcionarioAtual
    }

    /**
     * Cria uma nova conta de funcionário (apenas admins podem fazer isso)
     *
     * @param email Email do novo funcionário
     * @param senha Senha inicial
     * @param nome Nome completo
     * @param isAdmin Se é administrador ou não
     */
    suspend fun criarFuncionario(
        email: String,
        senha: String,
        nome: String,
        isNovoFuncionarioAdmin: Boolean
    ): ResultadoFirebase<String> {
        // Verifica se quem está criando é admin
        if (!this.isAdmin()) {
            return ResultadoFirebase.Erro("Apenas administradores podem criar funcionários")
        }

        return try {
            // 1. Cria a conta no Firebase Auth
            val resultado = auth.createUserWithEmailAndPassword(email, senha).await()
            val novoUsuario = resultado.user

            if (novoUsuario != null) {
                // 2. Cria o documento do funcionário no Firestore
                val novoFuncionario = FuncionarioFirebase(
                    id = novoUsuario.uid,
                    nome = nome,
                    email = email,
                    isadmin = isNovoFuncionarioAdmin,
                    ativo = true
                )

                firestore.collection("funcionarios")
                    .document(novoUsuario.uid)
                    .set(novoFuncionario)
                    .await()

                ResultadoFirebase.Sucesso("Funcionário criado com sucesso")
            } else {
                ResultadoFirebase.Erro("Erro ao criar funcionário")
            }

        } catch (e: Exception) {
            val mensagemErro = when {
                e.message?.contains("email") == true -> "Email já cadastrado"
                e.message?.contains("password") == true -> "Senha deve ter pelo menos 6 caracteres"
                else -> "Erro ao criar funcionário: ${e.message}"
            }
            ResultadoFirebase.Erro(mensagemErro)
        }
    }

    /**
     * Lista todos os funcionários (apenas admins)
     */
    suspend fun listarFuncionarios(): ResultadoFirebase<List<FuncionarioFirebase>> {
        if (!isAdmin()) {
            return ResultadoFirebase.Erro("Apenas administradores podem ver funcionários")
        }

        return try {
            val funcionarios = firestore.collection("funcionarios")
                .get()
                .await()
                .toObjects(FuncionarioFirebase::class.java)

            ResultadoFirebase.Sucesso(funcionarios)
        } catch (e: Exception) {
            ResultadoFirebase.Erro("Erro ao buscar funcionários: ${e.message}")
        }
    }

    /**
     * Ativa ou desativa um funcionário (apenas admins)
     */
    suspend fun alterarStatusFuncionario(funcionarioId: String, ativo: Boolean): ResultadoFirebase<String> {
        if (!isAdmin()) {
            return ResultadoFirebase.Erro("Apenas administradores podem alterar funcionários")
        }

        return try {
            firestore.collection("funcionarios")
                .document(funcionarioId)
                .update("ativo", ativo)
                .await()

            val status = if (ativo) "ativado" else "desativado"
            ResultadoFirebase.Sucesso("Funcionário $status com sucesso")
        } catch (e: Exception) {
            ResultadoFirebase.Erro("Erro ao alterar funcionário: ${e.message}")
        }
    }
}