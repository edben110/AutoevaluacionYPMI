<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, RouterLink, useRoute, useRouter } from 'vue-router'
import ValuationTotals from '@/components/ValuationTotals.vue'
import EvaluationPeriodNotice from '@/components/EvaluationPeriodNotice.vue'
import ComponentGuidanceDialog from '@/components/ComponentGuidanceDialog.vue'
import { loadEvaluationPeriod, type EvaluationPeriod } from '@/selfEvaluation/period'
import {
  allowsStrengths,
  allowsImprovementOpportunities,
  valuationStates,
  type Valuation,
  type ValuationLevel,
  type AreaEvaluationStatus,
} from '@/selfEvaluation/valuation'
import {
  authorizedFetch,
  currentProfile,
  hasSession,
  loadProfile,
  responseError,
} from '@/auth/session'

interface Area { id: string; name: string }
interface Process { id: string; areaId: string; name: string }
interface Component { id: string; processId: string; name: string; description: string }
interface Evaluation {
  id: string
  year: number
  status: 'DRAFT'
  createdAt: string
  updatedAt: string
  valuations: Valuation[]
}
interface ComponentForm extends Component {
  level: ValuationLevel | ''
  evidenceUrl: string
  evidenceNote: string
  strengths: string
  improvementOpportunities: string
  saved: boolean
  saving: boolean
  error: string
  savedSnapshot: string
}

const router = useRouter()
const route = useRoute()
const areaId = computed(() => typeof route.params.areaId === 'string' ? route.params.areaId : '')
const requestedYear = Number(route.query.year)
const year = ref(Number.isInteger(requestedYear) && requestedYear > 0 && requestedYear <= 9999 ? requestedYear : new Date().getFullYear())
const period = ref<EvaluationPeriod | null>(null)
const periodWritable = computed(() => period.value?.writable === true)
const areaStatus = ref<AreaEvaluationStatus | null>(null)
const submitting = ref(false)
const submitError = ref('')
const availabilityError = ref('')
const writeAllowed = computed(() => periodWritable.value && !loading.value && areaStatus.value?.status === 'DRAFT')
const canEdit = computed(() => writeAllowed.value && !submitting.value)
const loading = ref(true)
const error = ref('')
const evaluation = ref<Evaluation | null>(null)
const areas = ref<Area[]>([])
const processes = ref<Process[]>([])
const components = ref<ComponentForm[]>([])
const helpComponent = ref<ComponentForm | null>(null)
const timers = new Map<string, ReturnType<typeof setTimeout>>()
const inFlight = new Map<string, Promise<void>>()
const queued = new Set<string>()
let contextVersion = 0

// Los subtotales se calculan sobre los datos guardados y los componentes del catálogo visible.
function savedValuationsFor(items: Component[]): Valuation[] {
  const ids = new Set(items.map((component) => component.id))
  return evaluation.value?.valuations.filter((valuation) => ids.has(valuation.componentId)) ?? []
}

const groups = computed(() =>
  areas.value
    .map((area) => {
      const areaProcesses = processes.value
        .filter((process) => process.areaId === area.id)
        .map((process) => {
          const items = components.value.filter((component) => component.processId === process.id)
          return { ...process, components: items, valuations: savedValuationsFor(items) }
        })
        .filter((process) => process.components.length > 0)
      return {
        ...area,
        processes: areaProcesses,
        valuations: savedValuationsFor(areaProcesses.flatMap((process) => process.components)),
      }
    })
)
const selectedArea = computed(() => groups.value.find((area) => area.id === areaId.value))
const valuedCount = computed(() => selectedArea.value?.valuations.length ?? 0)
const areaComponentCount = computed(() =>
  selectedArea.value?.processes.reduce((total, process) => total + process.components.length, 0) ?? 0,
)
const areaForms = computed(() => selectedArea.value?.processes.flatMap(process => process.components) ?? [])
const allFormsValued = computed(() => areaForms.value.length > 0 && areaForms.value.every(component => component.level !== ''))
const hasPendingChanges = computed(() => areaForms.value.some(component => dirty(component) || component.saving))

