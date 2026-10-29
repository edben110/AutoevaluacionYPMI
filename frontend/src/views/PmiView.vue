<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, RouterLink, useRoute, useRouter } from 'vue-router'
import { authorizedFetch, hasSession, loadProfile, responseError } from '@/auth/session'
import { formatPeriodDate } from '@/selfEvaluation/period'
import type { PriorityComponent, PriorityComponentRequest, PriorityMatrix } from '@/pmi/types'

interface ComponentForm {
  criticalFactors: string
  urgency: number | ''
  tendency: number | ''
  impact: number | ''
  error: string
  success: string
}
interface MatrixRow { component: PriorityComponent; form: ComponentForm }
interface ProcessGroup { id: string; name: string; rows: MatrixRow[] }
interface AreaGroup { id: string; name: string; processes: ProcessGroup[] }

const route = useRoute()
const router = useRouter()
const authorized = ref(false)
const loading = ref(true)
const error = ref('')
const matrix = ref<PriorityMatrix | null>(null)
const forms = reactive<Record<string, ComponentForm>>({})
const savingComponent = ref<string | null>(null)
const selectionSaving = ref(false)
const selectionError = ref('')
const selectionSuccess = ref('')
const selectedChoiceIds = ref<string[]>([])
const selectionSnapshot = ref('')
const year = computed(() => Number(route.params.year))
const areaId = computed(() => String(route.params.areaId ?? ''))
const apiBase = computed(() => `/api/pmi/${year.value}/areas/${encodeURIComponent(areaId.value)}`)
const busy = computed(() => loading.value || savingComponent.value !== null || selectionSaving.value)
let requestNumber = 0

function newForm(component: PriorityComponent): ComponentForm {
  return {
    criticalFactors: component.criticalFactors ?? '', urgency: component.urgency ?? '',
    tendency: component.tendency ?? '', impact: component.impact ?? '', error: '', success: '',
  }
}

const areas = computed<AreaGroup[]>(() => {
  const groups: AreaGroup[] = []
  for (const component of matrix.value?.components ?? []) {
    let area = groups.find(item => item.id === component.areaId)
    if (!area) {
      area = { id: component.areaId, name: component.areaName, processes: [] }
      groups.push(area)
    }
    let process = area.processes.find(item => item.id === component.processId)
    if (!process) {
      process = { id: component.processId, name: component.processName, rows: [] }
      area.processes.push(process)
    }
    const form = forms[component.componentId]
    if (form) process.rows.push({ component, form })
  }
  return groups
})
const scored = computed(() => matrix.value?.components.filter(item => item.eligible && item.total !== null) ?? [])
const highest = computed(() => scored.value.filter(item => matrix.value?.highestComponentIds.includes(item.componentId)))
const candidates = computed(() => matrix.value?.components.filter(item => item.eligible) ?? [])
const allCandidatesScored = computed(() => candidates.value.length > 0 && candidates.value.every(item => item.total !== null))
const selected = computed(() => (matrix.value?.selectedComponentIds ?? [])
  .map(id => matrix.value?.components.find(item => item.componentId === id))
  .filter((item): item is PriorityComponent => item !== undefined))
const inactiveCount = computed(() => matrix.value?.components.filter(item => !item.eligible).length ?? 0)

function setSelectionForm(): void {
  selectedChoiceIds.value = (matrix.value?.selectedComponentIds ?? [])
    .filter(id => highest.value.some(item => item.componentId === id))
  selectionSnapshot.value = JSON.stringify([...selectedChoiceIds.value].sort())
}

async function loadMatrix(): Promise<void> {
  const request = ++requestNumber
  loading.value = true
  error.value = ''
  matrix.value = null
  for (const id of Object.keys(forms)) delete forms[id]
  savingComponent.value = null
  selectionSaving.value = false
  selectionError.value = ''
  selectionSuccess.value = ''
  try {
    if (!Number.isInteger(year.value) || year.value < 1 || year.value > 9999) throw new Error('El año de la autoevaluación no es válido.')
    if (!areaId.value) throw new Error('Selecciona el área del PMI.')
    const response = await authorizedFetch(apiBase.value)
    if (!response.ok) throw await responseError(response)
    const loaded = await response.json() as PriorityMatrix
    if (request !== requestNumber) return
    for (const id of Object.keys(forms)) delete forms[id]
    for (const component of loaded.components) forms[component.componentId] = newForm(component)
    matrix.value = loaded
    setSelectionForm()
  } catch (cause) {
    if (request === requestNumber) error.value = cause instanceof Error ? cause.message : 'No se pudo cargar la priorización.'
    if (!hasSession.value) await router.replace({ name: 'login' })
  } finally { if (request === requestNumber) loading.value = false }
}

