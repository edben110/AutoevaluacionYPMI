---
name: pmi-backend
description: Implementar una funcionalidad del backend Spring Boot de AutoevaluacionYPMI con persistencia, contratos y permisos coherentes. Usar al cambiar API, entidades, migraciones o servicios del PMI.
---

# Backend PMI

1. Revisa `git status`, rama activa, `docs/estado-proyecto.md` y la historia de la funcionalidad. Comprueba si depende de la rama `anton`, que contiene la autenticación y los catálogos iniciales.
2. Parte de una regla trazada a la guía/wiki. Modela las relaciones institucionales y anuales en entidad, DTO, servicio y API sin exponer entidades JPA directamente como respuestas.
3. Crea la migración Flyway que corresponda a cada cambio de esquema y comprueba su compatibilidad con `ddl-auto=validate`. Mantén los datos existentes y evita editar migraciones aplicadas sin plan de transición.
4. Aplica autorización de SEM/institución y comprueba pertenencia de los recursos a la institución en cada operación. Usa las variables de entorno previstas para BD y JWT.
5. Verifica los casos de éxito y rechazo que realmente puedan fallar por la nueva lógica. Ejecuta `backend/gradlew.bat test` cuando los requisitos del entorno estén disponibles; documenta un bloqueo concreto si faltan.
