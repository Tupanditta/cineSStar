# PRUEBAS MANUALES — CineTrack (CineSStar)

> Pruebas de tipo **M** de `sprints.md` y medidas que solo son fiables en un dispositivo real. Las automáticas corren con
> `./gradlew :app:testDebugUnitTest` (Robolectric, sin emulador). Anota en cada fila el dispositivo, la versión de
> Android, la fecha y el resultado (✔ / ✘ con la incidencia).

## Preparación

1. Instalar el APK de release firmado (ver README, "Firma del release") o el de debug para las pruebas que no dicen release.
2. Para las pruebas con datos: importar `app/src/test/resources/copias/cinetrack_backup_v2.json` (Ajustes → "Seleccionar archivo .JSON").
3. Para las de rendimiento, importar la copia de 1000 perfiles que deja `./gradlew :app:testDebugUnitTest` en
   `app/build/capturas/cinetrack_backup_1000_perfiles.json` (fixture grande: 1000 perfiles, 30 listas, 5000 elementos).

## Lista de comprobación

| ID | Qué | Pasos | Resultado esperado | Dispositivo / Android / fecha | Resultado |
|---|---|---|---|---|---|
| T8.19 | RNF-06: copia de seguridad sin red | Modo avión. Exportar a Descargas; importar ese mismo archivo | M-15; luego M-19 con 0 nuevos y M-20 con ceros | | |
| T8.20 | Copia en memoria interna y SD, y en otro dispositivo | Exportar a memoria interna y, si hay, a la tarjeta SD. Copiar el archivo a otro móvil e importarlo | Se importa; los datos coinciden | | |
| T9.05 | Scroll de la Biblioteca con 1000 perfiles | Opciones de desarrollador → "Mostrar barras de GPU" (o Android Studio → Profiler). Recorrer la Biblioteca de arriba abajo | ≥ 55 fps de media, sin saltos apreciables | | |
| T9.06 | Arranque en frío con 1000 perfiles | Forzar detención de la app; abrirla y cronometrar hasta ver la primera tarjeta (o `adb shell am start -W`) | < 2 s | | |
| T9.10 | TalkBack en las pantallas clave | Activar TalkBack. Recorrer Biblioteca, detalle, Listas, lista abierta, Buscar, Ajustes y Añadir | Todo lo pulsable se anuncia con un nombre; se puede crear un perfil y una lista solo con TalkBack | | |
| T9.15 | RNF-07: los datos sobreviven a un reinicio | Crear datos; reiniciar el dispositivo; abrir la app | Datos intactos | | |
| T9.16 | RNF-10: versiones de Android | Repetir T9.13 a mano en Android 8.0 (minSdk 26), una intermedia (p. ej. 12) y la última | Funciona en las tres | | |
| T9.17 | RNF-06: una sesión completa sin red | Modo avión y sin datos móviles. Usar todas las funciones (alta, edición, listas, búsqueda, copia) | Todo funciona | | |
| T9.19 | Release con R8 | Instalar el APK de release firmado y repetir T9.13 a mano | Sin diferencias con debug | | |
| T9.09 | Bloqueo vertical | Girar el móvil en cada pantalla | La app no gira. En tablets o plegables abiertos con Android 16+ el sistema puede girarla: el contenido sigue siendo usable | | |

## Recorrido de T9.13 (para T9.16 y T9.19)

Crear 3 perfiles → puntuar uno → comentar → crear una lista de cada tipo → añadir títulos → reordenar el ranking →
buscar → agrupar por género → exportar → desinstalar y reinstalar (o borrar los datos de la app) → importar →
comprobar que perfiles, notas, comentarios, listas, posiciones y prioridades son los mismos.