function validScore(value: number | ''): value is number {
  return typeof value === 'number' && Number.isInteger(value) && value >= 1 && value <= 5
}

function formChanged(component: PriorityComponent, form: ComponentForm): boolean {
  return form.criticalFactors !== (component.criticalFactors ?? '') || form.urgency !== (component.urgency ?? '')
    || form.tendency !== (component.tendency ?? '') || form.impact !== (component.impact ?? '')
}

const hasUnsavedChanges = computed(() => matrix.value !== null && (
  matrix.value.components.some(component => {
    const form = forms[component.componentId]
    return form && formChanged(component, form)
  }) || selectionSnapshot.value !== JSON.stringify([...selectedChoiceIds.value].sort())
))

function confirmDiscard(): boolean {
  return !hasSession.value || !hasUnsavedChanges.value
    || window.confirm('Hay cambios sin guardar en el Anexo 2. ¿Salir y descartarlos?')
}

function beforeUnload(event: BeforeUnloadEvent): void {
  if (!hasSession.value || !hasUnsavedChanges.value) return
  event.preventDefault()
  event.returnValue = ''
}

async function saveComponent(component: PriorityComponent): Promise<void> {
  const form = forms[component.componentId]
  if (!form || busy.value || !component.eligible || !matrix.value?.selfEvaluationComplete || !matrix.value.selfEvaluationSubmitted) return
  const request = requestNumber
  form.error = ''
  form.success = ''
  selectionSuccess.value = ''
  if (!form.criticalFactors.trim()) { form.error = 'Describe los factores críticos de este componente.'; return }
  if (!validScore(form.urgency) || !validScore(form.tendency) || !validScore(form.impact)) {
    form.error = 'Urgencia, tendencia e impacto deben ser números enteros entre 1 y 5.'
    return
  }
  const body: PriorityComponentRequest = {
    criticalFactors: form.criticalFactors.trim(), urgency: form.urgency, tendency: form.tendency, impact: form.impact,
  }
  savingComponent.value = component.componentId
  try {
    const response = await authorizedFetch(`${apiBase.value}/components/${component.componentId}`, {
      method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body),
    })
    if (!response.ok) throw await responseError(response)
    const saved = await response.json() as PriorityMatrix
    if (request !== requestNumber) return
    for (const item of saved.components) {
      // Los demás formularios conservan lo que la institución aún no ha guardado.
      if (!forms[item.componentId] || item.componentId === component.componentId) forms[item.componentId] = newForm(item)
    }
    matrix.value = saved
    setSelectionForm()
    const savedForm = forms[component.componentId]
    if (savedForm) savedForm.success = 'Factores y puntajes guardados.'
  } catch (cause) {
    if (request === requestNumber) form.error = cause instanceof Error ? cause.message : 'No se pudo guardar este componente.'
    if (!hasSession.value) await router.replace({ name: 'login' })
  } finally { if (request === requestNumber) savingComponent.value = null }
}

