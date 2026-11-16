// Helpers shared by the login page and the three portals.

// ---------- Talking to the Java server ----------
async function api(url, body) {
  const options = body === undefined ? {} : { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) };
  const res = await fetch(url, options);
  if (res.status === 401 && url !== '/api/login') { location.href = '/login'; throw new Error('Please log in.'); }
  let data = {};
  try { data = await res.json(); } catch (e) { /* empty response */ }
  if (!res.ok || data.error) throw new Error(data.error || 'Request failed');
  return data;
}

// Runs an action on the server, shows the result, and refreshes the page data.
async function act(url, body, okMessage) {
  try {
    await api(url, body);
    if (okMessage) toast(okMessage, true);
  } catch (e) {
    toast(e.message, false);
  }
  await loadData(true);
}

async function logout() {
  await api('/api/logout', {});
  location.href = '/login';
}

// ---------- Small helpers ----------
function val(id) { return document.getElementById(id).value; }
function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
function toast(msg, ok) {
  const t = document.getElementById('toast');
  t.textContent = msg; t.className = 'toast' + (ok ? ' ok' : ''); t.style.display = 'block';
  clearTimeout(t.timer); t.timer = setTimeout(() => t.style.display = 'none', 3500);
}
function toggleTheme() {
  const dark = document.documentElement.dataset.theme
    ? document.documentElement.dataset.theme === 'dark'
    : matchMedia('(prefers-color-scheme: dark)').matches;
  document.documentElement.dataset.theme = dark ? 'light' : 'dark';
}
// True while the user is typing in a field, so a refresh does not wipe what they typed.
function isTyping() {
  const el = document.activeElement;
  return el && ['INPUT', 'TEXTAREA', 'SELECT'].includes(el.tagName);
}

// ---------- Page parts ----------
function renderHeader(me, subtitle) {
  document.getElementById('hdr').innerHTML = `
    <div><h1>✈ ASIMS — Aviation Safety Incident Management System</h1><span class="sub">${esc(subtitle)}</span></div>
    <div class="right"><span class="chip">${esc(me.role)}</span><span>${esc(me.fullName)}</span>
      <button onclick="toggleTheme()">Theme</button><button onclick="logout()">Log out</button></div>`;
}

const STEPS = [['REPORTED', 'Reported'], ['ESCALATED', 'Escalated by admin'], ['UNDER_INVESTIGATION', 'Under investigation'], ['CLOSED', 'Closed']];
function pipeline(status) {
  const at = STEPS.findIndex(s => s[0] === status);
  return '<div class="pipeline">' + STEPS.map((s, i) =>
    `<span class="${i < at ? 'done' : i === at ? 'now' : ''}">${s[1]}</span>`).join('') + '</div>';
}
function riskBadge(i) { return `<span class="badge ${i.riskLevel}">${i.riskScore} ${i.riskLevel}</span>`; }
function statusText(s) { return `<span class="status ${s}">${s.replace('_', ' ')}</span>`; }

function evidenceHtml(i) {
  return `<label>Evidence (${i.evidence.length})</label><ul class="log">` +
    (i.evidence.map(e => `<li><b>${esc(e.id)}</b> ${esc(e.description)}<br><span class="muted">${esc(e.collectedBy)} · ${esc(e.collectedAt)}</span></li>`).join('')
      || '<li class="muted">No evidence yet.</li>') + '</ul>';
}
function infoHtml(i) {
  return `<p><b>${esc(i.type)} incident ${esc(i.id)}</b> · flight ${esc(i.flightNumber)} · ${esc(i.location)}<br>
    <span class="muted">${esc(i.details)}<br>Aircraft ${esc(i.aircraft)} · reported by ${esc(i.reportedBy)} at ${esc(i.reportedAt)} · severity ${i.severity}/5<br>
    Investigator: ${i.investigator ? esc(i.investigator) : '—'}</span></p>`;
}
function notificationsHtml(observers) {
  return observers.map(o => `<p><b>${esc(o.name)}</b> <span class="muted">(${o.notifications.length})</span></p>
    <ul class="log">${o.notifications.slice().reverse().map(n => `<li>${esc(n)}</li>`).join('') || '<li class="muted">Nothing yet.</li>'}</ul>`).join('')
    || '<p class="muted">No notifications for your role.</p>';
}

// ---------- Loading data, shared by the portals ----------
// Each portal defines render(); it reloads automatically so new incidents appear live.
let data = null, selectedId = null, lastSignature = '';
async function loadData(force) {
  try {
    const fresh = await api('/api/data');
    const signature = JSON.stringify(fresh);
    if (!force && (signature === lastSignature || isTyping())) return;
    lastSignature = signature;
    data = fresh;
    render();
  } catch (e) { /* the login redirect is handled in api() */ }
}
function select(id) { selectedId = id; render(); }
