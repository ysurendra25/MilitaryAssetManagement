import React, { useEffect, useState } from 'react';
import { api } from '../api.js';
import { useUser } from '../App.jsx';
import { BaseSelect, EquipSelect, FilterBar } from './Meta.jsx';

export default function Transfers() {
  const user = useUser();
  const isCommander = user.role === 'COMMANDER';

  const [form, setForm] = useState({
    fromBaseId: '', toBaseId: '', equipmentTypeId: '', quantity: '', reason: '', transferDate: ''
  });
  const [rows, setRows] = useState([]);
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');

  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [baseId, setBaseId] = useState(null);
  const [equipId, setEquipId] = useState(null);

  function load() {
    const params = new URLSearchParams();
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    if (!isCommander && baseId) params.set('baseId', baseId);
    if (equipId) params.set('equipmentTypeId', equipId);
    api('/transfers?' + params).then(setRows).catch(e => setError(e.message));
  }

  useEffect(load, []); // eslint-disable-line

  async function save(e) {
    e.preventDefault();
    setMsg(''); setError('');
    try {
      await api('/transfers', {
        method: 'POST',
        body: {
          fromBaseId: Number(form.fromBaseId),
          toBaseId: Number(form.toBaseId),
          equipmentTypeId: Number(form.equipmentTypeId),
          quantity: Number(form.quantity),
          reason: form.reason,
          transferDate: form.transferDate
        }
      });
      setMsg('Transfer recorded.');
      setForm({ fromBaseId: '', toBaseId: '', equipmentTypeId: '', quantity: '', reason: '', transferDate: '' });
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  return (
    <div className="grid-2">
      <div className="card">
        <h3 className="card-title">New Transfer</h3>
        <form onSubmit={save}>
          <div className="field">
            <label>FROM BASE</label>
            <BaseSelect value={form.fromBaseId} onChange={v => set('fromBaseId', v)} allowAll={false} />
          </div>
          <div className="field">
            <label>TO BASE</label>
            <BaseSelect value={form.toBaseId} onChange={v => set('toBaseId', v)} allowAll={false} />
          </div>
          <div className="field">
            <label>EQUIPMENT TYPE</label>
            <EquipSelect value={form.equipmentTypeId} onChange={v => set('equipmentTypeId', v)} allowAll={false} />
          </div>
          <div className="field">
            <label>QUANTITY</label>
            <input type="number" min="1" value={form.quantity}
                   onChange={e => set('quantity', e.target.value)} required />
          </div>
          <div className="field">
            <label>REASON (optional)</label>
            <input value={form.reason} onChange={e => set('reason', e.target.value)} />
          </div>
          <div className="field">
            <label>TRANSFER DATE</label>
            <input type="date" value={form.transferDate}
                   onChange={e => set('transferDate', e.target.value)} required />
          </div>
          <button className="btn">Submit Transfer</button>
        </form>
      </div>

      <div className="card">
        <h3 className="card-title">Transfer History</h3>
        <FilterBar from={from} to={to} setFrom={setFrom} setTo={setTo}
                   baseId={baseId} setBaseId={setBaseId} equipId={equipId} setEquipId={setEquipId}
                   onApply={() => { setError(''); load(); }} showBase={!isCommander} />
        {msg && <div className="success-box" style={{ marginTop: 12 }}>{msg}</div>}
        {error && <div className="error-box" style={{ marginTop: 12 }}>{error}</div>}
        <div className="table-wrap" style={{ marginTop: 12, maxHeight: 420, overflowY: 'auto' }}>
          <table>
            <thead>
              <tr><th>DATE</th><th>FROM</th><th>TO</th><th>EQUIPMENT</th><th>QTY</th><th>REASON</th><th>BY</th></tr>
            </thead>
            <tbody>
              {rows.map(t => (
                <tr key={t.id}>
                  <td>{t.date}</td>
                  <td>{t.fromBase}</td>
                  <td>{t.toBase}</td>
                  <td>{t.equipment}</td>
                  <td>{t.quantity}</td>
                  <td className="muted">{t.reason || '—'}</td>
                  <td className="muted">{t.createdBy}</td>
                </tr>
              ))}
              {rows.length === 0 && <tr><td colSpan="7" className="muted">No transfers found.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
