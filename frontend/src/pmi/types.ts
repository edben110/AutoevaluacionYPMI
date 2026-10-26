export interface PriorityComponent {
  componentId: string
  valuationId: string
  areaId: string
  areaName: string
  processId: string
  processName: string
  componentName: string
  level: number
  state: string
  stateLabel: string
  improvementOpportunities: string | null
  eligible: boolean
  criticalFactors: string | null
  urgency: number | null
  tendency: number | null
  impact: number | null
  total: number | null
  selected: boolean
}

export interface PriorityMatrix {
  periodConfigured?: boolean
  id: string | null
  selfEvaluationId: string
  year: number
  areaId: string
  areaName: string
  selfEvaluationComplete: boolean
  selfEvaluationSubmitted: boolean
  submittedAt: string | null
  totalComponents: number
  valuedComponents: number
  candidateLevel: number | null
  candidateStateLabel: string | null
  prioritizationComplete: boolean
  canFormulate: boolean
  status: 'DRAFT'
  projectionYears: 1
  filledOn: string | null
  updatedAt: string | null
  establishmentName: string
  components: PriorityComponent[]
  highestComponentIds: string[]
  selectedComponentIds: string[]
  selectionNeedsReview: boolean
}

export interface PmiAreaSummary {
  areaId: string
  areaName: string
  totalComponents: number
  valuedComponents: number
  selfEvaluationComplete: boolean
  selfEvaluationSubmitted: boolean
  submittedAt: string | null
  candidateLevel: number | null
  candidateStateLabel: string | null
  candidateCount: number
}

export interface PmiOverview { year: number; areas: PmiAreaSummary[]; periodConfigured?: boolean }

export interface PriorityComponentRequest {
  criticalFactors: string
  urgency: number
  tendency: number
  impact: number
}
