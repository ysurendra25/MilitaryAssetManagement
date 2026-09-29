import React, { useEffect, useState } from 'react';
import { api } from '../api.js';
import { useUser } from '../App.jsx';
import { FilterBar } from './Meta.jsx';

const today = () => new Date().toISOString().slice(0, 10);
const yearAgo = () => {
  const d = new Date();
  d.setFullYear(d.getFullYear() - 1);
  return d.toISOString().slice(0, 10);
};

export default function Dashboard() {
  const user = useUser();
  const isCommander = user.role === 'COMMANDER';

  const [from, setFrom] = useState(yearAgo());
  const [to, setTo] = useState(today());
  const [baseId, setBaseId] = useState(null);
  const [equipId, setEquipId] = useState(null);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [popup, setPopup] = useState(false);

  function load() {
    const params = new URLSearchParams({ from, to });
    if (!isCommander && baseId) params.set('baseId', baseId);
    if (equipId) params.set('equipmentTypeId', equipId);
    api('/dashboard?' + params)
      .then(setData)
      .catch(e => setError(e.message));
  }

  useEffect(load, []); // eslint-disable-line

  const fmt = n => Number(n).toLocaleString('en-IN');
  const kpis = data ? [
    { label: 'OPENING BALANCE', value: fmt(data.openingBalance), cls: '' },
    { label: 'CLOSING BALANCE', value: fmt(data.closingBalance), cls: '' },
    { label: 'NET MOVEMENT', value: (data.netMovement >= 0 ? '+' : '') + fmt(data.netMovement),
      cls: data.netMovement >= 0 ? 'pos' : 'neg', clickable: true },
    { label: 'ASSIGNED', value: fmt(data.assigned), cls: '' },
    { label: 'EXPENDED', value: fmt(data.expended), cls: 'neg' }
  ] : [];

  const maxAbs = data ? Math.max(1, ...data.monthly.map(m => Math.abs(m.net))) : 1;
  const MN = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  const mLabel = m => { const [y, mo] = m.split('-'); return MN[+mo - 1] + ' ' + y.slice(2); };

  return (
    <div>
      <div className="card">
        <FilterBar from={from} to={to} setFrom={setFrom} setTo={setTo}
                   baseId={baseId} setBaseId={setBaseId} equipId={equipId} setEquipId={setEquipId}
                   onApply={() => { setError(''); load(); }}
                   showBase={!isCommander} />
        {isCommander && <p className="muted" style={{ margin: '10px 0 0' }}>
          Showing your base only — commanders are scoped to their assigned base.</p>}
      </div>

      {error && <div className="error-box">{error}</div>}

      <div className="kpi-row">
        {kpis.map(k => (
          <div key={k.label}
               className={'kpi' + (k.clickable ? ' clickable' : '')}
               onClick={k.clickable ? () => setPopup(true) : undefined}>
            <div className="kpi-label">{k.label}</div>
            <div className={'kpi-value ' + k.cls}>{k.value}</div>
            {k.clickable && <div className="kpi-hint blue">click for details</div>}
          </div>
        ))}
      </div>

      {data && (
        <div className="grid-2">
          <div className="card">
            <h3 className="card-title">Monthly Net Movement</h3>
            <p className="muted">purchases + transfers in − transfers out</p>
            <div className="chart">
              {data.monthly.map((m, i) => (
                <div className="bar" key={m.month}>
                  <span className={'bar-v' + (m.net < 0 ? ' neg' : '')}
                        style={{ height: Math.max(4, Math.abs(m.net) / maxAbs * 120) + 'px' }}>
                    {m.net > 0 ? '+' : ''}{m.net}
                  </span>
                  <span className="bar-l">
                    {data.monthly.length > 8 && i % 2 === 1 ? '' : mLabel(m.month)}
                  </span>
                </div>
              ))}
            </div>
          </div>

          <div className="card">
            <h3 className="card-title">By Equipment Type</h3>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>EQUIPMENT</th><th>OPENING</th><th>NET</th><th>EXPENDED</th><th>CLOSING</th>
                  </tr>
                </thead>
                <tbody>
                  {data.byEquipment.map(r => (
                    <tr key={r.equipment}>
                      <td>{r.equipment}</td>
                      <td>{fmt(r.opening)}</td>
                      <td className={r.purchases + r.transfersIn - r.transfersOut >= 0 ? 'pos' : 'neg'}>
                        {r.purchases + r.transfersIn - r.transfersOut >= 0 ? '+' : ''}
                        {fmt(r.purchases + r.transfersIn - r.transfersOut)}
                      </td>
                      <td className="neg">{fmt(r.expended)}</td>
                      <td><b>{fmt(r.closing)}</b></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {popup && data && (
        <div className="modal-backdrop" onClick={() => setPopup(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <button className="modal-close" onClick={() => setPopup(false)}>×</button>
            <h3>Net Movement — breakdown</h3>
            <p className="sub">{data.from} → {data.to}</p>
            <div className="modal-row"><span>Purchases</span><span className="pos">+{fmt(data.purchases)}</span></div>
            <div className="modal-row"><span>Transfers In</span><span className="pos">+{fmt(data.transfersIn)}</span></div>
            <div className="modal-row"><span>Transfers Out</span><span className="neg">−{fmt(data.transfersOut)}</span></div>
            <div className="modal-row total"><span>Net Movement</span>
              <span className={data.netMovement >= 0 ? 'pos' : 'neg'}>
                {data.netMovement >= 0 ? '+' : ''}{fmt(data.netMovement)}
              </span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
