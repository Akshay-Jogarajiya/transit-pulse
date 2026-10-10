import type { BriefDto } from '../../types/brief.types'
import './BriefPreview.css'

interface Props {
  brief: BriefDto
  onClose: () => void
}

export function BriefPreview({ brief, onClose }: Props) {
  const net = brief.networkSummary
  const reportDate = new Date(brief.reportDate + 'T00:00:00').toLocaleDateString('en-IN', {
    weekday: 'long', year: 'numeric', month: 'long', day: 'numeric',
  })

  return (
    <div className="brief-overlay no-print">
      <div className="brief-container" id="brief-print-area">
        {brief.isSyntheticData && (
          <div className="brief-watermark">SYNTHETIC DEMO DATA</div>
        )}

        {/* Header */}
        <div className="brief-header">
          <div>
            <div className="brief-title">Daily Transport Operations Brief</div>
            <div className="brief-subtitle">Ahmedabad City Transport Authority · TransitPulse</div>
          </div>
          <div className="brief-date">{reportDate}</div>
        </div>

        {/* Network Summary */}
        <div className="brief-network">
          <span className="brief-label">Network Ridership:</span>
          <span className="brief-value mono">{net.totalToday.toLocaleString()}</span>
          {net.pctChange != null && (
            <span className={`brief-change ${net.pctChange < 0 ? 'down' : 'up'}`}>
              {net.pctChange > 0 ? '+' : ''}{net.pctChange.toFixed(1)}% vs {net.totalYesterday.toLocaleString()} (baseline)
            </span>
          )}
        </div>

        {/* System Summaries */}
        <table className="brief-systems-table">
          <thead>
            <tr>
              <th>System</th>
              <th className="mono">Today</th>
              <th className="mono">Baseline</th>
              <th className="mono">Change</th>
              <th>Schedule</th>
            </tr>
          </thead>
          <tbody>
            {brief.systemSummaries.map(s => (
              <tr key={s.system}>
                <td><strong>{s.system}</strong></td>
                <td className="mono right">{s.totalToday.toLocaleString()}</td>
                <td className="mono right">{s.totalYesterday.toLocaleString()}</td>
                <td className={`mono right ${s.pctChange != null && s.pctChange < 0 ? 'red' : 'green'}`}>
                  {s.pctChange != null ? `${s.pctChange > 0 ? '+' : ''}${s.pctChange.toFixed(1)}%` : '—'}
                </td>
                <td className="right">
                  {s.scheduleAdherence != null ? `${s.scheduleAdherence.toFixed(1)}%` : '—'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* Exceptions */}
        <div className="brief-section-title">Critical Exceptions</div>
        {brief.exceptions.length === 0
          ? <p className="brief-none">No anomalies detected above threshold.</p>
          : brief.exceptions.map((ex, i) => (
            <div key={i} className={`brief-exception severity-${ex.severity}`}>
              <div className="bex-header">
                <span className="bex-name">{ex.entityName} ({ex.system})</span>
                <span className={`badge badge-${ex.severity}`}>{ex.severity}</span>
                <span className="bex-change mono">
                  {ex.pctChange != null ? `${ex.pctChange > 0 ? '+' : ''}${ex.pctChange.toFixed(1)}%` : 'N/A'}
                </span>
              </div>
              <div className="bex-counts mono">
                {ex.valueToday.toLocaleString()} today · {ex.valueBaseline.toLocaleString()} baseline
              </div>
              <ul className="bex-evidence">
                {ex.evidence.filter(e => e.type !== 'FACT').slice(0, 3).map((ev, j) => (
                  <li key={j} className={`ev-${ev.type}`}>{ev.description}</li>
                ))}
              </ul>
            </div>
          ))
        }

        {/* Recommended Actions */}
        {brief.recommendedActions.length > 0 && (
          <>
            <div className="brief-section-title">Recommended Actions</div>
            <ol className="brief-actions">
              {brief.recommendedActions.map((a, i) => <li key={i}>{a}</li>)}
            </ol>
          </>
        )}

        {/* AI Narrative */}
        {brief.narrative && (
          <div className="brief-narrative">
            <div className="brief-section-title">Management Summary</div>
            <p>{brief.narrative}</p>
          </div>
        )}

        {/* Footer */}
        <div className="brief-footer">
          {brief.dataQualityWarnings.length > 0 && (
            <div className="brief-dq">
              ⚠ Data quality: {brief.dataQualityWarnings.join(' · ')}
            </div>
          )}
          <div className="brief-meta">
            Narrative source: {brief.narrativeSource} ·
            Generated: {new Date(brief.generatedAt).toLocaleTimeString()} ·
            {brief.isSyntheticData && ' SYNTHETIC DATA — NOT OFFICIAL STATISTICS'}
          </div>
        </div>
      </div>

      {/* Controls (hidden in print) */}
      <div className="brief-controls no-print">
        <button className="btn-primary" onClick={() => window.print()}>Print / Export PDF</button>
        <button className="btn-secondary" onClick={onClose}>← Back to Dashboard</button>
      </div>
    </div>
  )
}
