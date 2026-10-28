<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, RouterLink, useRoute, useRouter } from 'vue-router'
import { authorizedFetch, hasSession, loadProfile, responseError } from '@/auth/session'
import { formatPeriodDate } from '@/selfEvaluation/period'
import { executionLabels, type ExecutionStatus, type FollowUpRow, type PmiFollowUp } from '@/pmi/followUp'

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
      <h1>Seguimiento del PMI {{ year }}</h1>
      <h2 v-if="source">{{ source.areaName }}</h2>
      <p class="intro">Aquí se reúne lo planeado en los anexos anteriores, organizado por proceso, componente, objetivo y meta.</p>
      <p v-if="source" class="metadata"><strong>{{ source.establishmentName }}</strong> · PMI {{ source.year }} · Seguimiento previo a la autoevaluación {{ source.reviewYear }}</p>
      <p v-if="loading" class="notice" role="status">Cargando el Anexo 5…</p>
      <div v-if="error" class="notice error" role="alert"><p>{{ error }}</p><button type="button" :disabled="loading || saving" @click="changeDate">Volver a consultar</button></div>
      <template v-if="authorized && source">
        <p v-if="source.phase === 'PLANNING'" class="notice phase-notice" role="status">Este anexo está disponible para consulta durante la formulación del PMI {{ year }}. Su diligenciamiento se habilitará cuando Secretaría abra el período de autoevaluación {{ source.reviewYear }}.</p>
        <p v-else-if="source.phase === 'CLOSED'" class="notice warning" role="status">El período {{ source.reviewYear }} está cerrado. Secretaría debe habilitar sus fechas para continuar con el seguimiento.</p>
        <section v-else-if="source.phase === 'COMPLETED'" class="summary-card completed-follow-up" role="status"><h2>Seguimiento del área finalizado</h2><p>Este Anexo 5 quedó para consulta. Finaliza las demás áreas pendientes para habilitar la autoevaluación {{ source.reviewYear }}.</p><RouterLink :to="{ name: 'dashboard' }">Volver a mi espacio →</RouterLink></section>
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

        <section v-for="group in groups" :key="group.key" class="objective-group">
          <header class="row-header"><span>PROCESO</span><h2>{{ group.context.processName }}</h2><p><strong>Componente:</strong> {{ group.context.componentName }}</p></header>
          <div class="row-content"><h3>Objetivo</h3><p class="planned-text">{{ group.context.objectiveDescription || 'Pendiente en el Anexo 3' }}</p>
            <section v-for="goal in group.goals" :key="goal.key" class="goal-group"><h4>Meta</h4><p class="planned-text">{{ goal.description || 'Pendiente en el Anexo 3' }}</p>
              <article v-for="form in goal.rows" :id="`follow-up-${form.source.rowId}`" :key="form.source.rowId" class="follow-up-card indicator-follow-up" :aria-labelledby="`row-title-${form.source.rowId}`">
                <div class="row-content">
                  <h5>Indicador</h5><p :id="`row-title-${form.source.rowId}`" class="indicator-title">{{ form.source.source.indicatorName || 'Sin nombre en el Anexo 3' }}</p>
                  <h5>Actividades previstas</h5><p class="planned-text">{{ form.source.source.activities || 'Sin actividades descritas en el Anexo 3' }}</p>
                  <table class="dates-table"><caption>Plazos del plan</caption><thead><tr><th scope="col">Inicio</th><th scope="col">Final</th></tr></thead><tbody><tr><td>{{ form.source.source.startDate ? formatPeriodDate(form.source.source.startDate) : 'Pendiente' }}</td><td>{{ form.source.source.finishDate ? formatPeriodDate(form.source.source.finishDate) : 'Pendiente' }}</td></tr></tbody></table>
                  <p v-if="form.source.sourceChanged" class="notice warning">El plan cambió después de registrar este control. Para finalizar el seguimiento, abre una fecha nueva y registra sus filas con el plan actualizado.</p>
                  <p v-if="form.source.sourceRemoved" class="notice warning">Esta fila se retiró del plan y su control anterior se conserva para consulta.</p>
                  <template v-if="source.canEdit && form.source.editable">
                    <p class="save-state" role="status">{{ changed(form) ? 'Cambios sin guardar' : form.source.id ? 'Avance guardado' : 'Avance pendiente' }}</p>
                    <form @submit.prevent="save(form)"><fieldset :disabled="loading || !verified || datePending || form.saving || finalizing">
                      <legend class="sr-only">Registrar avance de esta fila</legend>
                      <div class="execution-fields"><label :for="`execution-status-${form.source.rowId}`">Estado de ejecución<select :id="`execution-status-${form.source.rowId}`" v-model="form.status" @change="form.success = ''"><option value="">Sin registrar</option><option v-for="(label, code) in executionLabels" :key="code" :value="code">{{ label }} ({{ code }})</option></select></label><label :for="`execution-percentage-${form.source.rowId}`">Porcentaje de ejecución (%)<input :id="`execution-percentage-${form.source.rowId}`" v-model="form.percentage" type="text" inputmode="decimal" maxlength="6" placeholder="Ejemplo: 40" @input="form.success = ''" /><small>De 0 a 100. Escribe 40 para indicar 40 %.</small></label></div>
                      <label :for="`observations-${form.source.rowId}`">Observaciones de la institución y evidencias<textarea :id="`observations-${form.source.rowId}`" v-model="form.observations" rows="4" maxlength="10000" placeholder="Explica los avances o dificultades e indica las evidencias que los respaldan." @input="form.success = ''" /></label><small>Los tres campos deben estar completos para finalizar. Puedes guardar borradores parciales.</small>
                      <div class="row-actions"><button type="submit">{{ form.saving ? 'Guardando…' : 'Guardar avance de esta fila' }}</button><button v-if="changed(form)" type="button" class="outline" @click="discard(form)">Descartar cambios</button></div>
                    </fieldset></form>
                  </template>
                  <dl v-else-if="form.source.id" class="execution-summary"><div><dt>Estado</dt><dd>{{ form.source.executionStatus ? executionLabels[form.source.executionStatus] : 'Pendiente' }}</dd></div><div><dt>Porcentaje</dt><dd>{{ form.source.executionPercentage === null ? 'Pendiente' : `${form.source.executionPercentage} %` }}</dd></div><div><dt>Observaciones</dt><dd>{{ form.source.observations || 'Pendientes' }}</dd></div></dl>
                  <p v-if="form.error" class="notice error" role="alert">{{ form.error }}</p><p v-if="form.success" class="notice" role="status">{{ form.success }}</p>
                </div>
              </article>
            </section>
          </div>
        </section>
        <section v-if="source.phase === 'FOLLOW_UP'" class="summary-card completion-card"><h2>Finalizar seguimiento del área</h2><p>Guarda estado, porcentaje y observaciones de las {{ source.requiredRows }} filas en esta fecha. Completar el seguimiento no exige que todas las actividades hayan finalizado o estén al 100 %: registra su situación real.</p><p v-if="hasChanges" class="notice warning">Hay cambios sin guardar. Guárdalos antes de finalizar.</p><button id="complete-follow-up" type="button" :disabled="loading || saving || !source.canEdit || datePending || hasChanges || source.requiredRows === 0 || completedForReview !== source.requiredRows" @click="finalize">{{ finalizing ? 'Finalizando…' : 'Finalizar seguimiento del área' }}</button></section>
      </template>
    </div>
  </main>
