package com.cantina.pagamentos.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cantina.pagamentos.models.ClienteFirebase
import com.cantina.pagamentos.models.EstadoAutenticacao
import com.cantina.pagamentos.models.ResultadoFirebase
import com.cantina.pagamentos.models.TransacaoFirebase
import com.cantina.pagamentos.services.ServicoAutenticacao
import com.cantina.pagamentos.services.ServicoDados
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel principal do aplicativo com Firebase
 *
 * Este ViewModel é como um "centro de controle" que:
 * - Recebe comandos da interface (botões, formulários)
 * - Processa esses comandos usando os serviços
 * - Atualiza a interface com os resultados
 *
 * A grande diferença agora é que tudo está na nuvem!
 */
class CantinaFirebaseViewModel : ViewModel() {

    // Serviços do Firebase
    private val servicoAuth = ServicoAutenticacao()
    private val servicoDados = ServicoDados(servicoAuth)

    // Estados observáveis pela UI
    val estadoAutenticacao = servicoAuth.estadoAutenticacao

    // Lista de clientes atualizada em tempo real
    private val _clientes = MutableStateFlow<List<ClienteFirebase>>(emptyList())
    val clientes: StateFlow<List<ClienteFirebase>> = _clientes.asStateFlow()

    // Estado de carregamento para mostrar progress
    private val _isCarregando = MutableStateFlow(false)
    val isCarregando: StateFlow<Boolean> = _isCarregando.asStateFlow()

    // Mensagens de erro/sucesso para mostrar ao usuário
    private val _mensagem = MutableSharedFlow<String>()
    val mensagem: SharedFlow<String> = _mensagem.asSharedFlow()

    // Estado de visibilidade do saldo
    private val _saldoVisivel = MutableStateFlow(true)
    val saldoVisivel: StateFlow<Boolean> = _saldoVisivel.asStateFlow()

    // Estado de admin (otimizado para evitar verificações repetidas)
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()


    init {
        // Quando o ViewModel inicia, começa a observar os clientes
        observarClientes()
        // Observa mudanças no estado de autenticação para atualizar o status de admin
        observarStatusAdmin()
    }

    // ===== ESTADO DE VISUALIZAÇÃO DE SALDO =====

    /**
     * Estado que controla se o saldo deve ser exibido ou ocultado
     */


    /**
     * Observa mudanças no estado de autenticação para atualizar o status de admin
     * Isso evita verificações repetidas a cada chamada de isadmin()
     */
    private fun observarStatusAdmin() {
        viewModelScope.launch {
            estadoAutenticacao.collect { estado ->
                when (estado) {
                    is EstadoAutenticacao.Autenticado -> {
                        _isAdmin.value = estado.funcionario.isadmin
                    }
                    else -> {
                        _isAdmin.value = false
                    }
                }
            }
        }
    }

    /**
     * Alterna a visibilidade do saldo
     */
    fun alternarVisibilidadeSaldo() {
        val novoValor = !_saldoVisivel.value
        println("🔥 [ViewModel] Alternando visibilidade do saldo: ${_saldoVisivel.value} -> $novoValor")
        _saldoVisivel.value = novoValor

        viewModelScope.launch {
            _mensagem.emit("Saldo ${if (novoValor) "visível" else "oculto"}")
        }
    }

    // ===== FUNÇÕES DE CLIENTES =====

    /**
     * Busca clientes por nome
     * Agora a busca é feita localmente na lista já carregada
     */
    fun buscarClientesPorNome(nome: String): List<ClienteFirebase> {
        return if (nome.isEmpty()) {
            _clientes.value
        } else {
            _clientes.value.filter {
                it.nomeCompleto.contains(nome, ignoreCase = true)
            }
        }
    }

