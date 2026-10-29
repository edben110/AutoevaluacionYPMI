export type ExecutionStatus = 'NI' | 'ESP' | 'CANC' | 'FIN' | 'EJ'

export const executionLabels: Record<ExecutionStatus, string> = {
  NI: 'No iniciada', ESP: 'En espera', CANC: 'Cancelada', FIN: 'Finalizada', EJ: 'En ejecución',
}
export interface FollowUpSource {
  componentId: string
  objectiveId: string
  goalId: string
  processName: string
  componentName: string
  objectiveDescription: string | null
  goalDescription: string | null
  indicatorName: string | null
  activities: string | null
  startDate: string | null
  finishDate: string | null
}
export interface FollowUpRow {
  id: string | null
  rowId: string
  controlDate: string
  editable: boolean
  sourceRemoved: boolean
  sourceChanged: boolean
  source: FollowUpSource
  executionStatus: ExecutionStatus | null
  executionPercentage: string | null
  observations: string | null
  updatedAt: string | null
}
export interface PmiFollowUp {
  pmiId: string | null
  year: number
  areaId: string
  areaName: string
  establishmentName: string
  controlDate: string
  availableDates: string[]
  canEdit: boolean
  rows: FollowUpRow[]
  reviewYear: number
  phase: 'PLANNING' | 'FOLLOW_UP' | 'COMPLETED' | 'CLOSED'
  completedAt: string | null
  requiredRows: number
  completedRows: number
  canComplete: boolean
}
