import {
  Cell,
  Pie,
  PieChart as RechartsPie,
  Sector,
  Tooltip,
  type PieSectorDataItem,
} from 'recharts'
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

function ActiveSlice({
  cx,
  cy,
  innerRadius,
  outerRadius,
  startAngle,
  endAngle,
  fill,
}: PieSectorDataItem) {
  return (
    <g>
      <Sector
        cx={cx}
        cy={cy}
        innerRadius={innerRadius}
        outerRadius={Number(outerRadius) + 8}
        startAngle={startAngle}
        endAngle={endAngle}
        fill={fill}
        stroke="#0b1220"
        strokeWidth={1}
      />
      <Sector
        cx={cx}
        cy={cy}
        innerRadius={Number(outerRadius) + 12}
        outerRadius={Number(outerRadius) + 16}
        startAngle={startAngle}
        endAngle={endAngle}
        fill={fill}
      />
    </g>
  )
}

function sliceLabel({
  cx,
  cy,
  midAngle,
  outerRadius,
  percent,
  name,
}: {
  cx?: number
  cy?: number
  midAngle?: number
  outerRadius?: number
  percent?: number
  name?: string | number
}) {
  if (
    cx == null ||
    cy == null ||
    midAngle == null ||
    outerRadius == null ||
    percent == null ||
    percent < 0.06
  ) {
    return null
  }

  const rad = (-midAngle * Math.PI) / 180
  const x = cx + (outerRadius + 22) * Math.cos(rad)
  const y = cy + (outerRadius + 22) * Math.sin(rad)

  return (
    <text
      x={x}
      y={y}
      fill="#c5d0e6"
      textAnchor={x > cx ? 'start' : 'end'}
      dominantBaseline="central"
      fontSize={11}
      fontWeight={600}
    >
      {name} {(percent * 100).toFixed(0)}%
    </text>
  )
}

export function PieChart({ slices, emptyMessage, centerLabel }: PieChartProps) {
  const total = slices.reduce((sum, slice) => sum + slice.value, 0)

  if (slices.length === 0 || total <= 0) {
    return <p className="muted pie-empty">{emptyMessage}</p>
  }

  return (
    <div className="pie-chart">
      <div className="pie-visual">
        <RechartsPie width={320} height={260} margin={{ top: 8, right: 24, bottom: 8, left: 24 }}>
          <Pie
            data={slices}
            dataKey="value"
            nameKey="label"
            cx="50%"
            cy="50%"
            innerRadius={52}
            outerRadius={88}
            paddingAngle={3}
            cornerRadius={4}
            stroke="#0b1220"
            strokeWidth={2}
            label={sliceLabel}
            labelLine={{
              stroke: '#5b6b86',
              strokeWidth: 1,
            }}
            activeShape={ActiveSlice as never}
            isAnimationActive
          >
            {slices.map((slice) => (
              <Cell key={slice.label} fill={slice.color} />
            ))}
          </Pie>
          <Tooltip
            formatter={(value) => formatMoney(Number(value ?? 0))}
            contentStyle={{
              background: 'rgba(12, 18, 32, 0.96)',
              border: '1px solid rgba(255, 255, 255, 0.12)',
              borderRadius: 8,
              boxShadow: '0 10px 28px rgba(0, 0, 0, 0.35)',
              color: '#fff',
              fontSize: 13,
            }}
            itemStyle={{ color: '#fff' }}
            labelStyle={{ color: '#9aa8c0', fontWeight: 600, marginBottom: 2 }}
          />
        </RechartsPie>
        <div className="pie-center">
          <span className="pie-center-label">{centerLabel ?? 'Total'}</span>
          <span className="pie-center-value">{formatMoney(total)}</span>
        </div>
      </div>
      <ul className="pie-legend">
        {slices.map((slice) => {
          const percent = (slice.value / total) * 100
          return (
            <li key={slice.label}>
              <span className="spend-dot" style={{ background: slice.color }} />
              <span className="pie-legend-name">{slice.label}</span>
              <span className="pie-legend-meta">
                {formatMoney(slice.value)} · {percent.toFixed(1)}%
              </span>
            </li>
          )
        })}
      </ul>
    </div>
  )
}
