# RESTRICCIONES

> Todas son propuestas derivadas de contexto.md y requisitos.md; confirmar o ajustar.

## Plataforma
- La aplicación es móvil y se ejecuta solo en Android.
- Versión mínima: **Android 8.0 (API 26)**; `targetSdk` 36 (decidido en el S0, RES-02).

## Funcionamiento y conectividad
- La aplicación se ejecuta íntegramente en local, en el móvil.
- No usa servidores externos ni internet.

## Datos
- Los datos del usuario se almacenan en el propio dispositivo (por ejemplo, base de datos local SQLite).
- No se usa una API externa de catálogo de películas: el usuario introduce los datos manualmente.
- Cada perfil es único y se identifica por su nombre y tipo (película o serie).
- Puntuación: opcional. Si existe, valores entre 0.0 y 10, una unidad y un decimal; el 10 se escribe sin decimales.
- Ranking y prioridad pertenecen a la lista, no al perfil.
- Una lista no puede contener el mismo perfil dos veces.

## Interfaz
- Una lista muestra como máximo 10 elementos a la vez; el resto se ve con scroll.
- La interfaz está en español.

## Proyecto
- Proyecto individual: alcance, coste y tiempo limitados.
- Coste de infraestructura nulo (no hay servidores).
