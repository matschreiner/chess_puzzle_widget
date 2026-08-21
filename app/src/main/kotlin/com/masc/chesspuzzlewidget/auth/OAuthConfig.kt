package com.masc.chesspuzzlewidget.auth

object OAuthConfig {
    const val CLIENT_ID = "chess-puzzle-widget"
    const val REDIRECT_URI = "com.masc.chesspuzzlewidget://oauth-callback"
    const val AUTHORIZATION_ENDPOINT = "https://lichess.org/oauth"
    const val TOKEN_ENDPOINT = "https://lichess.org/api/token"
    const val SCOPE = "puzzle:read puzzle:write"
}
