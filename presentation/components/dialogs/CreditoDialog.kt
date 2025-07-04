package com.cantina.pagamentos.presentation.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cantina.pagamentos.presentation.components.common.CampoMonetario

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
        title = { Text("Adicionar Crédito") },
        text = {
            Column {
                Text("Digite o valor a ser adicionado:")
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