import { useState } from 'react'
import { formatMoney } from '../format'

export type PieSlice = {
  label: string
  value: number
  color: string
}

type PieChartProps = {
  slices: PieSlice[]
  emptyMessage: string
  centerLabel?: string
}

type HoveredSlice = {
  label: string
  value: number
  percent: number
  color: string
  x: number
  y: number
}

const SIZE = 220
const CX = SIZE / 2
const CY = SIZE / 2
const OUTER_R = 96
const INNER_R = 58

function polar(cx: number, cy: number, r: number, angleDeg: number) {
  const rad = ((angleDeg - 90) * Math.PI) / 180
  return { x: cx + r * Math.cos(rad), y: cy + r * Math.sin(rad) }
}

function donutPath(startAngle: number, endAngle: number) {
  if (endAngle - startAngle >= 359.999) {
    // Full circle: two semicircle arcs (SVG can't draw a full arc in one sweep cleanly).
    const top = polar(CX, CY, OUTER_R, 0)
    const bottom = polar(CX, CY, OUTER_R, 180)
    const iTop = polar(CX, CY, INNER_R, 0)
    const iBottom = polar(CX, CY, INNER_R, 180)
    return [
      `M ${top.x} ${top.y}`,
      `A ${OUTER_R} ${OUTER_R} 0 1 1 ${bottom.x} ${bottom.y}`,
      `A ${OUTER_R} ${OUTER_R} 0 1 1 ${top.x} ${top.y}`,
      `L ${iTop.x} ${iTop.y}`,
      `A ${INNER_R} ${INNER_R} 0 1 0 ${iBottom.x} ${iBottom.y}`,
      `A ${INNER_R} ${INNER_R} 0 1 0 ${iTop.x} ${iTop.y}`,
      'Z',
    ].join(' ')
  }

  const oStart = polar(CX, CY, OUTER_R, startAngle)
  const oEnd = polar(CX, CY, OUTER_R, endAngle)
  const iEnd = polar(CX, CY, INNER_R, endAngle)
  const iStart = polar(CX, CY, INNER_R, startAngle)
  const large = endAngle - startAngle > 180 ? 1 : 0
  return [
    `M ${oStart.x} ${oStart.y}`,
    `A ${OUTER_R} ${OUTER_R} 0 ${large} 1 ${oEnd.x} ${oEnd.y}`,
    `L ${iEnd.x} ${iEnd.y}`,
    `A ${INNER_R} ${INNER_R} 0 ${large} 0 ${iStart.x} ${iStart.y}`,
    'Z',
  ].join(' ')
}

export function PieChart({ slices, emptyMessage, centerLabel }: PieChartProps) {
  const [hovered, setHovered] = useState<HoveredSlice | null>(null)
  const total = slices.reduce((sum, slice) => sum + slice.value, 0)

  if (slices.length === 0 || total <= 0) {
    return <p className="muted pie-empty">{emptyMessage}</p>
  }

  let angle = 0
  const paths = slices.map((slice) => {
    const sweep = (slice.value / total) * 360
    const start = angle
    const end = angle + sweep
    angle = end
    return { ...slice, start, end, percent: (slice.value / total) * 100 }
  })

  return (
    <div className="pie-chart">
      <div className="pie-visual">
        <svg
          viewBox={`0 0 ${SIZE} ${SIZE}`}
          role="img"
          aria-label={centerLabel ?? 'Breakdown'}
          onMouseLeave={() => setHovered(null)}
        >
          {paths.map((slice) => {
            const isHovered = hovered?.label === slice.label
            return (
              <path
                key={slice.label}
                className={`pie-slice${isHovered ? ' is-hovered' : ''}${hovered && !isHovered ? ' is-dimmed' : ''}`}
                d={donutPath(slice.start, slice.end)}
                fill={slice.color}
                onMouseEnter={(event) => {
                  const rect = event.currentTarget.ownerSVGElement?.getBoundingClientRect()
                  if (!rect) {
                    return
                  }
                  setHovered({
                    label: slice.label,
                    value: slice.value,
                    percent: slice.percent,
                    color: slice.color,
                    x: event.clientX - rect.left,
                    y: event.clientY - rect.top,
                  })
                }}
                onMouseMove={(event) => {
                  const rect = event.currentTarget.ownerSVGElement?.getBoundingClientRect()
                  if (!rect) {
                    return
                  }
                  setHovered({
                    label: slice.label,
                    value: slice.value,
                    percent: slice.percent,
                    color: slice.color,
                    x: event.clientX - rect.left,
                    y: event.clientY - rect.top,
                  })
                }}
              />
            )
          })}
        </svg>
        <div className="pie-center">
          <span className="pie-center-label">{centerLabel ?? 'Total'}</span>
          <span className="pie-center-value">{formatMoney(total)}</span>
        </div>
        {hovered && (
          <div
            className="pie-tooltip"
            style={{
              left: hovered.x,
              top: hovered.y,
            }}
            role="tooltip"
          >
            <span className="pie-tooltip-dot" style={{ background: hovered.color }} />
            <span className="pie-tooltip-label">{hovered.label}</span>
            <span className="pie-tooltip-value">
              {formatMoney(hovered.value)}
              <span className="pie-tooltip-percent"> · {hovered.percent.toFixed(1)}%</span>
            </span>
          </div>
        )}
      </div>
      <ul className="pie-legend">
        {paths.map((slice) => (
          <li
            key={slice.label}
            className={hovered?.label === slice.label ? 'is-hovered' : undefined}
          >
            <span className="spend-dot" style={{ background: slice.color }} />
            <span className="pie-legend-name">{slice.label}</span>
            <span className="pie-legend-meta">
              {formatMoney(slice.value)} · {slice.percent.toFixed(1)}%
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}
