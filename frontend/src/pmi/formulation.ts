export type DecimalAmount = string | number

export interface FormulationComponent {
  componentId: string
  componentName: string
  areaId: string
  areaName: string
  processId: string
  processName: string
  eligible: boolean
}

export interface FormulationRow {
  id: string | null
  indicatorName: string | null
  activities: string | null
  totalCost: DecimalAmount | null
  fseAmount: DecimalAmount | null
  otherAmount: DecimalAmount | null
  responsible: string | null
  startDate: string | null
  finishDate: string | null
  collectionInstruments: string | null
  collectionFrequency: string | null
}

export interface FormulationGoal {
  id: string | null
  description: string | null
  rows: FormulationRow[]
}

export interface FormulationObjective extends Omit<FormulationComponent, 'eligible'> {
  id: string
  description: string | null
  editable: boolean
  goals: FormulationGoal[]
}

export interface FormulationResponse {
  pmiId: string | null
  year: number
  areaId: string
  areaName: string
  selfEvaluationComplete: boolean
  selfEvaluationSubmitted: boolean
  submittedAt: string | null
  prioritizationComplete: boolean
  canFormulate: boolean
  establishmentName: string
  projectionYears: 1
  filledOn: string | null
  selectionNeedsReview: boolean
  selectedComponents: FormulationComponent[]
  objectives: FormulationObjective[]
  totals: {
    totalCost: DecimalAmount
    fseAmount: DecimalAmount
    otherAmount: DecimalAmount
    pendingAmountFields: number
    excludedObjectives: number
  }
}

export interface FormulationObjectiveRequest {
  componentId: string
  description: string | null
  goals: FormulationGoal[]
}
