import { Outlet } from 'react-router-dom'
import { motion } from 'framer-motion'
import Sidebar from '../components/Sidebar.jsx'
import Header from '../components/Header.jsx'

export default function DashboardLayout() {
  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <div className="relative flex min-h-screen flex-1 flex-col lg:pl-64">
        <Header />
        <motion.main
          className="flex-1 px-4 py-6 sm:px-6 lg:px-10 lg:py-10"
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25 }}
        >
          <div className="dashboard-shell">
            <Outlet />
          </div>
        </motion.main>
        <footer className="mt-auto border-t border-slate-200/60 bg-white/40 px-6 py-5 backdrop-blur-sm lg:px-10">
          <p className="text-center text-[11px] font-medium uppercase tracking-wider text-slate-400">
            SLIIT Smart Campus Hub · IT3030 PAF
          </p>
        </footer>
      </div>
    </div>
  )
}
