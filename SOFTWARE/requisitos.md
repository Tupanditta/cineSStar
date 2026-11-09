el# REQUISITOS FUNCIONALES

## Gestión de perfiles (película o serie)

##### Crear
- El usuario puede añadir una nueva película o serie (cada una de estas contiene un perfil específico y único)
- El usuario puede crear un nuevo perfil para una película o serie
- Al añadir una nueva película/serie (un perfil) el único atributo obligatorio es el nombre. La puntuación, el género, comentarios, saga e idioma son opcionales (un perfil puede guardarse sin puntuación, sea cual sea su estado). 
- El usuario debe indicar si el perfil es una película o una serie al crearlo.
- El sistema debe impedir (o avisar de) la creación de un perfil duplicado (mismo nombre y mismo tipo).
- Si el usuario introduce una puntuación, el sistema debe validarla (rango y formato) y mostrar un mensaje de error si no es válida. No se podrá registrar un perfil con una puntuación de rango y/o formato no válido. Dejar la puntuación vacía es válido.
- El usuario puede asignar uno o varios géneros a un perfil, elegidos de una lista predefinida por la aplicación.
- El usuario puede asignar uno o varios idiomas a un perfil, elegidos de una lista predefinida por la aplicación.
- Un perfil pertenece como máximo a una saga (puede no pertenecer a ninguna). El orden dentro de la saga no se guarda.
- Las sagas quedan registradas en la aplicación: al asignar una saga, el usuario elige una ya existente o crea una nueva, sin tener que reescribirla.
- Una serie tiene una única puntuación para todo el perfil (no por temporada ni episodio).
- Una serie en estado "viendo" no guarda progreso (temporada o episodio).

##### Modificar
- El usuario puede modificar la puntuación de una película o serie: añadirla, cambiarla o quitarla (volver a "sin puntuación")
- El usuario puede modificar (añadir, cambiar, borrar) el comentario de una película o serie existente
- El usuario puede cambiar el estado de una película o serie de pendiente de ver a viendo o quitarlo y no poner nada (si no pone nada significa que ya la ha visto). En ningún caso es obligatorio introducir la puntuación.
- El usuario puede modificar el nombre, género, saga e idioma de un perfil existente.
- El usuario puede cambiar el estado de un perfil a "pendiente de ver" desde cualquier otro estado, y a "viendo" desde cualquier otro estado.

##### Borrar
- El usuario puede borrar una película o serie (se elimina el perfil completo)
- Antes de borrar un perfil, el sistema pide confirmación al usuario.
- Al borrar un perfil, este se elimina también de todas las listas (normales, de prioridad y de ranking) en las que estuviera, y de la base de datos

##### Visualizar
- El usuario puede visualizar el perfil de una película o serie
- El usuario puede ver el estado de cada perfil (pendiente de ver, viendo, visto) en la vista del perfil y en los listados.

## Agrupar
- El usuario puede visualizar las distintas películas y series en grupos según: rangos de puntuación (predeterminados por la aplicación), sagas, idioma y géneros. Podrá agrupar todas las series y películas juntas, solo las películas o solo las series. 
- El usuario puede agrupar los perfiles por estado (pendiente de ver, viendo, visto).
- Los rangos de puntuación predeterminados son 5: 0-2.9, 3-4.9, 5-6.9, 7-8.9 y 9-10. Los perfiles sin puntuación aparecen en un grupo adicional "Sin puntuación".
- Un perfil con varios géneros o idiomas aparece en cada uno de los grupos correspondientes.

## Listas

##### Crear
- El usuario puede crear una nueva lista de películas/series
- El usuario puede crear una lista de prioridad de películas/series
- El usuario puede crear una lista de ranking de películas/series
- Una lista puede contener películas y series mezcladas.
- Una película o serie puede pertenecer a varias listas a la vez.

##### Restricciones
- Una lista puede tener restricciones para que solo puedan pertenecer a esta series, y no películas (o vicebersa)

##### Modificar
- El usuario puede modificar el nombre de una lista existente
- El usuario puede añadir/borrar películas o series de una lista existente
- Si el usuario borra el último elemento de una lista, el sistema le manda un único mensaje (una única vez) para darle la opción de borrar la lista. El usuario puede aceptar o no (si no acepta, posteriormente puede borrar la lista manualmente)
- El usuario puede modificar la prioridad de una película/serie contenida en una lista de prioridad
- El usuario puede reordenar el orden de una película/serie de una lista de ranking
- El usuario puede borrar una lista completa, con confirmación previa. Borrar una lista no borra los perfiles que contiene.
- Al añadir un elemento a una lista de prioridad o ranking, el usuario indica (o el sistema asigna por defecto al final) su prioridad o posición.
- La prioridad tiene tres niveles: alta, media y baja.
- Una lista de ranking no tiene tamaño máximo.

##### Visualizar
- El usuario puede visualizar los elementos de una lista (normal, de prioridad o de ranking)
- El usuario puede ver todas sus listas con su nombre, tipo y número de elementos.

## Buscar
- El usuario puede usar el buscador para buscar las películas según sus atributos: puntuación mínima y máxima (rango de punturación; los perfiles sin puntuación quedan fuera de una búsqueda por rango), nombre, género, idioma o saga.
- El usuario puede combinar varios criterios de búsqueda a la vez.
- La búsqueda por nombre no distingue mayúsculas/minúsculas ni acentos.
- El usuario puede restringir la búsqueda a películas, a series o a ambas (por defecto, ambas).
- Si no hay resultados, el sistema muestra un mensaje indicándolo.

## Datos
- El usuario puede exportar sus datos (perfiles y listas) a un archivo y volver a importarlos (copia de seguridad).

# REQUISITOS NO FUNCIONALES
- El máximo de películas/series que puede mostrar a la vez la aplicación en una lista de cualquier tipo son 10. Para ver el resto se deberá hacer scroll. 
- El puesto en el ranking de una película/serie en una lista de ranking no es un atributo de la película/serie, solo sirve para ordenarla y mostrarla en esa lista en concreto
- La prioridad de una película/serie en una lista de prioridad no es un atributo de la película/serie, solo sirve para ordenarla y mostrarla en esa lista en concreto
- La puntuación de una película o serie puede tomar valores entre 0.0 y 10. La unidad y un decimal. La puntuación máxima es 10, sin decimales. 
- Se admite el 0.0 y cualquier valor con un decimal (pasos de 0.1) entre 0.0 y 10.
- Disponibilidad offline: todas las funciones funcionan sin conexión a internet.
- Persistencia: los datos del usuario se conservan al cerrar la aplicación o reiniciar el dispositivo.
- Rendimiento: abrir un perfil, una lista o una búsqueda local responde en menos de 1 segundo con hasta 1000 perfiles almacenados.
- Usabilidad: las acciones principales (añadir perfil, puntuar, añadir a lista) se completan en 3 pasos o menos.
- Compatibilidad: la aplicación funciona en Android (versión mínima por definir).
- Interfaz adaptable a distintos tamaños de pantalla móvil, en orientación vertical como mínimo.
- Idioma de la interfaz: español (otros idiomas fuera de alcance por ahora).
