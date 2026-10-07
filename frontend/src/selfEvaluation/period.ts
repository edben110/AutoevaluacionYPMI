import { authorizedFetch, responseError } from '@/auth/session'

export interface EvaluationPeriod {
  year: number
  startDate: string
  finishDate: string
  status: 'SCHEDULED' | 'OPEN' | 'CLOSED'
  writable: boolean
  today: string
  timeZone: string
  createdAt: string
  updatedAt: string
}

export const periodLabels: Record<EvaluationPeriod['status'], string> = {
  SCHEDULED: 'Programado', OPEN: 'Abierto', CLOSED: 'Cerrado',
}

export function formatPeriodDate(value: string): string {
  const [year, month, day] = value.split('-')
  return `${day}/${month}/${year}`
}

export async function loadEvaluationPeriod(year: number): Promise<EvaluationPeriod | null> {
  const response = await authorizedFetch(`/api/self-evaluation-periods/${year}`)
  if (response.status === 404) return null
  if (!response.ok) throw await responseError(response)
  return await response.json() as EvaluationPeriod
}
