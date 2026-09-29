import React, { useEffect, useState } from 'react';
import { api } from '../api.js';
import { useUser } from '../App.jsx';
import { BaseSelect, EquipSelect, FilterBar } from './Meta.jsx';

export default function Purchases() {
  const user = useUser();
  const isCommander = user.role === 'COMMANDER';

  const [form, setForm] = useState({
    baseId: '', equipmentTypeId: '', quantity: '', unitCost: '', supplier: '', purchaseDate: ''
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
    api('/purchases?' + params).then(setRows).catch(e => setError(e.message));
  }

  useEffect(load, []); // eslint-disable-line

  async function save(e) {
    e.preventDefault();
    setMsg(''); setError('');
    try {
      await api('/purchases', {
        method: 'POST',
        body: {
          baseId: Number(form.baseId),
          equipmentTypeId: Number(form.equipmentTypeId),
          quantity: Number(form.quantity),
          unitCost: form.unitCost === '' ? null : Number(form.unitCost),
          supplier: form.supplier,
          purchaseDate: form.purchaseDate
        }
      });
      setMsg('Purchase recorded.');
      setForm({ baseId: '', equipmentTypeId: '', quantity: '', unitCost: '', supplier: '', purchaseDate: '' });
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));
  const inr = n => n == null ? '—' : '₹' + Number(n).toLocaleString('en-IN');

  return (
    <div className="grid-2">
      <div className="card">
        <h3 className="card-title">Record Purchase</h3>
        <form onSubmit={save}>
          <div className="field">
            <label>BASE</label>
            <BaseSelect value={form.baseId} onChange={v => set('baseId', v)} allowAll={false} />
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
            <label>UNIT COST (INR, optional)</label>
            <input type="number" min="0" step="0.01" value={form.unitCost}
                   onChange={e => set('unitCost', e.target.value)} />
          </div>
          <div className="field">
            <label>SUPPLIER (optional)</label>
            <input value={form.supplier} onChange={e => set('supplier', e.target.value)} />
          </div>
          <div className="field">
            <label>PURCHASE DATE</label>
            <input type="date" value={form.purchaseDate}
                   onChange={e => set('purchaseDate', e.target.value)} required />
          </div>
          <button className="btn">Save Purchase</button>
        </form>
      </div>

      <div className="card">
        <h3 className="card-title">Purchase History</h3>
        <FilterBar from={from} to={to} setFrom={setFrom} setTo={setTo}
                   baseId={baseId} setBaseId={setBaseId} equipId={equipId} setEquipId={setEquipId}
                   onApply={() => { setError(''); load(); }} showBase={!isCommander} />
        {msg && <div className="success-box" style={{ marginTop: 12 }}>{msg}</div>}
        {error && <div className="error-box" style={{ marginTop: 12 }}>{error}</div>}
        <div className="table-wrap" style={{ marginTop: 12, maxHeight: 420, overflowY: 'auto' }}>
          <table>
            <thead>
              <tr><th>DATE</th><th>BASE</th><th>EQUIPMENT</th><th>QTY</th><th>UNIT COST</th><th>TOTAL</th><th>BY</th></tr>
            </thead>
            <tbody>
              {rows.map(p => (
                <tr key={p.id}>
                  <td>{p.date}</td>
                  <td>{p.base}</td>
                  <td>{p.equipment}</td>
                  <td>{p.quantity}</td>
                  <td>{inr(p.unitCost)}</td>
                  <td>{inr(p.total)}</td>
                  <td className="muted">{p.createdBy}</td>
                </tr>
              ))}
              {rows.length === 0 && <tr><td colSpan="7" className="muted">No purchases found.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
