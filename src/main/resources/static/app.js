/**
 * KAFKA CONTROL SYSTEM — app.js
 * Polls /api/stats and /api/events every 3s.
 * Sends messages via POST /send/foo/{payload}.
 */

'use strict';

const BASE = '';        // same-origin — served by Spring Boot
let pollTimer = null;
let autoRefresh = true;

/* ── CLOCK ─────────────────────────────────────────────────────── */
function updateClock() {
  const now = new Date();
  const pad = n => String(n).padStart(2, '0');
  document.getElementById('clock').textContent =
    `${now.getFullYear()}-${pad(now.getMonth()+1)}-${pad(now.getDate())} ` +
    `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
}
setInterval(updateClock, 1000);
updateClock();

/* ── HEALTH / BROKER STATUS ─────────────────────────────────────── */
async function checkHealth() {
  const dot  = document.getElementById('broker-dot');
  const text = document.getElementById('broker-status-text');
  try {
    const res  = await fetch(`${BASE}/api/health`, { signal: AbortSignal.timeout(3000) });
    const data = await res.json();
    dot.className  = 'dot online';
    text.textContent = `BROKER — ${data.broker} — UP`;
    document.getElementById('si-broker').textContent = data.broker;
  } catch {
    dot.className  = 'dot offline';
    text.textContent = 'BROKER — UNREACHABLE';
  }
}

/* ── STATS ──────────────────────────────────────────────────────── */
async function refreshStats() {
  try {
    const res  = await fetch(`${BASE}/api/stats`);
    const data = await res.json();
    setVal('val-produced', data.produced);
    setVal('val-consumed', data.consumed);
    setVal('val-errors',   data.errors);
    setVal('val-dlt',      data.dlt);
    document.getElementById('footer-total').textContent = `TOTAL EVENTS: ${data.total}`;
  } catch (e) {
    logTerminal('error', `[ ERR ] Stats fetch failed: ${e.message}`);
  }
}

function setVal(id, val) {
  const el = document.getElementById(id);
  if (el && el.textContent !== String(val)) el.textContent = val;
}

/* ── EVENTS TABLE ───────────────────────────────────────────────── */
async function refreshEvents() {
  try {
    const res    = await fetch(`${BASE}/api/events`);
    const events = await res.json();
    renderTable(events);
    document.getElementById('event-count').textContent = `${events.length} RECORDS`;
  } catch (e) {
    logTerminal('error', `[ ERR ] Events fetch failed: ${e.message}`);
  }
}

function renderTable(events) {
  const tbody = document.getElementById('event-tbody');
  if (!events || events.length === 0) {
    tbody.innerHTML = `<tr class="empty-row"><td colspan="6">// NO EVENTS — send a message to begin</td></tr>`;
    return;
  }
  tbody.innerHTML = events.map(ev => `
    <tr>
      <td>${escHtml(ev.id)}</td>
      <td>${formatTs(ev.timestamp)}</td>
      <td><span class="badge badge-${ev.type.toLowerCase().replace('_','-')}">${ev.type}</span></td>
      <td>${escHtml(ev.topic)}</td>
      <td>${escHtml(ev.payload)}</td>
      <td class="${statusClass(ev.status)}">${escHtml(ev.status)}</td>
    </tr>
  `).join('');
}

function formatTs(ts) {
  try {
    const d = new Date(ts);
    const pad = n => String(n).padStart(2, '0');
    return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${String(d.getMilliseconds()).padStart(3,'0')}`;
  } catch { return ts; }
}

function statusClass(s) {
  if (!s) return '';
  if (s === 'OK' || s === 'UP') return 'status-ok';
  if (s === 'FAILED')           return 'status-err';
  if (s === 'DLT')              return 'status-warn';
  if (s === 'SENT')             return 'status-sent';
  return '';
}

function escHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

/* ── SEND MESSAGE ───────────────────────────────────────────────── */
async function sendMessage() {
  const input   = document.getElementById('msg-input');
  const payload = input.value.trim();
  if (!payload) {
    showResponse('error', '// PAYLOAD is required');
    return;
  }
  await doSend(payload);
  input.value = '';
}

async function sendFail() {
  await doSend('fail');
}

async function doSend(payload) {
  const btn = document.getElementById('btn-send');
  btn.disabled = true;
  showResponse('', '// Sending...');

  try {
    const res  = await fetch(`${BASE}/send/foo/${encodeURIComponent(payload)}`, { method: 'POST' });
    const data = await res.json();
    showResponse('ok', `// SENT → topic: ${data.topic}  payload: "${data.payload}"  status: ${data.status}`);
    logTerminal('sent', `[ PROD ] Sent "${payload}" → topic1`);
    await refreshStats();
    await refreshEvents();
  } catch (e) {
    showResponse('err', `// ERROR: ${e.message}`);
    logTerminal('error', `[ ERR ] Send failed: ${e.message}`);
  } finally {
    btn.disabled = false;
  }
}

function showResponse(cls, msg) {
  const el = document.getElementById('send-response');
  el.className = `response-line ${cls}`;
  el.textContent = msg;
}

/* ── TERMINAL ───────────────────────────────────────────────────── */
function logTerminal(type, msg) {
  const out = document.getElementById('terminal-output');
  const div = document.createElement('div');
  div.className = `term-line term-${type}`;
  div.textContent = msg;
  out.appendChild(div);
  out.scrollTop = out.scrollHeight;
  // Cap at 100 lines
  while (out.children.length > 100) out.removeChild(out.firstChild);
}

function clearTerminal() {
  document.getElementById('terminal-output').innerHTML = '';
  logTerminal('info', '[ SYS ] Terminal cleared');
}

/* ── CLEAR LOG VIEW (UI only) ───────────────────────────────────── */
function clearLog() {
  document.getElementById('event-tbody').innerHTML =
    `<tr class="empty-row"><td colspan="6">// VIEW CLEARED — data still held in memory</td></tr>`;
  document.getElementById('event-count').textContent = '0 RECORDS';
  logTerminal('info', '[ SYS ] Event log view cleared (server-side data retained)');
}

/* ── AUTO POLL ──────────────────────────────────────────────────── */
document.getElementById('auto-refresh').addEventListener('change', function () {
  autoRefresh = this.checked;
  if (autoRefresh) {
    startPolling();
    logTerminal('info', '[ SYS ] Auto-refresh ENABLED (3s)');
  } else {
    stopPolling();
    logTerminal('info', '[ SYS ] Auto-refresh DISABLED');
  }
});

function startPolling() {
  if (pollTimer) return;
  pollTimer = setInterval(async () => {
    if (autoRefresh) {
      await refreshStats();
      await refreshEvents();
    }
  }, 3000);
}

function stopPolling() {
  clearInterval(pollTimer);
  pollTimer = null;
}

/* ── KEYBOARD SHORTCUT: Enter to send ──────────────────────────── */
document.getElementById('msg-input').addEventListener('keydown', e => {
  if (e.key === 'Enter') sendMessage();
});

/* ── INIT ────────────────────────────────────────────────────────── */
(async function init() {
  logTerminal('info', `[ SYS ] Dashboard loaded at ${new Date().toISOString()}`);
  await checkHealth();
  await refreshStats();
  await refreshEvents();
  startPolling();
  setInterval(checkHealth, 10000);
})();
