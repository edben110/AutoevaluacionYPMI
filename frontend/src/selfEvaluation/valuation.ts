export type ValuationLevel = 1 | 2 | 3 | 4
export type ValuationState = 'EXISTENCE' | 'PERTINENCE' | 'APPROPRIATION' | 'CONTINUOUS_IMPROVEMENT'

export const valuationStates: {
  level: ValuationLevel
  state: ValuationState
  label: string
  strengthsAllowed: boolean
  improvementOpportunitiesAllowed: boolean
}[] = [
  { level: 1, state: 'EXISTENCE', label: 'Existencia', strengthsAllowed: false, improvementOpportunitiesAllowed: true },
  { level: 2, state: 'PERTINENCE', label: 'Pertinencia', strengthsAllowed: true, improvementOpportunitiesAllowed: true },
  { level: 3, state: 'APPROPRIATION', label: 'Apropiación', strengthsAllowed: true, improvementOpportunitiesAllowed: true },
  { level: 4, state: 'CONTINUOUS_IMPROVEMENT', label: 'Mejoramiento continuo', strengthsAllowed: true, improvementOpportunitiesAllowed: false },
]

export function allowsStrengths(level: ValuationLevel | ''): boolean {
  return valuationStates.find((state) => state.level === level)?.strengthsAllowed ?? false
}

export function allowsImprovementOpportunities(level: ValuationLevel | ''): boolean {
  return valuationStates.find((state) => state.level === level)?.improvementOpportunitiesAllowed ?? false
}

export interface Valuation {
  componentId: string
  level: ValuationLevel
  state: ValuationState
  stateLabel: string
  improvementChance: boolean
  strength: boolean
  strengthsAllowed: boolean
  improvementOpportunitiesAllowed: boolean
  evidenceUrl: string | null
  evidenceNote: string | null
  strengths: string | null
  improvementOpportunities: string | null
  updatedAt: string
}