function payload(component: ComponentForm) {
  return {
    level: component.level, evidenceUrl: component.evidenceUrl.trim(), evidenceNote: component.evidenceNote.trim(),
    strengths: component.strengths.trim(), improvementOpportunities: component.improvementOpportunities.trim(),
  }
}
function snapshot(component: ComponentForm): string { return JSON.stringify(payload(component)) }
function dirty(component: ComponentForm): boolean { return snapshot(component) !== component.savedSnapshot }
function cancelTimers(): void {
  for (const timer of timers.values()) clearTimeout(timer)
  timers.clear(); queued.clear()
}
function cancelTimer(component: ComponentForm): void {
  const timer = timers.get(component.id)
  if (timer) clearTimeout(timer)
  timers.delete(component.id)
}
function validationError(component: ComponentForm): string {
  if (component.level === '') return 'Selecciona un estado para guardar la valoración.'
  if (!allowsStrengths(component.level) && component.strengths.trim()) return 'Existencia no permite fortalezas. Quita ese texto antes de guardar.'
  if (!allowsImprovementOpportunities(component.level) && component.improvementOpportunities.trim()) return 'Mejoramiento continuo no permite oportunidades de mejora. Quita ese texto antes de guardar.'
  if ([component.evidenceUrl, component.evidenceNote, component.strengths, component.improvementOpportunities].some(value => value.length > 10000)) return 'Revisa la longitud de los textos antes de guardar.'
  return ''
}

function changed(component: ComponentForm): void {
  if (!canEdit.value) return
  cancelTimer(component); queued.delete(component.id)
  component.saved = !dirty(component)
  component.error = validationError(component)
  if ((!dirty(component) && !component.saving) || component.error) return
  const version = contextVersion
  timers.set(component.id, setTimeout(() => {
    timers.delete(component.id)
    if (version === contextVersion) void save(component)
  }, 800))
}

async function readJson<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await authorizedFetch(path, options)
  if (!response.ok) throw await responseError(response)
  return (await response.json()) as T
}

async function loadCatalog(draft: Evaluation | null, version: number, selectedId: string): Promise<void> {
  const [areaRows, processRows, componentRows] = await Promise.all([
    readJson<Area[]>('/api/areas?onlyActive=true'),
    readJson<Process[]>('/api/processes?onlyActive=true'),
    readJson<Component[]>('/api/components?onlyActive=true'),
  ])
  if (version !== contextVersion) return
  areas.value = areaRows
  if (!areaRows.some((area) => area.id === selectedId)) {
    throw new Error('El área seleccionada no está disponible. Vuelve a tu espacio y elige un área activa.')
  }
  processes.value = processRows
  const processIds = new Set(processRows.filter(process => process.areaId === selectedId).map(process => process.id))
  components.value = componentRows.filter(component => processIds.has(component.processId)).map((component) => {
    const valuation = draft?.valuations.find((item) => item.componentId === component.id)
    const form: ComponentForm = {
      ...component,
      level: valuation?.level ?? '',
      evidenceUrl: valuation?.evidenceUrl ?? '',
      evidenceNote: valuation?.evidenceNote ?? '',
      strengths: valuation?.strengths ?? '',
      improvementOpportunities: valuation?.improvementOpportunities ?? '',
      saved: Boolean(valuation),
      saving: false,
      error: '',
      savedSnapshot: '',
    }
    form.savedSnapshot = snapshot(form)
    return form
  })
}

async function loadYear(): Promise<void> {
  helpComponent.value = null
  const version = ++contextVersion
  const selectedId = areaId.value
  const selectedYear = year.value
  cancelTimers(); inFlight.clear()
  submitting.value = false
  submitError.value = ''
  availabilityError.value = ''
  areaStatus.value = null
  error.value = ''
  evaluation.value = null
  period.value = null
  components.value = []
  areas.value = []
  processes.value = []
  if (!Number.isInteger(year.value) || year.value < 1) {
    error.value = 'Escribe un año válido.'
    return
  }
  loading.value = true
  try {
    const loadedPeriod = await loadEvaluationPeriod(selectedYear)
    if (version !== contextVersion) return
    period.value = loadedPeriod
    if (!loadedPeriod?.writable) return
    const response = await authorizedFetch(`/api/self-evaluations/${selectedYear}`)
    if (version !== contextVersion) return
    if (response.status === 404) {
      await loadCatalog(null, version, selectedId)
      return
    }
    if (!response.ok) throw await responseError(response)
    const draft = (await response.json()) as Evaluation
    if (version !== contextVersion) return
    const status = await readJson<AreaEvaluationStatus>(`/api/self-evaluations/${selectedYear}/areas/${encodeURIComponent(selectedId)}`)
    if (version !== contextVersion) return
    areaStatus.value = status
    evaluation.value = draft
    await loadCatalog(draft, version, selectedId)
  } catch (cause) {
    if (!hasSession.value) {
      await router.replace({ name: 'login' })
      return
    }
    if (version === contextVersion) error.value = cause instanceof Error ? cause.message : 'No se pudo cargar la autoevaluación.'
  } finally {
    if (version === contextVersion) loading.value = false
  }
}

