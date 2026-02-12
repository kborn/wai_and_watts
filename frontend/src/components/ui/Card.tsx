import React from 'react'

interface CardProps {
  children: React.ReactNode
  className?: string
  hover?: boolean
}

export const Card: React.FC<CardProps> = ({
  children,
  className = '',
  hover = false,
}) => {
  const baseStyles = 'bg-white rounded-xl border border-neutral-200 shadow-soft'
  const hoverStyles = hover
    ? 'transition-all duration-200 hover:shadow-soft-md hover:border-neutral-300'
    : ''

  return (
    <div className={`${baseStyles} ${hoverStyles} ${className}`}>
      {children}
    </div>
  )
}

export const CardHeader: React.FC<{
  children: React.ReactNode
  className?: string
}> = ({ children, className = '' }) => (
  <div className={`px-6 py-4 border-b border-neutral-200 ${className}`}>
    {children}
  </div>
)

export const CardContent: React.FC<{
  children: React.ReactNode
  className?: string
}> = ({ children, className = '' }) => (
  <div className={`px-6 py-4 ${className}`}>{children}</div>
)

export const CardFooter: React.FC<{
  children: React.ReactNode
  className?: string
}> = ({ children, className = '' }) => (
  <div
    className={`px-6 py-4 border-t border-neutral-200 bg-neutral-50 rounded-b-xl ${className}`}
  >
    {children}
  </div>
)
