package com.cantina.pagamentos.presentation.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cantina.pagamentos.presentation.components.common.CampoMonetario
import com.cantina.pagamentos.presentation.theme.CoresPastel


/**
 * Dialog para alterar limite negativo do cliente
 */
@Composable
fun DialogAlterarLimite(
    limiteAtual: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    onRemoverCliente: () -> Unit,
    onExportarPdf: () -> Unit,
    isAdmin: Boolean,
) {
    var novoLimite by remember {
        mutableStateOf(
            "0"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Configurações",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp // ✅ Aumentado em ~4.dp (titleMedium é ~16sp + 4sp = 20sp)
                ),
                color = CoresPastel.AzulCeuPastel // ✅ Cor azul céu aplicada
            )
        },
        text = {
            Column {
                Text(
                    "Limite negativo atual: R$ %.2f".format(limiteAtual),
                    color = CoresPastel.VerdeMenta // ✅ Cor verde menta aplicada
                )
                Spacer(modifier = Modifier.height(16.dp))
                CampoMonetario(
                    valor = novoLimite,
                    onValueChange = { novoLimite = it },
                    label = "Novo limite"
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (isAdmin) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Botão Exportar PDF
                        OutlinedButton(
                            onClick = onExportarPdf,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📄 Exportar PDF")
                        }

                        // Botão Remover Cliente
                        OutlinedButton(
                            onClick = onRemoverCliente,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🗑️ Remover")
                                Text("Cliente")
                            }
                        }
                    }
                }
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
                Text(
                    "Salvar",
                    color = CoresPastel.VerdeMenta // ✅ Cor verde menta aplicada
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancelar",
                    color = CoresPastel.VerdeMenta // ✅ Cor verde menta aplicada
                )
            }
        }
    )
}