async function saveSelection(): Promise<void> {
  if (busy.value) return
  selectionError.value = ''
  selectionSuccess.value = ''
  if (!matrix.value?.selfEvaluationComplete || !matrix.value.selfEvaluationSubmitted || !allCandidatesScored.value) {
    selectionError.value = 'Primero guarda los factores y puntajes de todos los candidatos del área.'
    return
  }
  if (highest.value.length <= 2 || selectedChoiceIds.value.length !== 2
      || !selectedChoiceIds.value.every(id => highest.value.some(item => item.componentId === id))) {
    selectionError.value = 'Selecciona exactamente dos componentes entre los empatados con el mayor total.'
    return
  }
  const request = requestNumber
  selectionSaving.value = true
  try {
    const componentIds = [...selectedChoiceIds.value]
    const response = await authorizedFetch(`${apiBase.value}/selection`, {
      method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ componentIds }),
    })
    if (!response.ok) throw await responseError(response)
    const saved = await response.json() as PriorityMatrix
    if (request !== requestNumber) return
    matrix.value = saved
    setSelectionForm()
    selectionSuccess.value = 'Selección de componentes guardada.'
  } catch (cause) {
    if (request === requestNumber) selectionError.value = cause instanceof Error ? cause.message : 'No se pudo guardar la selección.'
    if (!hasSession.value) await router.replace({ name: 'login' })
  } finally { if (request === requestNumber) selectionSaving.value = false }
}

onMounted(async () => {
  window.addEventListener('beforeunload', beforeUnload)
  try {
    const profile = await loadProfile()
    if (profile.role !== 'ESTABLISHMENT') { await router.replace({ name: 'dashboard' }); return }
    authorized.value = true
    await loadMatrix()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'No se pudo abrir la priorización.'
    loading.value = false
    if (!hasSession.value) await router.replace({ name: 'login' })
  }
})
watch(() => [route.params.year, route.params.areaId], () => { if (authorized.value) void loadMatrix() })
onBeforeRouteLeave(confirmDiscard)
onBeforeRouteUpdate((to, from) => (to.params.year === from.params.year && to.params.areaId === from.params.areaId) || confirmDiscard())
onUnmounted(() => window.removeEventListener('beforeunload', beforeUnload))
</script>

