import React, { useEffect, useState } from "react";
import axios from "axios";
import { AlertCircle, CheckCircle2, LogIn, UserPlus, X } from "lucide-react";

const BACKEND = process.env.REACT_APP_BACKEND_URL || "http://localhost:8080";

const EMPTY_FORM = {
    fullName: "",
    email: "",
    phone: "",
    metaMaskAddress: "",
    password: "",
};

export default function LoginModal({ isOpen, onClose, onAuthSuccess }) {
    const [mode, setMode] = useState("login");
    const [form, setForm] = useState(EMPTY_FORM);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    useEffect(() => {
        if (!isOpen) {
            return undefined;
        }
        const onEsc = (event) => {
            if (event.key === "Escape") {
                onClose();
            }
        };
        window.addEventListener("keydown", onEsc);
        return () => window.removeEventListener("keydown", onEsc);
    }, [isOpen, onClose]);

    useEffect(() => {
        if (isOpen) {
            setError("");
            setSuccess("");
            setLoading(false);
        }
    }, [isOpen, mode]);

    if (!isOpen) {
        return null;
    }

    function updateField(key, value) {
        setForm((prev) => ({ ...prev, [key]: value }));
    }

    function switchMode(nextMode) {
        setMode(nextMode);
        setForm(EMPTY_FORM);
        setError("");
        setSuccess("");
    }

    async function onSubmit(event) {
        event.preventDefault();
        setError("");
        setSuccess("");
        setLoading(true);

        const endpoint = mode === "login" ? "/auth/login" : "/auth/register";
        const payload = mode === "login"
            ? {
                email: form.email,
                password: form.password,
            }
            : {
                fullName: form.fullName,
                email: form.email,
                phone: form.phone,
                password: form.password,
                metaMaskAddress: form.metaMaskAddress,
            };

        try {
            const response = await axios.post(`${BACKEND}${endpoint}`, payload);
            setSuccess(response?.data?.message || "Success");
            if (typeof onAuthSuccess === "function") {
                onAuthSuccess(response.data);
            }
        } catch (err) {
            setError(
                err?.response?.data?.message ||
                err?.response?.data?.detail ||
                err?.message ||
                "Authentication failed"
            );
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="modal-overlay" onMouseDown={onClose}>
            <div
                className="modal-panel glass-card"
                role="dialog"
                aria-modal="true"
                aria-label="Login or register"
                onMouseDown={(event) => event.stopPropagation()}
            >
                <div className="modal-header">
                    <div>
                        <div className="section-title" style={{ marginBottom: 2 }}>
                            {mode === "login" ? "Login" : "Create Account"}
                        </div>
                        <div className="section-subtitle" style={{ fontSize: 13 }}>
                            Loan availability remains open without login.
                        </div>
                    </div>
                    <button
                        type="button"
                        className="modal-close-btn"
                        onClick={onClose}
                        aria-label="Close login dialog"
                    >
                        <X size={16} />
                    </button>
                </div>

                <div className="modal-switch">
                    <button
                        type="button"
                        className={`btn btn-sm ${mode === "login" ? "btn-primary" : "btn-ghost"}`}
                        onClick={() => switchMode("login")}
                    >
                        <LogIn size={14} /> Login
                    </button>
                    <button
                        type="button"
                        className={`btn btn-sm ${mode === "register" ? "btn-primary" : "btn-ghost"}`}
                        onClick={() => switchMode("register")}
                    >
                        <UserPlus size={14} /> Register
                    </button>
                </div>

                <form onSubmit={onSubmit} style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                    {mode === "register" && (
                        <input
                            className="ct-input"
                            placeholder="Full name"
                            value={form.fullName}
                            onChange={(event) => updateField("fullName", event.target.value)}
                            required
                        />
                    )}

                    <input
                        className="ct-input"
                        type="email"
                        placeholder="Email"
                        value={form.email}
                        onChange={(event) => updateField("email", event.target.value)}
                        required
                    />

                    {mode === "register" && (
                        <input
                            className="ct-input"
                            type="tel"
                            placeholder="Phone (+1 234-567-8901)"
                            value={form.phone}
                            onChange={(event) => updateField("phone", event.target.value)}
                            required
                            pattern="^\\+?[0-9\\s\\-()]{7,25}$"
                            title="Phone can include digits, spaces, (), and - with an optional leading +"
                        />
                    )}

                    {mode === "register" && (
                        <input
                            className="ct-input"
                            placeholder="MetaMask wallet (optional)"
                            value={form.metaMaskAddress}
                            onChange={(event) => updateField("metaMaskAddress", event.target.value)}
                            pattern="^$|^0x[a-fA-F0-9]{40}$"
                            title="MetaMask address must be a valid EVM address"
                        />
                    )}

                    <input
                        className="ct-input"
                        type="password"
                        placeholder="Password (min 8 characters)"
                        value={form.password}
                        onChange={(event) => updateField("password", event.target.value)}
                        required
                        minLength={8}
                    />

                    {error && (
                        <div className="alert alert-error">
                            <AlertCircle size={15} />
                            <span>{error}</span>
                        </div>
                    )}
                    {success && (
                        <div className="alert alert-success">
                            <CheckCircle2 size={15} />
                            <span>{success}</span>
                        </div>
                    )}

                    <div style={{ display: "flex", gap: 10, justifyContent: "flex-end", marginTop: 4 }}>
                        <button type="button" className="btn btn-ghost btn-sm" onClick={onClose}>
                            Close
                        </button>
                        <button type="submit" className="btn btn-primary btn-sm" disabled={loading}>
                            {loading ? "Please wait..." : mode === "login" ? "Login" : "Create Account"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
