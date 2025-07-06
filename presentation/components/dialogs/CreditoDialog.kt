package com.cantina.pagamentos.presentation.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cantina.pagamentos.presentation.components.common.CampoMonetario
import com.cantina.pagamentos.presentation.theme.CoresPastel

/**
 * Dialog para adicionar crédito ao cliente
 */
@Composable
fun CreditoDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var valorCredito by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Adicionar Crédito",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp
                ),
                color = CoresPastel.AzulCeuPastel
            )
        },
        text = {
            Column {
                Text(
                    "Digite o valor a ser adicionado:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 18.sp
                    ),
                    color = CoresPastel.VerdeMenta
                )
                Spacer(modifier = Modifier.height(16.dp))
                CampoMonetario(
                    valor = valorCredito,
                    onValueChange = { valorCredito = it },
                    label = "Valor R$"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val valorEmCentavos = valorCredito.toLongOrNull() ?: 0L
                    val valor = valorEmCentavos / 100.0
                    if (valor > 0) {
                        onConfirm(valor)
                    }
                }
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