<template>
  <main class="pmi-page">
    <header class="pmi-header"><RouterLink class="u-return-button" :to="{ name: 'dashboard' }">Volver a mi espacio</RouterLink><RouterLink :to="{ name: 'pmi-overview', params: { year } }">Volver a los PMI por área</RouterLink><span>Ruta de Mejoramiento</span></header>
    <div class="pmi-content">
      <span class="pmi-eyebrow">PLAN DE MEJORAMIENTO INSTITUCIONAL · ANEXO 2</span>
      <h1>Priorización de componentes</h1>
      <h2 v-if="matrix" class="pmi-area-title">{{ matrix.areaName }}</h2>
      <p class="pmi-intro">Identifica los factores críticos y valora la urgencia, tendencia e impacto de las oportunidades de mejora.</p>
      <p v-if="loading" class="pmi-card" role="status">Cargando la matriz de priorización…</p>
      <section v-else-if="error" class="pmi-card" role="alert"><p>{{ error }}</p><button class="pmi-button pmi-button-outline" type="button" @click="loadMatrix">Reintentar</button></section>
      <template v-else-if="authorized && matrix">
        <section class="pmi-card pmi-metadata" aria-label="Datos de la matriz">
          <div><span>Institución educativa</span><strong>{{ matrix.establishmentName }}</strong></div>
          <div><span>Autoevaluación de origen</span><strong>{{ matrix.year }}</strong></div>
          <div><span>Proyección del PMI</span><strong>{{ matrix.projectionYears }} año</strong></div>
          <div><span>Fecha de diligenciamiento</span><strong>{{ matrix.filledOn ? formatPeriodDate(matrix.filledOn) : 'Pendiente del primer guardado' }}</strong></div>
          <span class="pmi-badge">Borrador</span>
        </section>
        <section v-if="matrix.periodConfigured === false" class="pmi-warning" role="status">
          <h2>Período no publicado</h2>
          <p>Secretaría retiró el período de este año o aún no lo ha publicado. No puedes iniciar ni guardar un PMI hasta que vuelva a publicarlo.</p>
        </section>
        <template v-else>
        <section v-if="!matrix.selfEvaluationComplete" class="pmi-warning" role="status">
          <h2>Autoevaluación del área incompleta</h2>
          <p>Hay {{ matrix.valuedComponents }} de {{ matrix.totalComponents }} componentes valorados. Completa esta área para habilitar la priorización de su PMI.</p>
        </section>
        <section v-else-if="!matrix.selfEvaluationSubmitted" class="pmi-warning" role="status">
          <h2>Autoevaluación pendiente de envío</h2>
          <p>Envía la autoevaluación de esta área para habilitar su PMI.</p>
          <RouterLink :to="{ name: 'self-evaluation', params: { areaId }, query: { year } }">Volver a la autoevaluación del área →</RouterLink>
        </section>

        <p v-if="matrix.status === 'SUBMITTED'" class="pmi-warning" role="status">PMI enviado a Secretaría. Los anexos 2, 3 y 4 están disponibles para consulta.</p>
        <section class="pmi-guide" aria-labelledby="scoring-guide">
          <h2 id="scoring-guide">Cómo diligenciar la matriz</h2>
          <p>Solo se priorizan los componentes del menor estado presente en esta área: Existencia, o Pertinencia si no hay Existencia, o Apropiación si no hay estados inferiores. Si todos están en Mejoramiento continuo, no hay candidatos.</p>
          <p v-if="matrix.candidateStateLabel">Estado candidato del área: <strong>{{ matrix.candidateStateLabel }}</strong> · {{ candidates.length }} componentes.</p>
          <dl class="pmi-score-guide">
            <div><dt>U · Urgencia</dt><dd>Qué tan pronto se necesita intervenir.</dd></div>
            <div><dt>T · Tendencia</dt><dd>Cómo podría evolucionar la situación si no se interviene.</dd></div>
            <div><dt>I · Impacto</dt><dd>Cuánto afecta la situación a los resultados de la institución.</dd></div>
          </dl>
          <p>El total es U + T + I. Debes guardar los puntajes de todos los candidatos. Si hay un solo máximo, pasa ese componente; si hay dos, pasan ambos. Si hay más de dos empatados en el máximo, selecciona exactamente dos de ellos.</p>
        </section>
        <p v-if="matrix.selectionNeedsReview || inactiveCount > 0" class="pmi-warning" role="status">Revisa la priorización: hay cambios respecto a la autoevaluación o a los puntajes. Los componentes que ya no pertenecen al menor estado del área conservan sus datos para consulta y no se pueden seleccionar.</p>
        <section v-if="candidates.length === 0" class="pmi-card">
          <h2>No hay candidatos para priorizar</h2>
          <p>{{ matrix.selfEvaluationComplete ? 'Todos los componentes del área están en Mejoramiento continuo. No se habilita la formulación de un PMI.' : 'Primero completa las valoraciones de esta área.' }}</p>
        </section>

        <section v-for="area in areas" :key="area.id" class="pmi-card pmi-area" :aria-labelledby="`area-${area.id}`">
          <h2 :id="`area-${area.id}`">{{ area.name }}</h2>
          <div class="pmi-table-wrap" tabindex="0" :aria-label="`Matriz de ${area.name}, desplazamiento horizontal disponible`">
            <table class="pmi-table">
              <caption>Factores críticos y valoración de componentes · Autoevaluación {{ matrix.year }}</caption>
              <thead><tr><th scope="col">Componente y oportunidad de mejora</th><th scope="col">Factores críticos</th><th scope="col"><abbr title="Urgencia">U</abbr></th><th scope="col"><abbr title="Tendencia">T</abbr></th><th scope="col"><abbr title="Impacto">I</abbr></th><th scope="col">Total guardado</th><th scope="col">Registro</th></tr></thead>
              <tbody v-for="process in area.processes" :key="process.id">
                <tr class="pmi-process"><th scope="rowgroup" colspan="7">Proceso · {{ process.name }}</th></tr>
                <tr v-for="row in process.rows" :key="row.component.componentId" :class="{ 'pmi-row-inactive': !row.component.eligible }">
                  <th scope="row" class="pmi-component">
                    <strong>{{ row.component.componentName }}</strong>
                    <span class="pmi-state">{{ row.component.stateLabel }}</span>
                    <p class="pmi-opportunity">{{ row.component.improvementOpportunities || 'Sin oportunidad de mejora escrita en la autoevaluación.' }}</p>
                    <span v-if="!row.component.eligible" class="pmi-inactive-label">Solo consulta · ya no es candidato</span>
                    <span v-else-if="row.component.selected" class="pmi-selected-label">Seleccionado para el PMI</span>
                    <span v-if="formChanged(row.component, row.form)" class="pmi-unsaved">Cambios sin guardar</span>
                  </th>
                  <td class="pmi-factors">
                    <label class="pmi-sr-only" :for="`factors-${row.component.componentId}`">Factores críticos de {{ row.component.componentName }}</label>
                    <textarea :id="`factors-${row.component.componentId}`" v-model="row.form.criticalFactors" rows="5" maxlength="10000" :disabled="busy || matrix.status === 'SUBMITTED' || !matrix.selfEvaluationComplete || !matrix.selfEvaluationSubmitted || !row.component.eligible" placeholder="Describe las dificultades que explican esta oportunidad de mejora." />
                  </td>
                  <td v-for="score in (['urgency', 'tendency', 'impact'] as const)" :key="score" class="pmi-score-cell">
                    <label class="pmi-sr-only" :for="`${score}-${row.component.componentId}`">{{ score === 'urgency' ? 'Urgencia' : score === 'tendency' ? 'Tendencia' : 'Impacto' }} de {{ row.component.componentName }}</label>
                    <input :id="`${score}-${row.component.componentId}`" v-model.number="row.form[score]" type="number" min="1" max="5" step="1" inputmode="numeric" :disabled="busy || matrix.status === 'SUBMITTED' || !matrix.selfEvaluationComplete || !matrix.selfEvaluationSubmitted || !row.component.eligible" />
                  </td>
                  <td class="pmi-total"><strong>{{ row.component.total ?? '—' }}</strong><span v-if="row.component.total !== null">de 15</span></td>
                  <td class="pmi-save-cell">
                    <button v-if="row.component.eligible" class="pmi-button pmi-button-outline" type="button" :disabled="busy || matrix.status === 'SUBMITTED' || !matrix.selfEvaluationComplete || !matrix.selfEvaluationSubmitted" :aria-label="`Guardar ${row.component.componentName}`" @click="saveComponent(row.component)">{{ savingComponent === row.component.componentId ? 'Guardando…' : 'Guardar fila' }}</button>
                    <span v-else>Solo consulta</span>
                    <p v-if="row.form.error" class="pmi-error" role="alert">{{ row.form.error }}</p>
                    <p v-if="row.form.success" class="pmi-success" role="status">{{ row.form.success }}</p>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="pmi-card pmi-selection" aria-labelledby="selection-title">
          <h2 id="selection-title">Componentes priorizados para el PMI</h2>
          <p>Avance de priorización: {{ scored.length }} de {{ candidates.length }} candidatos con puntajes guardados.</p>
          <div v-if="selected.length" class="pmi-saved-selection">
            <h3>Componentes seleccionados</h3>
            <ol><li v-for="component in selected" :key="component.componentId">{{ component.componentName }} · {{ component.total ?? 'sin puntaje' }} puntos <span v-if="!component.eligible">· requiere revisión</span></li></ol>
          </div>
          <p v-if="!allCandidatesScored" class="pmi-empty-note">Guarda los factores y los tres puntajes de todos los candidatos del área para continuar.</p>
          <p v-else-if="highest.length <= 2">{{ highest.length === 1 ? 'El componente con el mayor total se selecciona automáticamente.' : 'Los dos componentes empatados con el mayor total se seleccionan automáticamente.' }}</p>
          <form v-else @submit.prevent="saveSelection">
            <p class="pmi-warning">Hay {{ highest.length }} componentes empatados con el mayor puntaje. Elige exactamente dos.</p>
            <fieldset :disabled="busy || matrix.status === 'SUBMITTED' || !matrix.selfEvaluationComplete || !matrix.selfEvaluationSubmitted" class="pmi-selection-fields">
              <label v-for="component in highest" :key="component.componentId" class="pmi-choice">
                <input v-model="selectedChoiceIds" type="checkbox" :value="component.componentId" :disabled="selectedChoiceIds.length >= 2 && !selectedChoiceIds.includes(component.componentId)" />
                <span>{{ component.componentName }} · {{ component.total }} puntos</span>
              </label>
              <button class="pmi-button" type="submit">{{ selectionSaving ? 'Guardando…' : 'Guardar selección' }}</button>
            </fieldset>
            <p v-if="selectionError" class="pmi-error" role="alert">{{ selectionError }}</p>
            <p v-if="selectionSuccess" class="pmi-success" role="status">{{ selectionSuccess }}</p>
          </form>
        </section>
        <section class="pmi-card">
          <h2>Continúa con la formulación</h2>
          <p>En el Anexo 3 puedes formular objetivos, metas, indicadores y actividades a partir de los componentes priorizados. La matriz del Anexo 2 se conserva.</p>
          <RouterLink v-if="matrix.prioritizationComplete && matrix.selfEvaluationComplete && matrix.selfEvaluationSubmitted" class="pmi-button formulation-link" :to="{ name: 'pmi-formulation', params: { year, areaId } }">Elegir componentes y preparar el plan · Anexo 3 →</RouterLink>
          <p v-else class="pmi-empty-note">Completa y envía la autoevaluación y guarda los puntajes de todos los candidatos para continuar con el plan.</p>
        </section>
        </template>
      </template>
    </div>
  </main>
