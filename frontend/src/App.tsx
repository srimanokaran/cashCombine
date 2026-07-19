import { NavLink, Route, Routes } from 'react-router-dom'
import { AccountDetailPage } from './pages/AccountDetailPage'
import { AccountsPage } from './pages/AccountsPage'
import { CategoriesRulesPage } from './pages/CategoriesRulesPage'
import { ChartsDashboardPage } from './pages/ChartsDashboardPage'
import { DashboardPage } from './pages/DashboardPage'
import './App.css'

function App() {
  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">Cash Combine</div>
        <nav>
          <NavLink to="/" end>
            Import
          </NavLink>
          <NavLink to="/dashboard">Dashboard</NavLink>
          <NavLink to="/expenses">Expenses</NavLink>
          <NavLink to="/categories">Categories &amp; rules</NavLink>
        </nav>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<AccountsPage />} />
          <Route path="/dashboard" element={<ChartsDashboardPage />} />
          <Route path="/expenses" element={<DashboardPage />} />
          <Route path="/accounts/:id" element={<AccountDetailPage />} />
          <Route path="/categories" element={<CategoriesRulesPage />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
