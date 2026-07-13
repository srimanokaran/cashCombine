import { NavLink, Route, Routes } from 'react-router-dom'
import { AccountDetailPage } from './pages/AccountDetailPage'
import { AccountsPage } from './pages/AccountsPage'
import { CategoriesRulesPage } from './pages/CategoriesRulesPage'
import './App.css'

function App() {
  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">cashCombine</div>
        <nav>
          <NavLink to="/" end>
            Accounts
          </NavLink>
          <NavLink to="/categories">Categories &amp; rules</NavLink>
        </nav>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<AccountsPage />} />
          <Route path="/accounts/:id" element={<AccountDetailPage />} />
          <Route path="/categories" element={<CategoriesRulesPage />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
