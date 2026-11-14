import { NavLink, Route, Routes } from 'react-router-dom'
import { AccountDetailPage } from './pages/AccountDetailPage'
import { AccountsPage } from './pages/AccountsPage'
import { CategoriesRulesPage } from './pages/CategoriesRulesPage'
import { CategoryTrendsPage } from './pages/CategoryTrendsPage'
import { ChartsDashboardPage } from './pages/ChartsDashboardPage'
import { DashboardPage } from './pages/DashboardPage'
import { SearchPage } from './pages/SearchPage'
import { TrendsPage } from './pages/TrendsPage'
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
          <NavLink to="/trends">Trends</NavLink>
          <NavLink to="/category-trends">Category trends</NavLink>
          <NavLink to="/expenses">Expenses</NavLink>
          <NavLink to="/search">Search</NavLink>
          <NavLink to="/categories">Categories</NavLink>
        </nav>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<AccountsPage />} />
          <Route path="/dashboard" element={<ChartsDashboardPage />} />
          <Route path="/trends" element={<TrendsPage />} />
          <Route path="/category-trends" element={<CategoryTrendsPage />} />
          <Route path="/expenses" element={<DashboardPage />} />
          <Route path="/search" element={<SearchPage />} />
          <Route path="/accounts/:id" element={<AccountDetailPage />} />
          <Route path="/categories" element={<CategoriesRulesPage />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
