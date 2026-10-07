# Desarrollo local y pruebas con Postman

## PostgreSQL

1. Crea una base de datos llamada `pmiautoevaluacion` en tu PostgreSQL local.
2. Crea `backend/src/main/resources/application-local.properties` con estas propiedades y tus valores locales:

   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/pmiautoevaluacion
   spring.datasource.username=postgres
   spring.datasource.password=<tu_contrasena_local>
   jwt.secret=<una_clave_larga_y_privada>
   ```

   Este archivo está excluido de Git. `application.properties` usa variables de entorno y debe permanecer sin credenciales.

3. Desde PowerShell, dentro de `backend`, inicia el servidor:

   ```powershell
   $env:SPRING_PROFILES_ACTIVE = 'local'
   .\gradlew.bat bootRun
   ```

Flyway crea las tablas al iniciar, aplica V6 para cargar 4 áreas, 15 procesos y 45 componentes activos de la Guía 34 de 2026 y V7 para agregar los textos de fortalezas y oportunidades. Las migraciones quedan registradas y no se repiten en cada arranque. El servidor escucha en `http://localhost:8080`. Consulta [el inventario y su fuente](catalogo-guia34-2026.md).

## Postman

Necesitas un usuario de prueba en la base de datos: el proyecto todavía no expone un endpoint de registro. La contraseña almacenada debe ser un hash BCrypt.

| Acción | Método y URL | JSON o cabecera |
| --- | --- | --- |
| Entrar como Secretaría | `POST http://localhost:8080/api/auth/secretary/login` | `{"email":"<correo>","password":"<contraseña>"}` |
| Entrar como institución | `POST http://localhost:8080/api/auth/establishment/login` | `{"daneCode":"<código DANE>","password":"<contraseña>"}` |
| Consultar la sesión | `GET http://localhost:8080/api/me` | `Authorization: Bearer <token recibido>` |

Las rutas de login devuelven un `token`. `/api/me` requiere ese token; sin él responde `401`.

## Borrador anual

Con el token de una institución educativa, usa estas rutas:

| Acción | Método y URL | Cuerpo |
| --- | --- | --- |
| Crear o recuperar borrador | `PUT http://localhost:8080/api/self-evaluations/2026` | Sin cuerpo |
| Consultar borrador | `GET http://localhost:8080/api/self-evaluations/2026` | Sin cuerpo |
| Listar años propios | `GET http://localhost:8080/api/self-evaluations` | Sin cuerpo |
| Guardar valoración | `PUT http://localhost:8080/api/self-evaluations/2026/valuations/<componentId>` | `{"level":3,"evidenceUrl":"https://ejemplo.org/evidencia","evidenceNote":"Descripción de la evidencia","strengths":"Trabajo articulado entre docentes","improvementOpportunities":"Fortalecer el seguimiento"}` |

`level` identifica el estado elegido manualmente: `1` = Existencia, `2` = Pertinencia, `3` = Apropiación y `4` = Mejoramiento continuo. La nota y el enlace son opcionales mientras el registro esté en borrador. El componente debe existir y estar activo en el catálogo. Si la vista todavía muestra el catálogo vacío, verifica que el backend haya iniciado con V6 aplicada y que estés usando la base de datos configurada en el perfil local.

La respuesta de una valoración conserva `level` y añade `state` (por ejemplo, `APPROPRIATION`), `stateLabel` (`Apropiación`), `improvementChance` y `strength`. La clasificación viene del diagrama del equipo: estados 1/2 son oportunidades de mejora y 3/4 son fortalezas. La consulta o creación del borrador devuelve además `totals`, con el número de valoraciones guardadas en `existence`, `pertinence`, `appropriation`, `continuousImprovement` y su `total`. Un componente aporta una unidad a su estado, sin multiplicar por el código 1–4. El borrador sin valoraciones devuelve todos los conteos en cero.

La vista muestra los estados por nombre y calcula subtotales por proceso y totales del área seleccionada a partir del catálogo visible y las valoraciones guardadas. La API conserva el total anual del borrador, también con componentes desactivados posteriormente. Seleccionar otro estado sin guardar todavía no modifica los conteos. Los registros numéricos existentes se reconstruyen como objetos State al consultarlos.

