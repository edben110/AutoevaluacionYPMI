-- Catálogo oficial de la Guía 34 actualizada en 2026.
-- Fuente: https://www.mineducacion.gov.co/1780/articles-430002_recurso_3.pdf
-- Anexo 2, pp. 164-167; nombres y relación área -> proceso -> componente.
-- 4 áreas, 15 procesos y 45 componentes. IDs fijos para conservar referencias entre entornos.
-- Los descriptores por nivel (anexo 1) y ejemplos de evidencia (anexo 3) no se cargan aquí.
-- Flyway registra esta migración y la ejecuta una sola vez; no ejecutarla manualmente.

INSERT INTO area (id, name, state) VALUES
    ('34002026-0001-4000-8000-000000000001', 'Gestión directiva y estratégica', 'ACTIVE'),
    ('34002026-0001-4000-8000-000000000002', 'Gestión académica y pedagógica', 'ACTIVE'),
    ('34002026-0001-4000-8000-000000000003', 'Gestión administrativa y financiera', 'ACTIVE'),
    ('34002026-0001-4000-8000-000000000004', 'Gestión de la comunidad', 'ACTIVE');

INSERT INTO process (id, name, state, area_id) VALUES
    ('34002026-0002-4000-8000-000000000001', 'Direccionamiento y gestión estratégica', 'ACTIVE', '34002026-0001-4000-8000-000000000001'),
    ('34002026-0002-4000-8000-000000000002', 'Gobierno escolar, instancias y otros mecanismos de participación', 'ACTIVE', '34002026-0001-4000-8000-000000000001'),
    ('34002026-0002-4000-8000-000000000003', 'Cultura institucional', 'ACTIVE', '34002026-0001-4000-8000-000000000001'),
    ('34002026-0002-4000-8000-000000000004', 'Ambiente y clima escolar', 'ACTIVE', '34002026-0001-4000-8000-000000000001'),
    ('34002026-0002-4000-8000-000000000005', 'Estrategias para el bienestar y la permanencia de estudiantes', 'ACTIVE', '34002026-0001-4000-8000-000000000001'),
    ('34002026-0002-4000-8000-000000000006', 'Diseño pedagógico', 'ACTIVE', '34002026-0001-4000-8000-000000000002'),
    ('34002026-0002-4000-8000-000000000007', 'Prácticas pedagógicas y gestión de aula', 'ACTIVE', '34002026-0001-4000-8000-000000000002'),
    ('34002026-0002-4000-8000-000000000008', 'Acompañamiento y seguimiento académico', 'ACTIVE', '34002026-0001-4000-8000-000000000002'),
    ('34002026-0002-4000-8000-000000000009', 'Apoyo al proceso de gestión de la cobertura educativa', 'ACTIVE', '34002026-0001-4000-8000-000000000003'),
    ('34002026-0002-4000-8000-000000000010', 'Administración de la planta física y de los recursos', 'ACTIVE', '34002026-0001-4000-8000-000000000003'),
    ('34002026-0002-4000-8000-000000000011', 'Talento humano', 'ACTIVE', '34002026-0001-4000-8000-000000000003'),
    ('34002026-0002-4000-8000-000000000012', 'Apoyo financiero y contable', 'ACTIVE', '34002026-0001-4000-8000-000000000003'),
    ('34002026-0002-4000-8000-000000000013', 'Accesibilidad', 'ACTIVE', '34002026-0001-4000-8000-000000000004'),
    ('34002026-0002-4000-8000-000000000014', 'Proyección a la comunidad', 'ACTIVE', '34002026-0001-4000-8000-000000000004'),
    ('34002026-0002-4000-8000-000000000015', 'Participación y convivencia', 'ACTIVE', '34002026-0001-4000-8000-000000000004');

