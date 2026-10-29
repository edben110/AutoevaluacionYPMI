<script setup lang="ts">
import { formatPeriodDate, periodLabels, type EvaluationPeriod } from '@/selfEvaluation/period'
defineProps<{ period: EvaluationPeriod | null; year: number }>()
</script>

<template>
  <section class="status-card" aria-label="Período de autoevaluación" role="status">
    <template v-if="period">
      <strong>Período {{ period.year }} · {{ periodLabels[period.status] }}</strong>
      <p>Del {{ formatPeriodDate(period.startDate) }} al {{ formatPeriodDate(period.finishDate) }}, ambos días incluidos.</p>
      <p v-if="period.writable">Puedes diligenciar las cuatro áreas y guardar tus avances.</p>
      <p v-else-if="period.status === 'SCHEDULED'">El diligenciamiento estará disponible desde la fecha de inicio.</p>
      <p v-else>El período terminó; el diligenciamiento está cerrado.</p>
      <small>Las fechas se verifican con la hora de Bogotá.</small>
    </template>
    <template v-else>
      <strong>Período {{ year }} sin habilitar</strong>
      <p>Las autoevaluaciones estarán disponibles cuando Secretaría habilite el período.</p>
    </template>
  </section>
</template>
