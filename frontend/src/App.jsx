import React from "react";
<<<<<<< HEAD
import { BrowserRouter, Routes, Route } from "react-router-dom";
import Navbar from "./components/Navbar";
import HomePage from "./pages/HomePage";
import AnalyzePage from "./pages/AnalyzePage";
import LoanPage from "./pages/LoanPage";
import HistoryPage from "./pages/HistoryPage";
import AboutPage from "./pages/AboutPage";
import "./styles.css";

export default function App() {
  return (
    <BrowserRouter>
      <div className="app-shell">
        <Navbar />
        <main className="page-content">
          <Routes>
            <Route path="/"        element={<HomePage />} />
            <Route path="/analyze" element={<AnalyzePage />} />
            <Route path="/loan"    element={<LoanPage />} />
            <Route path="/history" element={<HistoryPage />} />
            <Route path="/about"   element={<AboutPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
=======
import WalletInput from "./components/WalletInput";

export default function App() {
  return (
    <div className="page">
      <div className="card">
        <h1>ChainTrust Wallet Risk Analyzer</h1>
        <p className="subtitle">
          Analyze wallet activity, score risk via ML, and generate a loan decision hash.
        </p>
        <WalletInput />
      </div>
    </div>
>>>>>>> e6bab9ff3e4c81f53c66b24db7e96dd1d61d97c1
  );
}
