import React, { useEffect, useState } from 'react';
import { api } from '../api.js';
import { useUser } from '../App.jsx';
import { BaseSelect, EquipSelect } from './Meta.jsx';

export default function Assignments() {
  const user = useUser();
  const isCommander = user.role === 'COMMANDER';

  const [aForm, setAForm] = useState({
    baseId: '', equipmentTypeId: '', quantity: '', personnelName: '', assignedDate: ''
  });
  const [eForm, setEForm] = useState({
    baseId: '', equipmentTypeId: '', quantity: '', reason: '', expendDate: ''
  });
  const [assignments, setAssignments] = useState([]);
  const [expenditures, setExpenditures] = useState([]);
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');

  function load() {
    api('/assignments').then(setAssignments).catch(e => setError(e.message));
    api('/expenditures').then(setExpenditures).catch(e => setError(e.message));
  }

  useEffect(load, []);

  async function assign(e) {
    e.preventDefault();
    setMsg(''); setError('');
    try {
      await api('/assignments', {
        method: 'POST',
        body: {
          baseId: Number(aForm.baseId),
          equipmentTypeId: Number(aForm.equipmentTypeId),
          quantity: Number(aForm.quantity),
          personnelName: aForm.personnelName,
          assignedDate: aForm.assignedDate
        }
      });
      setMsg('Asset assigned.');
      setAForm({ baseId: '', equipmentTypeId: '', quantity: '', personnelName: '', assignedDate: '' });
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  async function expend(e) {
    e.preventDefault();
    setMsg(''); setError('');
    try {
      await api('/expenditures', {
        method: 'POST',
        body: {
          baseId: Number(eForm.baseId),
          equipmentTypeId: Number(eForm.equipmentTypeId),
          quantity: Number(eForm.quantity),
          reason: eForm.reason,
          expendDate: eForm.expendDate
        }
      });
      setMsg('Expenditure recorded.');
      setEForm({ baseId: '', equipmentTypeId: '', quantity: '', reason: '', expendDate: '' });
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  async function returnIt(id) {
    setMsg(''); setError('');
    try {
      await api('/assignments/' + id + '/return', { method: 'POST' });
      setMsg('Asset returned to base stock.');
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  const setA = (k, v) => setAForm(f => ({ ...f, [k]: v }));
  const setE = (k, v) => setEForm(f => ({ ...f, [k]: v }));

  return (
    <div>
      <div className="grid-2">
        <div className="card">
          <h3 className="card-title">Assign Asset</h3>
          <form onSubmit={assign}>
            <div className="field">
              <label>BASE</label>
              <BaseSelect value={aForm.baseId} onChange={v => setA('baseId', v)} allowAll={false} />
            </div>
            <div className="field">
              <label>EQUIPMENT TYPE</label>
              <EquipSelect value={aForm.equipmentTypeId} onChange={v => setA('equipmentTypeId', v)} allowAll={false} />
            </div>
            <div className="field">
              <label>PERSONNEL NAME</label>
              <input value={aForm.personnelName} onChange={e => setA('personnelName', e.target.value)} required />
            </div>
            <div className="field">
              <label>QUANTITY</label>
              <input type="number" min="1" value={aForm.quantity}
                     onChange={e => setA('quantity', e.target.value)} required />
            </div>
            <div className="field">
              <label>ASSIGN DATE</label>
              <input type="date" value={aForm.assignedDate}
                     onChange={e => setA('assignedDate', e.target.value)} required />
            </div>
            <button className="btn">Assign</button>
          </form>
        </div>

        <div className="card">
          <h3 className="card-title">Record Expenditure</h3>
          <form onSubmit={expend}>
            <div className="field">
              <label>BASE</label>
              <BaseSelect value={eForm.baseId} onChange={v => setE('baseId', v)} allowAll={false} />
            </div>
            <div className="field">
              <label>EQUIPMENT TYPE</label>
              <EquipSelect value={eForm.equipmentTypeId} onChange={v => setE('equipmentTypeId', v)} allowAll={false} />
            </div>
            <div className="field">
              <label>QUANTITY</label>
              <input type="number" min="1" value={eForm.quantity}
                     onChange={e => setE('quantity', e.target.value)} required />
            </div>
            <div className="field">
              <label>REASON</label>
              <input value={eForm.reason} onChange={e => setE('reason', e.target.value)} required />
            </div>
            <div className="field">
              <label>DATE</label>
              <input type="date" value={eForm.expendDate}
                     onChange={e => setE('expendDate', e.target.value)} required />
            </div>
            <button className="btn danger">Record Expenditure</button>
          </form>
        </div>
      </div>

      {msg && <div className="success-box">{msg}</div>}
      {error && <div className="error-box">{error}</div>}

      <div className="grid-2">
        <div className="card">
          <h3 className="card-title">Assignments</h3>
          <div className="table-wrap" style={{ maxHeight: 340, overflowY: 'auto' }}>
            <table>
              <thead>
                <tr><th>PERSONNEL</th><th>EQUIPMENT</th><th>QTY</th><th>DATE</th><th>STATUS</th><th></th></tr>
              </thead>
              <tbody>
                {assignments.map(a => (
                  <tr key={a.id}>
                    <td>{a.personnelName}</td>
                    <td>{a.equipment}</td>
                    <td>{a.quantity}</td>
                    <td>{a.assignedDate}</td>
                    <td>
                      <span className={'chip ' + (a.status === 'ACTIVE' ? 'ok' : 'gray')}>{a.status}</span>
                    </td>
                    <td>
                      {a.status === 'ACTIVE' &&
                        <button className="btn ghost small" onClick={() => returnIt(a.id)}>Return</button>}
                    </td>
                  </tr>
                ))}
                {assignments.length === 0 &&
                  <tr><td colSpan="6" className="muted">No assignments yet.</td></tr>}
              </tbody>
            </table>
          </div>
        </div>

        <div className="card">
          <h3 className="card-title">Expenditures</h3>
          <div className="table-wrap" style={{ maxHeight: 340, overflowY: 'auto' }}>
            <table>
              <thead>
                <tr><th>DATE</th><th>EQUIPMENT</th><th>QTY</th><th>REASON</th><th>BY</th></tr>
              </thead>
              <tbody>
                {expenditures.map(x => (
                  <tr key={x.id}>
                    <td>{x.date}</td>
                    <td>{x.equipment}</td>
                    <td>{x.quantity}</td>
                    <td className="muted">{x.reason}</td>
                    <td className="muted">{x.createdBy}</td>
                  </tr>
                ))}
                {expenditures.length === 0 &&
                  <tr><td colSpan="5" className="muted">No expenditures yet.</td></tr>}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