INSERT INTO component (id, name, state, process_id) VALUES
    ('34002026-0003-4000-8000-000000000001', 'Construcción del Proyecto educativo institucional, en sus diversas denominaciones', 'ACTIVE', '34002026-0002-4000-8000-000000000001'),
    ('34002026-0003-4000-8000-000000000002', 'Misión, visión y metas institucionales', 'ACTIVE', '34002026-0002-4000-8000-000000000001'),
    ('34002026-0003-4000-8000-000000000003', 'Liderazgo y articulación institucional', 'ACTIVE', '34002026-0002-4000-8000-000000000001'),
    ('34002026-0003-4000-8000-000000000004', 'Gestión integral de riesgos socionaturales, naturales, antrópicos no intencionados y derivados del conflicto armado', 'ACTIVE', '34002026-0002-4000-8000-000000000001'),
    ('34002026-0003-4000-8000-000000000005', 'Consejo directivo', 'ACTIVE', '34002026-0002-4000-8000-000000000002'),
    ('34002026-0003-4000-8000-000000000006', 'Consejo académico', 'ACTIVE', '34002026-0002-4000-8000-000000000002'),
    ('34002026-0003-4000-8000-000000000007', 'Participación de estudiantes en el establecimiento educativo (Consejo de Estudiantes, personería y contraloría estudiantil, representantes al Consejo Directivo)', 'ACTIVE', '34002026-0002-4000-8000-000000000002'),
    ('34002026-0003-4000-8000-000000000008', 'Instancias de participación de las familias en la dirección del establecimiento educativo', 'ACTIVE', '34002026-0002-4000-8000-000000000002'),
    ('34002026-0003-4000-8000-000000000009', 'Comunicación y trabajo en equipo', 'ACTIVE', '34002026-0002-4000-8000-000000000003'),
    ('34002026-0003-4000-8000-000000000010', 'Identificación de buenas prácticas, experiencias significativas y de fomento CTEI lideradas por docentes', 'ACTIVE', '34002026-0002-4000-8000-000000000003'),
    ('34002026-0003-4000-8000-000000000011', 'Inducción a nuevos estudiantes', 'ACTIVE', '34002026-0002-4000-8000-000000000004'),
    ('34002026-0003-4000-8000-000000000012', 'Manual de Convivencia', 'ACTIVE', '34002026-0002-4000-8000-000000000004'),
    ('34002026-0003-4000-8000-000000000013', 'Comité Escolar de Convivencia', 'ACTIVE', '34002026-0002-4000-8000-000000000004'),
    ('34002026-0003-4000-8000-000000000014', 'Motivación hacia el aprendizaje, desarrollo del sentido de pertenencia y participación de estudiantes', 'ACTIVE', '34002026-0002-4000-8000-000000000004'),
    ('34002026-0003-4000-8000-000000000015', 'Gestión de la jornada escolar para la formación integral', 'ACTIVE', '34002026-0002-4000-8000-000000000005'),
    ('34002026-0003-4000-8000-000000000016', 'Servicios complementarios para el bienestar y la permanencia de los estudiantes', 'ACTIVE', '34002026-0002-4000-8000-000000000005'),
    ('34002026-0003-4000-8000-000000000017', 'Enfoque Pedagógico y propósitos formativos', 'ACTIVE', '34002026-0002-4000-8000-000000000006'),
    ('34002026-0003-4000-8000-000000000018', 'Plan de Estudios', 'ACTIVE', '34002026-0002-4000-8000-000000000006'),
    ('34002026-0003-4000-8000-000000000019', 'Tiempo para el aprendizaje', 'ACTIVE', '34002026-0002-4000-8000-000000000006'),
    ('34002026-0003-4000-8000-000000000020', 'Planeación para el aprendizaje', 'ACTIVE', '34002026-0002-4000-8000-000000000006'),
    ('34002026-0003-4000-8000-000000000021', 'Evaluación para y del aprendizaje', 'ACTIVE', '34002026-0002-4000-8000-000000000006'),
    ('34002026-0003-4000-8000-000000000022', 'Estrategias pedagógicas para el aprendizaje', 'ACTIVE', '34002026-0002-4000-8000-000000000007'),
    ('34002026-0003-4000-8000-000000000023', 'Mediación pedagógica con recursos para el aprendizaje y la enseñanza', 'ACTIVE', '34002026-0002-4000-8000-000000000007'),
    ('34002026-0003-4000-8000-000000000024', 'Gestión pedagógica del aula', 'ACTIVE', '34002026-0002-4000-8000-000000000007'),
    ('34002026-0003-4000-8000-000000000025', 'Uso pedagógico de la información para la formación integral', 'ACTIVE', '34002026-0002-4000-8000-000000000008'),
    ('34002026-0003-4000-8000-000000000026', 'Apoyo pedagógico a todas las personas sin excepción', 'ACTIVE', '34002026-0002-4000-8000-000000000008'),
    ('34002026-0003-4000-8000-000000000027', 'Seguimiento a los aprendizajes y trayectorias educativas', 'ACTIVE', '34002026-0002-4000-8000-000000000008'),
    ('34002026-0003-4000-8000-000000000028', 'Promoción, matrícula y novedades', 'ACTIVE', '34002026-0002-4000-8000-000000000009'),
    ('34002026-0003-4000-8000-000000000029', 'Gestión documental y archivo académico', 'ACTIVE', '34002026-0002-4000-8000-000000000009'),
    ('34002026-0003-4000-8000-000000000030', 'Seguimiento al plan de mantenimiento de la planta física y al uso de los espacios', 'ACTIVE', '34002026-0002-4000-8000-000000000010'),
    ('34002026-0003-4000-8000-000000000031', 'Administración y aprovechamiento de equipos y recursos para el aprendizaje', 'ACTIVE', '34002026-0002-4000-8000-000000000010'),
    ('34002026-0003-4000-8000-000000000032', 'Apoyo a la gestión de planta de personal', 'ACTIVE', '34002026-0002-4000-8000-000000000011'),
    ('34002026-0003-4000-8000-000000000033', 'Mecanismos de seguimiento y evaluación de desempeño', 'ACTIVE', '34002026-0002-4000-8000-000000000011'),
    ('34002026-0003-4000-8000-000000000034', 'Desarrollo de capacidades del talento humano', 'ACTIVE', '34002026-0002-4000-8000-000000000011'),
    ('34002026-0003-4000-8000-000000000035', 'Bienestar del talento humano', 'ACTIVE', '34002026-0002-4000-8000-000000000011'),
    ('34002026-0003-4000-8000-000000000036', 'Gestión financiera y ejecución del Fondo de Servicios Educativos', 'ACTIVE', '34002026-0002-4000-8000-000000000012'),
    ('34002026-0003-4000-8000-000000000037', 'Implementación del proceso/estrategia de rendición de cuentas', 'ACTIVE', '34002026-0002-4000-8000-000000000012'),
    ('34002026-0003-4000-8000-000000000038', 'Lectura de contexto con base en los problemas socialmente relevantes', 'ACTIVE', '34002026-0002-4000-8000-000000000013'),
    ('34002026-0003-4000-8000-000000000039', 'Atención educativa a grupos poblacionales diversos y de especial atención constitucional', 'ACTIVE', '34002026-0002-4000-8000-000000000013'),
    ('34002026-0003-4000-8000-000000000040', 'Relaciones con el sector productivo, autoridades municipales, cooperantes internacionales y otras instituciones', 'ACTIVE', '34002026-0002-4000-8000-000000000014'),
    ('34002026-0003-4000-8000-000000000041', 'Procesos para fortalecer la alianza e interacción entre familias y escuela', 'ACTIVE', '34002026-0002-4000-8000-000000000014'),
    ('34002026-0003-4000-8000-000000000042', 'Uso de la planta física y de los medios', 'ACTIVE', '34002026-0002-4000-8000-000000000014'),
    ('34002026-0003-4000-8000-000000000043', 'Participación de estudiantes', 'ACTIVE', '34002026-0002-4000-8000-000000000015'),
    ('34002026-0003-4000-8000-000000000044', 'Conocimiento y apropiación de la identidad institucional por parte de la comunidad educativa', 'ACTIVE', '34002026-0002-4000-8000-000000000015'),
    ('34002026-0003-4000-8000-000000000045', 'Participación de egresados', 'ACTIVE', '34002026-0002-4000-8000-000000000015');

