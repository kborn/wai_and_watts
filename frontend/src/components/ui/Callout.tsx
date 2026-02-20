import React from 'react'

type CalloutVariant = 'info' | 'success' | 'warning' | 'error'

interface CalloutProps {
  variant?: CalloutVariant
  title?: string
  children: React.ReactNode
  className?: string
}

const variantStyles: Record<CalloutVariant, string> = {
  info: 'bg-blue-50 border-blue-200 text-blue-800',
  success: 'bg-success-50 border-success-200 text-success-800',
  warning: 'bg-yellow-50 border-yellow-200 text-yellow-800',
  error: 'bg-error-50 border-error-200 text-error-800',
}

export const Callout: React.FC<CalloutProps> = ({
  variant = 'info',
  title,
  children,
  className = '',
}) => {
  return (
    <div
      className={`border-l-4 px-4 py-3 rounded-lg ${variantStyles[variant]} ${className}`}
    >
      {title && <h4 className="font-medium text-sm mb-1">{title}</h4>}
      <div className="text-sm">{children}</div>
    </div>
  )
}

export const RefusalCallout: React.FC<{
  message: string
  title?: string
  variant?: CalloutVariant
}> = ({ message, title = 'Unable to Answer', variant = 'warning' }) => {
  return (
    <Callout variant={variant} title={title}>
      {message}
    </Callout>
  )
}

export const ErrorCallout: React.FC<{ message: string }> = ({ message }) => {
  return (
    <Callout variant="error" title="Error">
      {message}
    </Callout>
  )
}
