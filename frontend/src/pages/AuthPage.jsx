import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { storage } from "../api";
import { Mark } from "../shell";
import { useStore } from "../store";

function Eye({ off = false }) {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
      {off ? (
        <>
          <path d="M3 3l18 18" />
          <path d="M10.6 10.7a2 2 0 0 0 2.7 2.7" />
          <path d="M9.9 5.2A11 11 0 0 1 12 5c5 0 9.3 3.1 11 7-.6 1.3-1.5 2.5-2.6 3.5" />
          <path d="M6.6 6.6C4.6 7.9 3 9.8 1 12c1.7 3.9 6 7 11 7 1.6 0 3.1-.3 4.5-.9" />
        </>
      ) : (
        <>
          <path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12z" />
          <circle cx="12" cy="12" r="3" />
        </>
      )}
    </svg>
  );
}

export function AuthPage({ mode }) {
  const { login, register, call } = useStore();
  const navigate = useNavigate();
  const [email, setEmail] = useState(mode === "login" ? "demo@isoko.rw" : "");
  const [password, setPassword] = useState(mode === "login" ? "IsokoDemo1" : "");
  const [phone, setPhone] = useState("0780000001");
  const [showPassword, setShowPassword] = useState(false);
  const [challenge, setChallenge] = useState(null);
  const [verification, setVerification] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    if (pending) return;
    setPending(true);
    try {
      if (challenge) {
        const { data } = await call("/auth/verify", { method: "POST", body: { challengeId: challenge.challengeId, code: verification } });
        storage.setToken(data.accessToken); window.location.assign("/"); return;
      }
      const result = mode === "login" ? await login(email, password) : await register({ email, password, phone });
      if (result?.challengeId) { setChallenge(result); return; }
      navigate("/");
    } catch (err) {
      setError(err.message);
    } finally { setPending(false); }
  }

  return (
    <div className="auth-screen">
      <form className="auth-card" onSubmit={onSubmit}>
        <div style={{ display: "flex", justifyContent: "center" }}><Mark /></div>
        <h1>{mode === "login" ? "Sign in" : "Create account"}</h1>
        <p className="lead">Isoko ecommerce</p>
        {!challenge && <><div className="field">
          <label htmlFor="auth-email">{mode === "login" ? "Email or mobile" : "Email"}</label>
          <input id="auth-email" type={mode === "login" ? "text" : "email"} value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="username" />
        </div>
        <div className="field">
          <label htmlFor="auth-password">Password</label>
          <div className="password-field">
            <input
              id="auth-password"
              type={showPassword ? "text" : "password"}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={8}
              autoComplete={mode === "login" ? "current-password" : "new-password"}
            />
            <button
              type="button"
              className="password-toggle"
              aria-label={showPassword ? "Hide password" : "Show password"}
              aria-pressed={showPassword}
              onClick={() => setShowPassword((open) => !open)}
            >
              <Eye off={showPassword} />
            </button>
          </div>
        </div>
        {mode === "register" ? (
          <div className="field">
            <label>Mobile (07xxxxxxxx)</label>
            <input value={phone} onChange={(e) => setPhone(e.target.value)} required pattern="^07[0-9]{8}$" />
          </div>
        ) : null}
        </>}
        {challenge && <><p>{challenge.challengePurpose === "MFA" ? "Verify your second factor" : "Verify your mobile number"}</p>{challenge.labCode && <p className="muted">Lab SMS code: <strong>{challenge.labCode}</strong></p>}<div className="field"><label htmlFor="verification-code">6-digit code</label><input id="verification-code" inputMode="numeric" pattern="[0-9]{6}" maxLength={6} required value={verification} onChange={e => setVerification(e.target.value)} /></div><button className="btn" type="button" onClick={async () => { try { const { data } = await call("/auth/resend", { method: "POST", body: { challengeId: challenge.challengeId } }); setChallenge(data); } catch(e) { setError(e.message); } }}>Resend code</button></>}
        {error ? <p className="error">{error}</p> : null}
        <button className="btn btn-primary btn-block" type="submit" disabled={pending}>
          {challenge ? "Verify and continue" : mode === "login" ? "Sign in" : "Create account"}
        </button>
        <p className="muted" style={{ textAlign: "center", marginTop: 16 }}>
          {mode === "login" ? (
            <>Need an account? <Link to="/register">Register</Link></>
          ) : (
            <>Already a customer? <Link to="/login">Sign in</Link></>
          )}
        </p>
      </form>
    </div>
  );
}