</template>

<style scoped>
.pmi-page { min-height: 100vh; background: var(--color-background); }
.pmi-header { display: flex; justify-content: space-between; flex-wrap: wrap; gap: 1rem; padding: 1.2rem clamp(1rem, 5vw, 5rem); border-bottom: 1px solid var(--color-background-mute); background: var(--color-background-top); }
.pmi-header a:not(.u-return-button) { color: var(--color-text-highlight); font-weight: 700; text-decoration: none; }
.pmi-header span { color: var(--color-text-disabled); }
.pmi-content { width: min(100% - 3rem, 1300px); margin: 3rem auto 5rem; }
.pmi-eyebrow { font-size: .74rem; color: var(--color-text-highlight); font-weight: 700; letter-spacing: .1em; }
h1 { margin: .6rem 0; color: var(--color-text-highlight); font-size: clamp(1.9rem, 4vw, 2.8rem); line-height: 1.2; }
h2 { color: var(--color-text-highlight); font-size: 1.25rem; }
h3 { font-size: .95rem; }
.pmi-intro { margin: .8rem 0 1.5rem; color: var(--color-text-disabled); }
.pmi-card { margin-top: 1.5rem; padding: clamp(1rem, 2.5vw, 2rem); border: 1px solid var(--color-background-mute); border-radius: .9rem; background: var(--color-background-top); }
.pmi-card p { margin-top: .65rem; }
.pmi-metadata { position: relative; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1.4rem; padding-right: 7rem; }
.pmi-metadata div { display: grid; gap: .3rem; }
.pmi-metadata div > span { font-size: .8rem; color: var(--color-text-disabled); }
.pmi-metadata strong { overflow-wrap: anywhere; }
.pmi-badge { position: absolute; top: 1rem; right: 1rem; border-radius: 2rem; padding: .3rem .8rem; color: var(--color-text-highlight); background: var(--color-background-mute); font-size: .8rem; font-weight: 700; }
.pmi-guide { padding: 1.4rem; margin-top: 1.5rem; border-left: 4px solid var(--sem-yellow); background: var(--color-background-top); }
.pmi-guide p { margin-top: .7rem; }
.pmi-score-guide { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 1rem; margin-top: 1rem; }
.pmi-score-guide dt { color: var(--color-text-highlight); font-weight: 700; }
.pmi-score-guide dd { color: var(--color-text-disabled); font-size: .88rem; }
.pmi-warning { padding: .9rem 1rem; margin-top: 1rem; border-left: 4px solid var(--sem-yellow); background: var(--color-background-mute); }
.pmi-table-wrap { position: relative; margin-top: .8rem; overflow-x: auto; }
.pmi-table { width: 100%; min-width: 960px; border-collapse: collapse; font-size: .87rem; }
.pmi-table caption { padding: .5rem 0 1rem; text-align: left; color: var(--color-text-disabled); }
.pmi-table th, .pmi-table td { padding: .8rem; border: 1px solid var(--color-background-mute); text-align: left; vertical-align: top; }
.pmi-table thead { color: var(--color-text-white); background: var(--sem-blue); }
.pmi-table thead th { font-weight: 700; vertical-align: middle; }
.pmi-process { color: var(--color-text-highlight); background: var(--color-background-mute); }
.pmi-process th { font-weight: 700; padding: .75rem 1rem; }
.pmi-component { width: 26%; min-width: 220px; }
.pmi-component > strong, .pmi-component > span { display: block; }
.pmi-state { margin-top: .4rem; color: var(--color-text-highlight); font-weight: 700; font-size: .78rem; }
.pmi-opportunity { white-space: pre-wrap; overflow-wrap: anywhere; color: var(--color-text-disabled); font-weight: 400; }
.pmi-factors { width: 30%; min-width: 250px; }
.pmi-table textarea { min-height: 8rem; width: 100%; resize: vertical; }
.pmi-table input { width: 4rem; padding: .7rem .4rem; }
.pmi-table input, .pmi-table textarea, .pmi-selection select { border: 1px solid var(--color-border-disabled); border-radius: .4rem; padding: .6rem; font: inherit; color: var(--color-text); background: var(--color-background-top); }
.pmi-table textarea:disabled, .pmi-table input:disabled { background: var(--color-background-mute); color: var(--color-text-disabled); }
.pmi-total { text-align: center !important; }
.pmi-total strong { display: block; color: var(--color-text-highlight); font-size: 1.4rem; }
.pmi-total span { font-size: .75rem; white-space: nowrap; }
.pmi-save-cell { min-width: 135px; }
.pmi-button { display: inline-flex; justify-content: center; align-items: center; padding: .7rem 1rem; border: 1px solid var(--sem-blue); border-radius: .5rem; color: var(--color-text-white); background: var(--sem-blue); font: inherit; font-weight: 700; }
.pmi-button-outline { color: var(--color-text-highlight); background: var(--color-background-top); }
.formulation-link { margin-top: 1rem; text-decoration: none; }
.pmi-button:hover:not(:disabled) { filter: brightness(.94); }
.pmi-button:disabled { opacity: .6; cursor: wait; }
.pmi-button:focus-visible, input:focus-visible, textarea:focus-visible, select:focus-visible, a:focus-visible, .pmi-table-wrap:focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 3px; }
.pmi-error { color: var(--sem-red); font-size: .83rem; overflow-wrap: anywhere; }
.pmi-success { color: var(--color-text-highlight); font-size: .83rem; }
.pmi-selected-label { margin-top: .6rem; color: var(--color-text-highlight); font-size: .76rem; font-weight: 700; }
.pmi-inactive-label, .pmi-unsaved { margin-top: .6rem; color: var(--color-text-disabled); font-size: .76rem; }
.pmi-unsaved { border-left: 3px solid var(--sem-yellow); padding-left: .4rem; }
.pmi-row-inactive { background: var(--color-background); }
.pmi-selection-fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1.2rem; min-width: 0; margin: 1.2rem 0 0; padding: 0; border: 0; }
.pmi-selection-fields label { display: grid; gap: .5rem; font-weight: 700; }
.pmi-selection-fields .pmi-choice { display: flex; align-items: start; gap: .7rem; padding: .8rem; border: 1px solid var(--color-background-mute); border-radius: .4rem; }
.pmi-choice input { flex: 0 0 auto; margin-top: .35rem; accent-color: var(--sem-blue); }
.pmi-area-title { margin: 1rem 0; }
.pmi-selection-fields select { width: 100%; min-width: 0; }
.pmi-selection-fields .pmi-button { justify-self: start; }
.pmi-saved-selection { margin-top: 1rem; padding: 1rem; border-radius: .5rem; background: var(--color-background-mute); }
.pmi-saved-selection ol { padding-left: 1.4rem; margin-top: .5rem; }
.pmi-saved-selection li { padding: .25rem 0; }
.pmi-empty-note { color: var(--color-text-disabled); }
.pmi-sr-only { position: absolute; width: 1px; height: 1px; padding: 0; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
@media (max-width: 680px) {
  .pmi-content { width: min(100% - 2rem, 1300px); margin-top: 2rem; }
  .pmi-score-guide, .pmi-selection-fields { grid-template-columns: 1fr; }
  .pmi-metadata { grid-template-columns: 1fr; padding-right: 1rem; padding-top: 3.5rem; }
  .pmi-header > span { display: none; }
}
</style>
