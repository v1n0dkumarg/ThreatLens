const api = "/api/v1";
const authKey = "threatlens-auth";
let indicators = [];
let matches = [];
let toastTimer;

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
}

function showAuth(message = "") {
  document.querySelector("#auth-overlay").hidden = false;
  document.querySelector("#auth-error").textContent = message;
}

async function request(path, options = {}) {
  const headers = new Headers(options.headers || {});
  headers.set("Content-Type", "application/json");
  const credentials = sessionStorage.getItem(authKey);
  if (credentials) headers.set("Authorization", `Basic ${credentials}`);
  const response = await fetch(`${api}${path}`, { ...options, headers });
  if (response.status === 401) {
    sessionStorage.removeItem(authKey);
    showAuth();
    throw new Error("AUTH_REQUIRED");
  }
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(response.status === 403 ? "Your account is not permitted to perform this action." : body.detail || body.message || `Request failed (${response.status})`);
  return body;
}

function toast(message) {
  const element = document.querySelector("#toast");
  element.textContent = message;
  element.classList.add("visible");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => element.classList.remove("visible"), 2900);
}

function renderIndicators() {
  const query = document.querySelector("#indicator-search").value.trim().toLowerCase();
  const type = document.querySelector("#type-filter").value;
  const filtered = indicators.filter((item) => (!type || item.type === type)
    && [item.value, item.type, item.source, item.techniqueId, item.techniqueName, ...item.tags]
      .some((value) => String(value || "").toLowerCase().includes(query)));
  document.querySelector("#indicator-rows").innerHTML = filtered.length ? filtered.map((item) => `
    <tr><td class="observable">${escapeHtml(item.value)}<div class="source-line">${escapeHtml(item.source)} · ${item.tags.map(escapeHtml).join(" · ")}</div></td>
      <td><span class="type-badge">${escapeHtml(item.type)}</span></td>
      <td><span class="confidence-cell"><span class="confidence-track"><span class="confidence-fill" style="width:${item.confidence}%"></span></span><span class="confidence-num">${item.confidence}</span></span></td>
      <td class="attack-id">${escapeHtml(item.techniqueId || "—")}</td><td class="expiry">${escapeHtml(new Date(item.expiresAt).toLocaleDateString())}</td></tr>`).join("")
    : '<tr><td colspan="5" class="empty-row">No indicators match these filters.</td></tr>';
  document.querySelector("#indicator-total").textContent = `${filtered.length} of ${indicators.length} indicators`;
  document.querySelector("#metric-indicators").textContent = indicators.length;
  document.querySelector("#metric-confidence").textContent = indicators.filter((item) => item.confidence >= 80).length;
}

function renderMatches() {
  document.querySelector("#metric-matches").textContent = matches.length;
  const list = document.querySelector("#match-list");
  list.innerHTML = matches.length ? matches.map((match) => `
    <div class="match-row"><span class="match-time">${escapeHtml(new Date(match.observedAt).toLocaleString())}</span>
      <span class="match-value">${escapeHtml(match.observable)}</span><span class="match-asset">${escapeHtml(match.asset)}</span>
      <span class="match-context">${escapeHtml(match.context)}</span><span class="match-state">CONFIRMED</span></div>`).join("")
    : '<div class="no-match">No sightings recorded in the synthetic telemetry.</div>';
}

async function refresh() {
  try {
    [indicators, matches] = await Promise.all([request("/indicators"), request("/matches")]);
    renderIndicators();
    renderMatches();
  } catch (error) {
    if (error.message !== "AUTH_REQUIRED") toast(`Could not load intelligence: ${error.message}`);
  }
}

document.querySelector("#auth-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const username = form.elements.username.value.trim();
  const password = form.elements.password.value;
  sessionStorage.setItem(authKey, btoa(`${username}:${password}`));
  try {
    await request("/indicators");
    form.reset();
    document.querySelector("#auth-overlay").hidden = true;
    await refresh();
  } catch {
    sessionStorage.removeItem(authKey);
    showAuth("Sign-in failed. Check the username and password.");
  }
});

document.querySelector("#sign-out").addEventListener("click", () => {
  sessionStorage.removeItem(authKey);
  indicators = [];
  matches = [];
  renderIndicators();
  renderMatches();
  showAuth();
});

document.querySelector("#indicator-search").addEventListener("input", renderIndicators);
document.querySelector("#type-filter").addEventListener("change", renderIndicators);
document.querySelector("[name=confidence]").addEventListener("input", (event) => {
  document.querySelector("#confidence-value").textContent = event.currentTarget.value;
});

document.querySelector("#indicator-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const button = form.querySelector("button[type=submit]");
  const fields = new FormData(form);
  const tags = String(fields.get("tags")).split(",").map((tag) => tag.trim()).filter(Boolean);
  const payload = {
    type: fields.get("type"),
    value: fields.get("value"),
    source: fields.get("source"),
    confidence: Number(fields.get("confidence")),
    techniqueId: fields.get("techniqueId") || null,
    techniqueName: fields.get("techniqueName") || null,
    tags,
  };
  button.disabled = true;
  try {
    await request("/indicators", { method: "POST", body: JSON.stringify(payload) });
    form.elements.value.value = "";
    form.elements.tags.value = "";
    await refresh();
    toast("Indicator added to the local collection.");
  } catch (error) {
    toast(error.message);
  } finally {
    button.disabled = false;
  }
});

refresh();