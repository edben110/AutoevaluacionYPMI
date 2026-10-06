<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import ValuationTotals from '@/components/ValuationTotals.vue'
import { valuationStates, type Valuation, type ValuationLevel } from '@/selfEvaluation/valuation'
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
  saved: boolean
  saving: boolean
  error: string
}

const router = useRouter()
const year = ref(new Date().getFullYear())
const loading = ref(true)
const creating = ref(false)
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
    .filter((area) => area.processes.length > 0),
)
const valuedCount = computed(() =>
  components.value.filter((component) =>
    evaluation.value?.valuations.some((valuation) => valuation.componentId === component.id),
  ).length,
)

async function readJson<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await authorizedFetch(path, options)
  if (!response.ok) throw await responseError(response)
  return (await response.json()) as T
}

async function loadCatalog(draft: Evaluation): Promise<void> {
  const [areaRows, processRows, componentRows] = await Promise.all([
    readJson<Area[]>('/api/areas?onlyActive=true'),
    readJson<Process[]>('/api/processes?onlyActive=true'),
    readJson<Component[]>('/api/components?onlyActive=true'),
  ])
  areas.value = areaRows
  processes.value = processRows
  components.value = componentRows.map((component) => {
    const valuation = draft.valuations.find((item) => item.componentId === component.id)
    return {
      ...component,
      level: valuation?.level ?? '',
      evidenceUrl: valuation?.evidenceUrl ?? '',
      evidenceNote: valuation?.evidenceNote ?? '',
      saved: Boolean(valuation),
      saving: false,
      error: '',
    }
  })
}

async function loadYear(): Promise<void> {
  error.value = ''
  evaluation.value = null
  components.value = []
  if (!Number.isInteger(year.value) || year.value < 1) {
    error.value = 'Escribe un año válido.'
    return
  }
  loading.value = true
  try {
    const response = await authorizedFetch(`/api/self-evaluations/${year.value}`)
    if (response.status === 404) return
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

async function createDraft(): Promise<void> {
  creating.value = true
  error.value = ''
  try {
    const draft = await readJson<Evaluation>(`/api/self-evaluations/${year.value}`, { method: 'PUT' })
    evaluation.value = draft
    await loadCatalog(draft)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'No se pudo crear el borrador.'
  } finally {
    creating.value = false
  }
}

async function save(component: ComponentForm): Promise<void> {
  if (!evaluation.value || component.level === '') return
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
  } finally {
    component.saving = false
  }
}

onMounted(async () => {
  try {
    const profile = await loadProfile()
    if (profile.role !== 'ESTABLISHMENT') {
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
      <p>Registra el avance de tu institución durante el año seleccionado. Puedes volver a este borrador y continuar después.</p>

      <form class="year-form" @submit.prevent="loadYear">
        <label for="evaluation-year">Año de la autoevaluación</label>
        <input id="evaluation-year" v-model.number="year" type="number" min="1" required />
        <button class="secondary-button" type="submit">Consultar</button>
      </form>

      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <p v-if="loading" class="status-card" role="status">Cargando autoevaluación…</p>

      <section v-else-if="!evaluation" class="evaluation-empty">
        <h2>Aún no hay un borrador para {{ year }}</h2>
        <p>Créalo para comenzar a valorar los componentes de gestión.</p>
        <button class="primary-button" type="button" :disabled="creating" @click="createDraft">
          {{ creating ? 'Creando…' : 'Crear borrador' }}
        </button>
      </section>

      <template v-else>
        <div class="evaluation-summary">
          <div>
            <strong>Borrador {{ evaluation.year }}</strong>
            <span>Se guarda por institución y año.</span>
          </div>
          <span>{{ valuedCount }} de {{ components.length }} componentes valorados</span>
        </div>

        <section class="status-card" aria-label="Conteo de componentes por estado">
          <h2>Componentes valorados por estado</h2>
          <ValuationTotals title="Total del borrador" :valuations="evaluation.valuations" />
          <p>Cada componente guardado cuenta una vez en el estado elegido por la institución. El conteo incluye todas las valoraciones del borrador, incluso si un componente fue desactivado después.</p>
        </section>

        <section v-if="components.length === 0" class="evaluation-empty">
          <h2>El catálogo todavía no tiene componentes activos</h2>
          <p>El borrador ya está guardado. Podrás valorar los componentes cuando se cargue el catálogo de la Guía 34.</p>
        </section>

        <section v-for="area in groups" :key="area.id" class="evaluation-area">
          <h2>{{ area.name }}</h2>
          <div v-for="process in area.processes" :key="process.id" class="evaluation-process">
            <h3>{{ process.name }}</h3>
            <article v-for="component in process.components" :key="component.id" class="evaluation-component">
              <div class="component-heading">
                <div>
                  <h4>{{ component.name }}</h4>
                  <p v-if="component.description">{{ component.description }}</p>
                </div>
                <span v-if="component.saved" class="saved-label">Guardado</span>
              </div>

              <div class="valuation-fields">
                <label>
                  Estado de valoración
                  <select v-model.number="component.level" @change="component.saved = false">
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
              </div>
              <p v-if="component.error" class="form-error" role="alert">{{ component.error }}</p>
              <button class="secondary-button" type="button" :disabled="component.saving || component.level === ''" @click="save(component)">
                {{ component.saving ? 'Guardando…' : 'Guardar valoración' }}
              </button>
            </article>
            <ValuationTotals title="Subtotal del proceso" :valuations="process.valuations" />
          </div>
          <ValuationTotals title="Total del área" :valuations="area.valuations" />
        </section>
      </template>
    </div>
  </main>
</template>
