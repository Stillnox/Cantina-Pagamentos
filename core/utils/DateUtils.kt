package com.cantina.pagamentos.core.utils

import java.util.Calendar

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
