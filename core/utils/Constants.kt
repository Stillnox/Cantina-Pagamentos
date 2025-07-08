package com.cantina.pagamentos.core.utils

/**
 * Objeto que contém todas as constantes do aplicativo
 * Facilita manutenção e tradução de mensagens
 */
object Constants {
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
