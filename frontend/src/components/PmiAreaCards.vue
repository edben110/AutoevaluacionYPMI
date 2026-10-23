<script setup lang="ts">
import { RouterLink } from 'vue-router'
import type { PmiOverview } from '@/pmi/types'
defineProps<{ overview: PmiOverview }>()
</script>

<template>
  <div class="pmi-area-grid">
    <article v-for="area in overview.areas" :key="area.areaId" class="pmi-area-card" :class="{ 'pmi-area-locked': !area.selfEvaluationComplete || !area.selfEvaluationSubmitted }">
      <h3>PMI · {{ area.areaName }}</h3>
      <p class="pmi-progress">Autoevaluación: {{ area.valuedComponents }} de {{ area.totalComponents }} componentes valorados</p>
      <progress :value="area.valuedComponents" :max="area.totalComponents || 1" :aria-label="`Avance de autoevaluación de ${area.areaName}`" />
      <template v-if="area.selfEvaluationComplete && area.selfEvaluationSubmitted">
        <p>{{ area.candidateCount ? `${area.candidateCount} candidatos en ${area.candidateStateLabel}` : 'Sin candidatos: todos los componentes están en Mejoramiento continuo.' }}</p>
        <RouterLink class="pmi-area-action" :to="{ name: 'pmi', params: { year: overview.year, areaId: area.areaId } }">Abrir PMI del área →</RouterLink>
      </template>
      <p v-else-if="area.selfEvaluationComplete" class="pmi-locked-note">Envía la autoevaluación de esta área para habilitar su PMI.</p>
      <p v-else class="pmi-locked-note">Completa y envía la autoevaluación de esta área para habilitar su PMI.</p>
    </article>
  </div>
</template>

<style scoped>
.pmi-area-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1rem; margin: 1rem 0; }
.pmi-area-card { display: flex; flex-direction: column; gap: .7rem; min-width: 0; padding: 1.2rem; border: 1px solid var(--color-background-mute); border-top: 4px solid var(--sem-blue); border-radius: .7rem; background: var(--color-background-top); }
h3 { color: var(--color-text-highlight); font-size: 1rem; }
p { font-size: .85rem; }
.pmi-progress, .pmi-locked-note { color: var(--color-text-disabled); }
progress { width: 100%; height: .7rem; accent-color: var(--sem-blue); }
.pmi-area-action { align-self: start; margin-top: auto; padding: .55rem .75rem; border: 1px solid var(--sem-blue); border-radius: .4rem; color: var(--color-text-highlight); text-decoration: none; font-weight: 700; font-size: .85rem; }
.pmi-area-action:focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 3px; }
.pmi-area-locked { border-top-color: var(--color-border-disabled); background: var(--color-background); }
@media (max-width: 680px) { .pmi-area-grid { grid-template-columns: 1fr; } }
</style>
