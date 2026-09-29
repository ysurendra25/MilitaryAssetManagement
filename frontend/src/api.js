/** Small fetch wrapper: JSON in/out, session cookie always sent,
 *  errors surfaced as plain Error objects.
 *
 *  API_BASE is empty in local/dev use (same origin as the backend), and can
 *  be set at build time when the frontend is hosted separately, e.g.:
 *    VITE_API_BASE=https://mams-backend.onrender.com npm run build
 */
const API_BASE = import.meta.env.VITE_API_BASE || '';

export async function api(path, options = {}) {
  const opts = {
    credentials: 'include',
    method: options.method || 'GET',
    headers: { 'Content-Type': 'application/json' }
  };
  if (options.body !== undefined) {
    opts.body = JSON.stringify(options.body);
  }
  const res = await fetch(API_BASE + '/api' + path, opts);
  let data = {};
  try {
    data = await res.json();
  } catch {
    // empty body is fine
  }
  if (!res.ok) {
    throw new Error(data.error || data.message || 'Request failed (' + res.status + ')');
  }
  return data;
}
