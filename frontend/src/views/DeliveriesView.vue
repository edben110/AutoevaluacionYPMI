<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import SemIcon from '@/components/SemIcon.vue'
import { authorizedFetch, loadProfile, responseError } from '@/auth/session'
import { formatReviewDate, type DeliveryPackage } from '@/review/types'

const loading = ref(true)
const route = useRoute()
const error = ref('')
const secretary = ref(false)
const packages = ref<DeliveryPackage[]>([])
const reviewing = ref('')
const reviewError = ref<{ key: string; message: string } | null>(null)
const notice = ref('')
const selectedStatus = ref('')
const selectedYear = ref('')
const search = ref('')
const searchMode = ref<'name' | 'dane'>('name')
const years = computed(() => [...new Set(packages.value.map(item => item.year))].sort((a, b) => b - a))
const pendingCount = computed(() => packages.value.filter(item => !item.reviewed).length)
const filtered = computed(() => packages.value.filter(item => (!selectedYear.value || item.year === Number(selectedYear.value))
  && (!selectedStatus.value || item.reviewed === (selectedStatus.value === 'reviewed'))
  && (searchMode.value === 'dane' ? item.daneCode : `${item.establishmentName} ${item.areas.map(area => area.areaName).join(' ')}`).toLocaleLowerCase('es').includes(search.value.trim().toLocaleLowerCase('es'))))
function key(item: DeliveryPackage): string { return `${item.establishmentId}-${item.year}` }

async function changeReview(item: DeliveryPackage): Promise<void> {
  if (!secretary.value || reviewing.value) return
  reviewing.value = key(item); reviewError.value = null; notice.value = ''
  try {
    const response = await authorizedFetch(`/api/reviews/packages/${item.year}/review?establishmentId=${encodeURIComponent(item.establishmentId)}`, {
      method: item.reviewed ? 'DELETE' : 'PUT',
      ...(item.reviewed ? {} : { headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ version: item.version }) }),
    })
    if (!response.ok) throw await responseError(response)
    const updated = await response.json() as DeliveryPackage
    packages.value = packages.value.map(current => key(current) === key(updated) ? updated : current)
    notice.value = `Paquete ${item.year} de ${item.establishmentName}: ${updated.reviewed ? 'marcado como revisado' : 'marcado como pendiente'}.`
  } catch (cause) { reviewError.value = { key: key(item), message: cause instanceof Error ? cause.message : 'No se pudo actualizar la revisión.' } }
  finally { reviewing.value = '' }
}

async function load(): Promise<void> {
  if (reviewing.value) return
  loading.value = true; error.value = ''; packages.value = []; reviewError.value = null; notice.value = ''
  try {
    secretary.value = (await loadProfile()).role === 'SECRETARY'
    const response = await authorizedFetch('/api/reviews/packages')
    if (!response.ok) throw await responseError(response)
    packages.value = await response.json() as DeliveryPackage[]
  } catch (cause) { error.value = cause instanceof Error ? cause.message : 'No se pudieron cargar las entregas.' }
  finally { loading.value = false }
}
onMounted(() => void load())
</script>

