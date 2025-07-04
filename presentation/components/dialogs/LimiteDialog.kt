package com.cantina.pagamentos.presentation.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cantina.pagamentos.presentation.components.common.CampoMonetario


/**
 * Dialog para alterar limite negativo do cliente
 */
@Composable
fun DialogAlterarLimite(
    limiteAtual: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var novoLimite by remember {
        mutableStateOf(
            (kotlin.math.abs(limiteAtual) * 100).toLong().toString()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurações") },
        text = {
            Column {
                Text("Limite negativo atual: R$ %.2f".format(limiteAtual))
                Spacer(modifier = Modifier.height(16.dp))
                CampoMonetario(
                    valor = novoLimite,
                    onValueChange = { novoLimite = it },
                    label = "Novo limite"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val valorEmCentavos = novoLimite.toLongOrNull() ?: 0L
                    val valor = valorEmCentavos / 100.0
                    if (valor >= 0) {
                        onConfirm(-valor)
                    }
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}