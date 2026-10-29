<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, RouterLink, useRoute } from 'vue-router'
import { authorizedFetch, loadProfile, responseError } from '@/auth/session'
import { formatReviewDate, type DeliveryDetail, type ReviewComment } from '@/review/types'
import PmiReadOnly from '@/components/PmiReadOnly.vue'
import EvidenceText from '@/components/EvidenceText.vue'
import ReviewAreaLinks from '@/components/ReviewAreaLinks.vue'

const route = useRoute()
const detail = ref<DeliveryDetail | null>(null)
const comments = ref<ReviewComment[]>([])
const secretary = ref(false)
const loading = ref(true)
const error = ref('')
const commentError = ref('')
const commentsLoaded = ref(false)
const drafts = reactive<Record<string, string>>({})
const saving = ref('')
const saveError = reactive<Record<string, string>>({})
const success = reactive<Record<string, string>>({})
const tab = ref<'evaluation' | 'pmi'>('evaluation')
let sequence = 0
const apiBase = computed(() => `/api/reviews/${String(route.params.year)}/areas/${encodeURIComponent(String(route.params.areaId))}`)
const selector = computed(() => secretary.value ? `?establishmentId=${encodeURIComponent(String(route.query.establishmentId ?? ''))}` : '')
const groups = computed(() => {
  const processes = new Map<string, { id: string; name: string; components: DeliveryDetail['components'] }>()
  for (const component of detail.value?.components ?? []) {
    if (!processes.has(component.processId)) processes.set(component.processId, { id: component.processId, name: component.processName, components: [] })
    processes.get(component.processId)!.components.push(component)
  }
  return [...processes.values()]
})
const totals = computed(() => ['Existencia', 'Pertinencia', 'Apropiación', 'Mejoramiento continuo'].map((label, index) => ({ label,
  count: detail.value?.components.filter(item => item.valuation.level === index + 1).length ?? 0,
})))
async function loadComments(): Promise<void> {
  commentError.value = ''; commentsLoaded.value = false
  const current = sequence
  try {
    const response = await authorizedFetch(`${apiBase.value}/comments${selector.value}`)
    if (!response.ok) throw await responseError(response)
    const data = await response.json() as ReviewComment[]
    if (current === sequence) { comments.value = data; commentsLoaded.value = true }
  } catch (cause) { if (current === sequence) commentError.value = cause instanceof Error ? cause.message : 'No se pudieron cargar las observaciones.' }
}
async function load(): Promise<void> {
  const current = ++sequence
  loading.value = true; error.value = ''; detail.value = null; comments.value = []; tab.value = route.query.section === 'pmi' ? 'pmi' : 'evaluation'
  Object.keys(drafts).forEach(key => delete drafts[key])
  Object.keys(saveError).forEach(key => delete saveError[key])
  Object.keys(success).forEach(key => delete success[key])
  try {
    const profile = await loadProfile()
    if (current !== sequence) return
    secretary.value = profile.role === 'SECRETARY'
    if (secretary.value && !route.query.establishmentId) throw new Error('Selecciona una entrega desde la bandeja de instituciones.')
    if (!secretary.value && route.query.establishmentId && route.query.establishmentId !== profile.id) throw new Error('Solo puedes consultar las entregas de tu institución.')
    const response = await authorizedFetch(`${apiBase.value}${selector.value}`)
    if (!response.ok) throw await responseError(response)
    const data = await response.json() as DeliveryDetail
    if (current !== sequence) return
    detail.value = data
    await loadComments()
  } catch (cause) { if (current === sequence) error.value = cause instanceof Error ? cause.message : 'No se pudo consultar la entrega.' }
  finally { if (current === sequence) loading.value = false }
}
async function addComment(componentId: string): Promise<void> {
  if (saving.value || !secretary.value || !commentsLoaded.value || !drafts[componentId]?.trim()) return
  saving.value = componentId; saveError[componentId] = ''; success[componentId] = ''
  try {
    const response = await authorizedFetch(`${apiBase.value}/components/${encodeURIComponent(componentId)}/comments${selector.value}`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ body: drafts[componentId] }),
    })
    if (!response.ok) throw await responseError(response)
    comments.value.push(await response.json() as ReviewComment)
    drafts[componentId] = ''; success[componentId] = 'Observación publicada para la institución.'
  } catch (cause) { saveError[componentId] = cause instanceof Error ? cause.message : 'No se pudo guardar la observación.' }
  finally { saving.value = '' }
}
function mayLeave(): boolean {
  if (saving.value) return false
  return !Object.values(drafts).some(value => value.trim()) || window.confirm('Tienes observaciones sin publicar. ¿Quieres salir y descartarlas?')
}
onBeforeRouteLeave(mayLeave)
onBeforeRouteUpdate(mayLeave)
watch(() => route.fullPath, () => void load(), { immediate: true })
</script>

