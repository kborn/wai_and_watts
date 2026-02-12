import React from 'react'

interface SelectOption {
  value: string
  label: string
}

interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string
  options: SelectOption[]
  placeholder?: string
  error?: string
}

export const Select: React.FC<SelectProps> = ({
  label,
  options,
  placeholder,
  error,
  className = '',
  id,
  ...props
}) => {
  const selectId = id || label?.toLowerCase().replace(/\s+/g, '-')

  return (
    <div className="space-y-1">
      {label && (
        <label
          htmlFor={selectId}
          className="block text-sm font-medium text-neutral-700"
        >
          {label}
        </label>
      )}
      <select
        id={selectId}
        className={`select-base ${error ? 'border-error-500 focus:ring-error-500' : ''} ${className}`}
        {...props}
      >
        {placeholder && (
          <option value="" disabled>
            {placeholder}
          </option>
        )}
        {options.map(option => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error && <p className="text-sm text-error-600">{error}</p>}
    </div>
  )
}
