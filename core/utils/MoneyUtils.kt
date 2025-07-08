package com.cantina.pagamentos.core.utils

/**
 * Formata o telefone para exibição
 */
fun formatarTelefone(telefone: String): String {
    return when (telefone.length) {
        0 -> ""
        in 1..2 -> "($telefone"
        in 3..6 -> "(${telefone.substring(0, 2)}) ${telefone.substring(2)}"
        in 7..10 -> "(${telefone.substring(0, 2)}) ${telefone.substring(2, 6)}-${telefone.substring(6)}"
        11 -> "(${telefone.substring(0, 2)}) ${telefone.substring(2, 7)}-${telefone.substring(7)}"
        else -> telefone
    }
}