<template>
  <main class="dashboard-page">
    <header class="dashboard-header"><RouterLink :to="{ name: 'deliveries' }">{{ secretary ? 'Volver a las entregas' : 'Volver al historial' }}</RouterLink></header>
    <div class="dashboard-content">
      <p v-if="loading" class="status-card" role="status">Cargando entrega y observaciones…</p>
      <div v-else-if="error" class="status-card" role="alert"><p>{{ error }}</p><button type="button" class="secondary-button" @click="load">Reintentar</button></div>
      <template v-else-if="detail">
        <span class="section-number">AUTOEVALUACIÓN {{ detail.delivery.year }} · SOLO LECTURA</span>
        <h1>{{ detail.delivery.areaName }}</h1><h2>{{ detail.delivery.establishmentName }}</h2>
        <p>Enviada el {{ formatReviewDate(detail.delivery.submittedAt) }}</p>
        <p>Las respuestas de esta entrega se conservan. Las observaciones de Secretaría sirven como referencia para el siguiente período.</p>
        <ReviewAreaLinks :delivery="detail.delivery" :secretary="secretary" />
        <nav class="tabs" aria-label="Contenido de la entrega">
          <button type="button" :aria-pressed="tab === 'evaluation'" @click="tab = 'evaluation'">Autoevaluación y comentarios</button>
          <button type="button" :aria-pressed="tab === 'pmi'" @click="tab = 'pmi'">Consultar PMI · Anexos 2 a 5</button>
        </nav>
        <template v-if="tab === 'evaluation'">
          <div class="summary"><div v-for="total in totals" :key="total.label"><strong>{{ total.count }}</strong><span>{{ total.label }}</span></div></div>
          <div v-if="commentError" class="form-error" role="alert"><p>{{ commentError }}</p><button type="button" class="secondary-button" @click="loadComments">Reintentar comentarios</button></div>
          <section v-for="process in groups" :key="process.id" class="process">
            <h2 class="process-title">{{ process.name }}</h2>
            <div class="review-column-headings" aria-hidden="true"><span>Componente</span><span>Valoración</span><span>Evidencias</span><span>Fortalezas</span><span>Oportunidades de mejora</span><span>Observaciones</span></div>
            <article v-for="component in process.components" :key="component.componentId" class="component">
              <header><h3>{{ component.componentName }}</h3><p v-if="component.description" class="description">{{ component.description }}</p></header>
              <strong class="state">{{ component.valuation.stateLabel }}</strong>
              <dl>
                <div><dt>Evidencias: enlaces o descripción</dt><dd><EvidenceText :text="[component.valuation.evidenceUrl, component.valuation.evidenceNote].filter(Boolean).join('\n\n') || 'Sin evidencia registrada'" /></dd></div>
                <div><dt>Fortalezas</dt><dd>{{ component.valuation.strengths || 'Sin fortaleza registrada' }}</dd></div>
                <div><dt>Oportunidades de mejora</dt><dd>{{ component.valuation.improvementOpportunities || 'Sin oportunidad registrada' }}</dd></div>
              </dl>
              <section class="comments" :aria-label="`Observaciones de ${component.componentName}`">
                <h4>Observaciones de Secretaría</h4>
                <template v-if="commentsLoaded">
                  <p v-if="!comments.some(item => item.componentId === component.componentId)">Sin observaciones registradas.</p>
                  <article v-for="comment in comments.filter(item => item.componentId === component.componentId)" :key="comment.id" class="comment">
                    <p>{{ comment.body }}</p><small>{{ comment.authorName }} · {{ formatReviewDate(comment.createdAt) }}</small>
                  </article>
                  <form v-if="secretary" @submit.prevent="addComment(component.componentId)">
                    <label :for="`comment-${component.componentId}`">Nueva observación sobre este componente</label>
                    <textarea :id="`comment-${component.componentId}`" v-model="drafts[component.componentId]" maxlength="10000" rows="3" :disabled="Boolean(saving)" placeholder="Indica qué evidencia requiere aclaración o qué aspecto debe revisarse en la próxima autoevaluación." required />
                    <button class="secondary-button" type="submit" :disabled="Boolean(saving) || !drafts[component.componentId]?.trim()">{{ saving === component.componentId ? 'Publicando…' : 'Publicar observación' }}</button>
                    <p v-if="saveError[component.componentId]" class="form-error" role="alert">{{ saveError[component.componentId] }}</p>
                    <p v-if="success[component.componentId]" role="status">{{ success[component.componentId] }}</p>
                  </form>
                </template>
                <p v-else>Los comentarios todavía no están disponibles. Reintenta su consulta.</p>
              </section>
            </article>
          </section>
        </template>
        <PmiReadOnly v-else :key="`${detail.delivery.establishmentId}-${detail.delivery.year}-${detail.delivery.areaId}`" :delivery="detail.delivery" :secretary="secretary" />
      </template>
    </div>
  </main>
