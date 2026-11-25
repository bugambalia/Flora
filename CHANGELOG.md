# Historial de cambios (Changelog) — Flora

Todos los cambios relevantes de **Flora — Reproductor Musical Botánico** se documentan en este archivo.
El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y las versiones siguen etiquetas `v<mayor>.<menor>` publicadas en [GitHub Releases](https://github.com/bugambalia/Flora/releases).

---

## [Sin publicar / Próxima v1.1]

### Novedades
- **Página web de Releases y Changelog en GitHub Pages** (`docs/`): portal visual con el estilo de herbario ilustrado de Flora para consultar versiones, notas de cambio, checksums SHA-256, capturas de pantalla y descargas directas de APK/AAB.
- **Despliegue automático de GitHub Pages** (`.github/workflows/pages.yml`): actualiza el historial público automáticamente al subir cambios a `main` o publicar una nueva release.
- **Visor de Versiones y Changelog dentro de la app**: nueva sección accesible desde **Ajustes → Versiones y Releases** con acceso directo a GitHub Releases, GitHub Pages y `CHANGELOG.md`.

### Mejoras
- **Workflow de Releases enriquecido** (`.github/workflows/release.yml`): extrae automáticamente las notas de `CHANGELOG.md` correspondientes a la etiqueta `v*` al publicar cada GitHub Release.

---

## [1.0] — 2026-11-25

Primera versión pública oficial de **Flora** (`versionName "1.0"`, `versionCode 1`, `com.aistudio.herbariumplayer.bvnqtx`).

### Novedades
- **Reproductor Media3 / ExoPlayer** con servicio multimedia en primer plano (`mediaPlayback`), cola flexible (*Reproducir siguiente*, reordenamiento), control de velocidad y temporizador de reposo.
- **Reproducción Gapless y Crossfade**: transición continua sin cortes entre pistas y fundido cruzado progresivo.
- **Visualizador orgánico en tiempo real**: análisis FFT de graves, medios y agudos que anima hojas y pétalos en *Ahora suena* al ritmo de la música.
- **Ecualizador visual de 5 bandas**: representado como tallos botánicos animados con perfiles preestablecidos (*Acústico cálido*, *Bosque*, *Resonancia*, etc.), **Bass Boost** y **Virtualizer**.
- **Paleta camaleón y apariencia adaptativa**: extracción dinámica de colores desde la carátula con `AndroidX Palette` y soporte para tema **Claro**, **Oscuro** y **Sistema**.
- **Letras sincronizadas (`.lrc`)**: importación, edición y seguimiento en vivo dentro de *Ahora suena*.
- **Detección automática de fonoteca**: escaneo por SAF y carpetas locales con umbral de duración configurable (90 s por defecto) y clasificación en *Botanical Library*.
- **Catálogo Canciones e índice A–Z**: búsqueda instantánea, ordenamiento múltiple y filtro por fuentes/géneros.
- **Playlists M3U / JSON y Herbario interactivo**: creación, duplicado, reordenamiento, exportación/importación y desbloqueo de láminas botánicas por hábitos de escucha.

### Notas técnicas y compilación
- **Optimización Release**: minificación R8 (`isMinifyEnabled = true`) y reducción de recursos (`isShrinkResources = true`) con reglas ProGuard para KotlinX Serialization, Room, DataStore y Media3.
- **Gestión de memoria**: caché LRU de carátulas en `RGB_565`, reescalado automático y hash SHA-256 sin colisiones para carátulas personalizadas.
- **Privacidad 100 % local**: sin cuentas, sin telemetría ni conexión requerida; respaldo selectivo de Room y DataStore.
- **Artefactos firmados**: `Flora-v1.0.apk`, `Flora-v1.0.apk.sha256` y `Flora-v1.0.aab`.
