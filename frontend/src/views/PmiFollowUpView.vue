<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, RouterLink, useRoute, useRouter } from 'vue-router'
import { authorizedFetch, hasSession, loadProfile, responseError } from '@/auth/session'
import { formatPeriodDate } from '@/selfEvaluation/period'
import { executionLabels, type ExecutionStatus, type FollowUpRow, type PmiFollowUp } from '@/pmi/followUp'
import PmiSubmissionPanel from '@/components/PmiSubmissionPanel.vue'

interface RowForm {
  source: FollowUpRow
  status: ExecutionStatus | ''
  percentage: string
  observations: string
  snapshot: string
  saving: boolean
  error: string
  success: string
}
const route = useRoute()
const router = useRouter()
const year = computed(() => Number(route.params.year))
const areaId = computed(() => String(route.params.areaId ?? ''))
const apiBase = computed(() => `/api/pmi/${year.value}/areas/${encodeURIComponent(areaId.value)}/follow-ups`)
const source = ref<PmiFollowUp | null>(null)
const rows = reactive<RowForm[]>([])
const selectedDate = ref(today())
const historyDate = ref('')
const loading = ref(true)
const authorized = ref(false)
const verified = ref(false)
const finalizing = ref(false)
const error = ref('')
const saving = computed(() => finalizing.value || rows.some(row => row.saving))
const hasChanges = computed(() => rows.some(changed))
const datePending = computed(() => source.value !== null && selectedDate.value !== source.value.controlDate)
const statusCounts = computed(() => Object.entries(executionLabels).map(([code, label]) => ({ code, label,
  count: rows.filter(row => row.source.id !== null && row.source.executionStatus === code).length,
})))
const completedForReview = computed(() => rows.filter(form => !form.source.sourceRemoved && !form.source.sourceChanged
  && form.source.id !== null && form.source.executionStatus !== null && form.source.executionPercentage !== null
  && Boolean(form.source.observations?.trim())).length)
const groups = computed(() => {
  const groups: { key: string; context: FollowUpRow['source']; goals: { key: string; description: string | null; rows: RowForm[] }[] }[] = []
  for (const form of rows) {
    const item = form.source.source
    let group = groups.find(group => group.key === item.objectiveId)
    if (!group) { group = { key: item.objectiveId, context: item, goals: [] }; groups.push(group) }
    let goal = group.goals.find(goal => goal.key === item.goalId)
    if (!goal) { goal = { key: item.goalId, description: item.goalDescription, rows: [] }; group.goals.push(goal) }
    goal.rows.push(form)
  }
  return groups
})
let sequence = 0
let request: AbortController | null = null

