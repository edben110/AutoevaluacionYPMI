<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, RouterLink, useRoute, useRouter } from 'vue-router'
import { authorizedFetch, hasSession, loadProfile, responseError } from '@/auth/session'
import { formatPeriodDate } from '@/selfEvaluation/period'
import type { DecimalAmount, FormulationComponent, FormulationGoal, FormulationObjective, FormulationObjectiveRequest, FormulationResponse, FormulationRow } from '@/pmi/formulation'
import type { PriorityMatrix } from '@/pmi/types'

interface RowForm {
  key: string; id: string | null; indicatorName: string; activities: string
  totalCost: string; fseAmount: string; otherAmount: string; responsible: string
  startDate: string; finishDate: string; collectionInstruments: string; collectionFrequency: string
}
interface GoalForm { key: string; id: string | null; description: string; rows: RowForm[] }
interface ObjectiveForm {
  key: string; id: string | null; componentId: string; description: string; goals: GoalForm[]
  snapshot: string; error: string; success: string
}
const route = useRoute()
const router = useRouter()
const year = computed(() => Number(route.params.year))
const areaId = computed(() => String(route.params.areaId ?? ''))
const apiBase = computed(() => `/api/pmi/${year.value}/areas/${encodeURIComponent(areaId.value)}/formulation`)
const authorized = ref(false)
const loading = ref(true)
const savingKey = ref<string | null>(null)
const deletingKey = ref<string | null>(null)
const error = ref('')
const success = ref('')
const formulation = ref<FormulationResponse | null>(null)
const priority = ref<PriorityMatrix | null>(null)
const componentChoices = ref<string[]>([])
const generating = ref(false)
const selectionRefreshRequired = ref(false)
const selectionError = ref('')
const selectionSuccess = ref('')
const contexts = reactive<Record<string, Omit<FormulationComponent, 'eligible'>>>({})
const objectives = reactive<ObjectiveForm[]>([])
const selectedComponentId = ref('')
const busy = computed(() => loading.value || generating.value || savingKey.value !== null || deletingKey.value !== null)
const components = computed(() => formulation.value?.selectedComponents.filter(item => item.eligible) ?? [])
const highestComponents = computed(() => priority.value?.components.filter(component => component.eligible
  && component.total !== null && priority.value?.highestComponentIds.includes(component.componentId)) ?? [])
const requiredChoices = computed(() => Math.min(2, highestComponents.value.length))
const selectionReady = computed(() => priority.value?.selfEvaluationComplete === true && priority.value.selfEvaluationSubmitted
  && priority.value.prioritizationComplete && priority.value.status !== 'SUBMITTED' && highestComponents.value.length > 0)
const choiceChanged = computed(() => selectionReady.value && priority.value !== null && !sameIds(componentChoices.value, priority.value.selectedComponentIds))
const hasObjectiveChanges = computed(() => objectives.some(changed))
const canCreate = computed(() => formulation.value?.canFormulate === true && !selectionRefreshRequired.value && !choiceChanged.value && components.value.length > 0)
let loadSequence = 0

function rowForm(row?: FormulationRow): RowForm {
  return {
    key: row?.id ?? crypto.randomUUID(), id: row?.id ?? null, indicatorName: row?.indicatorName ?? '',
    activities: row?.activities ?? '', totalCost: row?.totalCost == null ? '' : String(row.totalCost),
    fseAmount: row?.fseAmount == null ? '' : String(row.fseAmount), otherAmount: row?.otherAmount == null ? '' : String(row.otherAmount),
    responsible: row?.responsible ?? '', startDate: row?.startDate ?? '', finishDate: row?.finishDate ?? '',
    collectionInstruments: row?.collectionInstruments ?? '', collectionFrequency: row?.collectionFrequency ?? '',
  }
}
function goalForm(goal?: FormulationGoal): GoalForm {
  return { key: goal?.id ?? crypto.randomUUID(), id: goal?.id ?? null, description: goal?.description ?? '', rows: goal?.rows.map(rowForm) ?? [] }
}
function snapshot(form: ObjectiveForm): string {
  return JSON.stringify({ componentId: form.componentId, description: form.description, goals: form.goals })
}
function objectiveForm(objective: FormulationObjective): ObjectiveForm {
  const form: ObjectiveForm = {
    key: objective.id, id: objective.id, componentId: objective.componentId, description: objective.description ?? '',
    goals: objective.goals.map(goalForm), snapshot: '', error: '', success: '',
  }
  form.snapshot = snapshot(form)
  return form
}
function changed(form: ObjectiveForm): boolean { return form.snapshot !== snapshot(form) }
const hasUnsavedChanges = computed(() => hasObjectiveChanges.value || choiceChanged.value)
function confirmDiscard(): boolean {
  return !hasSession.value || !hasUnsavedChanges.value
    || window.confirm('Hay cambios sin guardar o una selección pendiente en el Anexo 3. ¿Salir y descartarlos?')
}
function beforeUnload(event: BeforeUnloadEvent): void {
  if (!hasSession.value || !hasUnsavedChanges.value) return
  event.preventDefault()
  event.returnValue = ''
}
function context(form: ObjectiveForm): Omit<FormulationComponent, 'eligible'> | undefined {
  return formulation.value?.objectives.find(item => item.id === form.id)
    ?? formulation.value?.selectedComponents.find(item => item.componentId === form.componentId)
    ?? contexts[form.componentId]
    ?? priority.value?.components.find(item => item.componentId === form.componentId)
}
function editable(form: ObjectiveForm): boolean {
  if (!formulation.value?.canFormulate || selectionRefreshRequired.value || choiceChanged.value) return false
  if (!form.id) return components.value.some(item => item.componentId === form.componentId)
  return formulation.value?.objectives.some(item => item.id === form.id && item.editable) ?? false
}

