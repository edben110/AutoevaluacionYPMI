export interface Delivery {
  establishmentId: string
  establishmentName: string
  daneCode: string
  year: number
  areaId: string
  areaName: string
  submittedAt: string
  pmiId: string | null
  pmiUpdatedAt: string | null
}
export interface DeliveryPackage {
  establishmentId: string
  establishmentName: string
  daneCode: string
  year: number
  areas: Delivery[]
  expectedAreas: number
  reviewed: boolean
  changedSinceReview: boolean
  reviewedAt: string | null
  reviewedBy: string | null
  version: string
}
export interface ReviewedComponent {
  componentId: string
  componentName: string
  description: string | null
  processId: string
  processName: string
  valuation: {
    level: number
    stateLabel: string
    evidenceUrl: string | null
    evidenceNote: string | null
    strengths: string | null
    improvementOpportunities: string | null
  }
}
export interface DeliveryDetail { delivery: Delivery; components: ReviewedComponent[] }
export interface ReviewComment {
  id: string
  componentId: string
  authorId: string
  authorName: string
  body: string
  createdAt: string
}
export function formatReviewDate(value: string): string {
  return new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'America/Bogota' }).format(new Date(value))
}
