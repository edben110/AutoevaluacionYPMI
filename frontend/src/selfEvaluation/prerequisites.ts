export interface FollowUpPrerequisite {
  year: number
  previousYear: number
  ready: boolean
  plans: { pmiId: string; year: number; areaId: string; areaName: string; completed: boolean; completedAt: string | null; requiredRows: number }[]
}