function clearNarrative(component: ComponentForm, field: 'strengths' | 'improvementOpportunities'): void {
  if (!canEdit.value) return
  component[field] = ''
  changed(component)
}

function save(component: ComponentForm): Promise<void> {
  cancelTimer(component)
  const running = inFlight.get(component.id)
  if (running) { queued.add(component.id); return running }
  if (!writeAllowed.value || !evaluation.value || !dirty(component)) return Promise.resolve()
  component.error = validationError(component)
  if (component.error) return Promise.resolve()
  const version = contextVersion
  const sent = snapshot(component)
  const selectedYear = evaluation.value.year
  component.saving = true
  component.error = ''
  const task: Promise<void> = Promise.resolve().then(async () => {
   try {
    const valuation = await readJson<Valuation>(
      `/api/self-evaluations/${selectedYear}/valuations/${component.id}`,
      {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: sent,
      },
    )
    if (version !== contextVersion || !evaluation.value) return
    availabilityError.value = ''
    component.savedSnapshot = sent
    component.saved = !dirty(component)
    evaluation.value.valuations = [
      ...evaluation.value.valuations.filter((item) => item.componentId !== component.id),
      valuation,
    ]
   } catch (cause) {
    if (version !== contextVersion) return
    component.error = cause instanceof Error ? cause.message : 'No se pudo guardar la valoración.'
    queued.delete(component.id)
    if (!hasSession.value) { await router.replace({ name: 'login' }); return }
    await Promise.allSettled([refreshPeriod(), refreshAreaStatus()])
   } finally {
    if (version === contextVersion) {
      component.saving = false
      if (inFlight.get(component.id) === task) inFlight.delete(component.id)
      if (queued.delete(component.id) && !component.error && dirty(component) && writeAllowed.value) void save(component)
    }
   }
  })
  inFlight.set(component.id, task)
  return task
}

async function flush(component: ComponentForm): Promise<void> {
  cancelTimer(component); queued.delete(component.id)
  while (writeAllowed.value) {
    const running = inFlight.get(component.id)
    if (running) { await running; continue }
    if (!dirty(component) || component.error) return
    await save(component)
    if (component.error) return
  }
}

async function refreshAreaStatus(): Promise<AreaEvaluationStatus | null> {
  const version = contextVersion
  const status = await readJson<AreaEvaluationStatus>(`/api/self-evaluations/${year.value}/areas/${encodeURIComponent(areaId.value)}`)
  if (version !== contextVersion) return null
  areaStatus.value = status
  return status
}

async function submitArea(): Promise<void> {
  if (!canEdit.value || !selectedArea.value || !allFormsValued.value) return
  if (!window.confirm(`¿Enviar la autoevaluación de ${selectedArea.value.name}? Se guardarán los cambios pendientes y esta área quedará solo para consulta. Las otras áreas seguirán su propio proceso.`)) return
  const version = contextVersion
  const selectedYear = year.value
  const selectedId = areaId.value
  submitting.value = true
  submitError.value = ''
  try {
    await Promise.all(areaForms.value.map(flush))
    if (version !== contextVersion) return
    if (areaForms.value.some(component => component.error || dirty(component) || component.saving)) throw new Error('Hay cambios pendientes o errores de guardado. Revisa las filas y usa Reintentar antes de enviar.')
    const status = await refreshAreaStatus()
    if (version !== contextVersion || !status) return
    if (status.status === 'SUBMITTED') return
    if (!status.complete || !status.canSubmit) throw new Error('El área debe estar completa y dentro del período abierto para enviarla.')
    const submitted = await readJson<AreaEvaluationStatus>(`/api/self-evaluations/${selectedYear}/areas/${encodeURIComponent(selectedId)}/submit`, { method: 'POST' })
    if (version !== contextVersion) return
    areaStatus.value = submitted
    availabilityError.value = ''
    cancelTimers()
  } catch (cause) {
    if (version === contextVersion) submitError.value = cause instanceof Error ? cause.message : 'No se pudo enviar esta área.'
    if (!hasSession.value) await router.replace({ name: 'login' })
  } finally { if (version === contextVersion) submitting.value = false }
}