function mergeResponse(response: FormulationResponse, savedKey?: string, savedId?: string): void {
  for (const item of [...response.selectedComponents, ...response.objectives]) contexts[item.componentId] = item
  for (const objective of response.objectives) {
    const existing = objectives.findIndex(item => item.id === objective.id || (item.key === savedKey && objective.id === savedId))
    if (existing < 0) objectives.push(objectiveForm(objective))
    else if (objectives[existing]?.key === savedKey) objectives.splice(existing, 1, objectiveForm(objective))
  }
  formulation.value = response
  if (!components.value.some(item => item.componentId === selectedComponentId.value)) selectedComponentId.value = components.value[0]?.componentId ?? ''
}

function sameIds(first: string[], second: string[]): boolean {
  const orderedSecond = [...second].sort()
  return first.length === second.length && [...first].sort().every((id, index) => id === orderedSecond[index])
}
function setChoices(): void {
  if (highestComponents.value.length <= 2) componentChoices.value = highestComponents.value.map(item => item.componentId)
  else componentChoices.value = (priority.value?.selectedComponentIds ?? [])
    .filter(id => highestComponents.value.some(item => item.componentId === id)).slice(0, 2)
}
function toggleChoice(componentId: string, event: Event): void {
  const checkbox = event.target as HTMLInputElement
  if (busy.value || hasObjectiveChanges.value) {
    checkbox.checked = componentChoices.value.includes(componentId)
    selectionError.value = 'Guarda o descarta los cambios pendientes de los formularios antes de cambiar los componentes.'
    return
  }
  if (checkbox.checked) {
    if (componentChoices.value.length >= requiredChoices.value) { checkbox.checked = false; return }
    if (!componentChoices.value.includes(componentId)) componentChoices.value = [...componentChoices.value, componentId]
  } else componentChoices.value = componentChoices.value.filter(id => id !== componentId)
  selectionError.value = ''; selectionSuccess.value = ''
}
function createObjective(componentId: string): ObjectiveForm {
  const goal = goalForm()
  goal.rows.push(rowForm())
  return { key: crypto.randomUUID(), id: null, componentId, description: '', goals: [goal], snapshot: '', error: '', success: '' }
}
function prepareFields(form: ObjectiveForm): void {
  if (form.goals.length === 0) form.goals.push(goalForm())
  for (const goal of form.goals) if (goal.rows.length === 0) goal.rows.push(rowForm())
}
async function generateForms(): Promise<void> {
  if (busy.value || !selectionReady.value) return
  selectionError.value = ''; selectionSuccess.value = ''
  if (componentChoices.value.length !== requiredChoices.value
      || !componentChoices.value.every(id => highestComponents.value.some(item => item.componentId === id))) {
    selectionError.value = `Selecciona ${requiredChoices.value === 1 ? 'el componente' : 'exactamente dos componentes'} con el mayor puntaje.`
    return
  }
  if (choiceChanged.value && hasObjectiveChanges.value) {
    selectionError.value = 'Guarda o descarta los cambios pendientes de los formularios antes de confirmar otra selección.'
    return
  }
  const sequence = loadSequence
  const path = apiBase.value
  const ids = [...componentChoices.value]
  generating.value = true
  selectionRefreshRequired.value = true
  try {
    if (choiceChanged.value) {
      const selected = await authorizedFetch(`${path.slice(0, -'/formulation'.length)}/selection`, {
        method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ componentIds: ids }),
      })
      if (!selected.ok) throw await responseError(selected)
      const updated = await selected.json() as PriorityMatrix
      if (sequence !== loadSequence) return
      priority.value = updated
    }
    const response = await authorizedFetch(path)
    if (!response.ok) throw await responseError(response)
    const updated = await response.json() as FormulationResponse
    if (sequence !== loadSequence) return
    mergeResponse(updated)
    if (!updated.canFormulate || !sameIds(ids, updated.selectedComponents.filter(item => item.eligible).map(item => item.componentId))) {
      throw new Error('La selección necesita revisión. Los formularios anteriores se conservan para consulta; revisa la priorización antes de continuar.')
    }
    selectionRefreshRequired.value = false
    for (const id of ids) {
      const existing = objectives.filter(form => form.componentId === id)
      if (existing.length === 0) objectives.push(createObjective(id))
      else for (const form of existing) prepareFields(form)
    }
    selectionSuccess.value = 'Formularios preparados. Completa el plan y usa Guardar este plan para conservar sus respuestas.'
  } catch (cause) {
    if (sequence === loadSequence) selectionError.value = cause instanceof Error ? cause.message : 'No se pudieron preparar los formularios. Reintenta; tus respuestas se conservan.'
    if (!hasSession.value) await router.replace({ name: 'login' })
  } finally { if (sequence === loadSequence) generating.value = false }
}

