import { useState } from 'react'
import { UploadPanel } from './components/UploadPanel/UploadPanel'
import { AnalysisDashboard } from './components/AnalysisDashboard/AnalysisDashboard'
import { BriefPreview } from './components/BriefPreview/BriefPreview'
import { runAnalysis } from './api/api'
import type { BriefDto } from './types/brief.types'
import './App.css'

type View = 'upload' | 'dashboard' | 'brief'

export default function App() {
  const [ready, setReady] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [brief, setBrief] = useState<BriefDto | null>(null)
  const [view, setView] = useState<View>('upload')

  async function handleGenerate() {
    setLoading(true)
    setError(null)
    try {
      const result = await runAnalysis()
      setBrief(result)
      setView('dashboard')
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Analysis failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="app">
      {/* Header — always visible */}
      <header className="app-header no-print">
        <div className="app-logo">
          <span className="logo-icon">⬡</span>
          <span className="logo-text">TransitPulse</span>
        </div>
        <div className="app-tagline">From Transport Data to Decisions in Two Minutes</div>
        <div className="header-actions">
          {brief && (
            <>
              <button className="btn-secondary" onClick={() => setView('upload')}>
                Upload Files
              </button>
              <button className="btn-secondary" onClick={() => setView('dashboard')}>
                Dashboard
              </button>
              <button className="btn-primary" onClick={() => setView('brief')}>
                View Brief
              </button>
            </>
          )}
          {!brief && (
            <button className="btn-primary generate-btn"
              disabled={!ready || loading}
              onClick={handleGenerate}>
              {loading ? 'Analysing…' : 'Generate Brief'}
            </button>
          )}
        </div>
      </header>

      {/* Body */}
      <main className="app-main">
        {/* Upload + generate pane — shown when no brief yet or user goes back */}
        {(view === 'upload' || !brief) && (
          <div className="upload-layout">
            <aside className="upload-sidebar">
              <h2 className="sidebar-title">Data Upload</h2>
              <p className="sidebar-desc">
                Upload today's CSV files from Metro, BRTS, and AMTS. Add baseline (yesterday) files
                to enable day-over-day comparisons.
              </p>
              <UploadPanel onAllRequiredUploaded={setReady} />

              {error && <div className="error-banner">⚠ {error}</div>}

              <button className="btn-primary generate-btn-sidebar"
                disabled={!ready || loading}
                onClick={handleGenerate}>
                {loading ? 'Analysing data…' : 'Generate Brief'}
              </button>

              {loading && (
                <div className="loading-state">
                  <div className="spinner" />
                  Analysing data and generating brief…
                </div>
              )}
            </aside>

            <div className="upload-main-content">
              <div className="welcome-card card">
                <h3>How TransitPulse works</h3>
                <ol>
                  <li>Upload daily CSV data from Metro, BRTS, and AMTS.</li>
                  <li>Add baseline data from the previous day for comparison.</li>
                  <li>Optionally upload incident logs and weather observations.</li>
                  <li>Click <strong>Generate Brief</strong> to detect anomalies and produce the report.</li>
                </ol>
                <p className="welcome-note">
                  Sample synthetic CSV files are available in the <code>backend/src/main/resources/sample-data/</code> folder.
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Dashboard view */}
        {view === 'dashboard' && brief && (
          <div className="content-area">
            <div className="content-header">
              <h2>Analysis — {brief.reportDate}</h2>
              <button className="btn-primary" onClick={() => setView('brief')}>View One-Page Brief →</button>
            </div>
            <AnalysisDashboard brief={brief} />
          </div>
        )}
      </main>

      {/* Brief overlay */}
      {view === 'brief' && brief && (
        <BriefPreview brief={brief} onClose={() => setView('dashboard')} />
      )}
    </div>
  )
}
