<script setup lang="ts">
import { computed } from 'vue'
import { valuationStates, type Valuation } from '@/selfEvaluation/valuation'

const props = defineProps<{ title: string; valuations: Valuation[] }>()
const counts = computed(() =>
  valuationStates.map((state) => ({
    ...state,
    count: props.valuations.filter((valuation) => valuation.level === state.level).length,
  })),
)
</script>

<template>
  <table>
    <caption>{{ title }}</caption>
    <thead>
      <tr>
        <th v-for="state in counts" :key="state.state" scope="col">{{ state.label }}</th>
        <th scope="col">Total</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td v-for="state in counts" :key="state.state">{{ state.count }}</td>
        <td>{{ valuations.length }}</td>
      </tr>
    </tbody>
  </table>
</template>