</template>

<style scoped>
h1, h2, h3, h4, a { color: var(--sem-blue); }
h1 { font-size: 1.7rem; margin: .6rem 0; }
h2 { font-size: 1.2rem; }
p { margin: .6rem 0; }
.tabs { display: flex; flex-wrap: wrap; gap: .7rem; margin: 1.5rem 0; }
.tabs button { padding: .8rem; border: 1px solid var(--sem-blue); border-radius: .4rem; color: var(--sem-blue); background: var(--color-background-top); font: inherit; }
.tabs button[aria-pressed='true'] { background: var(--sem-blue); color: var(--sem-white-top); }
.summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); border: 1px solid var(--sem-blue); border-radius: .4rem; overflow: hidden; }
.summary div { display: grid; gap: .3rem; padding: .8rem; text-align: center; background: var(--color-background-top); }
.summary strong { color: var(--sem-blue); font-size: 1.5rem; }
.summary span { font-size: .85rem; }
.process { margin-top: 1.5rem; border: 1px solid var(--color-background-mute); border-radius: .6rem; overflow: hidden; }
.process-title { padding: 1rem; background: var(--sem-blue); color: var(--sem-white-top); }
.component { background: var(--color-background-top); padding: 1.3rem; border-bottom: 3px solid var(--color-background-mute); overflow-wrap: anywhere; }
.component header { display: flex; justify-content: space-between; align-items: start; flex-wrap: wrap; gap: .8rem; }
.state { background: var(--color-background-mute); color: var(--sem-blue); border-left: 3px solid var(--sem-yellow); padding: .3rem .6rem; }
.description, small { color: var(--sem-black-soft); }
.review-column-headings { display: none; }
dl > div { margin: 1rem 0; }
dt, label { font-weight: 700; }
dd, .comment p { white-space: pre-wrap; overflow-wrap: anywhere; margin: .4rem 0; }
.comments { background: var(--color-background); padding: 1rem; border-radius: .4rem; }
.comment { margin: .8rem 0; padding: .5rem .8rem; border-left: 3px solid var(--sem-yellow); }
form { margin-top: 1rem; }
label { display: block; }
textarea { width: 100%; margin-top: .5rem; border: 1px solid var(--sem-black-mute); border-radius: .4rem; padding: .7rem; color: var(--color-text); background: var(--color-background-top); font: inherit; resize: vertical; }
button:focus-visible, a:focus-visible, textarea:focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 3px; }
@media (max-width: 600px) { .summary { grid-template-columns: repeat(2, minmax(0, 1fr)); } .component { padding: .8rem; } }
@media (min-width: 1300px) {
  .review-column-headings, .component { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, .7fr) minmax(0, 1fr) minmax(0, .8fr) minmax(0, .8fr) minmax(0, 1.4fr); }
  .review-column-headings { background: var(--sem-white-mute); }
  .review-column-headings span { padding: .8rem; border-right: 1px solid var(--sem-black-mute); font-size: .8rem; font-weight: 700; }
  .component { padding: 0; border-top: 1px solid var(--sem-black-mute); gap: 0; }
  .component header { display: block; padding: .8rem; border-right: 1px solid var(--sem-black-mute); font-size: .85rem; }
  .component header h3 { font-size: .9rem; text-decoration: underline; }
  .component > .state { padding: .8rem; font-size: .8rem; border: 0; border-right: 1px solid var(--sem-black-mute); background: transparent; }
  .component > dl { display: contents; }
  .component > dl > div { margin: 0; padding: .8rem; border-right: 1px solid var(--sem-black-mute); font-size: .8rem; }
  .component > dl dt { font-size: .75rem; }
  .comments { border-radius: 0; padding: .8rem; font-size: .8rem; }
  .comments h4 { font-size: .85rem; }
  .comments .comment { padding: .4rem; }
  .comments textarea { font-size: .8rem; }
  .comments .secondary-button { padding: .55rem; font-size: .8rem; }
}
</style>