async function loadFormulation(): Promise<void> {
  const sequence = ++loadSequence
  loading.value = true
  error.value = ''
  success.value = ''
  formulation.value = null
  priority.value = null
  componentChoices.value = []
  generating.value = false
  selectionRefreshRequired.value = false
  selectionError.value = ''; selectionSuccess.value = ''
  for (const id of Object.keys(contexts)) delete contexts[id]
  objectives.splice(0)
  savingKey.value = null
  deletingKey.value = null
  try {
    if (!Number.isInteger(year.value) || year.value < 1 || year.value > 9999) throw new Error('El año de la autoevaluación no es válido.')
    if (!areaId.value) throw new Error('Selecciona el área del PMI.')
    const path = apiBase.value
    const [response, priorityResponse] = await Promise.all([authorizedFetch(path), authorizedFetch(path.slice(0, -'/formulation'.length))])
    if (!response.ok) throw await responseError(response)
    if (!priorityResponse.ok) throw await responseError(priorityResponse)
    const [loaded, loadedPriority] = await Promise.all([response.json() as Promise<FormulationResponse>, priorityResponse.json() as Promise<PriorityMatrix>])
    if (sequence !== loadSequence) return
    objectives.splice(0)
    mergeResponse(loaded)
    priority.value = loadedPriority
    setChoices()
  } catch (cause) {
    if (sequence === loadSequence) error.value = cause instanceof Error ? cause.message : 'No se pudo cargar el Anexo 3.'
    if (!hasSession.value) await router.replace({ name: 'login' })
  } finally { if (sequence === loadSequence) loading.value = false }
}

function addObjective(): void {
  if (busy.value || !canCreate.value || !components.value.some(item => item.componentId === selectedComponentId.value)) return
  objectives.push(createObjective(selectedComponentId.value))
  success.value = ''
}
function addGoal(form: ObjectiveForm): void {
  if (!busy.value && editable(form)) { const goal = goalForm(); goal.rows.push(rowForm()); form.goals.push(goal); form.success = '' }
}
function addRow(form: ObjectiveForm, goal: GoalForm): void {
  if (!busy.value && editable(form)) { goal.rows.push(rowForm()); form.success = '' }
}
function removeGoal(form: ObjectiveForm, index: number): void {
  const goal = form.goals[index]
  if (busy.value || !editable(form) || !goal) return
  if (goal.id && !window.confirm('¿Quitar esta meta, sus filas y las fichas de sus indicadores del Anexo 4? La eliminación se aplicará al guardar el objetivo.')) return
  form.goals.splice(index, 1)
  form.success = ''
}
function removeRow(form: ObjectiveForm, goal: GoalForm, index: number): void {
  const row = goal.rows[index]
  if (busy.value || !editable(form) || !row) return
  if (row.id && !window.confirm('¿Quitar esta fila y la ficha de su indicador del Anexo 4? La eliminación se aplicará al guardar el objetivo.')) return
  goal.rows.splice(index, 1)
  form.success = ''
}
const nullable = (value: string): string | null => value.trim() || null
function validAmount(value: string): boolean { return !value.trim() || /^\d{1,14}(?:\.\d{1,2})?$/.test(value.trim()) }
function requestFor(form: ObjectiveForm): FormulationObjectiveRequest {
  if (form.description.length > 10000) throw new Error('El objetivo admite hasta 10.000 caracteres.')
  return {
    componentId: form.componentId, description: nullable(form.description),
    goals: form.goals.map((goal, goalIndex) => {
      if (goal.description.length > 10000) throw new Error(`La meta ${goalIndex + 1} admite hasta 10.000 caracteres.`)
      return { id: goal.id, description: nullable(goal.description), rows: goal.rows.map((row, rowIndex) => {
        const prefix = `Meta ${goalIndex + 1}, fila ${rowIndex + 1}`
        if (![row.totalCost, row.fseAmount, row.otherAmount].every(validAmount)) throw new Error(`${prefix}: usa importes positivos o cero, hasta 14 cifras enteras y 2 decimales, separados con punto.`)
        if (row.startDate && row.finishDate && row.finishDate < row.startDate) throw new Error(`${prefix}: la fecha de fin debe ser igual o posterior a la fecha de inicio.`)
        if ([row.indicatorName, row.responsible, row.collectionFrequency].some(value => value.length > 500)) throw new Error(`${prefix}: indicador, responsable y frecuencia admiten hasta 500 caracteres.`)
        if ([row.activities, row.collectionInstruments].some(value => value.length > 10000)) throw new Error(`${prefix}: actividades e instrumentos admiten hasta 10.000 caracteres.`)
        return {
          id: row.id, indicatorName: nullable(row.indicatorName), activities: nullable(row.activities),
          totalCost: nullable(row.totalCost), fseAmount: nullable(row.fseAmount), otherAmount: nullable(row.otherAmount),
          responsible: nullable(row.responsible), startDate: nullable(row.startDate), finishDate: nullable(row.finishDate),
          collectionInstruments: nullable(row.collectionInstruments), collectionFrequency: nullable(row.collectionFrequency),
        }
      }) }
    }),
  }
}

