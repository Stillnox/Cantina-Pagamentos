package com.cantina.pagamentos.presentation.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cantina.pagamentos.data.models.ClienteFirebase
import com.cantina.pagamentos.presentation.theme.CoresPastel
import com.cantina.pagamentos.presentation.theme.CoresTexto


/** Card de um cliente individual **/
@Composable
fun ClienteCard(
    cliente: ClienteFirebase,
    saldoVisivel: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = getClienteBorderColor(cliente.saldo)
        ),
        border = BorderStroke(
            width = 2.dp,
            color = getClienteBorderColor(cliente.saldo)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            ClienteHeader(cliente = cliente)
            ClienteSaldo(cliente = cliente, saldoVisivel = saldoVisivel)
        }
    }
}

/**
 * Cabeçalho do card do cliente
 */
@Composable
private fun ClienteHeader(cliente: ClienteFirebase) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = cliente.nomeCompleto,
            style = MaterialTheme.typography.titleMedium
        )

        ClienteSaldoBadge(saldo = cliente.saldo)
    }
}

/**
 * Badge indicador do tipo de saldo
 */
@Composable
private fun ClienteSaldoBadge(saldo: Double) {
    Badge(
        containerColor = getClienteBorderColor(saldo)
    ) {
        Text(
            text = getClienteSaldoEmoji(saldo),
            color = CoresTexto.Principal,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * Seção de saldo do cliente
 */
@Composable
private fun ClienteSaldo(
    cliente: ClienteFirebase,
    saldoVisivel: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (saldoVisivel) "Saldo: R$ %.2f".format(cliente.saldo) else "Saldo: ****",
            style = MaterialTheme.typography.bodyLarge,
            color = CoresTexto.Principal
        )

        ClienteLimiteAviso(cliente = cliente, saldoVisivel = saldoVisivel)
    }
}

/**
 * Aviso de proximidade do limite
 */
@Composable
private fun ClienteLimiteAviso(
    cliente: ClienteFirebase,
    saldoVisivel: Boolean,
) {
    if (saldoVisivel && cliente.saldo < 0) {
        val faltaParaLimite = cliente.saldo - cliente.limiteNegativo
        if (faltaParaLimite <= 10.0 && faltaParaLimite > 0) {
            Text(
                text = "⚠️ Faltam R$ %.2f para o limite".format(faltaParaLimite),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Função auxiliar para obter cor da borda baseada no saldo
 */
private fun getClienteBorderColor(saldo: Double): Color {
    return when {
        saldo > 0 -> CoresPastel.VerdeMenta
        saldo == 0.0 -> CoresPastel.PessegoPastel
        else -> CoresPastel.CoralSuave
    }
}

/**
 * Função auxiliar para obter emoji baseado no saldo
 */
private fun getClienteSaldoEmoji(saldo: Double): String {
    return when {
        saldo > 0 -> "💰"
        saldo == 0.0 -> "⚪"
        else -> "⚠️"
    }
}
