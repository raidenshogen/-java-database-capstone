const qs  = (s, r = document) => r.querySelector(s);
const qsa = (s, r = document) => [...r.querySelectorAll(s)];
const reduceMotion = matchMedia("(prefers-reduced-motion: reduce)").matches;

/* ---------- Helpers ---------- */
const esc = s => String(s ?? "").replace(/[&<>"']/g, c => ({ "&":"&amp;", "<":"&lt;", ">":"&gt;", '"':"&quot;", "'":"&#39;" }[c]));
const icon = (n, cls = "") => `<svg class="i ${cls}" aria-hidden="true"><use href="#i-${n}"/></svg>`;
const hashStr = s => [...String(s)].reduce((a, c) => a + c.charCodeAt(0), 0);
const initials = name => {
  const p = String(name || "?").split(/[\s._@-]+/).filter(Boolean);
  return ((p[0]?.[0] || "?") + (p[1]?.[0] || "")).toUpperCase();
};
const avatar = (name, size = "") => `<span class="avatar ${size} av-${hashStr(name) % 5}" aria-hidden="true">${esc(initials(name))}</span>`;
const fmtDate = v => { if (!v) return ""; const d = new Date(String(v).length <= 10 ? v + "T00:00" : v); return isNaN(d) ? v : d.toLocaleDateString(undefined, { weekday:"short", day:"numeric", month:"short", year:"numeric" }); };
const fmtDateTime = v => { if (!v) return ""; const d = new Date(v); return isNaN(d) ? String(v).replace("T", " ") : d.toLocaleString(undefined, { dateStyle:"medium", timeStyle:"short" }); };
const statusBadge = s => { const slug = String(s).toLowerCase().replace("_", "-"); return `<span class="badge status-${slug}"><i class="dot"></i>${esc(String(s).replace("_", " "))}</span>`; };
const emptyState = (ic, title, text = "") => `<div class="empty-state"><span class="icon-badge">${icon(ic)}</span><strong>${esc(title)}</strong><span>${esc(text)}</span></div>`;
const skeletonRows = n => Array.from({ length: n }, () => '<span class="skeleton sk-row"></span>').join("");
const reloadSoon = (ms = 650) => setTimeout(() => location.reload(), ms);
function setBusy(btn, on) { if (!btn) return; btn.classList.toggle("is-busy", on); btn.disabled = on; }

/* ---------- API ---------- */
function csrfToken() {
  const cookie = document.cookie
    .split("; ")
    .find(value => value.startsWith("XSRF-TOKEN="));
  return cookie ? decodeURIComponent(cookie.substring("XSRF-TOKEN=".length)) : null;
}

async function api(url, options = {}) {
  let response;
  try {
    const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
    const token = csrfToken();
    if (token) headers["X-XSRF-TOKEN"] = token;
    response = await fetch(url, { ...options, headers });
  } catch (_) {
    throw new Error("Can't reach the server. Please check your connection and try again.");
  }
  if (!response.ok) {
    const text = await response.text();
    if (response.url.endsWith("/login")) throw new Error("Your session has expired. Please sign in again.");
    let message = text;
    try { const j = JSON.parse(text); message = j.message || j.error || text; } catch (_) { /* plain text */ }
    throw new Error(message || `Request failed (${response.status})`);
  }
  if (response.url.endsWith("/login") || (response.headers.get("content-type") || "").includes("text/html")) {
    throw new Error("Your session has expired. Please sign in again.");
  }
  return response.status === 204 ? null : response.json();
}

/* ---------- Inline notices ---------- */
function showMessage(target, message, kind = "success") {
  target.innerHTML = `${icon(kind === "error" ? "alert" : kind === "info" ? "info" : "check")}<span>${esc(message)}</span>`;
  target.className = `notice notice-${kind}`;
  target.classList.remove("hidden");
}
function hideMessage(target) { target.classList.add("hidden"); }

/* ---------- Toasts ---------- */
function toast(message, kind = "success", opts = {}) {
  let host = qs("#toasts");
  if (!host) {
    host = document.createElement("div");
    host.id = "toasts"; host.className = "toasts"; host.setAttribute("aria-live", "polite");
    document.body.appendChild(host);
  }
  const life = opts.life || (kind === "error" ? 6000 : 4000);
  const titles = { success: "Done", error: "Something went wrong", info: "Heads up" };
  const t = document.createElement("div");
  t.className = `toast toast-${kind}`; t.style.setProperty("--life", life + "ms");
  t.innerHTML = `<span class="toast-icon">${icon(kind === "error" ? "alert" : kind === "info" ? "info" : "check")}</span>
    <div class="toast-body"><strong>${esc(opts.title || titles[kind])}</strong><span>${esc(message)}</span></div>
    <button class="toast-close" aria-label="Dismiss">${icon("x")}</button>`;
  const close = () => { t.classList.add("leaving"); setTimeout(() => t.remove(), 300); };
  t.querySelector(".toast-close").onclick = close;
  host.appendChild(t);
  setTimeout(close, life);
}

/* ---------- Modals ---------- */
function buildDialog(inner) {
  const d = document.createElement("dialog");
  d.className = "modal";
  d.innerHTML = `<div class="modal-panel">${inner}</div>`;
  document.body.appendChild(d);
  d.showModal();
  return d;
}
function closeDialog(d) { d.classList.add("closing"); setTimeout(() => { d.close(); d.remove(); }, 180); }

function confirmAction(message, { title = "Please confirm", confirmLabel = "Confirm", cancelLabel = "Cancel", danger = false } = {}) {
  return new Promise(resolve => {
    const d = buildDialog(`
      <div class="modal-head"><span class="icon-badge ${danger ? "is-danger" : ""}">${icon(danger ? "alert" : "info")}</span><h3>${esc(title)}</h3></div>
      <p class="modal-text">${esc(message)}</p>
      <div class="modal-actions">
        <button type="button" class="btn" data-x="no">${esc(cancelLabel)}</button>
        <button type="button" class="btn ${danger ? "btn-danger-solid" : "btn-primary"}" data-x="yes">${esc(confirmLabel)}</button>
      </div>`);
    let done = false;
    const finish = v => { if (done) return; done = true; closeDialog(d); resolve(v); };
    d.addEventListener("click", e => { const b = e.target.closest("[data-x]"); if (b) finish(b.dataset.x === "yes"); else if (e.target === d) finish(false); });
    d.addEventListener("cancel", e => { e.preventDefault(); finish(false); });
    qs('[data-x="yes"]', d).focus();
  });
}

function fieldHtml(f) {
  const req = f.required ? "required" : "";
  const val = f.value ?? "";
  let control;
  if (f.type === "select") {
    control = `<select name="${f.name}" ${req}>${f.options.map(o => `<option value="${esc(o.value)}" ${String(o.value) === String(val) ? "selected" : ""}>${esc(o.label)}</option>`).join("")}</select>`;
  } else if (f.type === "textarea") {
    control = `<textarea name="${f.name}" rows="${f.rows || 4}" ${req} placeholder="${esc(f.placeholder || "")}">${esc(val)}</textarea>`;
  } else {
    control = `<input name="${f.name}" type="${f.type || "text"}" value="${esc(val)}" ${req} placeholder="${esc(f.placeholder || "")}">`;
  }
  return `<label class="${f.half ? "" : "f-full"}">${esc(f.label)}${control}</label>`;
}

/** Modal form. onSubmit(values) may throw — the error is shown inline and the modal stays open. */
function modalForm({ title, subtitle = "", icon: ic = "edit", fields, submitLabel = "Save", onSubmit }) {
  return new Promise(resolve => {
    const d = buildDialog(`
      <div class="modal-head"><span class="icon-badge">${icon(ic)}</span>
        <div><h3>${esc(title)}</h3>${subtitle ? `<p>${esc(subtitle)}</p>` : ""}</div>
        <button type="button" class="btn btn-icon btn-ghost modal-close" data-x="close" aria-label="Close">${icon("x")}</button></div>
      <form class="stack" novalidate>
        <div class="form-grid">${fields.map(fieldHtml).join("")}</div>
        <div class="notice notice-error hidden" data-err></div>
        <div class="modal-actions"><button type="button" class="btn" data-x="close">Cancel</button><button class="btn btn-primary" type="submit">${esc(submitLabel)}</button></div>
      </form>`);
    const form = qs("form", d), err = qs("[data-err]", d), btn = qs('[type="submit"]', d);
    let done = false;
    const finish = () => { if (done) return; done = true; closeDialog(d); resolve(); };
    d.addEventListener("click", e => { if (e.target.closest('[data-x="close"]') || e.target === d) finish(); });
    d.addEventListener("cancel", e => { e.preventDefault(); finish(); });
    form.addEventListener("submit", async e => {
      e.preventDefault();
      if (!form.checkValidity()) { form.reportValidity(); return; }
      hideMessage(err); setBusy(btn, true);
      try { await onSubmit(Object.fromEntries(new FormData(form))); finish(); }
      catch (x) { showMessage(err, x.message, "error"); setBusy(btn, false); }
    });
    qs("input,select,textarea", form)?.focus();
  });
}

/* ---------- Motion utilities ---------- */
function countUp(el, ms = 900) {
  const end = Number(el.textContent) || 0;
  if (reduceMotion || end === 0) return;
  const t0 = performance.now(); el.textContent = "0";
  (function step(t) {
    const p = Math.min((t - t0) / ms, 1);
    el.textContent = Math.round(end * (1 - Math.pow(1 - p, 3)));
    if (p < 1) requestAnimationFrame(step);
  })(t0);
}

function showTab(root, name) {
  qsa("[data-tab]", root).forEach(b => b.classList.toggle("active", b.dataset.tab === name));
  qsa("[data-panel]", root).forEach(p => {
    const show = p.dataset.panel === name;
    p.classList.toggle("hidden", !show);
    if (show) { p.classList.remove("anim-in"); void p.offsetWidth; p.classList.add("anim-in"); }
  });
}

/* ---------- Global wiring ---------- */
document.addEventListener("DOMContentLoaded", () => {
  // scroll reveal (stagger by position among reveal siblings)
  const reveals = qsa(".reveal");
  reveals.forEach(el => {
    const sibs = [...el.parentElement.children].filter(c => c.classList.contains("reveal"));
    el.style.setProperty("--d", sibs.indexOf(el));
  });
  if ("IntersectionObserver" in window) {
    const io = new IntersectionObserver(entries => entries.forEach(e => {
      if (e.isIntersecting) { e.target.classList.add("in"); io.unobserve(e.target); }
    }), { threshold: .08 });
    reveals.forEach(el => io.observe(el));
  }
  setTimeout(() => reveals.forEach(el => el.classList.add("in")), 2500); // safety net

  qsa("[data-count]").forEach(el => countUp(el));
  qsa("tbody").forEach(tb => [...tb.rows].forEach((r, i) => r.style.setProperty("--i", Math.min(i, 12))));
  qsa("[data-avatar]").forEach(el => { const n = el.textContent.trim(); el.classList.add("av-" + hashStr(n) % 5); el.textContent = initials(n); });
  qsa("[data-fmt-datetime]").forEach(el => el.textContent = fmtDateTime(el.textContent.trim()));
  qsa(".nav a").forEach(a => a.classList.toggle("active", a.pathname === location.pathname));

  // password strength meters
  qsa("input[data-strength]").forEach(input => {
    const m = document.createElement("div");
    m.className = "strength"; m.dataset.score = 0; m.innerHTML = "<i></i><i></i><i></i><i></i><small></small>";
    (input.closest(".field-icon") || input).after(m);
    const labels = ["", "Weak", "Fair", "Good", "Strong"];
    input.addEventListener("input", () => {
      const v = input.value;
      const s = v ? Math.max(1, (v.length >= 8) + (/[a-z]/.test(v) && /[A-Z]/.test(v)) + /\d/.test(v) + /[^A-Za-z0-9]/.test(v)) : 0;
      m.dataset.score = s; m.lastChild.textContent = labels[s];
    });
  });

  // live table filter
  qsa("input[data-filter]").forEach(input => {
    const table = qs(input.dataset.filter);
    if (!table) return;
    input.addEventListener("input", () => {
      const q = input.value.trim().toLowerCase();
      const rows = qsa("tbody tr:not(.empty-row):not(.filter-empty)", table);
      let visible = 0;
      rows.forEach(r => { const show = r.textContent.toLowerCase().includes(q); r.classList.toggle("hidden", !show); if (show) visible++; });
      let none = qs(".filter-empty", table);
      if (!visible && rows.length) {
        if (!none) { none = document.createElement("tr"); none.className = "filter-empty"; none.innerHTML = `<td colspan="9" class="empty">No results match “<span></span>”.</td>`; qs("tbody", table).appendChild(none); }
        qs("span", none).textContent = input.value;
        none.classList.remove("hidden");
      } else none?.classList.add("hidden");
    });
  });
});

document.addEventListener("click", e => {
  // ripple
  const btn = e.target.closest(".btn");
  if (btn && !reduceMotion && !btn.disabled) {
    const r = btn.getBoundingClientRect(), size = Math.max(r.width, r.height), s = document.createElement("span");
    s.className = "ripple";
    s.style.cssText = `width:${size}px;height:${size}px;left:${e.clientX - r.left - size / 2}px;top:${e.clientY - r.top - size / 2}px`;
    btn.appendChild(s); setTimeout(() => s.remove(), 600);
  }
  // password visibility
  const tg = e.target.closest("[data-toggle-password]");
  if (tg) {
    const input = qs("input", tg.closest(".field-icon")), show = input.type === "password";
    input.type = show ? "text" : "password";
    qs("use", tg).setAttribute("href", show ? "#i-eye-off" : "#i-eye");
  }
  // tabs
  const tab = e.target.closest("[data-tab]");
  if (tab) showTab(tab.closest(".tabs-root"), tab.dataset.tab);
});
