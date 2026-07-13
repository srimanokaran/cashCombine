import { useEffect, useState } from 'react'
import { api } from '../api'
import type { Category, Rule } from '../types'

export function CategoriesRulesPage() {
  const [categories, setCategories] = useState<Category[]>([])
  const [rules, setRules] = useState<Rule[]>([])
  const [categoryName, setCategoryName] = useState('')
  const [pattern, setPattern] = useState('')
  const [ruleCategoryId, setRuleCategoryId] = useState('')
  const [error, setError] = useState<string | null>(null)

  async function load() {
    setError(null)
    try {
      const [categoryData, ruleData] = await Promise.all([
        api.listCategories(),
        api.listRules(),
      ])
      setCategories(categoryData)
      setRules(ruleData)
      if (!ruleCategoryId && categoryData.length > 0) {
        const firstNonDefault =
          categoryData.find((c) => c.name !== 'Uncategorised') ?? categoryData[0]
        setRuleCategoryId(firstNonDefault.id)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load')
    }
  }

  useEffect(() => {
    void load()
  }, [])

  async function onCreateCategory(event: React.FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      await api.createCategory(categoryName.trim())
      setCategoryName('')
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create category')
    }
  }

  async function onCreateRule(event: React.FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      await api.createRule(pattern.trim(), ruleCategoryId)
      setPattern('')
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create rule')
    }
  }

  function categoryLabel(categoryId: string) {
    return categories.find((c) => c.id === categoryId)?.name ?? categoryId
  }

  return (
    <section className="page">
      <h1>Categories &amp; rules</h1>
      <p className="lede">
        Contains-match rules assign categories on import. Manual overrides stay on re-import.
      </p>

      {error && <p className="error">{error}</p>}

      <div className="split">
        <div className="panel">
          <h2>Categories</h2>
          <form className="row-form" onSubmit={onCreateCategory}>
            <input
              value={categoryName}
              onChange={(e) => setCategoryName(e.target.value)}
              placeholder="Category name"
              required
            />
            <button type="submit">Add</button>
          </form>
          <ul className="list compact">
            {categories.map((category) => (
              <li key={category.id}>
                <span>{category.name}</span>
              </li>
            ))}
          </ul>
        </div>

        <div className="panel">
          <h2>Rules</h2>
          <form className="stack-form" onSubmit={onCreateRule}>
            <input
              value={pattern}
              onChange={(e) => setPattern(e.target.value)}
              placeholder="Contains pattern (e.g. WOOLWORTHS)"
              required
            />
            <select
              value={ruleCategoryId}
              onChange={(e) => setRuleCategoryId(e.target.value)}
              required
            >
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
            <button type="submit">Add rule</button>
          </form>
          <ul className="list compact">
            {rules.length === 0 ? (
              <li className="muted">No rules yet.</li>
            ) : (
              rules.map((rule) => (
                <li key={rule.id}>
                  <span>
                    “{rule.pattern}” → {categoryLabel(rule.categoryId)}
                  </span>
                </li>
              ))
            )}
          </ul>
        </div>
      </div>
    </section>
  )
}
