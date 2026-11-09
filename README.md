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

Los tests de interfaz y de base de datos usan Robolectric, sin emulador. Los tests de capturas (T0.08, T2.31, T3.32, T4.20, T5.35, T6.24, T7.23, T8.21) dejan las imágenes de la app y del prototipo en `app/build/capturas/`.

## Estructura

```
app/src/main/java/com/cinesstar/cinetrack/
  data/        room (entidades, DAO, base de datos), repository, backup (copia de seguridad y archivos del selector)
  domain/      Puntuacion, Normalizador, modelo y errores de dominio (M-xx), formato de la copia y reglas de fusión
  navigation/  rutas y NavHost
  ui/theme/    tokens de design.md §2 (único sitio con colores y medidas)
  ui/components/ barras, iconos Material Symbols, campos, slider, toasts y diálogos
  ui/perfilform/ alta y edición de perfil (CU-01, CU-02, CU-10) y su ViewModel
  ui/perfil/   detalle del perfil (CU-07) con estado, nota, crítica y eliminar (CU-03..CU-06)
  ui/biblioteca/ Biblioteca (CU-08): contadores, filtros por tipo, estado y escala, tarjetas de medio y agrupar (CU-09)
  ui/listas/   Mis Listas (CU-12): crear (CU-11), renombrar (CU-14) y eliminar (CU-19)
  ui/lista/    lista abierta (CU-13): añadir títulos con posición o prioridad (CU-15), quitar con el aviso único M-11 (CU-16),
               cambiar prioridad (CU-17), reordenar el ranking con ▲▼ y arrastre (CU-18), renombrar y eliminar
  ui/buscar/   Buscar (CU-20): nombre, tipo, rango de puntuación con slider doble, géneros, idioma, saga y estado
  ui/datos/    Datos Locales (pestaña de Buscar) y Ajustes: métricas, exportar (CU-21) e importar por fusión (CU-22)
app/src/test/  tests de los sprints (T0.xx … T8.xx, TX.xx), fixtures y copias de referencia del formato (resources/copias)
app/schemas/   esquema exportado de Room (migraciones)
third_party/   licencias de las fuentes empaquetadas
```