    /**
     * Observa mudanças nos clientes em tempo real
     * Como uma câmera de segurança que mostra tudo que acontece
     */
    private fun observarClientes() {
        viewModelScope.launch {
            estadoAutenticacao.collect { estado ->
                when (estado) {
                    is EstadoAutenticacao.Autenticado -> {
                        _isCarregando.value = true // Inicia carregamento
                        var recebeuPrimeiroSnapshot = false
                        servicoDados.observarClientes()
                            .catch { erro ->
                                _mensagem.emit("Erro ao carregar clientes: ${erro.message}")
                                _isCarregando.value = false // Para o loading em caso de erro
                            }
                            .collect { listaClientes ->
                                _clientes.value = listaClientes
                                if (!recebeuPrimeiroSnapshot) {
                                    _isCarregando.value = false // Para o loading após o primeiro snapshot
                                    recebeuPrimeiroSnapshot = true
                                }
                            }
                    }
                    else -> {
                        _clientes.value = emptyList()
                        _isCarregando.value = false
                    }
                }
            }
        }
    }

    // ===== FUNÇÕES DE AUTENTICAÇÃO =====

    /**
     * Realiza login do funcionário
     */
    fun fazerLogin(email: String, senha: String) {
        println("🔥 [CantinaFirebaseViewModel] fazerLogin chamado com email: $email")

        viewModelScope.launch {
            _isCarregando.value = true
            println("🔥 [CantinaFirebaseViewModel] Iniciando processo de login...")

            when (val resultado = servicoAuth.fazerLogin(email, senha)) {
                is ResultadoFirebase.Sucesso -> {
                    println("🔥 [CantinaFirebaseViewModel] Login SUCESSO: ${resultado.dados.nome}")
                    _mensagem.emit("Bem-vindo, ${resultado.dados.nome}!")
                }
                is ResultadoFirebase.Erro -> {
                    println("🔥 [CantinaFirebaseViewModel] Login ERRO: ${resultado.mensagem}")
                    _mensagem.emit(resultado.mensagem)
                }
                else -> {
                    println("🔥 [CantinaFirebaseViewModel] Login RESULTADO DESCONHECIDO")
                }
            }

            _isCarregando.value = false
            println("🔥 [CantinaFirebaseViewModel] Processo de login finalizado")
        }
    }

    /**
     * Faz logout do funcionário
     */
    fun fazerLogout() {
        servicoAuth.fazerLogout()
        viewModelScope.launch {
            _mensagem.emit("Logout realizado com sucesso")
        }
    }

    /**
     * Verifica se o usuário atual é admin
     * Usa estado em cache para evitar verificações repetidas
     */
    fun isadmin(): Boolean = _isAdmin.value

    /**
     * Retorna o nome do funcionário logado
     */
    fun getNomeFuncionario(): String {
        return servicoAuth.getFuncionarioAtual()?.nome ?: "Usuário"
    }

    // ===== FUNÇÕES DE CLIENTES =====

    /**
     * Cria um novo cliente
     * Disponível para todos os funcionários
     */
    fun criarCliente(
        nomeCompleto: String,
        dataNascimento: String,
        telefone: String
    ) {
        viewModelScope.launch {
            _isCarregando.value = true

            when (val resultado = servicoDados.criarCliente(nomeCompleto, dataNascimento, telefone)) {
                is ResultadoFirebase.Sucesso -> {
                    _mensagem.emit("Cliente cadastrado com sucesso!")
                }
                is ResultadoFirebase.Erro -> {
                    _mensagem.emit(resultado.mensagem)
                }
                else -> {
                    // Resultado inesperado
                }
            }

            _isCarregando.value = false
        }
    }

    /**
     * Adiciona crédito ao cliente
     * APENAS ADMINISTRADORES
     */
    fun adicionarCredito(clienteId: String, valor: Double) {
        // Verifica permissão localmente primeiro
        if (!isadmin()) {
            viewModelScope.launch {
                _mensagem.emit("Apenas administradores podem adicionar crédito")
            }
            return
        }

        viewModelScope.launch {
            _isCarregando.value = true

            when (val resultado = servicoDados.adicionarCredito(clienteId, valor)) {
                is ResultadoFirebase.Sucesso -> {
                    _mensagem.emit("Crédito adicionado com sucesso!")
                }
                is ResultadoFirebase.Erro -> {
                    _mensagem.emit(resultado.mensagem)
                }
                else -> {}
            }

            _isCarregando.value = false
        }
    }

