import { useRef, useState } from 'react'
import type { FileUploadState } from '../../types/brief.types'
import { uploadFile } from '../../api/api'
import './UploadPanel.css'

interface FileSlot {
  key: string
  label: string
  endpoint: string
  required: boolean
  baseline?: boolean
  hint: string
}

const FILE_SLOTS: FileSlot[] = [
  { key: 'metro', label: 'Metro Ridership (Today)', endpoint: 'metro', required: true, hint: 'metro-today.csv' },
  { key: 'metro-baseline', label: 'Metro Ridership (Baseline)', endpoint: 'metro', required: false, baseline: true, hint: 'metro-baseline.csv' },
  { key: 'brts', label: 'BRTS Ridership (Today)', endpoint: 'brts', required: true, hint: 'brts-today.csv' },
  { key: 'brts-baseline', label: 'BRTS Ridership (Baseline)', endpoint: 'brts', required: false, baseline: true, hint: 'brts-baseline.csv' },
  { key: 'amts', label: 'AMTS Ridership (Today)', endpoint: 'amts', required: true, hint: 'amts-today.csv' },
  { key: 'amts-baseline', label: 'AMTS Ridership (Baseline)', endpoint: 'amts', required: false, baseline: true, hint: 'amts-baseline.csv' },
  { key: 'incidents', label: 'Incidents & Disruptions', endpoint: 'incidents', required: false, hint: 'incidents.csv' },
  { key: 'weather', label: 'Weather Observations', endpoint: 'weather', required: false, hint: 'weather.csv' },
]

interface Props {
  onAllRequiredUploaded: (ready: boolean) => void
}

export function UploadPanel({ onAllRequiredUploaded }: Props) {
  const [states, setStates] = useState<Record<string, FileUploadState>>({})

  function updateState(key: string, update: Partial<FileUploadState>) {
    setStates(prev => {
      const next = { ...prev, [key]: { warnings: [], errors: [], ...prev[key], ...update } }
      // Check if all required slots are valid
      const allValid = FILE_SLOTS.filter(s => s.required).every(s => next[s.key]?.status === 'valid')
      onAllRequiredUploaded(allValid)
      return next
    })
  }

  async function handleFile(slot: FileSlot, file: File) {
    updateState(slot.key, { status: 'uploading', fileName: file.name, warnings: [], errors: [] })
    try {
      const result = await uploadFile(slot.endpoint, file, slot.baseline)
      if (result.status === 'OK') {
        updateState(slot.key, {
          status: 'valid',
          rowCount: result.rowCount,
          excludedRows: result.excludedRows,
          warnings: result.warnings,
          errors: [],
        })
      } else {
        updateState(slot.key, { status: 'error', errors: result.errors, warnings: result.warnings })
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Upload failed'
      updateState(slot.key, { status: 'error', errors: [msg] })
    }
  }

  return (
    <div className="upload-panel">
      {FILE_SLOTS.map(slot => {
        const state = states[slot.key] || { status: 'idle', warnings: [], errors: [] }
        return <FileSlotRow key={slot.key} slot={slot} state={state} onFile={f => handleFile(slot, f)} />
      })}
    </div>
  )
}

function FileSlotRow({ slot, state, onFile }: { slot: FileSlot; state: FileUploadState; onFile: (f: File) => void }) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [dragOver, setDragOver] = useState(false)

  function handleDrop(e: React.DragEvent) {
    e.preventDefault()
    setDragOver(false)
    const file = e.dataTransfer.files[0]
    if (file) onFile(file)
  }

  const statusIcon = {
    idle: '○',
    uploading: '◌',
    valid: '✓',
    error: '✗',
  }[state.status]

  const statusClass = `slot-status-${state.status}`

  return (
    <div className={`file-slot ${dragOver ? 'drag-over' : ''}`}
      onDragOver={e => { e.preventDefault(); setDragOver(true) }}
      onDragLeave={() => setDragOver(false)}
      onDrop={handleDrop}>
      <div className="slot-header">
        <span className={`slot-icon ${statusClass}`}>{statusIcon}</span>
        <span className="slot-label">
          {slot.label}
          {slot.required && <span className="required-star"> *</span>}
        </span>
        <button className="btn-secondary slot-pick"
          onClick={() => inputRef.current?.click()}>
          {state.status === 'uploading' ? 'Uploading…' : 'Choose file'}
        </button>
        <input ref={inputRef} type="file" accept=".csv" style={{ display: 'none' }}
          onChange={e => { const f = e.target.files?.[0]; if (f) onFile(f) }} />
      </div>

      {state.fileName && (
        <div className="slot-filename">{state.fileName}
          {state.rowCount !== undefined && (
            <span className="slot-count"> — {state.rowCount} rows
              {state.excludedRows ? `, ${state.excludedRows} excluded` : ''}
            </span>
          )}
        </div>
      )}

      {state.errors.length > 0 && (
        <div className="slot-messages slot-errors">
          {state.errors.map((e, i) => <div key={i}>{e}</div>)}
        </div>
      )}
      {state.warnings.length > 0 && state.status !== 'error' && (
        <div className="slot-messages slot-warnings">
          {state.warnings.slice(0, 3).map((w, i) => <div key={i}>{w}</div>)}
          {state.warnings.length > 3 && <div>… and {state.warnings.length - 3} more</div>}
        </div>
      )}

      {state.status === 'idle' && (
        <div className="slot-hint">Hint: {slot.hint}</div>
      )}
    </div>
  )
}
