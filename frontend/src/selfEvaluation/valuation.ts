export type ValuationLevel = 1 | 2 | 3 | 4
export type ValuationState = 'EXISTENCE' | 'PERTINENCE' | 'APPROPRIATION' | 'CONTINUOUS_IMPROVEMENT'

export const valuationStates: { level: ValuationLevel; state: ValuationState; label: string }[] = [
  { level: 1, state: 'EXISTENCE', label: 'Existencia' },
  { level: 2, state: 'PERTINENCE', label: 'Pertinencia' },
  { level: 3, state: 'APPROPRIATION', label: 'Apropiación' },
  { level: 4, state: 'CONTINUOUS_IMPROVEMENT', label: 'Mejoramiento continuo' },
]

export interface Valuation {
  componentId: string
  level: ValuationLevel
  state: ValuationState
  stateLabel: string
  evidenceUrl: string | null
  evidenceNote: string | null
  updatedAt: string
}
