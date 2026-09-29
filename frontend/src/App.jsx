import React, { createContext, useContext, useEffect, useState } from 'react';
import { NavLink, Navigate, Outlet, Route, Routes, useNavigate } from 'react-router-dom';
import { api } from './api.js';
import Login from './pages/Login.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Purchases from './pages/Purchases.jsx';
import Transfers from './pages/Transfers.jsx';
import Assignments from './pages/Assignments.jsx';
import AuditLog from './pages/AuditLog.jsx';

const UserContext = createContext(null);
export const useUser = () => useContext(UserContext);

const NAV = [
  { to: '/', label: 'Dashboard', roles: ['ADMIN', 'COMMANDER'], end: true },
  { to: '/purchases', label: 'Purchases', roles: ['ADMIN', 'COMMANDER', 'LOGISTICS'] },
  { to: '/transfers', label: 'Transfers', roles: ['ADMIN', 'COMMANDER', 'LOGISTICS'] },
  { to: '/assignments', label: 'Assignments & Expenditures', roles: ['ADMIN', 'COMMANDER'] },
  { to: '/audit', label: 'Audit Log', roles: ['ADMIN'] }
];

function Layout({ user, onLogout }) {
  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="logo">
          <div className="logo-badge">M</div>
          <div>
            <div className="logo-name">MAMS</div>
            <div className="logo-sub">Asset Management</div>
          </div>
        </div>
        {NAV.filter(n => n.roles.includes(user.role)).map(n => (
          <NavLink key={n.to} to={n.to} end={n.end}
                   className={({ isActive }) => 'nav-item' + (isActive ? ' active' : '')}>
            {n.label}
          </NavLink>
        ))}
      </aside>
      <div className="main">
        <div className="topbar">
          <div />
          <div className="user-chip">
            <div className="user-avatar">{user.username[0].toUpperCase()}</div>
            <div>
              <div className="user-name">{user.username}</div>
              <span className={'role-badge role-' + user.role}>
                {user.role === 'LOGISTICS' ? 'LOGISTICS' : user.role}
              </span>
            </div>
            <button className="btn ghost small" onClick={onLogout}>Sign out</button>
          </div>
        </div>
        <div className="content">
          <Outlet />
        </div>
      </div>
    </div>
  );
}

export default function App() {
  const [user, setUser] = useState(null);
  const [ready, setReady] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    api('/auth/me')
      .then(u => setUser({ username: u.username, role: u.role, baseId: u.baseId || null }))
      .catch(() => setUser(null))
      .finally(() => setReady(true));
  }, []);

  async function logout() {
    try { await api('/auth/logout', { method: 'POST' }); } catch { /* ignore */ }
    setUser(null);
    navigate('/login');
  }

  if (!ready) {
    return <div className="login-page"><div className="muted">Loading...</div></div>;
  }

  if (!user) {
    return (
      <Routes>
        <Route path="/login" element={<Login onLogin={setUser} />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }

  return (
    <UserContext.Provider value={user}>
      <Routes>
        <Route element={<Layout user={user} onLogout={logout} />}>
          <Route path="/" element={<Dashboard />} />
          <Route path="/purchases" element={<Purchases />} />
          <Route path="/transfers" element={<Transfers />} />
          <Route path="/assignments" element={<Assignments />} />
          <Route path="/audit" element={<AuditLog />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </UserContext.Provider>
  );
}
