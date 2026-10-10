export type TransportSystem = 'METRO' | 'BRTS' | 'AMTS'
export type Severity = 'LOW' | 'MEDIUM' | 'HIGH'
export type EvidenceType = 'FACT' | 'INCIDENT' | 'CORRELATION' | 'POSSIBLE_CAUSE' | 'UNCONFIRMED'
export type NarrativeSource = 'AI' | 'FALLBACK'

export interface EvidenceItem {
  type: EvidenceType
  description: string
}

export interface ExceptionDto {
  entityId: string
  entityName: string
  system: TransportSystem
  metric: string
  valueToday: number
  valueBaseline: number
  pctChange: number | null
  severity: Severity
  evidence: EvidenceItem[]
}

export interface NetworkSummaryDto {
  totalToday: number
  totalYesterday: number
  pctChange: number | null
}

export interface SystemSummaryDto {
  system: TransportSystem
  totalToday: number
  totalYesterday: number
  pctChange: number | null
  scheduleAdherence: number | null
}

export interface BriefDto {
  reportDate: string
  generatedAt: string
  networkSummary: NetworkSummaryDto
  systemSummaries: SystemSummaryDto[]
  exceptions: ExceptionDto[]
  recommendedActions: string[]
  narrative: string
  narrativeSource: NarrativeSource
  dataQualityWarnings: string[]
  isSyntheticData: boolean
}

export interface UploadResult {
  status: string
  rowCount: number
  excludedRows: number
  warnings: string[]
  errors: string[]
}

export type UploadStatus = 'idle' | 'uploading' | 'valid' | 'error'

export interface FileUploadState {
  status: UploadStatus
  rowCount?: number
  excludedRows?: number
  warnings: string[]
  errors: string[]
  fileName?: string
}