async function refreshPeriod(): Promise<void> {
  const version = contextVersion
  try {
    const loaded = await loadEvaluationPeriod(year.value)
    if (version === contextVersion) { period.value = loaded; availabilityError.value = '' }
  } catch (cause) {
    if (version === contextVersion) availabilityError.value = cause instanceof Error ? `No se pudo verificar el período: ${cause.message}` : 'No se pudo verificar el período. Tus cambios pendientes se conservan; reintenta cuando recuperes la conexión.'
  }
}

async function refreshAvailability(): Promise<void> {
  if (loading.value || submitting.value || currentProfile.value?.role !== 'ESTABLISHMENT') return
  const wasWritable = period.value?.writable === true
  await refreshPeriod()
  if (!wasWritable && period.value?.writable && !hasPendingChanges.value && components.value.length === 0) await loadYear()
}

function confirmDiscard(): boolean {
  return !hasSession.value || !hasPendingChanges.value || window.confirm('Hay valoraciones pendientes de guardar. ¿Salir y descartar los cambios pendientes?')
}
function beforeUnload(event: BeforeUnloadEvent): void {
  if (!hasSession.value || !hasPendingChanges.value) return
  event.preventDefault(); event.returnValue = ''
}

onMounted(async () => {
  window.addEventListener('focus', refreshAvailability)
  window.addEventListener('beforeunload', beforeUnload)
  try {
    const profile = await loadProfile()
    if (profile.role !== 'ESTABLISHMENT') {
      await router.replace({ name: 'dashboard' })
      return
    }
    if (!areaId.value) {
      await router.replace({ name: 'dashboard' })
      return
    }
    await loadYear()
  } catch (cause) {
    if (!hasSession.value) {
      await router.replace({ name: 'login' })
      return
    }
    error.value = cause instanceof Error ? cause.message : 'No se pudo abrir la autoevaluación.'
    loading.value = false
  }
})
onUnmounted(() => {
  contextVersion++
  cancelTimers()
  window.removeEventListener('focus', refreshAvailability)
  window.removeEventListener('beforeunload', beforeUnload)
})
onBeforeRouteLeave(confirmDiscard)
onBeforeRouteUpdate((to, from) => (to.params.areaId === from.params.areaId && to.query.year === from.query.year) || confirmDiscard())

watch([areaId, () => route.query.year], ([selectedId, selectedYear]) => {
  const value = Number(selectedYear)
  year.value = Number.isInteger(value) && value > 0 && value <= 9999 ? value : new Date().getFullYear()
  if (!selectedId) void router.replace({ name: 'dashboard' })
  else if (currentProfile.value?.role === 'ESTABLISHMENT') void loadYear()
})
</script>