async function saveObjective(form: ObjectiveForm): Promise<void> {
  if (busy.value || !editable(form)) return
  form.error = ''; form.success = ''; success.value = ''
  let body: FormulationObjectiveRequest
  try { body = requestFor(form) } catch (cause) { form.error = cause instanceof Error ? cause.message : 'Revisa los datos del objetivo.'; return }
  savingKey.value = form.key
  const sequence = loadSequence
  const previousIds = new Set(formulation.value?.objectives.map(item => item.id) ?? [])
  try {
    const response = await authorizedFetch(`${apiBase.value}/objectives${form.id ? '/' + form.id : ''}`, {
      method: form.id ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body),
    })
    if (!response.ok) throw await responseError(response)
    const saved = await response.json() as FormulationResponse
    if (sequence !== loadSequence) return
    const savedId = form.id ?? saved.objectives.find(item => !previousIds.has(item.id))?.id
    mergeResponse(saved, form.key, savedId)
    const updated = objectives.find(item => item.id === savedId)
    if (updated) updated.success = 'Objetivo, metas y filas guardados.'
  } catch (cause) {
    if (sequence === loadSequence) form.error = cause instanceof Error ? cause.message : 'No se pudo guardar el objetivo.'
    if (!hasSession.value) await router.replace({ name: 'login' })
  } finally { if (sequence === loadSequence) savingKey.value = null }
}

async function deleteObjective(form: ObjectiveForm): Promise<void> {
  if (busy.value || !window.confirm(form.id ? '¿Eliminar este objetivo, sus metas, filas y fichas de indicadores del Anexo 4? Esta acción no se puede deshacer.' : '¿Descartar este objetivo sin guardar?')) return
  if (!form.id) { objectives.splice(objectives.indexOf(form), 1); return }
  deletingKey.value = form.key
  const sequence = loadSequence
  const path = apiBase.value
  form.error = ''; success.value = ''; error.value = ''
  try {
    const response = await authorizedFetch(`${path}/objectives/${form.id}`, { method: 'DELETE' })
    if (!response.ok) throw await responseError(response)
    if (sequence !== loadSequence) return
    objectives.splice(objectives.indexOf(form), 1)
    success.value = 'Objetivo eliminado.'
    const refreshed = await authorizedFetch(path)
    if (!refreshed.ok) throw await responseError(refreshed)
    const saved = await refreshed.json() as FormulationResponse
    if (sequence === loadSequence) mergeResponse(saved)
  } catch (cause) {
    if (sequence === loadSequence) error.value = cause instanceof Error ? cause.message : 'No se pudo completar la eliminación.'
    if (!hasSession.value) await router.replace({ name: 'login' })
  } finally { if (sequence === loadSequence) deletingKey.value = null }
}
function money(value: DecimalAmount): string {
  const [integer = '0', fraction = ''] = String(value).split('.')
  return `${integer.replace(/\B(?=(\d{3})+(?!\d))/g, '.')},${fraction.padEnd(2, '0')}`
}
onMounted(async () => {
  window.addEventListener('beforeunload', beforeUnload)
  try {
    const profile = await loadProfile()
    if (profile.role !== 'ESTABLISHMENT') { await router.replace({ name: 'dashboard' }); return }
    authorized.value = true
    await loadFormulation()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'No se pudo abrir la formulación.'
    loading.value = false
    if (!hasSession.value) await router.replace({ name: 'login' })
  }
})
watch(() => [route.params.year, route.params.areaId], () => { if (authorized.value) void loadFormulation() })
onBeforeRouteLeave(confirmDiscard)
onBeforeRouteUpdate((to, from) => (to.params.year === from.params.year && to.params.areaId === from.params.areaId) || confirmDiscard())
onUnmounted(() => { loadSequence++; window.removeEventListener('beforeunload', beforeUnload) })
</script>