function today(): string {
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Bogota', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date())
  const value = (type: string) => parts.find(part => part.type === type)?.value ?? ''
  return `${value('year')}-${value('month')}-${value('day')}`
}
function snapshot(form: RowForm): string {
  return JSON.stringify({ status: form.status, percentage: form.percentage, observations: form.observations })
}
function formFor(row: FollowUpRow): RowForm {
  const form: RowForm = { source: row, status: row.executionStatus ?? '', percentage: row.executionPercentage ?? '',
    observations: row.observations ?? '', snapshot: '', saving: false, error: '', success: '' }
  form.snapshot = snapshot(form)
  return form
}
function changed(form: RowForm): boolean { return snapshot(form) !== form.snapshot }
function rowDisabled(form: RowForm): boolean {
  return source.value?.phase !== 'FOLLOW_UP' || !source.value.canEdit || !form.source.editable
    || loading.value || !verified.value || datePending.value || form.saving || finalizing.value
}
function confirmDiscard(): boolean {
  if (!hasSession.value) return true
  if (saving.value) { window.alert('Espera a que termine el guardado del seguimiento antes de cambiar de fecha o salir.'); return false }
  return !hasChanges.value || window.confirm('Hay avances sin guardar en esta fecha de control. ¿Descartarlos y continuar?')
}
function beforeUnload(event: BeforeUnloadEvent): void {
  if (!hasSession.value || (!hasChanges.value && !saving.value)) return
  event.preventDefault(); event.returnValue = ''
}
async function load(clear = false): Promise<void> {
  const current = ++sequence
  request?.abort(); request = new AbortController()
  const signal = request.signal
  loading.value = true; verified.value = false; error.value = ''
  if (clear) { source.value = null; rows.splice(0) }
  try {
    if (!Number.isInteger(year.value) || year.value < 1 || year.value > 9999 || !areaId.value) throw new Error('Selecciona un año y un área válidos del PMI.')
    if (!/^\d{4}-\d{2}-\d{2}$/.test(selectedDate.value)) throw new Error('Indica una fecha de control válida.')
    const response = await authorizedFetch(`${apiBase.value}?controlDate=${encodeURIComponent(selectedDate.value)}`, { signal })
    if (!response.ok) throw await responseError(response)
    const data = await response.json() as PmiFollowUp
    if (current !== sequence) return
    source.value = data; selectedDate.value = data.controlDate
    historyDate.value = data.availableDates.includes(data.controlDate) ? data.controlDate : ''
    rows.splice(0, rows.length, ...data.rows.map(formFor))
    verified.value = true
  } catch (cause) {
    if (current !== sequence || signal.aborted) return
    error.value = cause instanceof Error ? cause.message : 'No se pudo consultar el seguimiento.'
    if (!hasSession.value) await router.replace({ name: 'login' })
  } finally { if (current === sequence) loading.value = false }
}
async function initialize(): Promise<void> {
  loading.value = true; error.value = ''
  try {
    const profile = await loadProfile()
    if (profile.role !== 'ESTABLISHMENT') { await router.replace({ name: 'dashboard' }); return }
    authorized.value = true
    await load(true)
  } catch (cause) {
    loading.value = false
    error.value = cause instanceof Error ? cause.message : 'No se pudo abrir el Anexo 5.'
    if (!hasSession.value) await router.replace({ name: 'login' })
  }
}
async function changeDate(): Promise<void> {
  if (loading.value || !confirmDiscard()) return
  if (!authorized.value) await initialize()
  else await load()
}
function pickHistory(): void { if (historyDate.value) selectedDate.value = historyDate.value }
async function save(form: RowForm): Promise<void> {
  if (loading.value || !verified.value || datePending.value || form.saving || finalizing.value || !source.value?.canEdit || !form.source.editable) return
  form.error = ''; form.success = ''
  const percentage = form.percentage.trim().replace(',', '.')
  if (percentage && (!/^\d{1,3}(?:\.\d{1,2})?$/.test(percentage) || Number(percentage) > 100)) {
    form.error = 'Escribe un porcentaje entre 0 y 100 con hasta dos decimales. Por ejemplo, 40 significa 40 %.'
    return
  }
  const current = sequence
  const controlDate = source.value.controlDate
  const path = `${apiBase.value}/${controlDate}/rows/${encodeURIComponent(form.source.rowId)}`
  form.saving = true
  try {
    const response = await authorizedFetch(path, {
      method: 'PUT', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ executionStatus: form.status || null, executionPercentage: percentage || null, observations: form.observations.trim() || null }),
    })
    if (!response.ok) throw await responseError(response)
    const saved = await response.json() as FollowUpRow
    if (current !== sequence) return
    const index = rows.findIndex(row => row.source.rowId === saved.rowId)
    if (index >= 0) rows.splice(index, 1, { ...formFor(saved), success: `Avance guardado para el ${formatPeriodDate(controlDate)}.` })
    if (source.value && !source.value.availableDates.includes(controlDate)) source.value.availableDates = [...source.value.availableDates, controlDate].sort().reverse()
    historyDate.value = controlDate
  } catch (cause) {
    if (current === sequence) form.error = cause instanceof Error ? cause.message : 'No se pudo guardar el avance. Tus respuestas siguen aquí.'
    if (!hasSession.value) await router.replace({ name: 'login' })
  } finally { if (current === sequence) form.saving = false }
}
async function finalize(): Promise<void> {
  if (loading.value || saving.value || !source.value?.canEdit || datePending.value) return
  if (hasChanges.value) { error.value = 'Guarda los cambios pendientes de cada fila antes de finalizar el seguimiento.'; return }
  if (!window.confirm(`¿Finalizar el seguimiento de esta área del PMI ${year.value}? Deben estar guardados el estado, porcentaje y observaciones de todas sus filas. Después quedará para consulta.`)) return
  const current = sequence
  finalizing.value = true; error.value = ''
  try {
    const response = await authorizedFetch(`${apiBase.value}/${source.value.controlDate}/complete`, { method: 'POST' })
    if (!response.ok) throw await responseError(response)
    const data = await response.json() as PmiFollowUp
    if (current !== sequence) return
    source.value = data; rows.splice(0, rows.length, ...data.rows.map(formFor))
  } catch (cause) {
    if (current === sequence) error.value = cause instanceof Error ? cause.message : 'No se pudo finalizar el seguimiento.'
    if (!hasSession.value) await router.replace({ name: 'login' })
  } finally { if (current === sequence) finalizing.value = false }
}
function discard(form: RowForm): void {
  if (!form.saving && window.confirm('¿Descartar los cambios sin guardar de esta fila de seguimiento?')) rows.splice(rows.indexOf(form), 1, formFor(form.source))
}
onMounted(() => { window.addEventListener('beforeunload', beforeUnload); void initialize() })
watch(() => [route.params.year, route.params.areaId], () => {
  if (authorized.value) { selectedDate.value = today(); historyDate.value = ''; void load(true) }
})
onBeforeRouteLeave(confirmDiscard)
onBeforeRouteUpdate((to, from) => (to.params.year === from.params.year && to.params.areaId === from.params.areaId) || confirmDiscard())
onUnmounted(() => { sequence++; request?.abort(); window.removeEventListener('beforeunload', beforeUnload) })
</script>

