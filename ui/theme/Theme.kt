package com.cantina.pagamentos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

// ===========================================================================================
// PALETA DE CORES PASTÉIS - MÁXIMO 7 CORES
// ===========================================================================================

/**
 * Paleta minimalista com 7 cores pastéis
 * Foco em tons de verde com cores de contraste suaves
 */
object CoresPastel {
    // 1. Verde Menta Pastel - Cor principal
    val VerdeMenta = Color(0xFFB8E6D1)    // Verde menta suave

    // 2. Verde Sage Pastel - Cor secundária
    val AzulSage = Color(0xFF2C3E50)     // Verde sage muito claro

    // 3. Pêssego Pastel - Contraste quente
    val PessegoPastel = Color(0xFF5D6D7E)  // Pêssego suave para contrastes

    // 4. Amarelo Vanilla - Contraste suave
    val AmareloVanilla = Color(0xFFFFF4E6)  // Amarelo vanilla para destaques especiais

    // 5. Cinza Pérola - Neutro
    val CinzaPerola = Color(0xFF1A1A1A)   // Cinza muito claro para backgrounds

    // 6. Azul Céu Pastel - Para informações
    val AzulCeuPastel = Color(0xFFD6EAF8)  // Azul céu suave

    // 7. Coral Suave - Para erros/avisos
    val CoralSuave = Color(0xFFFFE0D0)      // Coral pastel para erros e saldos negativos
}

/**
 * Cores de texto para garantir contraste adequado
 * Como usamos apenas cores pastéis (claras), o texto será sempre escuro
 */
object CoresTexto {
    val Principal = Color(0xFF000000)      // Azul escuro para texto principal
    val Secundario = Color(0xFF5D6D7E)     // Cinza azulado para texto secundário
    val Suave = Color(0xFF85929E)          // Cinza médio para texto menos importante
}

// ===========================================================================================
// ESQUEMA DE CORES LIGHT (MODO CLARO)
// ===========================================================================================

/**
 * Esquema de cores personalizado com pastéis para o tema claro
 * Usa apenas as 7 cores definidas + cores de texto
 */
private val PastelLightColorScheme = lightColorScheme(
    // Cores principais - Verde Menta
    primary = CoresPastel.VerdeMenta,
    onPrimary = CoresTexto.Principal,
    primaryContainer = CoresPastel.AzulSage,
    onPrimaryContainer = CoresTexto.Principal,

    // Cores secundárias - Amarelo Vanilla
    secondary = CoresPastel.AmareloVanilla,
    onSecondary = CoresTexto.Principal,
    secondaryContainer = CoresPastel.AzulCeuPastel,
    onSecondaryContainer = CoresTexto.Principal,

    // Cores terciárias - Pêssego
    tertiary = CoresPastel.PessegoPastel,
    onTertiary = CoresTexto.Principal,
    tertiaryContainer = CoresPastel.AzulSage,
    onTertiaryContainer = CoresTexto.Principal,

    // Cores de erro - Coral
    error = CoresPastel.CoralSuave,
    onError = CoresTexto.Principal,
    errorContainer = CoresPastel.CoralSuave,
    onErrorContainer = CoresTexto.Principal,

    // Cores de background
    background = CoresPastel.CinzaPerola,
    onBackground = CoresTexto.Principal,

    // Cores de superfície
    surface = Color.White,
    onSurface = CoresTexto.Principal,
    surfaceVariant = CoresPastel.AzulSage,
    onSurfaceVariant = CoresTexto.Secundario,

    // Outras cores
    inverseSurface = CoresTexto.Principal,
    inverseOnSurface = CoresPastel.CinzaPerola,
    surfaceTint = CoresPastel.VerdeMenta,

    // Contornos
    outline = CoresTexto.Suave,
    outlineVariant = CoresPastel.AzulSage,
)

// ===========================================================================================
// ESQUEMA DE CORES DARK (MODO ESCURO)
// ===========================================================================================

/**
 * Esquema de cores para modo escuro
 * Inverte o uso das cores pastéis com fundos escuros
 */
private val PastelDarkColorScheme = darkColorScheme(
    // Cores principais
    primary = CoresPastel.VerdeMenta,
    onPrimary = CoresTexto.Principal,
    primaryContainer = CoresTexto.Principal,
    onPrimaryContainer = CoresPastel.VerdeMenta,

    // Cores secundárias
    secondary = CoresPastel.AmareloVanilla,
    onSecondary = CoresTexto.Principal,
    secondaryContainer = CoresTexto.Secundario,
    onSecondaryContainer = CoresPastel.AmareloVanilla,

    // Cores terciárias
    tertiary = CoresPastel.PessegoPastel,
    onTertiary = CoresTexto.Principal,
    tertiaryContainer = CoresTexto.Secundario,
    onTertiaryContainer = CoresPastel.PessegoPastel,

    // Cores de erro
    error = CoresPastel.CoralSuave,
    onError = CoresTexto.Principal,
    errorContainer = CoresTexto.Secundario,
    onErrorContainer = CoresPastel.CoralSuave,

    // Cores de background
    background = Color(0xFF1A1A1A),
    onBackground = CoresPastel.CinzaPerola,

    // Cores de superfície
    surface = Color(0xFF2D2D2D),
    onSurface = CoresPastel.CinzaPerola,
    surfaceVariant = Color(0xFF3D3D3D),
    onSurfaceVariant = CoresPastel.AzulSage,

    // Outras cores
    inverseSurface = CoresPastel.CinzaPerola,
    inverseOnSurface = CoresTexto.Principal,
    surfaceTint = CoresPastel.VerdeMenta,

    // Contornos
    outline = CoresTexto.Secundario,
    outlineVariant = Color(0xFF4D4D4D),
)

// ===========================================================================================
// TEMA PRINCIPAL
// ===========================================================================================

/**
 * Tema principal do aplicativo Cantina com cores pastéis
 * Suporta modo claro e escuro automaticamente
 */
@Composable
fun CantinaPastelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        PastelDarkColorScheme
    } else {
        PastelLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// ===========================================================================================
// CORES CUSTOMIZADAS PARA CASOS ESPECÍFICOS
// ===========================================================================================

/**
 * Cores específicas para diferentes estados de saldo
 * Usa as cores já definidas na paleta
 */
object CoresSaldo {
    val Positivo = CoresPastel.VerdeMenta     // Verde para saldo positivo
    val Negativo = CoresPastel.CoralSuave      // Coral para saldo negativo
    val Zerado = CoresPastel.AzulSage        // Verde sage para saldo zero
}

/**
 * Cores para badges e indicadores
 * Usa as cores já definidas na paleta
 */
object CoresBadges {
    val Admin = CoresPastel.AmareloVanilla    // Amarelo vanilla para admin
    val Funcionario = CoresPastel.AzulCeuPastel // Azul céu para funcionário
    val Aviso = CoresPastel.PessegoPastel     // Pêssego para avisos
}

/**
 * Função auxiliar para obter cor de texto baseada no fundo
 * Como todas as nossas cores são pastéis (claras), sempre retorna texto escuro
 */
@Composable
fun getTextColor(backgroundColor: Color): Color {
    return CoresTexto.Principal
}
