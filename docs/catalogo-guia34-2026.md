# Catálogo oficial: Guía 34 de 2026

Fuente: [Ministerio de Educación Nacional, Guía 34 actualizada en 2026](https://www.mineducacion.gov.co/1780/articles-430002_recurso_3.pdf), anexo 2, pp. 164-167. Se transcribieron los nombres y su agrupación; se normalizaron la capitalización de los títulos de área y los saltos de línea de las tablas. Este catálogo corresponde a la versión que enlaza la wiki del equipo, sin incorporar adaptaciones municipales no documentadas.

## Carga con Flyway

La migración `backend/src/main/resources/db/migration/V6__seed_guia34_2026_catalog.sql` agrega 4 áreas, 15 procesos y 45 componentes activos. Las tablas fueron creadas por V4. Cada proceso pertenece a su área y cada componente a su proceso. Los UUID son constantes para que las referencias coincidan entre las bases de datos de los desarrolladores.

Al iniciar el backend o sus pruebas con el perfil `local`, Flyway aplica V6 y la registra en `flyway_schema_history`. En los siguientes arranques comprueba que ya está aplicada y conserva los registros. La carga no crea valoraciones, no asigna estados de desarrollo a los componentes y no modifica borradores ni usuarios. `ACTIVE` significa que un elemento del catálogo está disponible: es distinto de Existencia/Pertinencia/Apropiación/Mejoramiento continuo, que elige cada institución.

La BD local estaba vacía en las tablas `area`, `process` y `component` antes de preparar la carga. Si se aplica en una base que ya tenga un catálogo cargado a mano, primero hay que comprobar coincidencias de nombres y relaciones. La migración inserta registros nuevos y no fusiona esos datos previos.

```powershell
# Dentro de backend
$env:SPRING_PROFILES_ACTIVE = 'local'
.\gradlew.bat bootRun
```

No hay que copiar y ejecutar el SQL en pgAdmin ni enviar 64 solicitudes POST. Flyway lleva el registro de qué versión se aplicó. Después, con el token de la institución, se pueden consultar `GET /api/areas?onlyActive=true`, `GET /api/processes?onlyActive=true` y `GET /api/components?onlyActive=true`. La vista `/autoevaluacion` consume esas rutas y presenta el catálogo al abrir un borrador.

## Trazabilidad y criterios de aceptación

| Regla | Fuente | Validación |
| --- | --- | --- |
| Cuatro áreas con procesos y componentes | Anexo 2, pp. 164-167 | 4 áreas, 15 procesos, 45 componentes; relaciones válidas |
| Catálogo disponible para diligenciar el borrador | Flujo de la wiki y decisión de carga inicial del equipo | Todos los registros cargados tienen estado `ACTIVE` |
| Valoración elegida manualmente por institución | Decisión de Juan y matriz del anexo 2 | La carga no agrega filas a `component_valuation` |
| Evidencias pertinentes al contexto institucional | Anexo 3, pp. 168-175 | No se precargan evidencias obligatorias |
| Carga única por base de datos | Decisión técnica: Flyway | V6 registrada una vez y nuevos arranques sin duplicados |

## Inventario de carga

| Área | Procesos | Componentes | Matriz |
| --- | ---: | ---: | --- |
| Gestión directiva y estratégica | 5 | 16 | p. 164-165 |
| Gestión académica y pedagógica | 3 | 11 | p. 165 |
| Gestión administrativa y financiera | 4 | 10 | p. 166 |
| Gestión de la comunidad | 3 | 8 | p. 167 |
| **Total** | **15** | **45** | pp. 164-167 |

### Gestión directiva y estratégica

**Direccionamiento y gestión estratégica** (p. 164)

- Construcción del Proyecto educativo institucional, en sus diversas denominaciones
- Misión, visión y metas institucionales
- Liderazgo y articulación institucional
- Gestión integral de riesgos socionaturales, naturales, antrópicos no intencionados y derivados del conflicto armado

**Gobierno escolar, instancias y otros mecanismos de participación** (p. 164)

- Consejo directivo
- Consejo académico
- Participación de estudiantes en el establecimiento educativo (Consejo de Estudiantes, personería y contraloría estudiantil, representantes al Consejo Directivo)
- Instancias de participación de las familias en la dirección del establecimiento educativo

**Cultura institucional** (p. 164)

- Comunicación y trabajo en equipo
- Identificación de buenas prácticas, experiencias significativas y de fomento CTEI lideradas por docentes

**Ambiente y clima escolar** (p. 165)

- Inducción a nuevos estudiantes
- Manual de Convivencia
- Comité Escolar de Convivencia
- Motivación hacia el aprendizaje, desarrollo del sentido de pertenencia y participación de estudiantes

**Estrategias para el bienestar y la permanencia de estudiantes** (p. 165)

- Gestión de la jornada escolar para la formación integral
- Servicios complementarios para el bienestar y la permanencia de los estudiantes

### Gestión académica y pedagógica

**Diseño pedagógico** (p. 165)

- Enfoque Pedagógico y propósitos formativos
- Plan de Estudios
- Tiempo para el aprendizaje
- Planeación para el aprendizaje
- Evaluación para y del aprendizaje

**Prácticas pedagógicas y gestión de aula** (p. 165)

- Estrategias pedagógicas para el aprendizaje
- Mediación pedagógica con recursos para el aprendizaje y la enseñanza
- Gestión pedagógica del aula

**Acompañamiento y seguimiento académico** (p. 165)

- Uso pedagógico de la información para la formación integral
- Apoyo pedagógico a todas las personas sin excepción
- Seguimiento a los aprendizajes y trayectorias educativas

### Gestión administrativa y financiera

**Apoyo al proceso de gestión de la cobertura educativa** (p. 166)

- Promoción, matrícula y novedades
- Gestión documental y archivo académico

**Administración de la planta física y de los recursos** (p. 166)

- Seguimiento al plan de mantenimiento de la planta física y al uso de los espacios
- Administración y aprovechamiento de equipos y recursos para el aprendizaje

**Talento humano** (p. 166)

- Apoyo a la gestión de planta de personal
- Mecanismos de seguimiento y evaluación de desempeño
- Desarrollo de capacidades del talento humano
- Bienestar del talento humano

**Apoyo financiero y contable** (p. 166)

- Gestión financiera y ejecución del Fondo de Servicios Educativos
- Implementación del proceso/estrategia de rendición de cuentas

### Gestión de la comunidad

**Accesibilidad** (p. 167)

- Lectura de contexto con base en los problemas socialmente relevantes
- Atención educativa a grupos poblacionales diversos y de especial atención constitucional

**Proyección a la comunidad** (p. 167)

- Relaciones con el sector productivo, autoridades municipales, cooperantes internacionales y otras instituciones
- Procesos para fortalecer la alianza e interacción entre familias y escuela
- Uso de la planta física y de los medios

**Participación y convivencia** (p. 167)

- Participación de estudiantes
- Conocimiento y apropiación de la identidad institucional por parte de la comunidad educativa
- Participación de egresados

## Alcance del borrador

Esta carga contiene el inventario de nombres y relaciones. Los descriptores específicos de cada componente para los cuatro niveles, que están en el anexo 1 (pp. 115-163), todavía no se representan como datos de la aplicación. Deben consultarse en la guía para fundamentar la elección de la institución. Tampoco se convierte en requisito obligatorio ninguno de los ejemplos del anexo 3. La precarga de descriptores y el versionado del catálogo por periodo son trabajos posteriores.
