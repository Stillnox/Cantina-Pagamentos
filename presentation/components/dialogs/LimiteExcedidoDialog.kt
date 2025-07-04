package com.cantina.pagamentos.presentation.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


/**
 * Dialog de erro quando limite é excedido
 */
@Composable
fun DialogLimiteExcedido(
    mensagem: String,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onAdicionarCredito: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("❌ ", style = MaterialTheme.typography.headlineMedium)
                Text("Compra Não Autorizada")
            }
        },
        text = {
            Column {
                Text(
                    text = mensagem,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Mostra sugestão para admin
                if (isAdmin) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 Dica: Como administrador, você pode adicionar crédito ao cliente ou aumentar o limite negativo.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendi")
            }
        },
        dismissButton = if (isAdmin) {
            {
                TextButton(onClick = onAdicionarCredito) {
                    Text("Adicionar Crédito")
                }
            }
        } else null
    )
}