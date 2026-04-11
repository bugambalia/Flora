import React, { useState, useMemo } from 'react';
import { RouterProvider } from 'react-router';
import { LoginScreen } from './components/LoginScreen';
import { createRouter } from './routes.tsx';

export default function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [currentUser, setCurrentUser] = useState('');

  const handleLogin = (username: string) => {
    setCurrentUser(username);
    setIsLoggedIn(true);
  };

  const handleLogout = () => {
    setIsLoggedIn(false);
    setCurrentUser('');
  };

  // Create router with current user info
  const router = useMemo(
    () => createRouter({ currentUser, onLogout: handleLogout }),
    [currentUser]
  );

  // Show login screen if not logged in
  if (!isLoggedIn) {
    return <LoginScreen onLogin={handleLogin} />;
  }

  return <RouterProvider router={router} />;
}