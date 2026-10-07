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
.valuation-table-wrap { margin-top: 1.25rem; overflow-x: auto; border: 1px solid #d5e2dc; border-radius: .65rem; background: #fff; }
.valuation-table-wrap:focus-visible { outline: 3px solid #8cca9d; outline-offset: 2px; }
.valuation-table { width: 100%; min-width: 580px; border-collapse: collapse; color: #234d40; font-size: .9rem; }
.valuation-table caption { padding: .85rem 1rem; background: #f0f5f2; color: #183c35; text-align: left; font-weight: 700; }
.valuation-table th, .valuation-table td { padding: .85rem .75rem; border-top: 1px solid #dce7e1; border-right: 1px solid #dce7e1; text-align: center; }
.valuation-table thead { background: #e3eee7; }
.valuation-table th:first-child { text-align: left; }
.valuation-table th:last-child, .valuation-table td:last-child { border-right: 0; background: #edf4ef; font-weight: 700; }
.valuation-table tbody td { font-variant-numeric: tabular-nums; }
.valuation-table .percentage-row { background: #f8faf8; color: #496a5b; }
</style>