    /**
     * Realiza uma compra (débito)
     * Disponível para todos os funcionários
     */
    fun realizarCompra(clienteId: String, valor: Double, onResult: (Boolean, String) -> Unit)  {
        viewModelScope.launch {
            _isCarregando.value = true

            when (val resultado = servicoDados.realizarCompra(clienteId, valor)) {
                is ResultadoFirebase.Sucesso -> {
                    _mensagem.emit("Compra realizada com sucesso!")
                    onResult(true, "Compra realizada com sucesso!")
                }
                is ResultadoFirebase.Erro -> {
                    _mensagem.emit(resultado.mensagem)
                    onResult(false, resultado.mensagem)
                }
                else -> {}
            }

            _isCarregando.value = false
        }
    }

    /**
     * Remove um cliente
     * APENAS ADMINISTRADORES
     */
    fun removerCliente(clienteId: String) {
        // Verifica permissão localmente primeiro
        if (!isadmin()) {
            viewModelScope.launch {
                _mensagem.emit("Apenas administradores podem remover clientes")
            }
            return
        }

        viewModelScope.launch {
            _isCarregando.value = true

            when (val resultado = servicoDados.removerCliente(clienteId)) {
                is ResultadoFirebase.Sucesso -> {
                    _mensagem.emit("Cliente removido com sucesso!")
                }
                is ResultadoFirebase.Erro -> {
                    _mensagem.emit(resultado.mensagem)
                }
                else -> {}
            }

            _isCarregando.value = false
        }
    }

    /**
     * Atualiza o limite negativo de um cliente
     * APENAS ADMINISTRADORES
     */
    fun atualizarLimiteNegativo(clienteId: String, novoLimite: Double) {
        // Verifica permissão localmente primeiro
        if (!isadmin()) {
            viewModelScope.launch {
                _mensagem.emit("Apenas administradores podem alterar limites")
            }
            return
        }

        viewModelScope.launch {
            _isCarregando.value = true

            when (val resultado = servicoDados.atualizarLimiteNegativo(clienteId, novoLimite)) {
                is ResultadoFirebase.Sucesso -> {
                    _mensagem.emit("Limite atualizado com sucesso!")
                }
                is ResultadoFirebase.Erro -> {
                    _mensagem.emit(resultado.mensagem)
                }
                else -> {}
            }

            _isCarregando.value = false
        }
    }

    // ===== FUNÇÕES DE TRANSAÇÕES =====

    /**
     * Estado das transações de um cliente específico
     */
    private val _transacoesCliente = MutableStateFlow<List<TransacaoFirebase>>(emptyList())
    val transacoesCliente: StateFlow<List<TransacaoFirebase>> = _transacoesCliente.asStateFlow()

    /**
     * Carrega as transações de um cliente
     */
    fun carregarTransacoes(clienteId: String) {
        viewModelScope.launch {
            when (val resultado = servicoDados.buscarTransacoes(clienteId)) {
                is ResultadoFirebase.Sucesso -> {
                    _transacoesCliente.value = resultado.dados
                }
                is ResultadoFirebase.Erro -> {
                    _mensagem.emit(resultado.mensagem)
                    _transacoesCliente.value = emptyList()
                }
                else -> {}
            }
        }
    }

    // ===== FUNÇÕES DE ESTATÍSTICAS =====

    /**
     * Estado das estatísticas
     */
    private val _estatisticas = MutableStateFlow<Map<String, Any>>(emptyMap())
    val estatisticas: StateFlow<Map<String, Any>> = _estatisticas.asStateFlow()

    /**
     * Carrega estatísticas do sistema
     */
    fun carregarEstatisticas() {
        viewModelScope.launch {
            when (val resultado = servicoDados.buscarEstatisticas()) {
                is ResultadoFirebase.Sucesso -> {
                    _estatisticas.value = resultado.dados
                }
                is ResultadoFirebase.Erro -> {
                    _mensagem.emit(resultado.mensagem)
                }
                else -> {}
            }
        }
    }