Consulta [valoración de componentes](valoracion-componentes.md) para el diseño State y la relación con la futura priorización del PMI.

### Textos y porcentajes

Reinicia el backend para cargar el código nuevo. Flyway aplica V7 automáticamente, conservando las valoraciones previas; no ejecutes el SQL manualmente. En el espacio de la institución, elige una de las cuatro entradas de autoevaluación por área. Cada componente muestra **Fortalezas** y **Oportunidades de mejora**, que se guardan con **Guardar valoración** y se recuperan al consultar de nuevo.

Los textos permitidos son opcionales en el borrador, de hasta 10.000 caracteres; valores vacíos o nulos se guardan como `NULL`. El PUT reemplaza los campos y omitir cualquiera de estos textos también lo borra. Son respuestas de la institución y no comentarios de revisión de Secretaría.

La aptitud confirmada por Juan es: Existencia admite solo oportunidades; Pertinencia y Apropiación admiten ambos textos; Mejoramiento continuo admite solo fortalezas. Vue deshabilita el campo que no corresponde y la API devuelve `strengthsAllowed` e `improvementOpportunitiesAllowed`. Enviar desde Postman una fortaleza no vacía con `level: 1` o una oportunidad no vacía con `level: 4` devuelve `400` y conserva la valoración guardada.

Al cambiar de estado, un texto que deja de corresponder permanece visible. Puedes regresar a un estado compatible o usar **Quitar fortaleza** / **Quitar oportunidad de mejora** y después guardar. La selección del estado no borra el texto por sí sola. Esta regla se aplica al código nuevo, sin cambiar V7 ni limpiar automáticamente registros previos.

`totals.percentages` agrega los porcentajes anuales 0–100 por estado, redondeados a dos decimales. Vue presenta tablas del área seleccionada y de sus procesos, con sus propios porcentajes. El denominador es el total de valoraciones guardadas de cada tabla, no los 45 componentes del catálogo. Sin valoraciones todos los porcentajes son cero; los cambios sin guardar no alteran los porcentajes.

### Acceso por área

Desde `/dashboard`, las instituciones ven cuatro tarjetas con los nombres del catálogo. Cada tarjeta abre `/autoevaluacion/<UUID de área>`, donde aparecen únicamente sus procesos y componentes. El avance, el resumen y el total corresponden al área elegida. La entrada antigua `/autoevaluacion` vuelve al selector del espacio institucional. Un área inexistente o desactivada muestra un aviso y un enlace para regresar.

La división organiza las pantallas y conserva el registro anual existente; no requiere otra migración. Las tablas tienen bordes, encabezados y una fila de porcentajes diferenciada; en móvil se desplazan dentro de su contenedor. Los procesos se distinguen con encabezados verde oscuro. Este diseño modesto fue solicitado por Juan y sigue siendo provisional mientras se define el manual de imagen.

## Consulta por Secretaría

Con el token de Secretaría, selecciona una institución mediante su UUID, no mediante el UUID del usuario de Secretaría:

```text
GET http://localhost:8080/api/self-evaluations?establishmentId=<UUID de institución>
GET http://localhost:8080/api/self-evaluations/2026?establishmentId=<UUID de institución>
Authorization: Bearer <token de Secretaría>
```

Secretaría sin selector recibe `400`. Una institución puede omitirlo para consultar sus propios registros; si intenta indicar una institución distinta recibe `403`. Consultar una institución inexistente o una autoevaluación anual inexistente devuelve `404`, mientras que una institución existente sin autoevaluaciones tiene una lista vacía. Secretaría recibe `403` al intentar crear borradores o cambiar valoraciones mediante PUT.

El frontend de Secretaría y el registro de observaciones todavía no están implementados; este acceso de consulta se puede verificar en Postman. En el borrador actual también se consultan registros `DRAFT`; el flujo formal de envío, revisión y devolución requiere sus propios estados y permisos.
