import { useEffect, useState, type ReactNode, type TransitionEvent } from 'react'

type AnimatedExpandProps = {
  open: boolean
  children: ReactNode
}

/** Height animation via grid 0fr/1fr; keeps children mounted until close finishes. */
export function AnimatedExpand({ open, children }: AnimatedExpandProps) {
  const [mounted, setMounted] = useState(open)

  useEffect(() => {
    if (open) {
      setMounted(true)
    }
  }, [open])

  function onTransitionEnd(event: TransitionEvent<HTMLDivElement>) {
    if (event.propertyName !== 'grid-template-rows') {
      return
    }
    if (!open) {
      setMounted(false)
    }
  }

  return (
    <div
      className={`animated-expand${open ? ' open' : ''}`}
      onTransitionEnd={onTransitionEnd}
      aria-hidden={!open}
    >
      <div className="animated-expand-inner">{mounted ? children : null}</div>
    </div>
  )
}