<template>
  <main class="formulation-page">
    <header class="formulation-header"><RouterLink :to="{ name: 'pmi', params: { year: route.params.year, areaId } }">← Volver a priorización · Anexo 2</RouterLink><RouterLink :to="{ name: 'dashboard' }">Mi espacio</RouterLink></header>
    <div class="formulation-content">
      <span class="formulation-eyebrow">PLAN DE MEJORAMIENTO INSTITUCIONAL · ANEXO 3</span>
      <h1>Preparemos el plan de mejoramiento</h1>
      <h2 v-if="formulation" class="formulation-area-name">{{ formulation.areaName }}</h2>
      <p class="intro">Elige los componentes y prepara sus formularios. Después responde las preguntas de cada plan y guarda tus avances.</p>
      <p v-if="loading" class="formulation-card" role="status">Cargando la formulación…</p>
      <div v-if="error" class="message error-message" role="alert"><p>{{ error }}</p><button v-if="!formulation" class="formulation-button outline" type="button" @click="loadFormulation">Reintentar</button></div>
      <p v-if="success" class="message" role="status">{{ success }}</p>
      <template v-if="authorized && formulation && !loading">
        <p class="metadata" aria-label="Datos del PMI"><strong>{{ formulation.establishmentName }}</strong><span>Autoevaluación {{ formulation.year }} · Proyección: {{ formulation.projectionYears }} año</span><span v-if="formulation.filledOn">Diligenciamiento: {{ formatPeriodDate(formulation.filledOn) }}</span></p>
        <section id="formulation-step-components" class="formulation-card component-step" aria-labelledby="component-step-title">
          <span class="step-label">PASO 1</span>
          <h2 id="component-step-title">Elige los componentes que vas a trabajar</h2>
          <p v-if="formulation.pmiSubmittedAt" class="message" role="status">PMI enviado a Secretaría. Los anexos 2, 3 y 4 quedan para consulta.</p>
          <template v-else-if="selectionReady">
            <p v-if="requiredChoices === 1">Hay un único componente con el mayor puntaje. Su formulario se preparará al continuar.</p>
            <p v-else>Elige dos componentes entre los que tienen el mayor puntaje de urgencia, tendencia e impacto.</p>
            <fieldset class="component-choice-grid" :disabled="busy">
              <label v-for="component in highestComponents" :key="component.componentId" class="formulation-component-choice" :data-component-id="component.componentId" :class="{ 'component-is-chosen': componentChoices.includes(component.componentId) }">
                <input type="checkbox" :checked="componentChoices.includes(component.componentId)" :value="component.componentId" :disabled="componentChoices.length >= requiredChoices && !componentChoices.includes(component.componentId)" @change="toggleChoice(component.componentId, $event)" />
                <span><strong>{{ component.componentName }}</strong><span>{{ component.processName }}</span><span>U {{ component.urgency }} + T {{ component.tendency }} + I {{ component.impact }} = <b>{{ component.total }} puntos</b></span></span>
              </label>
            </fieldset>
            <p class="choice-progress">Seleccionados: {{ componentChoices.length }} de {{ requiredChoices }}.</p>
            <p v-if="hasObjectiveChanges" class="selection-hint">Para cambiar los componentes, primero guarda o descarta las respuestas pendientes de los formularios.</p>
            <button id="generate-pmi-forms" class="formulation-button" type="button" :disabled="busy || componentChoices.length !== requiredChoices" @click="generateForms">{{ generating ? 'Preparando formularios…' : 'Generar formularios' }}</button>
            <p class="selection-hint">Se prepara un objetivo, una meta y un indicador por componente. Tus respuestas se conservan al volver a generar. Guarda cada plan para conservar sus respuestas al salir.</p>
          </template>
          <template v-else>
            <p>Completa y envía la autoevaluación del área y guarda los puntajes de todos sus candidatos en el Anexo 2. Si todos los componentes están en Mejoramiento continuo, no hay candidatos para formular un PMI.</p>
            <RouterLink :to="{ name: 'pmi', params: { year, areaId } }">Revisar priorización del área →</RouterLink>
          </template>
          <p v-if="selectionError" class="message error-message" role="alert">{{ selectionError }}</p>
          <p v-if="selectionSuccess" class="message" role="status">{{ selectionSuccess }}</p>
          <p v-if="selectionRefreshRequired && !generating" class="message warning" role="status">La edición está en pausa hasta verificar la selección con el servidor. Reintenta con Generar formularios; los datos que ya escribiste se conservan.</p>
        </section>
        <section id="formulation-step-plan" class="plan-step" aria-labelledby="plan-step-title">
          <span class="step-label">PASO 2</span><h2 id="plan-step-title">Completa el plan de cada componente</h2>
          <p>Completa los campos y pulsa Guardar este plan para conservar tus avances. Puedes guardar con campos pendientes.</p>
          <p v-if="objectives.length === 0" class="empty-plan">Cuando presiones Generar formularios, aquí aparecerán todos los espacios para completar tu plan.</p>
        </section>
        <section v-for="(objective, objectiveIndex) in objectives" :key="objective.key" class="formulation-card objective-card" :data-component-id="objective.componentId" :aria-labelledby="`objective-title-${objective.key}`">
          <header class="objective-header"><div><span class="formulation-eyebrow">{{ context(objective)?.areaName }}</span><h2 :id="`objective-title-${objective.key}`">Objetivo {{ objectiveIndex + 1 }} · {{ context(objective)?.componentName }}</h2><p>Proceso: {{ context(objective)?.processName }}</p></div><span class="draft-label">{{ objective.id ? 'Guardado' : 'Nuevo borrador' }}</span></header>
          <p v-if="!editable(objective)" class="message warning">{{ formulation.pmiSubmittedAt ? 'PMI enviado a Secretaría. Este plan está disponible para consulta.' : 'Solo consulta: genera los formularios para confirmar la selección actual. Los planes de componentes que ya no están seleccionados se conservan y puedes consultarlos o eliminarlos.' }}</p>
          <p v-if="changed(objective)" class="unsaved-label">Cambios sin guardar</p>
          <form @submit.prevent="saveObjective(objective)">
            <fieldset class="objective-fields" :disabled="busy || !editable(objective)">
              <label :for="`objective-${objective.key}`">¿Qué queremos mejorar? · Objetivo<textarea :id="`objective-${objective.key}`" v-model="objective.description" rows="3" maxlength="10000" placeholder="Describe el cambio que buscan lograr en este componente." /></label>
              <section v-for="(goal, goalIndex) in objective.goals" :key="goal.key" class="goal-card" :aria-labelledby="`goal-title-${goal.key}`">
                <div class="subsection-header"><h3 :id="`goal-title-${goal.key}`">Meta {{ goalIndex + 1 }}</h3><button class="small-button delete-button" type="button" @click="removeGoal(objective, goalIndex)">Quitar meta</button></div>
                <label :for="`goal-${goal.key}`">¿Qué resultado esperamos alcanzar? · Meta<textarea :id="`goal-${goal.key}`" v-model="goal.description" rows="3" maxlength="10000" placeholder="Explica el resultado esperado y cómo reconocerán que se alcanzó." /></label>
                <article v-for="(row, rowIndex) in goal.rows" :key="row.key" class="indicator-card">
                  <div class="subsection-header"><h4>Indicador {{ rowIndex + 1 }} y actividades</h4><button class="small-button delete-button" type="button" @click="removeRow(objective, goal, rowIndex)">Quitar indicador y actividades</button></div>
                  <div class="fields-grid">
                    <label class="full-width" :for="`indicator-${row.key}`">¿Cómo sabremos que avanzamos? · Nombre del indicador<input :id="`indicator-${row.key}`" v-model="row.indicatorName" type="text" maxlength="500" placeholder="Nombra aquello que van a observar para reconocer los avances." /></label>
                    <label class="full-width" :for="`activities-${row.key}`">¿Qué haremos? · Actividades<textarea :id="`activities-${row.key}`" v-model="row.activities" rows="4" maxlength="10000" placeholder="Describe las actividades que realizarán para alcanzar esta meta." /></label>
                  </div>
                  <h4 class="question-heading">¿Quién lo hará y cuándo?</h4>
                  <div class="fields-grid">
                    <label class="full-width" :for="`responsible-${row.key}`">Responsable<input :id="`responsible-${row.key}`" v-model="row.responsible" type="text" maxlength="500" placeholder="Persona o equipo encargado de las actividades." /></label>
                    <label :for="`start-${row.key}`">Fecha de inicio<input :id="`start-${row.key}`" v-model="row.startDate" type="date" /></label><label :for="`finish-${row.key}`">Fecha de fin<input :id="`finish-${row.key}`" v-model="row.finishDate" type="date" :min="row.startDate || undefined" /></label>
                  </div>
                  <h4 class="question-heading">¿Qué recursos necesitamos?</h4>
                  <div class="amount-fields"><label :for="`cost-${row.key}`">Costo total (COP)<input :id="`cost-${row.key}`" v-model="row.totalCost" type="text" inputmode="decimal" placeholder="Pendiente" /><small>Usa punto para decimales, por ejemplo 1250.50.</small></label><label :for="`fse-${row.key}`">FSE (COP)<input :id="`fse-${row.key}`" v-model="row.fseAmount" type="text" inputmode="decimal" placeholder="Pendiente" /></label><label :for="`other-${row.key}`">Otra fuente (COP)<input :id="`other-${row.key}`" v-model="row.otherAmount" type="text" inputmode="decimal" placeholder="Pendiente" /></label></div>
                  <h4 class="question-heading">¿Cómo recogeremos información para el seguimiento?</h4>
                  <div class="fields-grid">
                    <label class="full-width" :for="`instruments-${row.key}`">Instrumentos para la recolección de información<textarea :id="`instruments-${row.key}`" v-model="row.collectionInstruments" rows="3" maxlength="10000" /></label>
                    <label class="full-width" :for="`frequency-${row.key}`">Frecuencia de recolección<input :id="`frequency-${row.key}`" v-model="row.collectionFrequency" type="text" maxlength="500" placeholder="Por ejemplo, mensual." /></label>
                  </div>
                </article>
                <button class="formulation-button outline" type="button" @click="addRow(objective, goal)">Agregar fila de indicador</button>
              </section>
              <div class="objective-actions"><button class="formulation-button outline" type="button" @click="addGoal(objective)">Agregar otra meta</button><button class="formulation-button plan-save-button" type="submit">{{ savingKey === objective.key ? 'Guardando…' : 'Guardar este plan' }}</button></div>
            </fieldset>
          </form>
          <p v-if="objective.error" class="message error-message" role="alert">{{ objective.error }}</p><p v-if="objective.success" class="message" role="status">{{ objective.success }}</p>
          <button class="small-button delete-button objective-delete" type="button" :disabled="busy || Boolean(formulation.pmiSubmittedAt)" @click="deleteObjective(objective)">{{ deletingKey === objective.key ? 'Eliminando…' : objective.id ? 'Eliminar objetivo' : 'Descartar objetivo sin guardar' }}</button>
        </section>
        <section class="formulation-card" aria-labelledby="indicators-next-title">
          <h2 id="indicators-next-title">Continuar con los indicadores · Anexo 4</h2>
          <p>Guarda el plan con los nombres de sus indicadores. En el Anexo 4 encontrarás una tabla por cada indicador para definir cómo lo medirán.</p>
          <p v-if="hasObjectiveChanges" class="unsaved-label">Guarda los cambios pendientes para que los nuevos indicadores aparezcan en el Anexo 4.</p>
          <RouterLink :to="{ name: 'pmi-indicators', params: { year, areaId } }">Definir las fichas de los indicadores →</RouterLink>
          <p><RouterLink :to="{ name: 'pmi-follow-up', params: { year, areaId } }">Ver consolidado y seguimiento · Anexo 5 →</RouterLink></p>
        </section>
        <details v-if="canCreate" class="formulation-card extra-objectives">
          <summary>¿Necesitas otro objetivo para un componente?</summary>
          <form class="new-objective" @submit.prevent="addObjective">
            <label for="new-objective-component">Componente seleccionado<select id="new-objective-component" v-model="selectedComponentId" :disabled="busy"><option v-for="component in components" :key="component.componentId" :value="component.componentId">{{ component.componentName }}</option></select></label>
            <button class="formulation-button outline" type="submit" :disabled="busy">Agregar otro objetivo</button>
          </form>
        </details>
        <details class="formulation-card totals">
          <summary>Ver resumen de recursos guardados</summary>
          <div class="totals-grid"><div><span>Costo total (COP)</span><strong>{{ money(formulation.totals.totalCost) }}</strong></div><div><span>FSE (COP)</span><strong>{{ money(formulation.totals.fseAmount) }}</strong></div><div><span>Otra fuente (COP)</span><strong>{{ money(formulation.totals.otherAmount) }}</strong></div></div>
          <p>Este resumen incluye solo los objetivos activos guardados. Los importes vacíos siguen pendientes y no se registran como cero.</p>
          <p v-if="formulation.totals.pendingAmountFields">Importes pendientes de diligenciar: {{ formulation.totals.pendingAmountFields }}.</p>
          <p v-if="formulation.totals.excludedObjectives">Objetivos históricos excluidos del total: {{ formulation.totals.excludedObjectives }}.</p>
        </details>
        <p class="format-recommendation">El formato orienta a trabajar un objetivo por área de gestión y hasta cuatro actividades. Es una recomendación; puedes agregar objetivos, metas e indicadores según las necesidades de tu institución.</p>
      </template>
    </div>
  </main>
