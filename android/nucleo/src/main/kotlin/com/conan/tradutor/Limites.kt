package com.conan.tradutor

// limites das regras de traducao (conferidos pelos casos em casos/); o resto dos ajustes fica em app/.../Config.kt
const val MAX_CHARS = 3000
// teto da URL ja codificada (GET). Valor CONSERVADOR, ainda NAO medido no aparelho:
// falta testar no tablet com ~3000 caracteres acentuados e anotar aqui o limite real do Google.
const val MAX_URL = 8000
// idioma de destino (tl= na URL). o rotulo "Traduzir pt-BR" no manifest precisa acompanhar
const val IDIOMA_DESTINO = "pt"
