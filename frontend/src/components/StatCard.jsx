import { motion } from 'framer-motion'

const tones = {
  violet: {
    bg: 'from-violet-500 to-violet-600',
    iconShadow: 'shadow-violet-500/30',
    glow: 'bg-violet-400/20',
  },
  emerald: {
    bg: 'from-emerald-500 to-emerald-600',
    iconShadow: 'shadow-emerald-500/25',
    glow: 'bg-emerald-400/15',
  },
  amber: {
    bg: 'from-amber-500 to-orange-500',
    iconShadow: 'shadow-amber-500/25',
    glow: 'bg-amber-400/20',
  },
  sky: {
    bg: 'from-sky-500 to-cyan-500',
    iconShadow: 'shadow-sky-500/25',
    glow: 'bg-sky-400/15',
  },
}

export default function StatCard({ title, value, subtitle, icon, tone = 'violet' }) {
  const t = tones[tone] ?? tones.violet

  return (
    <motion.div
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35 }}
      whileHover={{ y: -4 }}
      className="group card relative overflow-hidden p-6 transition-shadow duration-300 hover:shadow-float"
    >
      <div className={`pointer-events-none absolute -right-8 -top-8 h-32 w-32 rounded-full blur-2xl ${t.glow}`} />

      <div className="relative flex items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">{title}</p>
          <p className="mt-3 font-display text-4xl font-bold tabular-nums tracking-tight text-slate-900">{value}</p>
          {subtitle && <p className="mt-2 text-xs font-medium text-slate-500">{subtitle}</p>}
        </div>
        <div
          className={`flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br ${t.bg} text-white shadow-lg ${t.iconShadow} ring-2 ring-white/30 transition-transform duration-300 group-hover:scale-105`}
        >
          {icon}
        </div>
      </div>
    </motion.div>
  )
}
