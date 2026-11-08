# CineTrack (CineSStar)

App Android 100 % local para catalogar películas y series. Especificación en [SOFTWARE/](SOFTWARE/) y prototipo en [GoogleStitch/](GoogleStitch/).

## Requisitos

- Android Studio (su JBR 21 se usa como JDK de Gradle, ver `gradle.properties`) y Android SDK con la plataforma 37.
- `local.properties` con `sdk.dir` (Android Studio lo crea al abrir el proyecto).

## Comandos

| Qué | Comando |
|---|---|
| Compilar | `./gradlew :app:assembleDebug` |
| Tests (JVM + Robolectric) | `./gradlew :app:testDebugUnitTest` |
| Lint | `./gradlew :app:ktlintCheck :app:detekt` |
| Formatear | `./gradlew :app:ktlintFormat` |
| Puerta completa de un sprint | `./gradlew :app:ktlintCheck :app:detekt :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease` |

Los tests de interfaz y de base de datos usan Robolectric, sin emulador. El test de capturas (T0.08) deja las imágenes de la app y del prototipo en `app/build/capturas/`.

## Estructura

```
app/src/main/java/com/cinesstar/cinetrack/
  data/        room (entidades, DAO, base de datos), repository
  domain/      Puntuacion, Normalizador, modelo y errores de dominio (M-xx)
  navigation/  rutas y NavHost
  ui/theme/    tokens de design.md §2 (único sitio con colores y medidas)
  ui/components/ barras superior e inferior, iconos Material Symbols
app/src/test/  tests de los sprints (T0.xx, T1.xx, TX.xx) y fixtures
app/schemas/   esquema exportado de Room (migraciones)
third_party/   licencias de las fuentes empaquetadas
```
