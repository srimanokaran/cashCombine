import { useEffect, useState } from 'react'
import { api } from '../api'
import type { Category, Rule } from '../types'

const UNCATEGORISED = 'Uncategorised'

function ruleTargetCategories(categories: Category[]) {
  return categories.filter((category) => category.name !== UNCATEGORISED)
}

export function CategoriesRulesPage() {
  const [categories, setCategories] = useState<Category[]>([])
  const [rules, setRules] = useState<Rule[]>([])
  const [categoryName, setCategoryName] = useState('')
  const [pattern, setPattern] = useState('')
  const [ruleCategoryId, setRuleCategoryId] = useState('')
  const [error, setError] = useState<string | null>(null)

  const targets = ruleTargetCategories(categories)
  const canAddRule = targets.length > 0 && ruleCategoryId !== ''

  async function load(selectCategoryId?: string) {
    setError(null)
    try {
      const [categoryData, ruleData] = await Promise.all([
        api.listCategories(),
        api.listRules(),
      ])
      setCategories(categoryData)
      setRules(ruleData)

      const available = ruleTargetCategories(categoryData)
      if (selectCategoryId && available.some((c) => c.id === selectCategoryId)) {
        setRuleCategoryId(selectCategoryId)
      } else if (ruleCategoryId && available.some((c) => c.id === ruleCategoryId)) {
        // keep current selection
      } else if (available.length > 0) {
        setRuleCategoryId(available[0].id)
      } else {
        setRuleCategoryId('')
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
      const created = await api.createCategory(categoryName.trim())
      setCategoryName('')
      await load(created.id)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create category')
    }
  }

  async function onCreateRule(event: React.FormEvent) {
    event.preventDefault()
    if (!canAddRule) {
      return
    }
    setError(null)
    try {
      await api.createRule(pattern.trim(), ruleCategoryId)
      setPattern('')
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create rule')
    }
  }

  async function onDeleteRule(rule: Rule) {
    if (!window.confirm(`Delete rule “${rule.pattern}”?`)) {
      return
    }
    setError(null)
    try {
      await api.deleteRule(rule.id)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete rule')
    }
  }

  async function onDeleteCategory(category: Category) {
    if (category.name === UNCATEGORISED) {
      return
    }
    if (
      !window.confirm(
        `Delete category “${category.name}”? Rules for it will be removed and its transactions become Uncategorised.`,
      )
    ) {
      return
    }
    setError(null)
    try {
      await api.deleteCategory(category.id)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete category')
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
                {category.name !== UNCATEGORISED && (
                  <button
                    type="button"
                    className="danger"
                    onClick={() => void onDeleteCategory(category)}
                  >
                    Delete
                  </button>
                )}
              </li>
            ))}
          </ul>
        </div>

        <div className="panel">
          <h2>Rules</h2>
          <form className="stack-form" onSubmit={onCreateRule}>
            <label className="field">
              <span>Pattern</span>
              <input
                value={pattern}
                onChange={(e) => setPattern(e.target.value)}
                placeholder="Contains pattern (e.g. WOOLWORTHS)"
                required
              />
            </label>
            <label className="field">
              <span>Category</span>
              <select
                value={ruleCategoryId}
                onChange={(e) => setRuleCategoryId(e.target.value)}
                required
                disabled={targets.length === 0}
              >
                <option value="" disabled>
                  Select category…
                </option>
                {targets.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
            </label>
            {targets.length === 0 && (
              <p className="muted">Create a category first, then add a rule that maps a pattern to it.</p>
            )}
            <button type="submit" disabled={!canAddRule}>
              Add rule
            </button>
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
                  <button
                    type="button"
                    className="danger"
                    onClick={() => void onDeleteRule(rule)}
                  >
                    Delete
                  </button>
                </li>
              ))
            )}
          </ul>
        </div>
      </div>
    </section>
  )
}
