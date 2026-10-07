<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import ValuationTotals from '@/components/ValuationTotals.vue'
import EvaluationPeriodNotice from '@/components/EvaluationPeriodNotice.vue'
import { loadEvaluationPeriod, type EvaluationPeriod } from '@/selfEvaluation/period'
import {
  allowsStrengths,
  allowsImprovementOpportunities,
  valuationStates,
  type Valuation,
  type ValuationLevel,
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
}

const router = useRouter()
const route = useRoute()
const areaId = computed(() => typeof route.params.areaId === 'string' ? route.params.areaId : '')
const requestedYear = Number(route.query.year)
const year = ref(Number.isInteger(requestedYear) && requestedYear > 0 && requestedYear <= 9999 ? requestedYear : new Date().getFullYear())
const period = ref<EvaluationPeriod | null>(null)
const canEdit = computed(() => period.value?.writable === true && !loading.value)
const loading = ref(true)
const error = ref('')
const evaluation = ref<Evaluation | null>(null)
const areas = ref<Area[]>([])
const processes = ref<Process[]>([])
const components = ref<ComponentForm[]>([])

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

async function readJson<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await authorizedFetch(path, options)
  if (!response.ok) throw await responseError(response)
  return (await response.json()) as T
}

async function loadCatalog(draft: Evaluation | null): Promise<void> {
  const [areaRows, processRows, componentRows] = await Promise.all([
    readJson<Area[]>('/api/areas?onlyActive=true'),
    readJson<Process[]>('/api/processes?onlyActive=true'),
    readJson<Component[]>('/api/components?onlyActive=true'),
  ])
  areas.value = areaRows
  if (!areaRows.some((area) => area.id === areaId.value)) {
    throw new Error('El área seleccionada no está disponible. Vuelve a tu espacio y elige un área activa.')
  }
  processes.value = processRows
  components.value = componentRows.map((component) => {
    const valuation = draft?.valuations.find((item) => item.componentId === component.id)
    return {
      ...component,
      level: valuation?.level ?? '',
      evidenceUrl: valuation?.evidenceUrl ?? '',
      evidenceNote: valuation?.evidenceNote ?? '',
      strengths: valuation?.strengths ?? '',
      improvementOpportunities: valuation?.improvementOpportunities ?? '',
      saved: Boolean(valuation),
      saving: false,
      error: '',
    }
  })
}

async function loadYear(): Promise<void> {
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
    period.value = await loadEvaluationPeriod(year.value)
    if (!period.value?.writable) return
    const response = await authorizedFetch(`/api/self-evaluations/${year.value}`)
    if (response.status === 404) {
      await loadCatalog(null)
      return
    }
    if (!response.ok) throw await responseError(response)
    const draft = (await response.json()) as Evaluation
    evaluation.value = draft
    await loadCatalog(draft)
  } catch (cause) {
    if (!hasSession.value) {
      await router.replace({ name: 'login' })
      return
    }
    error.value = cause instanceof Error ? cause.message : 'No se pudo cargar la autoevaluación.'
  } finally {
    loading.value = false
  }
}

function clearNarrative(component: ComponentForm, field: 'strengths' | 'improvementOpportunities'): void {
  if (!canEdit.value) return
  component[field] = ''
  component.saved = false
  component.error = ''
}

async function save(component: ComponentForm): Promise<void> {
  if (!canEdit.value || !evaluation.value || component.level === '') return
  if (!allowsStrengths(component.level) && component.strengths.trim()) {
    component.error = 'Existencia no permite fortalezas. Quita ese texto antes de guardar.'
    return
  }
  if (!allowsImprovementOpportunities(component.level) && component.improvementOpportunities.trim()) {
    component.error = 'Mejoramiento continuo no permite oportunidades de mejora. Quita ese texto antes de guardar.'
    return
  }
  component.saving = true
  component.error = ''
  try {
    const valuation = await readJson<Valuation>(
      `/api/self-evaluations/${evaluation.value.year}/valuations/${component.id}`,
      {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          level: component.level,
          evidenceUrl: component.evidenceUrl,
          evidenceNote: component.evidenceNote,
          strengths: component.strengths,
          improvementOpportunities: component.improvementOpportunities,
        }),
      },
    )
    component.saved = true
    evaluation.value.valuations = [
      ...evaluation.value.valuations.filter((item) => item.componentId !== component.id),
      valuation,
    ]
  } catch (cause) {
    component.error = cause instanceof Error ? cause.message : 'No se pudo guardar la valoración.'
    await refreshPeriod()
  } finally {
    component.saving = false
  }
}

