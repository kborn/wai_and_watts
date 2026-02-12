import React from 'react'

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
}

export const Input: React.FC<InputProps> = ({
  label,
  error,
  className = '',
  id,
  ...props
}) => {
  const inputId = id || label?.toLowerCase().replace(/\s+/g, '-')

  return (
    <div className="space-y-1">
      {label && (
        <label
          htmlFor={inputId}
          className="block text-sm font-medium text-neutral-700"
        >
          {label}
        </label>
      )}
      <input
        id={inputId}
        className={`input-base ${error ? 'border-error-500 focus:ring-error-500' : ''} ${className}`}
        {...props}
      />
      {error && <p className="text-sm text-error-600">{error}</p>}
    </div>
  )
}

interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string
  error?: string
}

export const Textarea: React.FC<TextareaProps> = ({
  label,
  error,
  className = '',
  id,
  ...props
}) => {
  const inputId = id || label?.toLowerCase().replace(/\s+/g, '-')

  return (
    <div className="space-y-1">
      {label && (
        <label
          htmlFor={inputId}
          className="block text-sm font-medium text-neutral-700"
        >
          {label}
        </label>
      )}
      <textarea
        id={inputId}
        className={`input-base resize-none ${error ? 'border-error-500 focus:ring-error-500' : ''} ${className}`}
        {...props}
      />
      {error && <p className="text-sm text-error-600">{error}</p>}
    </div>
  )
}
