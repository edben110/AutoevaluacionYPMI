<script setup lang="ts">
import { computed } from 'vue'
import { valuationStates, type Valuation } from '@/selfEvaluation/valuation'

const props = defineProps<{ title: string; valuations: Valuation[] }>()
const percentageFormat = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 2 })
function percentage(count: number): number {
  return props.valuations.length === 0 ? 0 : Math.round(count * 10000 / props.valuations.length) / 100
}
const counts = computed(() =>
  valuationStates.map((state) => ({
    ...state,
    count: props.valuations.filter((valuation) => valuation.level === state.level).length,
  })),
)
</script>

<template>
  <div class="valuation-table-wrap" role="region" :aria-label="title" tabindex="0">
    <table class="valuation-table">
      <caption>{{ title }}</caption>
      <thead>
        <tr>
          <th scope="col">Medida</th>
          <th v-for="state in counts" :key="state.state" scope="col">{{ state.label }}</th>
          <th scope="col">Total</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <th scope="row">Componentes</th>
          <td v-for="state in counts" :key="state.state">{{ state.count }}</td>
          <td>{{ valuations.length }}</td>
        </tr>
        <tr class="percentage-row">
          <th scope="row">Porcentaje</th>
          <td v-for="state in counts" :key="state.state">{{ percentageFormat.format(percentage(state.count)) }} %</td>
          <td>{{ valuations.length > 0 ? '100' : '0' }} %</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.valuation-table-wrap { margin-top: 1.25rem; overflow-x: auto; border: 1px solid var(--color-background-mute); border-radius: .65rem; background: var(--color-background-top); }
.valuation-table-wrap:focus-visible { outline: 3px solid var(--sem-yellow); outline-offset: 2px; }
.valuation-table { width: 100%; min-width: 580px; border-collapse: collapse; color: var(--color-text); font-size: .9rem; }
.valuation-table caption { padding: .85rem 1rem; background: var(--color-background-mute); color: var(--color-text-highlight); text-align: left; font-weight: 700; }
.valuation-table th, .valuation-table td { padding: .85rem .75rem; border-top: 1px solid var(--color-background-mute); border-right: 1px solid var(--color-background-mute); text-align: center; }
.valuation-table thead { background: var(--sem-blue); color: var(--color-text-white); }
.valuation-table th:first-child { text-align: left; }
.valuation-table th:last-child, .valuation-table td:last-child { border-right: 0; font-weight: 700; }
.valuation-table tbody th:last-child, .valuation-table tbody td:last-child { background: var(--color-background-mute); }
.valuation-table tbody td { font-variant-numeric: tabular-nums; }
.valuation-table .percentage-row { background: var(--color-background); color: var(--color-text-disabled); }
</style>
