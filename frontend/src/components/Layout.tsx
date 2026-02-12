import React from 'react'
import NavBar from './NavBar'

interface LayoutProps {
  children: React.ReactNode
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  return (
    <div className="min-h-screen bg-gradient-to-br from-neutral-50 to-neutral-100">
      <NavBar />
      <main className="page-container py-8">
        <div className="max-w-4xl mx-auto animate-fade-in">{children}</div>
      </main>
    </div>
  )
}

export default Layout
