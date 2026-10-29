export interface ComponentGuidance {
  componentId: string
  componentName: string
  areaName: string
  processName: string
  evaluationItem: string | null
  evidenceExamples: string[]
  evidenceNotice: string
  sourceTitle: string | null
  sourceUrl: string | null
  itemPages: number[]
  evidencePages: number[]
  officialGuidance: boolean
}
