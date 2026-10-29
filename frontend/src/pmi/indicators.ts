export interface IndicatorDefinition {
  type: 'PROCESS' | 'RESULT' | null
  description: string | null
  measureUnit: string | null
  variablesDefinition: string | null
  dataFormula: string | null
  dataSource: string | null
  methodologicalAspects: string | null
  calculationFrequency: string | null
  measurementResponsible: string | null
}

export interface IndicatorSheet {
  id: string
  rowId: string
  objectiveId: string
  goalId: string
  componentId: string
  componentName: string
  processName: string
  objectiveDescription: string | null
  goalDescription: string | null
  name: string | null
  editable: boolean
  complete: boolean
  definition: IndicatorDefinition
}

export interface PmiIndicators {
  pmiSubmittedAt: string | null
  pmiId: string | null
  year: number
  areaId: string
  areaName: string
  establishmentName: string
  canEdit: boolean
  unnamedRows: number
  indicators: IndicatorSheet[]
}
