package com.cantina.pagamentos.presentation.components.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation


/**
 * Visual Transformation para formatar data enquanto o usuário digita
 * Transforma entrada "31122024" em exibição "31/12/2024"
 */
class DateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Remove não-dígitos e limita a 8 caracteres
        val trimmed = text.text.filter { it.isDigit() }.take(8)

        // Constrói a string formatada com barras
        val output = buildString {
            for (i in trimmed.indices) {
                append(trimmed[i])
                // Adiciona barras após dia (posição 1) e mês (posição 3)
                if (i == 1 || i == 3) append("/")
            }
        }

        // Mapeia as posições para a transformação
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 4) return offset + 1
                if (offset <= 8) return offset + 2
                return output.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset - 1
                if (offset <= 10) return offset - 2
                return trimmed.length
            }
        }

        return TransformedText(AnnotatedString(output), offsetMapping)
    }
}