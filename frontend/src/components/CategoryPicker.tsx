import { useEffect, useId, useLayoutEffect, useMemo, useRef, useState, type KeyboardEvent } from 'react'
import { createPortal } from 'react-dom'
import type { Category } from '../types'

type CategoryPickerProps = {
  categories: Category[]
  value: string
  disabled?: boolean
  ariaLabel: string
  onChange: (categoryId: string) => void | Promise<void>
}

type MenuPosition = {
  top: number
  left: number
  width: number
  openUp: boolean
}

export function CategoryPicker({
  categories,
  value,
  disabled = false,
  ariaLabel,
  onChange,
}: CategoryPickerProps) {
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [activeIndex, setActiveIndex] = useState(0)
  const [menuPosition, setMenuPosition] = useState<MenuPosition | null>(null)
  const rootRef = useRef<HTMLDivElement>(null)
  const triggerRef = useRef<HTMLButtonElement>(null)
  const menuRef = useRef<HTMLDivElement>(null)
  const searchRef = useRef<HTMLInputElement>(null)
  const listId = useId()

  const selected = categories.find((category) => category.id === value)
  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase()
    if (!needle) {
      return categories
    }
    return categories.filter((category) => category.name.toLowerCase().includes(needle))
  }, [categories, query])

  function updateMenuPosition() {
    const trigger = triggerRef.current
    if (!trigger) {
      return
    }
    const rect = trigger.getBoundingClientRect()
    const menuHeight = 280
    const gap = 6
    const spaceBelow = window.innerHeight - rect.bottom
    const openUp = spaceBelow < menuHeight && rect.top > spaceBelow
    const width = Math.max(rect.width, 220)
    const left = Math.min(rect.right - width, window.innerWidth - width - 8)
    setMenuPosition({
      top: openUp ? rect.top - gap : rect.bottom + gap,
      left: Math.max(8, left),
      width,
      openUp,
    })
  }

  useLayoutEffect(() => {
    if (!open) {
      setMenuPosition(null)
      return
    }
    updateMenuPosition()
    const selectedIndex = filtered.findIndex((category) => category.id === value)
    setActiveIndex(selectedIndex >= 0 ? selectedIndex : 0)
    const frame = window.requestAnimationFrame(() => searchRef.current?.focus())

    function onReposition() {
      updateMenuPosition()
    }
    window.addEventListener('resize', onReposition)
    window.addEventListener('scroll', onReposition, true)
    return () => {
      window.cancelAnimationFrame(frame)
      window.removeEventListener('resize', onReposition)
      window.removeEventListener('scroll', onReposition, true)
    }
  }, [open, filtered, value])

  useEffect(() => {
    if (!open) {
      return
    }

    function onPointerDown(event: MouseEvent) {
      const target = event.target as Node
      if (rootRef.current?.contains(target) || menuRef.current?.contains(target)) {
        return
      }
      setOpen(false)
      setQuery('')
    }

    function onKeyDown(event: globalThis.KeyboardEvent) {
      if (event.key === 'Escape') {
        event.preventDefault()
        setOpen(false)
        setQuery('')
        triggerRef.current?.focus()
      }
    }

    document.addEventListener('mousedown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('mousedown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  function close() {
    setOpen(false)
    setQuery('')
  }

  function choose(categoryId: string) {
    close()
    triggerRef.current?.focus()
    if (categoryId !== value) {
      void onChange(categoryId)
    }
  }

  function onTriggerKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    if (event.key === 'ArrowDown' || event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      if (!disabled) {
        setOpen(true)
      }
    }
  }

  function onSearchKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      setActiveIndex((index) => Math.min(index + 1, Math.max(filtered.length - 1, 0)))
      return
    }
    if (event.key === 'ArrowUp') {
      event.preventDefault()
      setActiveIndex((index) => Math.max(index - 1, 0))
      return
    }
    if (event.key === 'Enter') {
      event.preventDefault()
      const choice = filtered[activeIndex]
      if (choice) {
        choose(choice.id)
      }
    }
  }

  const menu =
    open && menuPosition
      ? createPortal(
          <div
            ref={menuRef}
            className={`category-picker-menu${menuPosition.openUp ? ' open-up' : ''}`}
            role="presentation"
            style={{
              top: menuPosition.openUp ? undefined : menuPosition.top,
              bottom: menuPosition.openUp ? window.innerHeight - menuPosition.top : undefined,
              left: menuPosition.left,
              width: menuPosition.width,
            }}
          >
            <input
              ref={searchRef}
              className="category-picker-search"
              type="search"
              value={query}
              placeholder="Filter categories…"
              aria-label="Filter categories"
              onChange={(event) => setQuery(event.target.value)}
              onKeyDown={onSearchKeyDown}
            />
            <ul id={listId} className="category-picker-list" role="listbox" aria-label={ariaLabel}>
              {filtered.length === 0 && (
                <li className="category-picker-empty" role="presentation">
                  No matches
                </li>
              )}
              {filtered.map((category, index) => {
                const isSelected = category.id === value
                const isActive = index === activeIndex
                return (
                  <li key={category.id} role="presentation">
                    <button
                      type="button"
                      role="option"
                      aria-selected={isSelected}
                      className={`category-picker-option${isSelected ? ' selected' : ''}${isActive ? ' active' : ''}`}
                      onMouseEnter={() => setActiveIndex(index)}
                      onClick={() => choose(category.id)}
                    >
                      <span>{category.name}</span>
                      {isSelected && <span className="category-picker-check" aria-hidden>✓</span>}
                    </button>
                  </li>
                )
              })}
            </ul>
          </div>,
          document.body,
        )
      : null

  return (
    <div className={`category-picker${open ? ' open' : ''}`} ref={rootRef}>
      <button
        ref={triggerRef}
        type="button"
        className="category-picker-trigger"
        aria-label={ariaLabel}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={listId}
        disabled={disabled}
        onClick={() => {
          if (disabled) {
            return
          }
          setOpen((wasOpen) => !wasOpen)
          if (open) {
            setQuery('')
          }
        }}
        onKeyDown={onTriggerKeyDown}
      >
        <span className="category-picker-label">{selected?.name ?? 'Choose category'}</span>
        {disabled ? (
          <span className="category-picker-spinner" aria-hidden />
        ) : (
          <span className="category-picker-chevron" aria-hidden>
            ▾
          </span>
        )}
      </button>
      {menu}
    </div>
  )
}
