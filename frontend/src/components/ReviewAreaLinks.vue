<script setup lang="ts">
import { ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { authorizedFetch, responseError } from '@/auth/session'
import type { Delivery, DeliveryPackage } from '@/review/types'
const props = defineProps<{ delivery: Delivery; secretary: boolean }>()
const areas = ref<Delivery[]>([])
const error = ref('')
let sequence = 0
async function load(): Promise<void> {
  const request = ++sequence
  error.value = ''; areas.value = []
  try {
    const response = await authorizedFetch('/api/reviews/packages')
    if (!response.ok) throw await responseError(response)
    const packages = await response.json() as DeliveryPackage[]
    if (request === sequence) areas.value = packages.find(item => item.establishmentId === props.delivery.establishmentId && item.year === props.delivery.year)?.areas ?? []
  } catch (cause) { if (request === sequence) error.value = cause instanceof Error ? cause.message : 'No se pudieron consultar las áreas del paquete.' }
}
watch(() => `${props.delivery.establishmentId}-${props.delivery.year}`, () => void load(), { immediate: true })
</script>

<template>
  <div v-if="error" class="form-error" role="alert"><p>{{ error }}</p><button type="button" class="text-button" @click="load">Reintentar áreas del paquete</button></div>
  <nav v-else-if="areas.length > 1" class="review-area-links" aria-label="Áreas recibidas en este paquete">
    <RouterLink v-for="area in areas" :key="area.areaId" :class="{ active: area.areaId === delivery.areaId }" :aria-current="area.areaId === delivery.areaId ? 'page' : undefined" :to="{ name: 'delivery-review', params: { year: delivery.year, areaId: area.areaId }, query: secretary ? { establishmentId: delivery.establishmentId } : {} }">{{ area.areaName }}</RouterLink>
  </nav>
</template>

<style scoped>
.review-area-links { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: .6rem; margin: 1.5rem 0; }
a { padding: .8rem; border: 1px solid var(--sem-black-mute); border-bottom: 3px solid var(--sem-black-mute); background: var(--sem-white-top); color: var(--sem-black); text-align: center; text-decoration: none; font-size: .85rem; font-weight: 700; border-radius: 6px; }
a.active { background: var(--sem-white-mute); border-color: var(--sem-blue); color: var(--sem-blue); }
a:focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 3px; }
@media (max-width: 750px) { .review-area-links { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>
