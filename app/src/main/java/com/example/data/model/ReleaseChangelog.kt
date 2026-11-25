package com.example.data.model

enum class ReleaseChangeType(val label: String) {
  ADDED("Novedades"),
  IMPROVED("Mejoras"),
  FIXED("Correcciones"),
  TECHNICAL("Técnico"),
}

data class ReleaseChangeGroup(
  val type: ReleaseChangeType,
  val items: List<String>,
)

data class ReleaseEntry(
  val tag: String,
  val versionName: String,
  val versionCode: Int,
  val title: String,
  val date: String,
  val isLatest: Boolean,
  val summary: String,
  val artifacts: List<String>,
  val groups: List<ReleaseChangeGroup>,
)

object ReleaseCatalog {
  const val REPO_OWNER = "bugambalia"
  const val REPO_NAME = "Flora"
  const val GITHUB_REPO_URL = "https://github.com/$REPO_OWNER/$REPO_NAME"
  const val GITHUB_RELEASES_URL = "$GITHUB_REPO_URL/releases"
  const val GITHUB_PAGES_URL = "https://$REPO_OWNER.github.io/$REPO_NAME/"
  const val GITHUB_CHANGELOG_URL = "$GITHUB_REPO_URL/blob/main/CHANGELOG.md"

  val entries: List<ReleaseEntry> = listOf(
    ReleaseEntry(
      tag = "v1.1-dev",
      versionName = "1.1 (en desarrollo)",
      versionCode = 2,
      title = "Portal GitHub Releases & Historial en la app",
      date = "Próxima etiqueta",
      isLatest = false,
      summary = "Incorpora la página web oficial en GitHub Pages para Releases y Changelog, automatización de notas desde CHANGELOG.md y visor integrado en Ajustes.",
      artifacts = listOf("docs/index.html", "CHANGELOG.md", ".github/workflows/pages.yml"),
      groups = listOf(
        ReleaseChangeGroup(
          type = ReleaseChangeType.ADDED,
          items = listOf(
            "Página web en GitHub Pages (docs/) con búsqueda, filtros por tipo de cambio, descargas APK/AAB, SHA-256 y galería de capturas.",
            "Despliegue automático de GitHub Pages al subir cambios a main o publicar una release.",
            "Pantalla de Versiones y Releases dentro de Ajustes con enlaces directos a GitHub.",
          ),
        ),
        ReleaseChangeGroup(
          type = ReleaseChangeType.IMPROVED,
          items = listOf(
            "Workflow de Releases (release.yml) sincronizado con CHANGELOG.md para incluir notas detalladas en cada etiqueta v*.",
          ),
        ),
      ),
    ),
    ReleaseEntry(
      tag = "v1.0",
      versionName = "1.0",
      versionCode = 1,
      title = "Flora v1.0 — Edición Herbario Inicial",
      date = "2026-11-25",
      isLatest = true,
      summary = "Primera versión pública oficial de Flora: reproductor musical local y sin conexión con estética de herbario ilustrado, motor Media3/ExoPlayer, ecualizador botánico y paletas dinámicas.",
      artifacts = listOf("Flora-v1.0.apk", "Flora-v1.0.apk.sha256", "Flora-v1.0.aab"),
      groups = listOf(
        ReleaseChangeGroup(
          type = ReleaseChangeType.ADDED,
          items = listOf(
            "Motor Media3 / ExoPlayer con servicio multimedia en primer plano, cola reordenable, control de velocidad y temporizador de reposo.",
            "Reproducción Gapless sin cortes y Crossfade progresivo entre canciones.",
            "Visualizador botánico en tiempo real por FFT (graves, medios y agudos).",
            "Ecualizador visual de 5 bandas como tallos botánicos animados, Bass Boost y Virtualizer.",
            "Paletas dinámicas (Modo Camaleón) desde carátulas y tema Claro, Oscuro o Sistema.",
            "Letras sincronizadas (.lrc), escaneo automático de fonoteca por SAF e índice vertical A–Z.",
            "Playlists con exportación/importación M3U y JSON y Herbario botánico desbloqueable.",
          ),
        ),
        ReleaseChangeGroup(
          type = ReleaseChangeType.TECHNICAL,
          items = listOf(
            "Compilación Release optimizada con R8 y reducción de recursos.",
            "Caché LRU de carátulas en RGB_565 y nombres SHA-256 sin colisiones.",
            "Operación 100 % local y sin conexión con respaldo selectivo de Room y DataStore.",
          ),
        ),
      ),
    ),
  )
}
