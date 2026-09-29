import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api.js';

const DEMO = [
  { label: 'Admin', username: 'admin', password: 'admin123' },
  { label: 'Base Commander', username: 'cmdr_alpha', password: 'commander123' },
  { label: 'Logistics Officer', username: 'logistics01', password: 'logistics123' }
];

export default function Login({ onLogin }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();

  async function submit(u, p) {
    setBusy(true);
    setError('');
    try {
      const me = await api('/auth/login', { method: 'POST', body: { username: u, password: p } });
      onLogin({ username: me.username, role: me.role, baseId: me.baseId || null });
      navigate(me.role === 'LOGISTICS' ? '/purchases' : '/');
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login-page">
      <div className="login-banner">RESTRICTED SYSTEM — AUTHORIZED PERSONNEL ONLY</div>
      <form className="login-card" onSubmit={e => { e.preventDefault(); submit(username, password); }}>
        <div className="login-logo"><div>MAMS</div></div>
        <h1 className="login-title">Military Asset Management System</h1>
        <p className="login-sub">Sign in with your service credentials</p>
        {error && <div className="error-box">{error}</div>}
        <div className="field">
          <label>USERNAME</label>
          <input value={username} onChange={e => setUsername(e.target.value)} autoFocus />
        </div>
        <div className="field">
          <label>PASSWORD</label>
          <input type="password" value={password} onChange={e => setPassword(e.target.value)} />
        </div>
        <button className="btn" style={{ width: '100%', marginTop: 8, padding: 11 }} disabled={busy}>
          {busy ? 'Signing in...' : 'Sign In'}
        </button>
        <div className="demo-row">
          <p>DEMO ACCOUNTS (CLICK TO FILL)</p>
          {DEMO.map(d => (
            <button type="button" key={d.username}
                    onClick={() => { setUsername(d.username); setPassword(d.password); submit(d.username, d.password); }}>
              {d.label}
            </button>
          ))}
        </div>
      </form>
    </div>
  );
}