<template>
  <main class="follow-up-page">
    <header class="page-header"><RouterLink :to="{ name: 'pmi-indicators', params: { year: route.params.year, areaId } }">← Anexo 4</RouterLink><RouterLink :to="{ name: 'pmi-formulation', params: { year: route.params.year, areaId } }">Plan · Anexo 3</RouterLink><RouterLink :to="{ name: 'dashboard' }">Mi espacio</RouterLink></header>
    <div class="page-content">
      <span class="eyebrow">PLAN DE MEJORAMIENTO INSTITUCIONAL · ANEXO 5</span>
      <h1>Revisión del cumplimiento del PMI {{ year }}</h1>
      <h2 v-if="source">{{ source.areaName }}</h2>
      <p class="intro">Aquí se reúne lo planeado en los anexos anteriores, organizado por proceso, componente, objetivo y meta.</p>
      <p v-if="source" class="metadata"><strong>{{ source.establishmentName }}</strong> · PMI {{ source.year }} · Seguimiento previo a la autoevaluación {{ source.reviewYear }}</p>
      <p v-if="loading" class="notice" role="status">Cargando el Anexo 5…</p>
      <div v-if="error" class="notice error" role="alert"><p>{{ error }}</p><button type="button" :disabled="loading || saving" @click="changeDate">Volver a consultar</button></div>
      <template v-if="authorized && source">
        <PmiSubmissionPanel :year="year" :area-id="areaId" @submitted="load()" />
        <p v-if="source.phase === 'PLANNING'" class="notice phase-notice" role="status">Este anexo está disponible para consulta durante la formulación del PMI {{ year }}. Su diligenciamiento se habilitará cuando Secretaría abra el período de autoevaluación {{ source.reviewYear }}.</p>
        <p v-else-if="source.phase === 'CLOSED'" class="notice warning" role="status">El período {{ source.reviewYear }} está cerrado. Secretaría debe habilitar sus fechas para continuar con el seguimiento.</p>
        <section v-else-if="source.phase === 'COMPLETED'" class="summary-card completed-follow-up" role="status"><h2>Seguimiento del área finalizado</h2><p>Este Anexo 5 quedó para consulta. Finaliza las demás áreas pendientes para habilitar la autoevaluación {{ source.reviewYear }}.</p><RouterLink class="u-return-button" :to="{ name: 'dashboard' }">Volver a mi espacio</RouterLink></section>
        <p v-else class="notice" role="status">Antes de la autoevaluación {{ source.reviewYear }}, diligencia el estado, porcentaje y observaciones de cada fila y finaliza el seguimiento de esta área.</p>

        <form v-if="source.phase === 'FOLLOW_UP'" class="date-card" @submit.prevent="changeDate">
          <label for="follow-up-date">Fecha de control<input id="follow-up-date" v-model="selectedDate" type="date" required min="0001-01-01" max="9999-12-31" :disabled="loading || saving" /></label>
          <label v-if="source.availableDates.length" for="follow-up-history">Controles guardados<select id="follow-up-history" v-model="historyDate" :disabled="loading || saving" @change="pickHistory"><option value="">Elige una fecha</option><option v-for="date in source.availableDates" :key="date" :value="date">{{ formatPeriodDate(date) }}</option></select></label>
          <button type="submit" :disabled="loading || saving">{{ loading ? 'Cargando…' : 'Abrir fecha de control' }}</button>
          <p>Usa una fecha dentro del período abierto de {{ source.reviewYear }}. Todas las filas deben quedar completas en el mismo control para finalizar el área.</p>
        </form>
        <p v-if="datePending" class="notice warning">Aún se muestra el control del {{ formatPeriodDate(source.controlDate) }}. Abre la fecha seleccionada antes de guardar.</p>
        <section v-if="source.phase !== 'PLANNING'" class="summary-card" aria-label="Avance guardado del seguimiento"><h2>Control del {{ formatPeriodDate(source.controlDate) }}</h2><p>{{ completedForReview }} de {{ source.requiredRows }} filas completas con estado, porcentaje y observaciones.</p><div class="status-summary"><span v-for="status in statusCounts" :key="status.code"><strong>{{ status.count }}</strong> {{ status.label }}</span></div></section>
        <section v-if="rows.length === 0 && !loading" class="empty-state"><h2>Aún no hay indicadores o actividades formulados</h2><p>Completa y guarda el plan en el Anexo 3 para que aparezca aquí su consolidado.</p><RouterLink :to="{ name: 'pmi-formulation', params: { year, areaId } }">Ir al Anexo 3 →</RouterLink></section>

        <template v-if="rows.length">
          <p class="conventions"><strong>Convenciones:</strong> <span v-for="(label, code) in executionLabels" :key="code"><strong>{{ code }}:</strong> {{ label }}.</span> <strong>% EJ:</strong> Porcentaje de ejecución.</p>
          <p v-if="source.phase === 'PLANNING'" class="locked-hint">Las columnas de ejecución y observaciones están bloqueadas hasta el período {{ source.reviewYear }}. No se ha asignado un estado ni un porcentaje automáticamente.</p>
          <p v-else-if="source.phase === 'FOLLOW_UP' && source.canEdit" class="hint">Marca un solo estado por fila e informa el porcentaje de 0 a 100 y las observaciones. Puedes guardar borradores; los tres datos son obligatorios para finalizar.</p>
          <p id="annex5-table-help" class="table-hint">Desplázate horizontalmente dentro de la tabla para consultar todas las columnas. La información del plan se toma del Anexo 3.</p>
          <div class="annex-table-wrapper" role="region" aria-label="Tabla del Anexo 5" aria-describedby="annex5-table-help" tabindex="0">
            <table class="annex-table">
              <caption>Revisión del cumplimiento del Plan de Mejoramiento Institucional · {{ source.areaName }}</caption>
              <colgroup>
                <col class="process-column" /><col class="component-column" /><col class="objective-column" /><col class="goal-column" />
                <col class="indicator-column" /><col class="activities-column" /><col span="2" class="date-column" />
                <col span="5" class="status-column" /><col class="percentage-column" /><col class="observations-column" />
                <col v-if="source.phase === 'FOLLOW_UP' && source.canEdit" class="actions-column" />
              </colgroup>
              <thead>
                <tr>
                  <th rowspan="2" scope="col">Proceso</th><th rowspan="2" scope="col">Componente</th><th rowspan="2" scope="col">Objetivo</th><th rowspan="2" scope="col">Meta</th><th rowspan="2" scope="col">Indicador</th><th rowspan="2" scope="col">Actividades</th>
                  <th colspan="2" scope="colgroup">Plazo</th><th colspan="6" scope="colgroup">Estado de ejecución</th><th rowspan="2" scope="col">Observaciones y evidencias</th>
                  <th v-if="source.phase === 'FOLLOW_UP' && source.canEdit" rowspan="2" scope="col">Guardar avance</th>
                </tr>
                <tr><th scope="col">Inicio</th><th scope="col">Final</th><th v-for="(label, code) in executionLabels" :key="code" scope="col" :title="label"><abbr :title="label">{{ code }}</abbr></th><th scope="col">% EJ</th></tr>
              </thead>
              <tbody v-for="group in groups" :key="group.key" class="objective-group">
                <template v-for="(goal, goalIndex) in group.goals" :key="goal.key">
                  <tr v-for="(form, rowIndex) in goal.rows" :id="`follow-up-${form.source.rowId}`" :key="form.source.rowId" class="indicator-follow-up" :class="{ 'goal-start': rowIndex === 0 }">
                    <template v-if="goalIndex === 0 && rowIndex === 0">
                      <td :rowspan="group.goals.reduce((count, item) => count + item.rows.length, 0)" class="process-cell">{{ group.context.processName }}</td>
                      <td :rowspan="group.goals.reduce((count, item) => count + item.rows.length, 0)" class="component-cell">{{ group.context.componentName }}</td>
                      <td :rowspan="group.goals.reduce((count, item) => count + item.rows.length, 0)" class="planned-text">{{ group.context.objectiveDescription || 'Pendiente en el Anexo 3' }}</td>
                    </template>
                    <td v-if="rowIndex === 0" :rowspan="goal.rows.length" class="planned-text">{{ goal.description || 'Pendiente en el Anexo 3' }}</td>
                    <th scope="row" class="indicator-cell">
                      <p :id="`row-title-${form.source.rowId}`">{{ form.source.source.indicatorName || 'Sin nombre en el Anexo 3' }}</p>
                      <p v-if="form.source.sourceChanged" class="notice warning">El plan cambió después de registrar este control. Abre una fecha nueva para registrar el plan actualizado.</p>
                      <p v-if="form.source.sourceRemoved" class="notice warning">Esta fila se retiró del plan. Se conserva su control para consulta.</p>
                    </th>
                    <td class="planned-text">{{ form.source.source.activities || 'Sin actividades descritas en el Anexo 3' }}</td>
                    <td>{{ form.source.source.startDate ? formatPeriodDate(form.source.source.startDate) : 'Pendiente' }}</td>
                    <td>{{ form.source.source.finishDate ? formatPeriodDate(form.source.source.finishDate) : 'Pendiente' }}</td>
                    <td v-for="(label, code) in executionLabels" :key="code" class="status-cell" :class="{ 'locked-cell': rowDisabled(form) }">
                      <label class="status-option" :for="`execution-status-${form.source.rowId}-${code}`">
                        <span class="sr-only">{{ label }} · {{ form.source.source.indicatorName || 'Fila sin nombre' }}</span>
                        <input :id="`execution-status-${form.source.rowId}-${code}`" v-model="form.status" :name="`execution-status-${form.source.rowId}`" type="radio" :value="code" :disabled="rowDisabled(form)" :form="`follow-up-form-${form.source.rowId}`" @change="form.success = ''" />
                      </label>
                    </td>
                    <td :class="{ 'locked-cell': rowDisabled(form) }">
                      <label class="sr-only" :for="`execution-percentage-${form.source.rowId}`">Porcentaje de ejecución de {{ form.source.source.indicatorName || 'esta fila' }} (0 a 100)</label>
                      <input :id="`execution-percentage-${form.source.rowId}`" v-model="form.percentage" type="text" inputmode="decimal" maxlength="6" :placeholder="source.phase === 'PLANNING' ? 'Pendiente' : '0–100'" :disabled="rowDisabled(form)" :form="`follow-up-form-${form.source.rowId}`" @input="form.success = ''" />
                    </td>
                    <td :class="{ 'locked-cell': rowDisabled(form) }">
                      <label class="sr-only" :for="`observations-${form.source.rowId}`">Observaciones y evidencias de {{ form.source.source.indicatorName || 'esta fila' }}</label>
                      <textarea :id="`observations-${form.source.rowId}`" v-model="form.observations" rows="5" maxlength="10000" :placeholder="source.phase === 'PLANNING' ? `Se diligencia en el seguimiento del período ${source.reviewYear}.` : 'Explica los avances o dificultades e indica sus evidencias.'" :disabled="rowDisabled(form)" :form="`follow-up-form-${form.source.rowId}`" @input="form.success = ''" />
                    </td>
                    <td v-if="source.phase === 'FOLLOW_UP' && source.canEdit" class="row-actions">
                      <template v-if="form.source.editable">
                        <p class="save-state" role="status">{{ changed(form) ? 'Cambios sin guardar' : form.source.id ? 'Avance guardado' : 'Avance pendiente' }}</p>
                        <form :id="`follow-up-form-${form.source.rowId}`" @submit.prevent="save(form)">
                          <button type="submit" :disabled="rowDisabled(form)">{{ form.saving ? 'Guardando…' : 'Guardar avance de esta fila' }}</button>
                          <button v-if="changed(form)" type="button" class="outline" :disabled="rowDisabled(form)" @click="discard(form)">Descartar cambios</button>
                        </form>
                      </template>
                      <p v-else class="hint">Solo consulta</p>
                      <p v-if="form.error" class="notice error" role="alert">{{ form.error }}</p><p v-if="form.success" class="notice" role="status">{{ form.success }}</p>
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
        </template>
        <section v-if="source.phase === 'FOLLOW_UP'" class="summary-card completion-card"><h2>Finalizar seguimiento del área</h2><p>Guarda estado, porcentaje y observaciones de las {{ source.requiredRows }} filas en esta fecha. Completar el seguimiento no exige que todas las actividades hayan finalizado o estén al 100 %: registra su situación real.</p><p v-if="hasChanges" class="notice warning">Hay cambios sin guardar. Guárdalos antes de finalizar.</p><button id="complete-follow-up" type="button" :disabled="loading || saving || !source.canEdit || datePending || hasChanges || source.requiredRows === 0 || completedForReview !== source.requiredRows" @click="finalize">{{ finalizing ? 'Finalizando…' : 'Finalizar seguimiento del área' }}</button></section>
      </template>
    </div>
  </main>
