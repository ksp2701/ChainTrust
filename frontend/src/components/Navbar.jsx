import React from "react";
import { NavLink, Link } from "react-router-dom";
import { Shield } from "lucide-react";

const links = [
    { to: "/", label: "Home" },
    { to: "/analyze", label: "Analyze" },
    { to: "/loan", label: "Get Loan" },
    { to: "/history", label: "History" },
    { to: "/about", label: "About" },
];

export default function Navbar() {
    return (
        <nav className="navbar">
            <Link to="/" className="navbar-brand">
                <div className="navbar-logo">
                    <Shield size={18} color="#040913" strokeWidth={2.5} />
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

            <Link to="/analyze" className="btn btn-primary btn-sm">
                Analyze Wallet →
            </Link>
        </nav>
    );
}
