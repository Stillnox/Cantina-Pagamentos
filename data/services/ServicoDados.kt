package com.cantina.pagamentos.data.services

import com.cantina.pagamentos.data.models.ClienteFirebase
import com.cantina.pagamentos.data.models.ResultadoFirebase
import com.cantina.pagamentos.data.models.TransacaoFirebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await


/**
 * Serviço responsável por todas as operações de dados no Firestore
 *
 * Este serviço é como um "gerente de arquivo" que:
 * - Salva informações dos clientes na nuvem
 * - Busca informações quando precisamos
 * - Controla quem pode fazer o quê baseado nas permissões
 */
class ServicoDados(private val servicoAuth: ServicoAutenticacao) {

    // Instância do Firestore
    private val firestore = FirebaseFirestore.getInstance()

    // Referências às coleções principais
    private val colecaoClientes = firestore.collection("clientes")

    /**
     * Observa todos os clientes em tempo real
     *
     * Flow é como um "canal de TV" que fica transmitindo atualizações
     * Sempre que alguém muda algo no Firebase, recebemos a atualização automaticamente
     */
    fun observarClientes(): Flow<List<ClienteFirebase>> {
        return colecaoClientes
            .orderBy("nomeCompleto") // Ordena por nome
            .snapshots() // Cria o "canal de TV" de atualizações
            .map { snapshot ->
                // Converte cada documento em um ClienteFirebase
                snapshot.toObjects(ClienteFirebase::class.java)
            }
    }

    /**
     * Busca um cliente específico por ID
     */
    suspend fun buscarCliente(clienteId: String): ResultadoFirebase<ClienteFirebase> {
        return try {
            val documento = colecaoClientes.document(clienteId).get().await()

            if (documento.exists()) {
                val cliente = documento.toObject(ClienteFirebase::class.java)
                if (cliente != null) {
                    ResultadoFirebase.Sucesso(cliente)
                } else {
                    ResultadoFirebase.Erro("Erro ao converter dados do cliente")
                }
            } else {
                ResultadoFirebase.Erro("Cliente não encontrado")
            }
        } catch (e: Exception) {
            ResultadoFirebase.Erro("Erro ao buscar cliente: ${e.message}")
        }
    }

    /**
     * Cria um novo cliente
     *
     * REGRA: Todos os funcionários podem criar clientes
     */
    suspend fun criarCliente(
        nomeCompleto: String,
        dataNascimento: String,
        telefone: String
    ): ResultadoFirebase<String> {
        // Verifica se tem alguém logado
        val funcionario = servicoAuth.getFuncionarioAtual()

        if (funcionario == null) {
            return ResultadoFirebase.Erro("É necessário estar logado para criar clientes")
        }

        return try {
            // Cria o novo cliente
            val novoCliente = ClienteFirebase(
                nomeCompleto = nomeCompleto,
                dataNascimento = dataNascimento,
                telefone = telefone,
                saldo = 0.0,
                limiteNegativo = -50.0
            )

            // Adiciona ao Firestore (o ID é gerado automaticamente)
            val documentoRef = colecaoClientes.add(novoCliente).await()

            ResultadoFirebase.Sucesso("Cliente criado com sucesso")
        } catch (e: Exception) {
            ResultadoFirebase.Erro("Erro ao criar cliente: ${e.message}")
        }
    }

    /**
     * Adiciona crédito (dinheiro) ao saldo do cliente
     *
     * REGRA: Apenas administradores podem adicionar crédito
     */
    suspend fun adicionarCredito(
        clienteId: String,
        valor: Double,
        descricao: String = "Crédito adicionado"
    ): ResultadoFirebase<String> {
        // Verifica se é admin
        if (!servicoAuth.isAdmin()) {
            return ResultadoFirebase.Erro("Apenas administradores podem adicionar crédito")
        }

        val funcionario = servicoAuth.getFuncionarioAtual()!!

        return try {
            // Usa uma transação para garantir consistência
            firestore.runTransaction { transacao ->
                // 1. Busca o cliente atual
                val clienteRef = colecaoClientes.document(clienteId)
                val clienteDoc = transacao.get(clienteRef)

                if (!clienteDoc.exists()) {
                    throw Exception("Cliente não encontrado")
                }

                val saldoAtual = clienteDoc.getDouble("saldo") ?: 0.0
                val novoSaldo = saldoAtual + valor

                // 2. Atualiza o saldo
                transacao.update(clienteRef, mapOf(
                    "saldo" to novoSaldo,
                    "ultimaAtualizacao" to Timestamp.now()
                ))

                // 3. Cria a transação de crédito
                val novaTransacao = TransacaoFirebase(
                    tipo = "CREDITO",
                    valor = valor,
                    descricao = descricao,
                    funcionarioId = funcionario.id,
                    funcionarioNome = funcionario.nome
                )

                // 4. Adiciona a transação como subcoleção
                val transacaoRef = clienteRef.collection("transacoes").document()
                transacao.set(transacaoRef, novaTransacao)

            }.await()

            ResultadoFirebase.Sucesso("Crédito adicionado com sucesso")
        } catch (e: Exception) {
            ResultadoFirebase.Erro("Erro ao adicionar crédito: ${e.message}")
        }
    }

