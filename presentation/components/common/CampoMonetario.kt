package com.cantina.pagamentos.presentation.components.common

import android.annotation.SuppressLint
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.cantina.pagamentos.presentation.theme.CoresPastel
import com.cantina.pagamentos.presentation.theme.CoresTexto

/**
 * Campo de entrada formatado para valores monetários
 * Converte automaticamente entrada em centavos para formato R$ X,XX
 * @param valor Valor em centavos como String
 * @param onValueChange Callback quando o valor muda
 * @param label Label do campo
 * @param modifier Modificadores do Compose
 * @param placeholder Placeholder quando vazio
 */
@SuppressLint("DefaultLocale")
@Composable
fun CampoMonetario(
    valor: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "0,00",
) {
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = if (valor.isEmpty()) "" else {
                    val valorEmCentavos = valor.toLongOrNull() ?: 0L
                    val reais = valorEmCentavos / 100
                    val centavos = valorEmCentavos % 100
                    String.format("%d,%02d", reais, centavos)
                },
                selection = TextRange(valor.length)
            )
        )
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            // Remove tudo que não é dígito
            val apenasNumeros = newValue.text.filter { it.isDigit() }
            onValueChange(apenasNumeros)

            // Formata o texto para exibição
            val novoTextoFormatado = if (apenasNumeros.isEmpty()) "" else {
                val valorEmCentavos = apenasNumeros.toLongOrNull() ?: 0L
                val reais = valorEmCentavos / 100
                val centavos = valorEmCentavos % 100
                String.format("%d,%02d", reais, centavos)
            }

            // Atualiza o campo com cursor no final
            textFieldValue = TextFieldValue(
                text = novoTextoFormatado,
                selection = TextRange(novoTextoFormatado.length)
            )
        },
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        placeholder = { Text(placeholder) },
        colors = OutlinedTextFieldDefaults.colors(
            // Cor do texto quando digitando (focado)
            focusedTextColor = CoresPastel.VerdeMenta,
            // Cor do texto quando não está digitando
            unfocusedTextColor = CoresTexto.Principal,
            // Cor da label quando focado
            focusedLabelColor = CoresPastel.VerdeMenta,
            // Cor da label quando não focado
            unfocusedLabelColor = CoresTexto.Secundario,
            // Cor da borda quando focado
            focusedBorderColor = CoresPastel.VerdeMenta,
            // Cor da borda quando não focado
            unfocusedBorderColor = CoresTexto.Suave,
            // Cor do cursor
            cursorColor = CoresPastel.VerdeMenta
        )
    )
}
