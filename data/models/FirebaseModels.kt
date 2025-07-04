package com.cantina.pagamentos.data.models


import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Modelo de Cliente para Firebase Firestore
 *
 * IMPORTANTE: Para o Firebase funcionar corretamente:
 * - Todas as propriedades devem ter valores padrão
 * - Use @DocumentId para o ID do documento
 * - Use tipos compatíveis com Firestore (não use Date diretamente, use Timestamp)
 */
data class ClienteFirebase(
    @DocumentId // Esta anotação diz ao Firebase que este é o ID do documento
    val id: String = "",
    val nomeCompleto: String = "",
    val dataNascimento: String = "",
    val telefone: String = "",
    val saldo: Double = 0.0,
    val limiteNegativo: Double = -50.0,
    val criadoEm: Timestamp = Timestamp.now(), // Quando o cliente foi cadastrado
    val ultimaAtualizacao: Timestamp = Timestamp.now()
) {
    // Construtor sem argumentos necessário para o Firebase
    constructor() : this("")
}

/**
 * Modelo de Transação para Firebase
 * Será armazenada como subcoleção do cliente
 */
data class TransacaoFirebase(
    @DocumentId
    val id: String = "",
    val tipo: String = "", // "CREDITO" ou "DEBITO"
    val valor: Double = 0.0,
    val data: Timestamp = Timestamp.now(),
    val descricao: String = "",
    val funcionarioId: String = "", // ID do funcionário que fez a operação
    val funcionarioNome: String = "" // Nome do funcionário para referência
) {
    constructor() : this("")
}

/**
 * Modelo de Funcionário para Firebase
 * Representa os usuários que podem fazer login no sistema
 */
data class FuncionarioFirebase(
    @DocumentId
    val id: String = "",
    val nome: String = "",
    val email: String = "",
    val isadmin: Boolean = false, // true = pode adicionar saldo, false = apenas vender
    val ativo: Boolean = true, // Para desativar funcionários sem deletar
    val criadoEm: Timestamp = Timestamp.now()
) {
    constructor() : this("")
}

/**
 * Estados de autenticação do usuário
 * Ajuda a controlar o que mostrar na tela
 */
sealed class EstadoAutenticacao {
    object Carregando : EstadoAutenticacao()
    object NaoAutenticado : EstadoAutenticacao()
    data class Autenticado(val funcionario: FuncionarioFirebase) : EstadoAutenticacao()
}



/**
 * Resultado de operações no Firebase
 * Ajuda a lidar com sucessos e erros
 */
sealed class ResultadoFirebase<out T> {
    data class Sucesso<T>(val dados: T) : ResultadoFirebase<T>()
    data class Erro(val mensagem: String) : ResultadoFirebase<Nothing>()
    object Carregando : ResultadoFirebase<Nothing>()
}