async function refreshPeriod(): Promise<void> {
  try { period.value = await loadEvaluationPeriod(year.value) }
  catch { period.value = null }
}

async function refreshAvailability(): Promise<void> {
  if (loading.value || currentProfile.value?.role !== 'ESTABLISHMENT') return
  const wasWritable = period.value?.writable === true
  await refreshPeriod()
  if (!wasWritable && period.value?.writable) await loadYear()
}

onMounted(async () => {
  window.addEventListener('focus', refreshAvailability)
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
onUnmounted(() => window.removeEventListener('focus', refreshAvailability))

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
      <p v-if="canEdit">Valora los componentes de esta área y guarda tus avances para continuar después.</p>

      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <p v-if="loading" class="status-card" role="status">Cargando autoevaluación…</p>

      <EvaluationPeriodNotice v-if="!loading && canEdit" :period="period" :year="year" />

      <section v-if="!loading && !error && !canEdit" class="evaluation-empty">
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

      <template v-else-if="!loading && !error && canEdit && selectedArea && evaluation">
        <div class="evaluation-summary">
          <div>
            <strong>Borrador {{ evaluation.year }}</strong>
            <span>{{ selectedArea.name }}</span>
          </div>
          <span>{{ valuedCount }} de {{ areaComponentCount }} componentes valorados</span>
        </div>

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
                <span v-if="component.saved" class="saved-label">Guardado</span>
              </div>

              <fieldset class="valuation-controls" :disabled="!canEdit || component.saving">
              <div class="valuation-fields">
                <label>
                  Estado de valoración
                  <select v-model.number="component.level" :disabled="component.saving" @change="component.saved = false; component.error = ''">
                    <option value="">Selecciona un estado</option>
                    <option v-for="state in valuationStates" :key="state.state" :value="state.level">{{ state.label }}</option>
                  </select>
                </label>
                <label>
                  Enlace de evidencia (opcional)
                  <input v-model="component.evidenceUrl" type="url" maxlength="1000" placeholder="https://…" @input="component.saved = false" />
                </label>
                <label class="note-field">
                  Nota sobre la evidencia (opcional)
                  <textarea v-model="component.evidenceNote" rows="3" maxlength="10000" @input="component.saved = false"></textarea>
                </label>
                <label class="note-field">
                  Fortalezas (opcional en el borrador)
                  <textarea v-model="component.strengths" rows="3" maxlength="10000" :disabled="!allowsStrengths(component.level) || component.saving" @input="component.saved = false"></textarea>
                  <span v-if="component.level === 1">Existencia no permite registrar fortalezas.</span>
                </label>
                <label class="note-field">
                  Oportunidades de mejora (opcional en el borrador)
                  <textarea v-model="component.improvementOpportunities" rows="3" maxlength="10000" :disabled="!allowsImprovementOpportunities(component.level) || component.saving" @input="component.saved = false"></textarea>
                  <span v-if="component.level === 4">Mejoramiento continuo no permite registrar oportunidades de mejora.</span>
                </label>
              </div>
              <p v-if="component.level !== '' && !allowsStrengths(component.level) && component.strengths.trim()">
                Este estado no admite la fortaleza escrita. Puedes volver a un estado compatible o quitarla y guardar.
                <button class="secondary-button" type="button" :disabled="component.saving" @click="clearNarrative(component, 'strengths')">Quitar fortaleza</button>
              </p>
              <p v-if="component.level !== '' && !allowsImprovementOpportunities(component.level) && component.improvementOpportunities.trim()">
                Este estado no admite la oportunidad escrita. Puedes volver a un estado compatible o quitarla y guardar.
                <button class="secondary-button" type="button" :disabled="component.saving" @click="clearNarrative(component, 'improvementOpportunities')">Quitar oportunidad de mejora</button>
              </p>
              <p v-if="component.error" class="form-error" role="alert">{{ component.error }}</p>
              <button class="secondary-button" type="button" :disabled="component.saving || component.level === ''" @click="save(component)">
                {{ component.saving ? 'Guardando…' : 'Guardar valoración' }}
              </button>
              </fieldset>
            </article>
            <ValuationTotals title="Subtotal del proceso" :valuations="process.valuations" />
          </div>
          <ValuationTotals title="Total del área" :valuations="selectedArea.valuations" />
        </section>
      </template>
    </div>
  </main>
</template>

<style scoped>
.valuation-controls { margin: 0; padding: 0; border: 0; min-width: 0; }
.valuation-controls:disabled input, .valuation-controls:disabled select,
.valuation-controls:disabled textarea { background: #edf1ed; color: #627369; cursor: not-allowed; }
</style>