</template>

<style scoped>
.formulation-page { min-height: 100vh; background: var(--color-background); }
.formulation-header { display: flex; justify-content: space-between; gap: 1rem; flex-wrap: wrap; padding: 1.2rem clamp(1rem, 5vw, 5rem); background: var(--color-background-top); border-bottom: 1px solid var(--color-background-mute); }
a { color: var(--color-text-highlight); font-weight: 700; text-decoration: none; }
.formulation-content { width: min(100% - 3rem, 1150px); margin: 3rem auto 5rem; }
.formulation-eyebrow { color: var(--color-text-highlight); font-size: .74rem; font-weight: 700; letter-spacing: .08em; }
h1 { margin: .6rem 0; color: var(--color-text-highlight); font-size: clamp(1.9rem, 4vw, 2.7rem); line-height: 1.2; }
h2 { color: var(--color-text-highlight); font-size: 1.2rem; }
h3 { color: var(--color-text-highlight); font-size: 1.05rem; }
h4 { color: var(--color-text-highlight); font-size: .96rem; }
.intro, .metadata span, .objective-header p, small { color: var(--color-text-disabled); }
.formulation-card { margin-top: 1.5rem; padding: clamp(1rem, 3vw, 2rem); border: 1px solid var(--color-background-mute); border-radius: .85rem; background: var(--color-background-top); }
.formulation-card p { margin: .65rem 0; }
.metadata { display: flex; align-items: center; flex-wrap: wrap; gap: .4rem 1.2rem; margin: 1rem 0; font-size: .83rem; }
.metadata strong { color: var(--color-text-highlight); }
.metadata span { font-size: .8rem; }
.step-label { display: inline-block; margin-bottom: .5rem; font-size: .75rem; font-weight: 700; letter-spacing: .1em; color: var(--color-text-highlight); }
.component-step { border-top: 4px solid var(--sem-blue); }
.component-choice-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: .9rem; margin-top: 1.2rem; }
.formulation-component-choice { display: flex; gap: .8rem; padding: 1rem; border: 1px solid var(--color-border-disabled); border-radius: .6rem; background: var(--color-background-top); cursor: pointer; }
.component-choice-grid input { flex: 0 0 auto; width: 1.1rem; height: 1.1rem; margin-top: .25rem; padding: 0; accent-color: var(--sem-blue); }
.formulation-component-choice > span { display: grid; gap: .45rem; min-width: 0; }
.formulation-component-choice strong { color: var(--color-text-highlight); }
.formulation-component-choice span span { color: var(--color-text-disabled); font-size: .8rem; font-weight: 400; overflow-wrap: anywhere; }
.component-is-chosen { border-color: var(--sem-blue); background: var(--color-background-mute); }
.choice-progress { color: var(--color-text-highlight); font-size: .85rem; font-weight: 700; }
.selection-hint { color: var(--color-text-disabled); font-size: .82rem; }
.plan-step { margin: 2.3rem 0 1rem; }
.plan-step > p { margin-top: .65rem; color: var(--color-text-disabled); }
.empty-plan { padding: 1.2rem; border: 1px dashed var(--color-border-disabled); border-radius: .5rem; }
.question-heading { margin-top: 1.5rem; padding-top: 1rem; border-top: 1px solid var(--color-background-mute); }
summary { color: var(--color-text-highlight); cursor: pointer; font-weight: 700; }
summary:focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 3px; }
.format-recommendation { margin: 1.3rem 0; color: var(--color-text-disabled); font-size: .8rem; }
.guidance { padding: 1.3rem; margin-top: 1.5rem; border-left: 4px solid var(--sem-yellow); background: var(--color-background-top); }
.guidance p { margin-top: .6rem; }
.message { padding: .85rem 1rem; margin-top: 1rem; color: var(--color-text-highlight); border-left: 4px solid var(--sem-blue); background: var(--color-background-mute); overflow-wrap: anywhere; }
.warning { border-left-color: var(--sem-yellow); color: var(--color-text); }
.error-message { border-left-color: var(--sem-red); color: var(--sem-red); }
.totals-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 1.5rem; margin-top: 1rem; }
.totals-grid div { display: grid; gap: .4rem; }
.totals-grid span { color: var(--color-text-disabled); font-size: .82rem; }
.totals-grid strong { color: var(--color-text-highlight); font-size: 1.15rem; overflow-wrap: anywhere; }
.totals p { color: var(--color-text-disabled); font-size: .84rem; }
label { display: grid; gap: .45rem; font-size: .88rem; font-weight: 700; }
input, textarea, select { width: 100%; min-width: 0; padding: .7rem .8rem; border: 1px solid var(--color-border-disabled); border-radius: .45rem; font: inherit; color: var(--color-text); background: var(--color-background-top); }
textarea { resize: vertical; }
fieldset { min-width: 0; margin: 0; padding: 0; border: 0; }
fieldset:disabled input, fieldset:disabled textarea { background: var(--color-background-mute); color: var(--color-text-disabled); }
input:focus-visible, textarea:focus-visible, select:focus-visible, button:focus-visible, a:focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 3px; }
.new-objective { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 1rem; align-items: end; margin-top: 1rem; }
.formulation-button { display: inline-flex; align-items: center; justify-content: center; gap: .5rem; padding: .7rem 1rem; border: 1px solid var(--sem-blue); border-radius: .5rem; font: inherit; font-weight: 700; color: var(--color-text-white); background: var(--sem-blue); }
.outline { color: var(--color-text-highlight); background: var(--color-background-top); }
.formulation-button:hover:not(:disabled) { filter: brightness(.95); }
button:disabled { opacity: .6; cursor: not-allowed; }
.objective-card { border-top: 4px solid var(--sem-blue); }
.objective-header { display: flex; justify-content: space-between; align-items: start; gap: 1rem; }
.objective-header h2 { margin-top: .35rem; }
.objective-header p { font-size: .84rem; }
.draft-label { padding: .3rem .7rem; border-radius: 1rem; background: var(--color-background-mute); color: var(--color-text-highlight); font-size: .76rem; white-space: nowrap; }
.unsaved-label { padding-left: .6rem; border-left: 3px solid var(--sem-yellow); color: var(--color-text-disabled); font-size: .8rem; }
.objective-fields { display: grid; gap: 1.3rem; margin-top: 1rem; }
.goal-card { padding: 1.2rem; border: 1px solid var(--color-background-mute); border-radius: .6rem; background: var(--color-background); }
.subsection-header { display: flex; justify-content: space-between; gap: 1rem; align-items: center; margin-bottom: .8rem; }
.indicator-card { margin: 1rem 0; padding: 1.2rem; border-radius: .5rem; border: 1px solid var(--color-background-mute); background: var(--color-background-top); }
.fields-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1rem; margin-top: 1rem; }
.amount-fields { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 1rem; margin-top: 1rem; }
.full-width { grid-column: 1 / -1; }
small { font-size: .74rem; font-weight: 400; }
.objective-actions { display: flex; flex-wrap: wrap; gap: 1rem; }
.small-button { padding: .4rem .6rem; border: 1px solid transparent; border-radius: .4rem; font: inherit; background: transparent; font-size: .82rem; }
.delete-button { color: var(--sem-red); }
.delete-button:hover:not(:disabled) { border-color: var(--sem-red); }
.objective-delete { margin-top: 1rem; }
@media (max-width: 680px) {
  .formulation-content { width: min(100% - 2rem, 1150px); margin-top: 2rem; }
  .totals-grid, .new-objective, .fields-grid, .amount-fields, .component-choice-grid { grid-template-columns: 1fr; }
  .objective-header { flex-wrap: wrap; }
  .indicator-card, .goal-card { padding: .85rem; }
}
</style>