<template>
  <main class="dashboard-page">
    <header class="dashboard-header"><RouterLink to="/dashboard">Volver a mi espacio</RouterLink></header>
    <div class="dashboard-content">
      <h1>{{ secretary ? 'Paquetes de las instituciones' : 'Mis autoevaluaciones e historial' }}</h1>
      <p>Cada paquete reúne las autoevaluaciones enviadas y los PMI de una institución en un mismo año, organizados por área.</p>
      <p v-if="secretary">Abre el paquete de la institución para consultar sus documentos. Puedes comentar cada componente de la autoevaluación y marcar el paquete como revisado.</p>
      <p v-else>Las observaciones de Secretaría te orientan para el siguiente período. Puedes consultar tus entregas aunque el período haya terminado.</p>
      <p v-if="notice" class="review-notice" role="status">{{ notice }}</p>
      <p v-if="loading" class="status-card" role="status">Consultando paquetes…</p>
      <div v-else-if="error" class="status-card" role="alert"><p>{{ error }}</p><button type="button" class="secondary-button" @click="load">Reintentar</button></div>
      <template v-else>
        <div class="package-browser">
        <aside class="package-search" aria-label="Buscar paquetes institucionales">
        <div v-if="secretary" class="search-mode" role="group" aria-label="Buscar institución por"><button type="button" :aria-pressed="searchMode === 'name'" @click="searchMode = 'name'; search = ''">Nombre</button><button type="button" :aria-pressed="searchMode === 'dane'" @click="searchMode = 'dane'; search = ''">Código DANE</button></div>
        <div class="filters">
          <label>Año<select v-model="selectedYear"><option value="">Todos los años</option><option v-for="year in years" :key="year" :value="String(year)">{{ year }}</option></select></label>
          <label>{{ secretary ? searchMode === 'dane' ? 'Código DANE' : 'Nombre de institución o área' : 'Buscar área' }}<input v-model="search" type="search" placeholder="Buscar en las entregas" /></label>
          <label>Revisión<select v-model="selectedStatus"><option value="">Todos</option><option value="pending">Pendientes</option><option value="reviewed">Revisados</option></select></label>
          <button type="button" class="secondary-button" @click="load">Actualizar</button>
        </div>
        </aside>
        <div class="package-results">
        <p v-if="packages.length === 0" class="status-card">Todavía no hay autoevaluaciones enviadas. El paquete aparecerá cuando la institución envíe su primera área.</p>
        <p v-else-if="filtered.length === 0" class="status-card">No hay entregas que coincidan con la búsqueda.</p>
        <p v-else>Paquetes visibles: {{ filtered.length }} · Pendientes: {{ pendingCount }} · Revisados: {{ packages.length - pendingCount }}</p>
        <div class="package-list">
          <article v-for="item in filtered" :key="key(item)" class="area-evaluation-card package-card">
            <header class="package-header">
              <div class="package-identity"><SemIcon name="document" :size="52" /><div><span class="section-number">PAQUETE {{ item.year }}</span><h2>{{ item.establishmentName }}</h2><p>DANE {{ item.daneCode }}</p></div></div>
              <strong class="review-badge" :class="{ reviewed: item.reviewed }">{{ item.reviewed ? 'Revisado' : 'Pendiente de revisión' }}</strong>
            </header>
            <p>{{ item.areas.length }} de {{ item.expectedAreas }} autoevaluaciones recibidas · {{ item.areas.filter(area => area.pmiId).length }} PMI enviados</p>
            <p v-if="item.changedSinceReview" class="change-notice">Hay nuevas entregas o cambios en el PMI desde la última revisión. Revisa de nuevo el paquete.</p>
            <p v-if="item.reviewedAt && item.reviewedBy" class="review-meta">{{ item.reviewed ? 'Revisado' : 'Última revisión' }} por {{ item.reviewedBy }} · {{ formatReviewDate(item.reviewedAt) }}</p>
            <details class="package-content">
              <summary>Abrir paquete · Autoevaluaciones y PMI</summary>
              <ul class="package-areas">
                <li v-for="area in item.areas" :key="area.areaId">
                  <h3>{{ area.areaName }}</h3>
                  <p>Autoevaluación recibida el {{ formatReviewDate(area.submittedAt) }}</p>
                  <p>{{ area.pmiId ? 'PMI enviado' + (area.pmiUpdatedAt ? ' · Último cambio: ' + formatReviewDate(area.pmiUpdatedAt) : '') : 'PMI aún no enviado' }}</p>
                  <RouterLink class="area-card-action" :to="{ name: 'delivery-review', params: { year: item.year, areaId: area.areaId }, query: { ...(secretary ? { establishmentId: item.establishmentId } : {}), ...(route.query.section === 'pmi' ? { section: 'pmi' } : {}) } }">{{ secretary ? 'Revisar autoevaluación y PMI' : 'Ver autoevaluación, comentarios y PMI' }} →</RouterLink>
                </li>
              </ul>
              <section v-if="secretary" class="review-actions" aria-label="Estado de revisión del paquete">
                <p>La marca corresponde a todo lo recibido en este paquete. Si llega otra área o cambia el PMI, volverá a pendiente de revisión.</p>
                <button type="button" class="secondary-button" :disabled="Boolean(reviewing)" @click="changeReview(item)">{{ reviewing === key(item) ? 'Guardando revisión…' : item.reviewed ? 'Volver a pendiente' : 'Marcar paquete como revisado' }}</button>
                <p v-if="reviewError?.key === key(item)" class="form-error" role="alert">{{ reviewError.message }}</p>
              </section>
            </details>
          </article>
        </div>
        </div>
        </div>
      </template>
    </div>
  </main>