<template>
  <main class="evaluation-page">
    <header class="evaluation-header">
      <RouterLink to="/dashboard">← Volver a mi espacio</RouterLink>
      <span v-if="currentProfile">{{ currentProfile.name }}</span>
    </header>

    <div class="evaluation-content">
      <span class="eyebrow eyebrow-dark">GUÍA 34 · ETAPA 01</span>
      <h1>Autoevaluación institucional</h1>
      <p v-if="selectedArea" class="evaluation-area-name">{{ selectedArea.name }}</p>
      <p v-if="canEdit">Tus cambios se guardan automáticamente después de una pausa. Cuando completes esta área, envíala para habilitar su PMI.</p>

      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <p v-if="availabilityError" class="form-error" role="alert">{{ availabilityError }}</p>
      <p v-if="loading" class="status-card" role="status">Cargando autoevaluación…</p>

      <EvaluationPeriodNotice v-if="!loading && periodWritable" :period="period" :year="year" />

      <section v-if="!loading && !error && !periodWritable" class="evaluation-empty">
        <h2>Autoevaluación no habilitada</h2>
        <p v-if="period?.status === 'SCHEDULED'">Secretaría programó este período; podrás entrar cuando llegue la fecha de inicio.</p>
        <p v-else-if="period?.status === 'CLOSED'">El período de autoevaluación de {{ year }} terminó.</p>
        <p v-else>Secretaría aún no ha habilitado la autoevaluación de {{ year }}.</p>
        <RouterLink class="secondary-button dashboard-action" to="/dashboard">Volver a mi espacio</RouterLink>
      </section>

      <section v-else-if="!loading && !error && !selectedArea" class="evaluation-empty">
        <h2>Área no disponible</h2>
        <RouterLink class="secondary-button dashboard-action" to="/dashboard">Volver a las áreas</RouterLink>
      </section>

      <section v-else-if="!loading && !error && selectedArea && !evaluation" class="evaluation-empty">
        <h2>Autoevaluación pendiente de preparación</h2>
        <p>Contacta a Secretaría para que prepare tu autoevaluación de {{ year }}.</p>
        <button class="secondary-button" type="button" @click="loadYear">Actualizar disponibilidad</button>
      </section>

      <template v-else-if="!loading && !error && periodWritable && selectedArea && evaluation">
        <div class="evaluation-summary">
          <div>
            <strong>{{ areaStatus?.status === 'SUBMITTED' ? 'Enviada' : 'Borrador' }} {{ evaluation.year }}</strong>
            <span>{{ selectedArea.name }}</span>
          </div>
          <span>{{ valuedCount }} de {{ areaComponentCount }} componentes valorados</span>
        </div>
        <section class="status-card submission-card" aria-labelledby="area-submission-title">
          <h2 id="area-submission-title">Entrega de esta área</h2>
          <template v-if="areaStatus?.status === 'SUBMITTED'">
            <p class="submission-confirmed" role="status">Autoevaluación enviada. Las valoraciones de esta área quedan solo para consulta; las demás áreas conservan su propio estado.</p>
            <p v-if="areaStatus.submittedAt">Fecha de envío: {{ new Date(areaStatus.submittedAt).toLocaleString('es-CO', { timeZone: 'America/Bogota' }) }}.</p>
            <RouterLink class="submission-action" :to="{ name: 'pmi', params: { year, areaId } }">Abrir PMI de esta área →</RouterLink>
          </template>
          <template v-else>
            <p>Enviar guarda y verifica las valoraciones pendientes. Después del envío, esta área queda solo para consulta y se habilita su PMI.</p>
            <p v-if="!allFormsValued">Valora todos los componentes de esta área para enviarla.</p>
            <p v-if="submitError" class="form-error" role="alert">{{ submitError }}</p>
            <button id="submit-evaluation-area" class="submission-action" type="button" :disabled="!canEdit || !allFormsValued" @click="submitArea">{{ submitting ? 'Guardando y enviando…' : 'Enviar autoevaluación de esta área' }}</button>
          </template>
        </section>

        <section class="status-card" aria-label="Conteo de componentes por estado">
          <h2>Componentes valorados por estado</h2>
          <ValuationTotals title="Resumen del área" :valuations="selectedArea.valuations" />
          <p>Cada componente guardado cuenta una vez. Los porcentajes se calculan sobre los componentes valorados de esta área.</p>
        </section>

        <section v-if="areaComponentCount === 0" class="evaluation-empty">
          <h2>Esta área todavía no tiene componentes activos</h2>
          <p>Podrás diligenciarla cuando sus componentes estén disponibles en el catálogo.</p>
        </section>

        <section class="evaluation-area">
          <h2>Procesos del área</h2>
          <div v-for="(process, index) in selectedArea.processes" :key="process.id" class="evaluation-process">
            <header class="process-header">
              <span>PROCESO {{ index + 1 }}</span>
              <h3>{{ process.name }}</h3>
            </header>
            <article v-for="component in process.components" :key="component.id" class="evaluation-component">
              <div class="component-heading">
                <div>
                  <h4>{{ component.name }}</h4>
                  <p v-if="component.description">{{ component.description }}</p>
                </div>
                <span class="save-status" :class="{ 'save-status-error': component.error }" role="status">{{ component.saving ? 'Guardando…' : component.error ? 'Pendiente de guardar' : component.saved ? 'Guardado' : dirty(component) ? 'Cambios pendientes' : 'Sin valorar' }}</span>
              </div>
              <button :id="`guidance-help-${component.id}`" class="guidance-help-button" type="button" @click="helpComponent = component">Ayuda del componente</button>

              <fieldset class="valuation-controls" :disabled="!canEdit">
              <div class="valuation-fields">
                <label>
                  Estado de valoración
                  <select v-model.number="component.level" @change="changed(component)">
                    <option value="">Selecciona un estado</option>
                    <option v-for="state in valuationStates" :key="state.state" :value="state.level">{{ state.label }}</option>
                  </select>
                </label>
                <label class="note-field">
                  Evidencia: enlace o descripción (opcional)
                  <textarea v-model="component.evidenceUrl" class="evidence-description" rows="4" maxlength="10000" placeholder="Escribe un enlace, describe tus evidencias o combina ambos. Para documentos físicos, indica nombre, fecha y ubicación." @input="changed(component)"></textarea>
                  <span>Puedes describir documentos en papel sin adjuntar escaneos.</span>
                </label>
                <label class="note-field">
                  Nota sobre la evidencia (opcional)
                  <textarea v-model="component.evidenceNote" rows="3" maxlength="10000" @input="changed(component)"></textarea>
                </label>
                <label class="note-field">
                  Fortalezas (opcional en el borrador)
                  <textarea v-model="component.strengths" rows="3" maxlength="10000" :disabled="!allowsStrengths(component.level)" @input="changed(component)"></textarea>
                  <span v-if="component.level === 1">Existencia no permite registrar fortalezas.</span>
                </label>
                <label class="note-field">
                  Oportunidades de mejora (opcional en el borrador)
                  <textarea v-model="component.improvementOpportunities" rows="3" maxlength="10000" :disabled="!allowsImprovementOpportunities(component.level)" @input="changed(component)"></textarea>
                  <span v-if="component.level === 4">Mejoramiento continuo no permite registrar oportunidades de mejora.</span>
                </label>
              </div>
              <p v-if="component.level !== '' && !allowsStrengths(component.level) && component.strengths.trim()">
                Este estado no admite la fortaleza escrita. Puedes volver a un estado compatible o quitarla y guardar.
                <button class="secondary-button" type="button" @click="clearNarrative(component, 'strengths')">Quitar fortaleza</button>
              </p>
              <p v-if="component.level !== '' && !allowsImprovementOpportunities(component.level) && component.improvementOpportunities.trim()">
                Este estado no admite la oportunidad escrita. Puedes volver a un estado compatible o quitarla y guardar.
                <button class="secondary-button" type="button" @click="clearNarrative(component, 'improvementOpportunities')">Quitar oportunidad de mejora</button>
              </p>
              <p v-if="component.error" class="form-error" role="alert">{{ component.error }}</p>
              <button class="secondary-button" type="button" :disabled="component.saving || component.level === ''" @click="save(component)">
                {{ component.saving ? 'Guardando…' : component.error ? 'Reintentar' : 'Guardar ahora' }}
              </button>
              </fieldset>
            </article>
            <ValuationTotals title="Subtotal del proceso" :valuations="process.valuations" />
          </div>
          <ValuationTotals title="Total del área" :valuations="selectedArea.valuations" />
        </section>
      </template>
    </div>
    <ComponentGuidanceDialog v-if="helpComponent" :component-id="helpComponent.id" :component-name="helpComponent.name" @close="helpComponent = null" />
  </main>
