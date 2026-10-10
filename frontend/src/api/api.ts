import type { BriefDto, UploadResult } from '../types/brief.types'

const BASE = '/api'

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const body = await res.json().catch(() => ({ error: res.statusText }))
    throw new Error(body.error || body.errors?.[0] || `HTTP ${res.status}`)
  }
  return res.json()
}

export async function uploadFile(
  endpoint: string,
  file: File,
  baseline = false
): Promise<UploadResult> {
  const form = new FormData()
  form.append('file', file)
  if (baseline) form.append('baseline', 'true')
  const res = await fetch(`${BASE}/upload/${endpoint}`, { method: 'POST', body: form })
  return handleResponse<UploadResult>(res)
}

export async function runAnalysis(reportDate?: string): Promise<BriefDto> {
  const res = await fetch(`${BASE}/analysis/run`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(reportDate ? { reportDate } : {}),
  })
  return handleResponse<BriefDto>(res)
}

export async function getAnalysisStatus(): Promise<{ state: string }> {
  const res = await fetch(`${BASE}/analysis/status`)
  return handleResponse<{ state: string }>(res)
}
