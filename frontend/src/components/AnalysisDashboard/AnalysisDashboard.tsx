import type { BriefDto, ExceptionDto, SystemSummaryDto, EvidenceItem } from '../../types/brief.types'
import './AnalysisDashboard.css'

interface Props {
  brief: BriefDto
}

export function AnalysisDashboard({ brief }: Props) {
  return (
    <div className="dashboard">
      {brief.isSyntheticData && (
        <div className="synthetic-banner">SYNTHETIC DEMO DATA — NOT OFFICIAL STATISTICS</div>
      )}

      <div className="system-tiles">
        {brief.systemSummaries.map(s => <SystemTile key={s.system} summary={s} />)}
      </div>

      {brief.dataQualityWarnings.length > 0 && (
        <details className="dq-panel">
          <summary>⚠ Data Quality Warnings ({brief.dataQualityWarnings.length})</summary>
          <ul>{brief.dataQualityWarnings.map((w, i) => <li key={i}>{w}</li>)}</ul>
        </details>
      )}

      <h2 className="section-title">Critical Exceptions</h2>
      {brief.exceptions.length === 0
        ? <p className="no-exceptions">No anomalies detected above threshold for the uploaded data.</p>
        : <div className="exception-list">
            {brief.exceptions.map((ex, i) => <ExceptionCard key={i} ex={ex} />)}
          </div>
      }
    </div>
  )
}

function SystemTile({ summary }: { summary: SystemSummaryDto }) {
  const { pctChange } = summary
  const changeClass = pctChange == null ? '' : pctChange < 0 ? 'change-down' : 'change-up'

  return (
    <div className="system-tile card">
      <div className="tile-system">{summary.system}</div>
      <div className="tile-total mono">{summary.totalToday.toLocaleString()}</div>
      <div className="tile-label">passengers today</div>
      {pctChange != null && (
        <div className={`tile-change ${changeClass}`}>
          {pctChange > 0 ? '+' : ''}{pctChange.toFixed(1)}% vs baseline
        </div>
      )}
      {summary.scheduleAdherence != null && (
        <div className="tile-adherence">Schedule: {summary.scheduleAdherence.toFixed(1)}%</div>
      )}
    </div>
  )
}

function ExceptionCard({ ex }: { ex: ExceptionDto }) {
  return (
    <div className={`exception-card card severity-${ex.severity}`}>
      <div className="ex-header">
        <div>
          <div className="ex-name">{ex.entityName}</div>
          <div className="ex-system">{ex.system} · {ex.metric.replace('_', ' ')}</div>
        </div>
        <div className="ex-right">
          <div className="ex-change mono">
            {ex.pctChange != null ? `${ex.pctChange > 0 ? '+' : ''}${ex.pctChange.toFixed(1)}%` : 'N/A'}
          </div>
          <span className={`badge badge-${ex.severity}`}>{ex.severity}</span>
        </div>
      </div>

      <div className="ex-counts">
        <span className="mono">{ex.valueToday.toLocaleString()}</span> today vs{' '}
        <span className="mono">{ex.valueBaseline.toLocaleString()}</span> baseline
      </div>

      <div className="ex-evidence">
        {ex.evidence.slice(0, 4).map((ev, i) => <EvidenceLine key={i} item={ev} />)}
      </div>
    </div>
  )
}

function EvidenceLine({ item }: { item: EvidenceItem }) {
  const typeColors: Record<string, string> = {
    FACT: '#475569',
    INCIDENT: '#DC2626',
    CORRELATION: '#2563EB',
    POSSIBLE_CAUSE: '#92400E',
    UNCONFIRMED: '#94A3B8',
  }
  const typeLabels: Record<string, string> = {
    FACT: 'Fact',
    INCIDENT: 'Incident',
    CORRELATION: 'Correlation',
    POSSIBLE_CAUSE: 'Possible cause',
    UNCONFIRMED: 'Unconfirmed',
  }

  return (
    <div className="evidence-line">
      <span className="evidence-badge" style={{ color: typeColors[item.type] }}>
        [{typeLabels[item.type]}]
      </span>{' '}
      {item.description}
    </div>
  )
}
