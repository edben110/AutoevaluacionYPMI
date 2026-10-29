<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { authorizedFetch, hasSession, loadProfile, responseError } from '@/auth/session'
import PmiAreaCards from '@/components/PmiAreaCards.vue'
import type { PmiOverview } from '@/pmi/types'

const route = useRoute()
const router = useRouter()
const overview = ref<PmiOverview | null>(null)
const loading = ref(true)
const error = ref('')
const authorized = ref(false)
const year = computed(() => Number(route.params.year))
let requestNumber = 0
async function load(): Promise<void> {
  const request = ++requestNumber
  loading.value = true; error.value = ''; overview.value = null
  try {
    if (!Number.isInteger(year.value) || year.value < 1 || year.value > 9999) throw new Error('El año de autoevaluación no es válido.')
    const response = await authorizedFetch(`/api/pmi/${year.value}`)
    if (!response.ok) throw await responseError(response)
    const body = await response.json() as PmiOverview
    if (request === requestNumber) overview.value = body
  } catch (cause) {
    if (request === requestNumber) error.value = cause instanceof Error ? cause.message : 'No se pudieron cargar los PMI.'
    if (!hasSession.value) await router.replace({ name: 'login' })
  } finally { if (request === requestNumber) loading.value = false }
}
onMounted(async () => {
  try {
    const profile = await loadProfile()
    if (profile.role !== 'ESTABLISHMENT') { await router.replace({ name: 'dashboard' }); return }
    authorized.value = true
    await load()
  } catch (cause) { error.value = cause instanceof Error ? cause.message : 'No se pudo abrir esta pantalla.'; loading.value = false }
})
watch(() => route.params.year, () => { if (authorized.value) void load() })
</script>

<template>
  <main class="pmi-overview-page">
    <header><RouterLink class="u-return-button" :to="{ name: 'dashboard' }">Volver a mi espacio</RouterLink></header>
    <div class="pmi-overview-content">
      <h1>PMI por área · {{ year }}</h1>
      <p>Completa y envía la autoevaluación de un área para habilitar su plan de mejoramiento.</p>
      <p v-if="loading" role="status">Consultando avance…</p>
      <div v-else-if="error" role="alert"><p>{{ error }}</p><button type="button" @click="load">Reintentar</button></div>
      <PmiAreaCards v-else-if="overview" :overview="overview" />
    </div>
  </main>
</template>

<style scoped>
.pmi-overview-page { min-height: 100vh; background: var(--color-background); }
header { padding: 1.2rem clamp(1rem, 5vw, 5rem); background: var(--color-background-top); }
a { color: var(--color-text-highlight); font-weight: 700; text-decoration: none; }
.pmi-overview-content { width: min(100% - 3rem, 1050px); margin: 3rem auto; }
h1 { margin-bottom: 1rem; color: var(--color-text-highlight); font-size: clamp(1.8rem, 4vw, 2.6rem); }
button { padding: .6rem 1rem; border: 1px solid var(--sem-blue); border-radius: .4rem; color: var(--color-text-highlight); background: var(--color-background-top); }
@media (max-width: 680px) { .pmi-overview-content { width: min(100% - 2rem, 1050px); } }
</style>
