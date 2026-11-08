# Recursos de terceros empaquetados

| Recurso | Archivo en la app | Origen | Licencia |
|---|---|---|---|
| Fuente Inter (variable, ejes `opsz` y `wght`) | `app/src/main/res/font/inter.ttf` | github.com/google/fonts (`ofl/inter`) | SIL Open Font License 1.1 (`Inter-OFL.txt`) |
| Material Symbols Outlined (variable, ejes `FILL`, `GRAD`, `opsz`, `wght`) | `app/src/main/res/font/material_symbols_outlined.ttf` | github.com/google/material-design-icons (`variablefont`) | Apache License 2.0 |
| Logo de CineTrack | `app/src/main/res/drawable-nodpi/cinetrack_logo.jpg` | Imagen `CineTrack Logo` del prototipo de Google Stitch | Del proyecto |

Se empaquetan para que la app funcione sin conexión (RES-03): no se usan fuentes descargables ni CDN.