</template>

<style scoped>
h1, h2, a { color: var(--sem-blue); }
h1 { font-size: 1.8rem; margin-bottom: .7rem; }
h2 { font-size: 1.15rem; }
p { margin: .6rem 0; }
.filters { display: flex; align-items: end; flex-wrap: wrap; gap: 1rem; margin: 1.5rem 0; }
label { display: grid; gap: .4rem; flex: 1; min-width: 10rem; }
input, select { width: 100%; padding: .7rem; border: 1px solid var(--sem-black-mute); border-radius: .4rem; background: var(--color-background-top); color: var(--color-text); font: inherit; }
.package-list { display: grid; gap: 1.2rem; margin-top: 1rem; }
.package-browser { display: grid; grid-template-columns: 230px minmax(0, 1fr); gap: 1.5rem; margin-top: 1.5rem; align-items: start; }
.package-search { position: sticky; top: 80px; }
.package-results { min-width: 0; }
.package-search .filters { margin-top: 1rem; display: grid; }
.search-mode { display: grid; grid-template-columns: 1fr 1fr; border: 1px solid var(--sem-black-mute); border-radius: 7px; overflow: hidden; }
.search-mode button { padding: .75rem .4rem; border: 0; color: var(--sem-black-soft); background: var(--sem-white-top); font: inherit; font-weight: 700; font-size: .83rem; }
.search-mode button[aria-pressed='true'] { box-shadow: inset 0 0 0 2px var(--sem-blue); color: var(--sem-blue); border-radius: 6px; }
.search-mode button:focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: -3px; }
.package-identity { display: flex; align-items: center; gap: .7rem; min-width: 0; }
.package-identity svg { flex-shrink: 0; }
.package-card { overflow-wrap: anywhere; }
.package-header { display: flex; justify-content: space-between; align-items: start; flex-wrap: wrap; gap: .8rem; }
.package-header h2 { margin-top: .4rem; font-size: 1.3rem; }
.review-badge { padding: .4rem .7rem; border-radius: .4rem; color: var(--sem-blue); background: var(--color-background-mute); border-left: 4px solid var(--sem-yellow); font-size: .85rem; }
.review-badge.reviewed { color: var(--sem-white-top); background: var(--sem-blue); border-color: var(--sem-blue); }
.change-notice, .review-notice { border-left: 4px solid var(--sem-yellow); padding: .7rem; background: var(--color-background-top); color: var(--sem-blue); }
.review-meta { color: var(--sem-black-soft); font-size: .9rem; }
summary { cursor: pointer; color: var(--sem-blue); font-weight: 700; padding: .7rem 0; }
.package-areas { list-style: none; padding: 0; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1rem; margin-top: .8rem; }
.package-areas li { padding: 1rem; border: 1px solid var(--color-background-mute); border-top: 3px solid var(--sem-blue); border-radius: .4rem; }
h3 { color: var(--sem-blue); font-size: 1rem; }
.review-actions { border-top: 1px solid var(--color-background-mute); margin-top: 1rem; padding-top: .8rem; }
.review-actions p { font-size: .9rem; }
a:focus-visible, input:focus-visible, select:focus-visible, summary:focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 3px; }
@media (max-width: 650px) { .package-areas { grid-template-columns: 1fr; } }
@media (max-width: 1100px) { .package-browser { grid-template-columns: 1fr; } .package-search { position: static; } .package-search .filters { display: flex; flex-wrap: wrap; } .search-mode { max-width: 300px; } }
</style>