    // ===== GERENCIAMENTO DE FUNCIONÁRIOS (APENAS ADMIN) =====

    /**
     * Cria um novo funcionário
     * APENAS ADMINISTRADORES
     */
    fun criarFuncionario(
        email: String,
        senha: String,
        nome: String,
        isAdmin: Boolean
    ) {
        viewModelScope.launch {
            _isCarregando.value = true

            when (val resultado = servicoAuth.criarFuncionario(email, senha, nome, isAdmin)) {
                is ResultadoFirebase.Sucesso -> {
                    _mensagem.emit(resultado.dados)
                }
                is ResultadoFirebase.Erro -> {
                    _mensagem.emit(resultado.mensagem)
                }
                else -> {}
            }

            _isCarregando.value = false
        }
    }


// ===== FUNÇÕES DE GERAÇÃO DE PDF =====

    /**
     * Gera PDF com extrato completo de um cliente
     * @return URI do PDF gerado ou null se houver erro
     */
    fun gerarPdfExtratoCliente(context: Context, clienteId: String): Uri? {
        try {
            // Busca o cliente na lista atual
            val cliente = _clientes.value.find { it.id == clienteId } ?: return null

            // Busca as transações do cliente (já carregadas)
            val transacoes = _transacoesCliente.value

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            val pdfDocument = PdfDocument()
            val titlePaint = Paint().apply {
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
            }
            val normalPaint = Paint().apply {
                textSize = 12f
            }

            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            var yPosition = 50f

            // Cabeçalho do extrato
            canvas.drawText("EXTRATO - ${cliente.nomeCompleto}", 50f, yPosition, titlePaint)
            yPosition += 30

            canvas.drawText("Data: ${dateFormat.format(Date())}", 50f, yPosition, normalPaint)
            yPosition += 20
            canvas.drawText("Telefone: ${cliente.telefone}", 50f, yPosition, normalPaint)
            yPosition += 20
            canvas.drawText("Data Nascimento: ${cliente.dataNascimento}", 50f, yPosition, normalPaint)
            yPosition += 30

            // Saldo atual
            titlePaint.color = if (cliente.saldo >= 0) 0xFF008000.toInt() else 0xFFFF0000.toInt()
            canvas.drawText("SALDO ATUAL: R$ ${"%.2f".format(cliente.saldo)}", 50f, yPosition, titlePaint)
            titlePaint.color = 0xFF000000.toInt()
            yPosition += 40

            canvas.drawText("HISTÓRICO DE TRANSAÇÕES", 50f, yPosition, titlePaint)
            yPosition += 30

            // Lista de transações
            transacoes.sortedByDescending { it.data }.forEach { transacao ->
                if (yPosition > 750) {
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPosition = 50f
                }

                // Converte Timestamp para Date
                val dataTransacao = transacao.data.toDate()
                canvas.drawText(dateFormat.format(dataTransacao), 50f, yPosition, normalPaint)
                yPosition += 20

                val valor = "${if (transacao.tipo == "CREDITO") "+" else "-"} R$ ${"%.2f".format(transacao.valor)}"
                normalPaint.color = if (transacao.tipo == "CREDITO") 0xFF008000.toInt() else 0xFFFF0000.toInt()
                canvas.drawText("${transacao.descricao}: $valor", 50f, yPosition, normalPaint)
                normalPaint.color = 0xFF000000.toInt()

                // Adiciona informação do funcionário
                canvas.drawText("Por: ${transacao.funcionarioNome}", 70f, yPosition + 15, normalPaint)
                yPosition += 35
            }

            pdfDocument.finishPage(page)

            // Salva o arquivo
            val fileName = "extrato_${cliente.nomeCompleto.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
            val file = File(context.getExternalFilesDir(null), fileName)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            return androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            viewModelScope.launch {
                _mensagem.emit("Erro ao gerar PDF: ${e.message}")
            }
            return null
        }
    }