</template>

<style scoped>
.planned-text { white-space: pre-wrap; overflow-wrap: anywhere; }
.completion-card button { margin-top: 1rem; }
.follow-up-page { min-width: 0; min-height: 100vh; background: var(--color-background); }
.page-header { display: flex; justify-content: space-between; flex-wrap: wrap; gap: 1rem; padding: 1.2rem clamp(1rem, 5vw, 5rem); background: var(--color-background-top); border-bottom: 1px solid var(--color-background-mute); }
a { color: var(--color-text-highlight); font-weight: 700; }
.page-content { width: min(100% - 3rem, 1600px); min-width: 0; margin: 3rem auto 5rem; }
.eyebrow { font-size: .74rem; font-weight: 700; letter-spacing: .08em; color: var(--color-text-highlight); }
h1 { color: var(--color-text-highlight); margin: .6rem 0; font-size: clamp(1.9rem, 4vw, 2.7rem); line-height: 1.2; }
h2 { color: var(--color-text-highlight); font-size: 1.2rem; }
.intro, .metadata { margin-top: .8rem; color: var(--color-text-disabled); }
.date-card, .summary-card, .empty-state { margin-top: 1.5rem; padding: 1.2rem; background: var(--color-background-top); border: 1px solid var(--color-background-mute); border-radius: .65rem; }
.date-card { display: flex; align-items: end; flex-wrap: wrap; gap: 1rem; }
.date-card p { flex-basis: 100%; font-size: .85rem; color: var(--color-text-disabled); }
.summary-card p, .empty-state p { margin: .6rem 0; }
.status-summary { display: flex; gap: .65rem; flex-wrap: wrap; margin: 1rem 0; }
.status-summary span { background: var(--color-background-mute); color: var(--sem-blue); padding: .4rem .7rem; border-radius: .35rem; font-size: .85rem; }
.status-summary strong { font-weight: 700; }
.conventions { display: flex; flex-wrap: wrap; gap: .35rem .7rem; margin: 1.5rem 0 .7rem; font-size: .85rem; }
.locked-hint { padding: .7rem 1rem; border-left: 4px solid var(--sem-yellow); background: var(--color-background-mute); font-size: .9rem; }
.table-hint { margin: 1rem 0 .5rem; color: var(--color-text-disabled); font-size: .85rem; }
.annex-table-wrapper { position: relative; max-width: 100%; overflow-x: auto; border: 1px solid var(--color-border-disabled); border-radius: .5rem; }
.annex-table { width: 100%; table-layout: fixed; border-collapse: collapse; background: var(--color-background-top); font-size: .88rem; line-height: 1.5; }
.annex-table caption { text-align: left; color: var(--color-text-highlight); font-weight: 700; padding: .9rem; background: var(--color-background-mute); }
.process-column { width: 160px; } .component-column { width: 185px; }
.objective-column, .goal-column { width: 240px; } .indicator-column { width: 210px; }
.activities-column { width: 280px; } .date-column { width: 105px; }
.status-column { width: 48px; } .percentage-column { width: 115px; }
.observations-column { width: 300px; } .actions-column { width: 190px; }
th, td { border: 1px solid var(--color-border-disabled); padding: .7rem; text-align: left; overflow-wrap: anywhere; vertical-align: top; }
thead { background: var(--sem-blue); color: var(--sem-white-top); }
thead th { text-align: center; vertical-align: middle; font-weight: 700; }
thead tr:last-child th { padding: .6rem .25rem; white-space: nowrap; }
abbr { text-decoration: none; }
.objective-group { border-top: 4px solid var(--sem-blue); }
.goal-start { border-top: 2px solid var(--color-border-disabled); }
.process-cell { color: var(--sem-white-top); background: var(--sem-blue); font-weight: 700; }
.component-cell { color: var(--sem-blue); background: var(--color-background-mute); font-weight: 700; }
.indicator-cell { color: var(--sem-blue); font-weight: 700; white-space: pre-wrap; }
.status-cell { padding: .25rem; text-align: center; }
.status-option { min-height: 44px; place-items: center; cursor: pointer; }
.status-option input { width: 1.1rem; height: 1.1rem; padding: 0; accent-color: var(--sem-blue); }
.locked-cell { background: var(--color-background-mute); }
.save-state { margin-bottom: .7rem; font-size: .8rem; font-weight: 700; color: var(--color-text-highlight); }
label { display: grid; gap: .45rem; color: var(--color-text-highlight); font-size: .9rem; font-weight: 700; }
input, select, textarea { width: 100%; min-width: 0; padding: .65rem; border: 1px solid var(--color-border-disabled); border-radius: .35rem; background: var(--color-background-top); color: var(--color-text); font: inherit; }
textarea { resize: vertical; }
small, .hint { color: var(--color-text-disabled); font-size: .8rem; font-weight: 400; }
small { display: block; margin-top: .35rem; }
.row-actions button { width: 100%; margin-bottom: .6rem; }
button { padding: .7rem 1rem; border: 1px solid var(--sem-blue); border-radius: .4rem; color: var(--sem-white-top); background: var(--sem-blue); font: inherit; font-weight: 700; cursor: pointer; }
button.outline { color: var(--sem-blue); background: var(--color-background-top); }
button:disabled { opacity: .6; cursor: default; }
input:disabled, textarea:disabled { background: var(--color-background-mute); color: var(--color-text); opacity: 1; cursor: not-allowed; }
input:disabled::placeholder, textarea:disabled::placeholder { color: var(--color-text-disabled); }
button:hover:enabled { filter: brightness(.92); }
:is(a, button, input, select, textarea, .annex-table-wrapper):focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 3px; }
.notice { margin: 1rem 0; padding: .9rem; border-left: 4px solid var(--sem-blue); background: var(--color-background-mute); overflow-wrap: anywhere; }
.notice.error { border-color: var(--sem-red); color: var(--color-text-red); }
.notice.warning { border-color: var(--sem-yellow); }
.notice button { margin-top: .7rem; }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip-path: inset(50%); white-space: nowrap; border: 0; }
@media (max-width: 600px) {
  .page-content { width: calc(100% - 1.5rem); margin-top: 1.8rem; }
  .date-card > label, .date-card > button { width: 100%; }
}
</style>
