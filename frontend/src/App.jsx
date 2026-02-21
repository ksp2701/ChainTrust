import React, { useEffect, useState } from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import Navbar from "./components/Navbar";
import LoginModal from "./components/LoginModal";
import HomePage from "./pages/HomePage";
import AnalyzePage from "./pages/AnalyzePage";
import LoanPage from "./pages/LoanPage";
import HistoryPage from "./pages/HistoryPage";
import AboutPage from "./pages/AboutPage";
import "./styles.css";

const USER_STORAGE_KEY = "chaintrust.currentUser";

export default function App() {
  const [loginOpen, setLoginOpen] = useState(false);
  const [currentUser, setCurrentUser] = useState(() => {
    try {
      const raw = localStorage.getItem(USER_STORAGE_KEY);
      if (!raw) {
        return null;
      }
      const parsed = JSON.parse(raw);
      return parsed && parsed.userId ? parsed : null;
    } catch (error) {
      return null;
    }
  });

  const openLogin = () => setLoginOpen(true);
  const closeLogin = () => setLoginOpen(false);
  const handleLogout = () => setCurrentUser(null);

  useEffect(() => {
    if (currentUser) {
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(currentUser));
    } else {
      localStorage.removeItem(USER_STORAGE_KEY);
    }
  }, [currentUser]);

  return (
    <BrowserRouter>
      <div className="app-shell">
        <Navbar onOpenLogin={openLogin} onLogout={handleLogout} currentUser={currentUser} />
        <main className="page-content">
          <Routes>
            <Route path="/"        element={<HomePage onOpenLogin={openLogin} currentUser={currentUser} />} />
            <Route path="/analyze" element={<AnalyzePage onOpenLogin={openLogin} currentUser={currentUser} />} />
            <Route path="/loan"    element={<LoanPage />} />
            <Route path="/history" element={<HistoryPage />} />
            <Route path="/about"   element={<AboutPage />} />
          </Routes>
        </main>
        <LoginModal
          isOpen={loginOpen}
          onClose={closeLogin}
          onAuthSuccess={(user) => {
            setCurrentUser(user);
            closeLogin();
          }}
        />
      </div>
    </BrowserRouter>
  );
}
