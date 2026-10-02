---
name: pmi-frontend
description: Construir pantallas Vue/TypeScript para autoevaluación, PMI o seguimiento con los contratos y roles reales del proyecto. Usar al implementar flujos de institución o SEM.
---

# Frontend PMI

1. Lee `docs/estado-proyecto.md`, la parte pertinente del flujo de la wiki y los DTO/controladores de la rama de trabajo. Define qué pantalla es viable con los endpoints existentes.
2. Representa el ciclo por periodo e institución con estados visibles. Muestra área, proceso, componente, valoración 1-4 y evidencias cuando esa información exista; distingue borrador, revisión y resultados según el contrato confirmado.
3. Tipifica solicitudes y respuestas; maneja carga, vacío, error, autorización y confirmación de guardado. No muestres a una institución información de otra ni ofrezcas acciones reservadas a SEM.
4. Si una pantalla requiere API pendiente, registra el contrato necesario y evita presentar datos simulados como guardados. Ejecuta `npm run build` en `frontend` para verificar el cambio.