</template>

<style scoped>
.objective-group { margin-top: 1.6rem; border: 1px solid var(--color-background-mute); border-radius: .7rem; overflow: hidden; background: var(--color-background-top); }
.goal-group { margin-top: 1.3rem; border-top: 2px solid var(--color-background-mute); padding-top: 1rem; }
.indicator-follow-up { margin-top: 1.1rem; border-left: 4px solid var(--sem-blue); }
.planned-text { white-space: pre-wrap; overflow-wrap: anywhere; margin: .4rem 0 1rem; }
h3, h4, h5, .indicator-title { color: var(--sem-blue); font-weight: 700; }
h3 { font-size: 1.15rem; } h4 { font-size: 1.05rem; } h5 { font-size: .85rem; margin: .6rem 0; }
.indicator-title { font-size: 1.05rem; overflow-wrap: anywhere; margin-bottom: 1rem; }
.execution-summary { margin-top: 1rem; display: grid; gap: .6rem; }
.completion-card button { margin-top: 1rem; }
.follow-up-page { min-height: 100vh; background: var(--color-background); }
.page-header { display: flex; justify-content: space-between; flex-wrap: wrap; gap: 1rem; padding: 1.2rem clamp(1rem, 5vw, 5rem); background: var(--color-background-top); border-bottom: 1px solid var(--color-background-mute); }
a { color: var(--color-text-highlight); font-weight: 700; }
.page-content { width: min(100% - 3rem, 1080px); margin: 3rem auto 5rem; }
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
.overview-table-wrapper { margin-top: 1.7rem; }
.overview-table, .dates-table { width: 100%; table-layout: fixed; border-collapse: collapse; background: var(--color-background-top); }
caption { text-align: left; color: var(--color-text-highlight); font-weight: 700; margin-bottom: .5rem; }
th, td { border: 1px solid var(--color-border-disabled); padding: .7rem; text-align: left; overflow-wrap: anywhere; vertical-align: top; }
thead { background: var(--sem-blue); color: var(--sem-white-top); }
.overview-table th:first-child { width: 52%; }
.overview-table tbody th { font-weight: 700; }
.follow-up-card { margin-top: 2rem; border-radius: .75rem; border: 1px solid var(--color-background-mute); overflow: hidden; scroll-margin-top: 1rem; background: var(--color-background-top); }
.row-header { padding: 1.2rem clamp(1rem, 3vw, 2rem); color: var(--sem-white-top); background: var(--sem-blue); overflow-wrap: anywhere; }
.row-header span { font-size: .75rem; letter-spacing: .06em; }
.row-header h2 { color: var(--sem-white-top); margin: .4rem 0; font-weight: 700; }
.row-header p { font-size: .85rem; }
.row-content { padding: clamp(1rem, 3vw, 2rem); }
.source-context { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: .9rem; margin-bottom: 1.2rem; }
.full-width { grid-column: 1 / -1; }
dt { color: var(--color-text-highlight); font-weight: 700; font-size: .85rem; }
dd { white-space: pre-wrap; overflow-wrap: anywhere; font-size: .9rem; }
.save-state { margin: 1rem 0; font-size: .85rem; font-weight: 700; color: var(--color-text-highlight); }
fieldset { border: 0; padding: 0; margin: 0; min-width: 0; }
.execution-fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1rem; margin-bottom: 1rem; }
label { display: grid; gap: .45rem; color: var(--color-text-highlight); font-size: .9rem; font-weight: 700; }
input, select, textarea { width: 100%; min-width: 0; padding: .65rem; border: 1px solid var(--color-border-disabled); border-radius: .35rem; background: var(--color-background-top); color: var(--color-text); font: inherit; }
textarea { resize: vertical; }
small, .hint { color: var(--color-text-disabled); font-size: .8rem; font-weight: 400; }
small { display: block; margin-top: .35rem; }
.footer-hint { margin-top: 1.5rem; }
.row-actions { display: flex; gap: .8rem; flex-wrap: wrap; margin-top: 1rem; }
button { padding: .7rem 1rem; border: 1px solid var(--sem-blue); border-radius: .4rem; color: var(--sem-white-top); background: var(--sem-blue); font: inherit; font-weight: 700; cursor: pointer; }
button.outline { color: var(--sem-blue); background: var(--color-background-top); }
button:disabled, fieldset:disabled button { opacity: .6; cursor: default; }
fieldset:disabled input, fieldset:disabled select, fieldset:disabled textarea { background: var(--color-background-mute); }
button:hover:enabled { filter: brightness(.92); }
:is(a, button, input, select, textarea):focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 3px; }
.notice { margin: 1rem 0; padding: .9rem; border-left: 4px solid var(--sem-blue); background: var(--color-background-mute); overflow-wrap: anywhere; }
.notice.error { border-color: var(--sem-red); color: var(--color-text-red); }
.notice.warning { border-color: var(--sem-yellow); }
.notice button { margin-top: .7rem; }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip-path: inset(50%); white-space: nowrap; border: 0; }
@media (max-width: 600px) {
  .page-content { width: calc(100% - 1.5rem); margin-top: 1.8rem; }
  .date-card > label, .date-card > button { width: 100%; }
  .source-context, .execution-fields { grid-template-columns: minmax(0, 1fr); }
  .row-actions button { width: 100%; }
  th, td { padding: .5rem; font-size: .82rem; }
  .overview-table th:first-child { width: 44%; }
}
</style>
