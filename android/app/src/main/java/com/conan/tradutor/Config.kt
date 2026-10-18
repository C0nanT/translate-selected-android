package com.conan.tradutor

// ajustes, como no topo do tradutor.py
const val HIDE_AFTER_S = 15      // popup some sozinho (pausa com dedo em cima)
const val MAX_CHARS = 3000
// teto da URL ja codificada (GET). Valor CONSERVADOR, ainda NAO medido no aparelho:
// falta testar no tablet com ~3000 caracteres acentuados e anotar aqui o limite real do Google.
const val MAX_URL = 8000
const val POPUP_CHARS = 80       // largura maxima do popup, em caracteres
// idioma de destino (tl= na URL). o rotulo "Traduzir pt-BR" no manifest precisa acompanhar
const val IDIOMA_DESTINO = "pt"
const val TOQUE_REPETIDO_MS = 500L  // toques na bolha mais proximos que isto sao ignorados