    /**
     * Gera PDF com lista de clientes filtrada
     * @param filtro "todos", "positivo", "negativo" ou "zerado"
     * @return URI do PDF gerado ou null se houver erro
     */
    fun gerarPdfListaClientes(context: Context, filtro: String): Uri? {
        try {
            val clientes = when (filtro) {
                "positivo" -> _clientes.value.filter { it.saldo > 0 }
                "negativo" -> _clientes.value.filter { it.saldo < 0 }
                "zerado" -> _clientes.value.filter { it.saldo == 0.0 }
                else -> _clientes.value
            }

            val titulo = when (filtro) {
                "positivo" -> "CLIENTES COM SALDO POSITIVO"
                "negativo" -> "CLIENTES COM SALDO NEGATIVO"
                "zerado" -> "CLIENTES COM SALDO ZERADO"
                else -> "TODOS OS CLIENTES"
            }

            val total = clientes.sumOf { it.saldo }
            val dataAtual = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            val funcionario = getNomeFuncionario()

            val pdfDocument = PdfDocument()
            val paint = Paint()
            val titlePaint = Paint().apply {
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
            }
            val normalPaint = Paint().apply {
                textSize = 14f
            }

            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas: Canvas = page.canvas

            var yPosition = 50f

            // Cabeçalho
            canvas.drawText(titulo, 50f, yPosition, titlePaint)
            yPosition += 40

            canvas.drawText("Data: $dataAtual", 50f, yPosition, normalPaint)
            yPosition += 20

            canvas.drawText("Gerado por: $funcionario", 50f, yPosition, normalPaint)
            yPosition += 30

            canvas.drawLine(50f, yPosition, 545f, yPosition, paint)
            yPosition += 20

            // Lista de clientes
            clientes.forEach { cliente ->
                // Verifica se precisa criar nova página
                if (yPosition > 750) {
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPosition = 50f
                }

                canvas.drawText(cliente.nomeCompleto, 50f, yPosition, normalPaint)
                yPosition += 20

                val saldoText = "Saldo: R$ ${"%.2f".format(cliente.saldo)}"
                paint.color = if (cliente.saldo >= 0) 0xFF008000.toInt() else 0xFFFF0000.toInt()
                canvas.drawText(saldoText, 50f, yPosition, paint)

                // Adiciona limite se o saldo for negativo
                if (cliente.saldo < 0) {
                    paint.color = 0xFF666666.toInt()
                    canvas.drawText("(Limite: R$ ${"%.2f".format(cliente.limiteNegativo)})", 250f, yPosition, paint)
                }

                paint.color = 0xFF000000.toInt()
                yPosition += 30
            }

            // Rodapé com total
            yPosition += 20
            canvas.drawLine(50f, yPosition, 545f, yPosition, paint)
            yPosition += 30

            titlePaint.textSize = 18f
            titlePaint.color = if (total >= 0) 0xFF008000.toInt() else 0xFFFF0000.toInt()
            canvas.drawText("TOTAL: R$ ${"%.2f".format(total)}", 50f, yPosition, titlePaint)

            yPosition += 30
            normalPaint.textSize = 10f
            canvas.drawText("Total de clientes: ${clientes.size}", 50f, yPosition, normalPaint)

            pdfDocument.finishPage(page)

            // Salva o arquivo
            val fileName = "relatorio_${filtro}_${System.currentTimeMillis()}.pdf"
            val file = File(context.getExternalFilesDir(null), fileName)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            return androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            viewModelScope.launch {
                _mensagem.emit("Erro ao gerar PDF: ${e.message}")
            }
            return null
        }
    }

