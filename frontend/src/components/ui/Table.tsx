import React from 'react'

interface Column<T> {
  key: string
  header: string
  render?: (row: T) => React.ReactNode
}

interface TableProps<T> {
  columns: Column<T>[]
  data: T[]
  keyField: keyof T
  isLoading?: boolean
  emptyMessage?: string
}

export function Table<T>({
  columns,
  data,
  keyField,
  isLoading = false,
  emptyMessage = 'No data available',
}: TableProps<T>) {
  if (isLoading) {
    return (
      <div className="table-container">
        <div className="p-8 text-center">
          <div className="spinner w-6 h-6 mx-auto mb-2" />
          <p className="text-neutral-500 text-sm">Loading...</p>
        </div>
      </div>
    )
  }

  if (data.length === 0) {
    return (
      <div className="table-container">
        <div className="p-8 text-center">
          <p className="text-neutral-500 text-sm">{emptyMessage}</p>
        </div>
      </div>
    )
  }

  return (
    <div className="table-container overflow-x-auto">
      <table className="table-base">
        <thead>
          <tr className="table-header">
            {columns.map(column => (
              <th
                key={column.key}
                className="px-6 py-3 text-left text-xs font-medium text-neutral-500 uppercase tracking-wider"
              >
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-neutral-100">
          {data.map(row => (
            <tr key={String(row[keyField])} className="table-row">
              {columns.map(column => (
                <td key={column.key} className="table-cell">
                  {column.render
                    ? column.render(row)
                    : String(row[column.key as keyof T] ?? '')}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