    /**
     * Realiza uma compra (débito) no saldo do cliente
     *
     * REGRA: Todos os funcionários podem realizar vendas
     */
    suspend fun realizarCompra(
        clienteId: String,
        valor: Double,
        descricao: String = "Compra na cantina"
    ): ResultadoFirebase<String> {
        // Verifica se tem alguém logado
        val funcionario = servicoAuth.getFuncionarioAtual()
        if (funcionario == null) {
            return ResultadoFirebase.Erro("É necessário estar logado para realizar vendas")
        }

        return try {
            // Usa uma transação para garantir consistência
            firestore.runTransaction { transacao ->
                // 1. Busca o cliente atual
                val clienteRef = colecaoClientes.document(clienteId)
                val clienteDoc = transacao.get(clienteRef)

                if (!clienteDoc.exists()) {
                    throw Exception("Cliente não encontrado")
                }

                val saldoAtual = clienteDoc.getDouble("saldo") ?: 0.0
                val limiteNegativo = clienteDoc.getDouble("limiteNegativo") ?: -50.0
                val novoSaldo = saldoAtual - valor

                // 2. Verifica se não ultrapassa o limite
                if (novoSaldo < limiteNegativo) {
                    val valorDisponivel = saldoAtual - limiteNegativo
                    throw Exception(
                        "Compra não autorizada! " +
                                "Saldo disponível: R$ ${"%.2f".format(valorDisponivel)}. " +
                                "Valor da compra: R$ ${"%.2f".format(valor)}. " +
                                "Faltam R$ ${"%.2f".format(valor - valorDisponivel)} para completar a compra."
                    )
                }

                // 3. Atualiza o saldo
                transacao.update(clienteRef, mapOf(
                    "saldo" to novoSaldo,
                    "ultimaAtualizacao" to Timestamp.now()
                ))

                // 4. Cria a transação de débito
                val novaTransacao = TransacaoFirebase(
                    tipo = "DEBITO",
                    valor = valor,
                    descricao = descricao,
                    funcionarioId = funcionario.id,
                    funcionarioNome = funcionario.nome
                )

                // 5. Adiciona a transação como subcoleção
                val transacaoRef = clienteRef.collection("transacoes").document()
                transacao.set(transacaoRef, novaTransacao)

            }.await()

            ResultadoFirebase.Sucesso("Compra realizada com sucesso")
        } catch (e: Exception) {
            ResultadoFirebase.Erro(e.message ?: "Erro ao realizar compra")
        }
    }

    /**
     * Busca o histórico de transações de um cliente
     */
    suspend fun buscarTransacoes(clienteId: String): ResultadoFirebase<List<TransacaoFirebase>> {
        return try {
            val transacoes = colecaoClientes
                .document(clienteId)
                .collection("transacoes")
                .orderBy("data", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(TransacaoFirebase::class.java)

            ResultadoFirebase.Sucesso(transacoes)
        } catch (e: Exception) {
            ResultadoFirebase.Erro("Erro ao buscar transações: ${e.message}")
        }
    }

    /**
     * Atualiza o limite negativo de um cliente
     *
     * REGRA: Apenas administradores podem alterar limites
     */
    suspend fun atualizarLimiteNegativo(
        clienteId: String,
        novoLimite: Double
    ): ResultadoFirebase<String> {
        // Verifica se é admin
        if (!servicoAuth.isAdmin()) {
            return ResultadoFirebase.Erro("Apenas administradores podem alterar limites")
        }

        return try {
            colecaoClientes
                .document(clienteId)
                .update(mapOf(
                    "limiteNegativo" to novoLimite,
                    "ultimaAtualizacao" to Timestamp.now()
                ))
                .await()

            ResultadoFirebase.Sucesso("Limite atualizado com sucesso")
        } catch (e: Exception) {
            ResultadoFirebase.Erro("Erro ao atualizar limite: ${e.message}")
        }
    }

    /**
     * Remove um cliente do sistema
     *
     * REGRA: Apenas administradores podem remover clientes
     */
    suspend fun removerCliente(clienteId: String): ResultadoFirebase<String> {
        // Verifica se é admin
        if (!servicoAuth.isAdmin()) {
            return ResultadoFirebase.Erro("Apenas administradores podem remover clientes")
        }

        return try {
            // Remove o cliente e todas as suas transações
            val clienteRef = colecaoClientes.document(clienteId)

            // Primeiro remove todas as transações
            val transacoes = clienteRef.collection("transacoes").get().await()
            transacoes.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            // Depois remove o cliente
            clienteRef.delete().await()

            ResultadoFirebase.Sucesso("Cliente removido com sucesso")
        } catch (e: Exception) {
            ResultadoFirebase.Erro("Erro ao remover cliente: ${e.message}")
        }
    }

    /**
     * Busca estatísticas gerais do sistema
     */
    suspend fun buscarEstatisticas(): ResultadoFirebase<Map<String, Any>> {
        return try {
            val clientes = colecaoClientes.get().await()

            var totalClientes = 0
            var clientesPositivos = 0
            var clientesNegativos = 0
            var clientesZerados = 0
            var saldoTotal = 0.0

            clientes.documents.forEach { doc ->
                totalClientes++
                val saldo = doc.getDouble("saldo") ?: 0.0
                saldoTotal += saldo

                when {
                    saldo > 0 -> clientesPositivos++
                    saldo < 0 -> clientesNegativos++
                    else -> clientesZerados++
                }
            }

            val estatisticas = mapOf(
                "totalClientes" to totalClientes,
                "clientesPositivos" to clientesPositivos,
                "clientesNegativos" to clientesNegativos,
                "clientesZerados" to clientesZerados,
                "saldoTotal" to saldoTotal
            )

            ResultadoFirebase.Sucesso(estatisticas)
        } catch (e: Exception) {
            ResultadoFirebase.Erro("Erro ao buscar estatísticas: ${e.message}")
        }
    }
}