    /**
     * Gera relatório completo do sistema (ADMIN)
     * Inclui estatísticas, lista de clientes e resumo financeiro
     */
    fun gerarRelatorioCompleto(context: Context): Uri? {
        if (!isadmin()) {
            viewModelScope.launch {
                _mensagem.emit("Apenas administradores podem gerar relatórios completos")
            }
            return null
        }

        try {
            val estatisticas = _estatisticas.value
            val clientes = _clientes.value.sortedBy { it.nomeCompleto }
            val dataAtual = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

            val pdfDocument = PdfDocument()
            val titlePaint = Paint().apply {
                textSize = 28f
                typeface = Typeface.DEFAULT_BOLD
            }
            val subtitlePaint = Paint().apply {
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
            }
            val normalPaint = Paint().apply {
                textSize = 14f
            }

            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            var yPosition = 50f

            // Título do relatório
            canvas.drawText("RELATÓRIO COMPLETO - SISTEMA CANTINA", 50f, yPosition, titlePaint)
            yPosition += 40

            canvas.drawText("Data: $dataAtual", 50f, yPosition, normalPaint)
            yPosition += 20
            canvas.drawText("Gerado por: ${getNomeFuncionario()} (ADMIN)", 50f, yPosition, normalPaint)
            yPosition += 40

            // Seção de Estatísticas
            canvas.drawText("ESTATÍSTICAS GERAIS", 50f, yPosition, subtitlePaint)
            yPosition += 30

            val totalClientes = estatisticas["totalClientes"] as? Int ?: 0
            val clientesPositivos = estatisticas["clientesPositivos"] as? Int ?: 0
            val clientesNegativos = estatisticas["clientesNegativos"] as? Int ?: 0
            val clientesZerados = estatisticas["clientesZerados"] as? Int ?: 0
            val saldoTotal = estatisticas["saldoTotal"] as? Double ?: 0.0

            canvas.drawText("Total de Clientes: $totalClientes", 50f, yPosition, normalPaint)
            yPosition += 20
            canvas.drawText("• Com saldo positivo: $clientesPositivos", 70f, yPosition, normalPaint)
            yPosition += 20
            canvas.drawText("• Com saldo negativo: $clientesNegativos", 70f, yPosition, normalPaint)
            yPosition += 20
            canvas.drawText("• Com saldo zerado: $clientesZerados", 70f, yPosition, normalPaint)
            yPosition += 30

            // Saldo total
            val paint = Paint().apply { textSize = 16f }
            paint.color = if (saldoTotal >= 0) 0xFF008000.toInt() else 0xFFFF0000.toInt()
            canvas.drawText("SALDO TOTAL DO SISTEMA: R$ ${"%.2f".format(saldoTotal)}", 50f, yPosition, paint)
            yPosition += 40

            // Lista detalhada de clientes
            canvas.drawText("LISTA DETALHADA DE CLIENTES", 50f, yPosition, subtitlePaint)
            yPosition += 30

            clientes.forEach { cliente ->
                if (yPosition > 750) {
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPosition = 50f
                }

                // Nome do cliente
                normalPaint.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText(cliente.nomeCompleto, 50f, yPosition, normalPaint)
                normalPaint.typeface = Typeface.DEFAULT
                yPosition += 20

                // Informações do cliente
                canvas.drawText("Tel: ${cliente.telefone} | Nasc: ${cliente.dataNascimento}", 70f, yPosition, normalPaint)
                yPosition += 20

                // Saldo
                paint.color = if (cliente.saldo >= 0) 0xFF008000.toInt() else 0xFFFF0000.toInt()
                canvas.drawText("Saldo: R$ ${"%.2f".format(cliente.saldo)} | Limite: R$ ${"%.2f".format(cliente.limiteNegativo)}",
                    70f, yPosition, paint)
                yPosition += 30
            }

            pdfDocument.finishPage(page)

            // Salva o arquivo
            val fileName = "relatorio_completo_${System.currentTimeMillis()}.pdf"
            val file = File(context.getExternalFilesDir(null), fileName)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            return androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            viewModelScope.launch {
                _mensagem.emit("Erro ao gerar relatório: ${e.message}")
            }
            return null
        }
    }

    /**
     * Compartilha um PDF via Intent
     */
    fun compartilharPdf(context: Context, uri: Uri, titulo: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, titulo)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar PDF"))
    }
}
