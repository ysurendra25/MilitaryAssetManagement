import React, { useEffect, useState } from 'react';
import { api } from '../api.js';

/** Loads bases + equipment types once; shared by every page's dropdowns. */
export function useMeta() {
  const [bases, setBases] = useState([]);
  const [equipment, setEquipment] = useState([]);

  useEffect(() => {
    api('/meta/bases').then(setBases).catch(() => {});
    api('/meta/equipment-types').then(setEquipment).catch(() => {});
  }, []);

  return { bases, equipment };
}

export function BaseSelect({ value, onChange, allowAll = true }) {
  const { bases } = useMeta();
  return (
    <select value={value == null ? '' : value}
            onChange={e => onChange(e.target.value === '' ? null : Number(e.target.value))}>
      {allowAll && <option value="">All Bases</option>}
      {bases.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}
    </select>
  );
}

export function EquipSelect({ value, onChange, allowAll = true }) {
  const { equipment } = useMeta();
  return (
    <select value={value == null ? '' : value}
            onChange={e => onChange(e.target.value === '' ? null : Number(e.target.value))}>
      {allowAll && <option value="">All Types</option>}
      {equipment.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
    </select>
  );
}

/** Date range + base + equipment filters (base hidden for commanders —
 *  the backend scopes them to their own base anyway). */
export function FilterBar({ from, to, setFrom, setTo, baseId, setBaseId, equipId, setEquipId, onApply, showBase = true }) {
  return (
    <div className="filters">
      <div className="field">
        <label>FROM DATE</label>
        <input type="date" value={from} onChange={e => setFrom(e.target.value)} />
      </div>
      <div className="field">
        <label>TO DATE</label>
        <input type="date" value={to} onChange={e => setTo(e.target.value)} />
      </div>
      {showBase && setBaseId && (
        <div className="field">
          <label>BASE</label>
          <BaseSelect value={baseId} onChange={setBaseId} />
        </div>
      )}
      {setEquipId && (
        <div className="field">
          <label>EQUIPMENT TYPE</label>
          <EquipSelect value={equipId} onChange={setEquipId} />
        </div>
      )}
      <button className="btn" onClick={onApply}>Apply</button>
    </div>
  );
}
