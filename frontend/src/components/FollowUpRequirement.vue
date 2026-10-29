<script setup lang="ts">
import { RouterLink } from 'vue-router'
import type { FollowUpPrerequisite } from '@/selfEvaluation/prerequisites'
defineProps<{ requirement: FollowUpPrerequisite }>()
</script>

<template>
  <section class="follow-up-requirement" aria-labelledby="follow-up-required-title">
    <h2 id="follow-up-required-title">Primero finaliza el seguimiento de {{ requirement.previousYear }}</h2>
    <p>Antes de diligenciar la autoevaluación de {{ requirement.year }}, debes completar el Anexo 5 de todos los PMI del año anterior. Registra estado, porcentaje y observaciones de cada fila y pulsa Finalizar seguimiento del área.</p>
    <ul><li v-for="plan in requirement.plans" :key="plan.pmiId">
      <strong>{{ plan.areaName }}</strong>
      <RouterLink :to="{ name: 'delivery-review', params: { year: plan.year, areaId: plan.areaId } }">Consultar autoevaluación {{ plan.year }} y comentarios de Secretaría →</RouterLink>
      <span v-if="plan.completed">Seguimiento finalizado</span>
      <template v-else>
        <p v-if="plan.requiredRows === 0">Este PMI aún no tiene indicadores o actividades formulados. Completa su Anexo 3 para registrar el seguimiento.</p>
        <RouterLink :to="{ name: plan.requiredRows === 0 ? 'pmi-formulation' : 'pmi-follow-up', params: { year: plan.year, areaId: plan.areaId } }">{{ plan.requiredRows === 0 ? 'Completar el plan anterior' : 'Diligenciar seguimiento' }} →</RouterLink>
      </template>
    </li></ul>
  </section>
</template>

<style scoped>
.follow-up-requirement { padding: 1.3rem; margin: 1rem 0; background: var(--color-background-top); border: 1px solid var(--color-background-mute); border-left: 4px solid var(--sem-yellow); border-radius: .5rem; }
h2, strong, a { color: var(--sem-blue); font-weight: 700; }
h2 { font-size: 1.2rem; margin-bottom: .7rem; }
ul { margin-top: .8rem; padding-left: 1.2rem; }
li { margin: .8rem 0; overflow-wrap: anywhere; }
li span, li a { display: block; margin-top: .3rem; }
li p { font-size: .85rem; margin-top: .4rem; }
a:focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 3px; }
</style>
