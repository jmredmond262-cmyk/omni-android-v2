# OMNI Android — v2

Aplicación Android construida con Kotlin y Jetpack Compose. Esta versión parte del ZIP actualizado entregado para OMNI e incorpora una pantalla de bienvenida animada con el logotipo oficial (sin eslogan).

## Compilación local

1. Abrir el proyecto en Android Studio (JDK 17).
2. Crear `.env` a partir de `.env.example` si se van a utilizar servicios de IA o Google Places.
3. Sincronizar Gradle y ejecutar la variante `debug`.

## Compilación en GitHub Actions

El workflow `.github/workflows/android-build.yml` genera un APK debug en cada push o pull request hacia `main`. Para habilitar integraciones, configurar los secretos del repositorio `GEMINI_API_KEY` y `GOOGLE_MAPS_API_KEY`. No subir `.env`, claves API ni keystores al repositorio.

El APK se publica como artefacto de la ejecución del workflow (`omni-v2-debug-apk`).