</template>

<style scoped>
.valuation-controls { margin: 0; padding: 0; border: 0; min-width: 0; }
.valuation-controls:disabled input, .valuation-controls:disabled select,
.valuation-controls:disabled textarea { background: var(--color-background-mute); color: var(--color-text-disabled); cursor: not-allowed; }
.submission-card { border-top: 4px solid var(--sem-blue); }
.submission-card h2 { color: var(--color-text-highlight); }
.submission-card p { margin-top: .7rem; }
.submission-action { display: inline-block; margin-top: 1rem; padding: .75rem 1rem; border: 1px solid var(--sem-blue); border-radius: .5rem; background: var(--sem-blue); color: var(--color-text-white); font: inherit; font-weight: 700; text-decoration: none; }
.submission-action:disabled { opacity: .6; cursor: not-allowed; }
.submission-action:focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 3px; }
.submission-confirmed, .save-status { color: var(--color-text-highlight); }
.save-status { flex: 0 0 auto; font-size: .8rem; font-weight: 700; }
.save-status-error { color: var(--sem-red); }
.guidance-help-button { margin-top: .8rem; padding: .5rem .75rem; border: 1px solid var(--sem-blue); border-radius: .4rem; background: var(--color-background-top); color: var(--color-text-highlight); font: inherit; font-size: .82rem; font-weight: 700; }
.guidance-help-button:focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 3px; }
</style>
