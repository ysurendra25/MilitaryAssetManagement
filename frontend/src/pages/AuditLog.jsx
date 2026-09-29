import React, { useEffect, useState } from 'react';
import { api } from '../api.js';

export default function AuditLog() {
  const [rows, setRows] = useState([]);
  const [user, setUser] = useState('');
  const [error, setError] = useState('');

  function load() {
    const params = new URLSearchParams({ limit: '200' });
    if (user.trim()) params.set('user', user.trim());
    api('/audit?' + params).then(setRows).catch(e => setError(e.message));
  }

  useEffect(load, []);

  return (
    <div className="card">
      <h3 className="card-title">Transaction Log</h3>
      <p className="muted" style={{ marginTop: -8 }}>
        Every purchase, transfer, assignment and expenditure is logged automatically —
        including denied attempts.
      </p>
      <div className="filters">
        <div className="field">
          <label>FILTER BY USER</label>
          <input value={user} onChange={e => setUser(e.target.value)}
                 placeholder="e.g. logistics01" />
        </div>
        <button className="btn" onClick={() => { setError(''); load(); }}>Filter</button>
      </div>
      {error && <div className="error-box" style={{ marginTop: 12 }}>{error}</div>}
      <div className="table-wrap" style={{ marginTop: 12, maxHeight: 520, overflowY: 'auto' }}>
        <table>
          <thead>
            <tr><th>TIMESTAMP</th><th>USER</th><th>ROLE</th><th>ACTION</th><th>DETAILS</th><th>STATUS</th></tr>
          </thead>
          <tbody>
            {rows.map(r => (
              <tr key={r.id}>
                <td>{r.timestamp.replace('T', ' ')}</td>
                <td>{r.username}</td>
                <td>{r.role}</td>
                <td>{r.action}</td>
                <td className="muted">{r.details}</td>
                <td className={r.status >= 400 ? 'neg' : ''}>{r.status}</td>
              </tr>
            ))}
            {rows.length === 0 && <tr><td colSpan="6" className="muted">No log entries.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}
