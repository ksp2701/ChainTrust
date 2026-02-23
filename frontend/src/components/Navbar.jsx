import React from "react";
import { NavLink, Link } from "react-router-dom";
import { LogIn, LogOut, Shield } from "lucide-react";

const links = [
    { to: "/", label: "Home" },
    { to: "/analyze", label: "Analyze" },
    { to: "/loan", label: "Get Loan" },
    { to: "/history", label: "History" },
    { to: "/about", label: "About" },
];

export default function Navbar({ onOpenLogin, onLogout, currentUser }) {
    return (
        <nav className="navbar">
            <Link to="/" className="navbar-brand">
                <div className="navbar-logo">
                    <img src="/logo.png" alt="ChainTrust Logo" style={{ height: '40px', width: 'auto' }} />
                </div>
                ChainTrust
            </Link>

            <ul className="navbar-links">
                {links.map(({ to, label }) => (
                    <li key={to}>
                        <NavLink
                            to={to}
                            end={to === "/"}
                            className={({ isActive }) => `navbar-link${isActive ? " active" : ""}`}
                        >
                            {label}
                        </NavLink>
                    </li>
                ))}
            </ul>

            <div className="navbar-actions">
                {currentUser ? (
                    <>
                        <span className="badge badge-success">Hi, {currentUser.fullName || "User"}</span>
                        <button type="button" className="btn btn-ghost btn-sm" onClick={onLogout}>
                            <LogOut size={14} /> Logout
                        </button>
                    </>
                ) : (
                    <button type="button" className="btn btn-ghost btn-sm" onClick={onOpenLogin}>
                        <LogIn size={14} /> Login
                    </button>
                )}
                <Link to="/analyze" className="btn btn-primary btn-sm">
                    Analyze Wallet
                </Link>
            </div>
        </nav>
    );
}
