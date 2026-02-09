import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import type { NavBarProps } from '../types';

const NavBar: React.FC<NavBarProps> = () => {
  const location = useLocation();

  const isActive = (path: string) => location.pathname === path;

  return (
    <nav className="bg-blue-600 text-white shadow-lg">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <h1 className="text-xl font-bold">Wai & Watts</h1>
            </div>
            <div className="hidden sm:ml-6 sm:flex sm:space-x-8">
              <Link
                to="/"
                className={`inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium ${
                  isActive('/')
                    ? 'border-white text-white'
                    : 'border-transparent text-gray-300 hover:border-gray-300 hover:text-white'
                }`}
              >
                Home
              </Link>
              <Link
                to="/ask"
                className={`inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium ${
                  isActive('/ask')
                    ? 'border-white text-white'
                    : 'border-transparent text-gray-300 hover:border-gray-300 hover:text-white'
                }`}
              >
                Ask
              </Link>
              <Link
                to="/browse/mbie"
                className={`inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium ${
                  isActive('/browse/mbie')
                    ? 'border-white text-white'
                    : 'border-transparent text-gray-300 hover:border-gray-300 hover:text-white'
                }`}
              >
                MBIE Data
              </Link>
              <Link
                to="/browse/lawa"
                className={`inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium ${
                  isActive('/browse/lawa')
                    ? 'border-white text-white'
                    : 'border-transparent text-gray-300 hover:border-gray-300 hover:text-white'
                }`}
              >
                LAWA Data
              </Link>
            </div>
          </div>
        </div>
      </div>
    </nav>
  );
};

export default NavBar;