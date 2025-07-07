package com.cantina.pagamentos.presentation.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cantina.pagamentos.data.models.ClienteFirebase
import com.cantina.pagamentos.presentation.theme.CoresPastel
import com.cantina.pagamentos.presentation.theme.CoresTexto

// Card de um cliente individual.relogio espelho poltrona e tapete sabonete e toalha de banho. chinelo descartavel
//mudar a porta .

/** Card de um cliente individual **/
@Composable
fun ClienteCard(
    cliente: ClienteFirebase,
    saldoVisivel: Boolean,
    onClick: () -> Unit = {}, // adicione valor padrão
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = getClienteBorderColor(cliente.saldo)
        ),
        border = BorderStroke(
            width = 2.dp,
            color = getClienteBorderColor(cliente.saldo)
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = cliente.nomeCompleto,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                modifier = Modifier.fillMaxWidth(0.67f)
            )
            Text(
                text = if (saldoVisivel) "R$ %.2f".format(cliente.saldo) else "R$ ****",
                style = MaterialTheme.typography.bodyLarge,
                color = CoresTexto.Principal
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
