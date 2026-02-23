import React from 'react'
import { Link, useLocation } from 'react-router-dom'
import type { NavBarProps } from '../types'

const NavBar: React.FC<NavBarProps> = () => {
  const location = useLocation()

  const isActive = (path: string) => {
    if (path === '/' && location.pathname === '/') return true
    return path !== '/' && location.pathname.startsWith(path)
  }

  const navigation = [
    { name: 'Home', href: '/' },
    { name: 'Ask', href: '/ask' },
    { name: 'MBIE Data', href: '/browse/mbie' },
    { name: 'LAWA Data', href: '/browse/lawa' },
  ]

  return (
    <nav className="bg-white/95 backdrop-blur-sm border-b border-neutral-200 shadow-sm sticky top-0 z-50">
      <div className="page-container">
        <div className="flex justify-between h-16">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <Link to="/" className="group">
                <h1 className="text-xl font-bold text-neutral-900 group-hover:text-blue-600 transition-colors duration-200">
                  Wai & Watts
                </h1>
                <p className="text-xs text-neutral-500">
                  Environmental Data Insights
                </p>
              </Link>
            </div>
          </div>

          <div className="hidden sm:ml-8 sm:flex sm:items-center sm:space-x-1">
            {navigation.map(item => {
              const active = isActive(item.href)
              return (
                <Link
                  key={item.name}
                  to={item.href}
                  className={`px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200 ${
                    active
                      ? 'bg-blue-50 text-blue-700 border-l-2 border-blue-600'
                      : 'text-neutral-600 hover:text-neutral-900 hover:bg-neutral-50'
                  }`}
                >
                  {item.name}
                </Link>
              )
            })}
          </div>
        </div>

        <div className="sm:hidden flex gap-1 pb-3 overflow-x-auto">
          {navigation.map(item => {
            const active = isActive(item.href)
            return (
              <Link
                key={`mobile-${item.name}`}
                to={item.href}
                className={`px-3 py-1.5 rounded-md text-xs font-medium whitespace-nowrap transition-all duration-200 ${
                  active
                    ? 'bg-blue-50 text-blue-700 border border-blue-200'
                    : 'text-neutral-600 border border-neutral-200'
                }`}
              >
                {item.name}
              </Link>
            )
          })}
        </div>
      </div>
    </nav>
  )
}

export default NavBar
