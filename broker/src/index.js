/**
 * CurTrack broker — Cloudflare Worker.
 *
 * Sits between the CurTrack app and the iTrackGPS API (https://www.itrackgps.com/api/). Holds the
 * dealer API key, knows which units/drivers belong to which app user, logs users in, and returns
 * data in the app's own shape (handoff README §5.3 / §6).
 *
 * Secrets (set via `wrangler secret` / the deploy workflow):
 *   ITRACK_API_KEY  — the iTrackGPS key (apikey header)
 *   USERS_JSON      — [{ userId, firstName, portalId, passwordHash, salt, units:[{esn,name,plate,group,driverId,hasDashcam}], driverIds:[] }]
 *   SESSION_SECRET  — HMAC key for session tokens
 * Vars (wrangler.toml): ITRACK_BASE, ALERT_EVENTS, SESSION_HOURS, APP_VERSION_URL
 */

const JSON_HEADERS = { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" };
const json = (body, status = 200, extra = {}) => new Response(JSON.stringify(body), { status, headers: { ...JSON_HEADERS, ...extra } });
const err = (status, error) => json({ statuscode: status, error }, status);

/* ───────── crypto helpers (WebCrypto only — no dependencies) ───────── */
const enc = new TextEncoder();
const b64url = (buf) => btoa(String.fromCharCode(...new Uint8Array(buf))).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
const unb64url = (s) => Uint8Array.from(atob(s.replace(/-/g, "+").replace(/_/g, "/")), (c) => c.charCodeAt(0));

async function pbkdf2(password, saltHex) {
  const key = await crypto.subtle.importKey("raw", enc.encode(password), "PBKDF2", false, ["deriveBits"]);
  const salt = Uint8Array.from(saltHex.match(/../g).map((h) => parseInt(h, 16)));
  const bits = await crypto.subtle.deriveBits({ name: "PBKDF2", hash: "SHA-256", salt, iterations: 100000 }, key, 256);
  return [...new Uint8Array(bits)].map((b) => b.toString(16).padStart(2, "0")).join("");
}
const timingSafeEqual = (a, b) => a.length === b.length && [...a].reduce((ok, ch, i) => ok & (ch === b[i] ? 1 : 0), 1) === 1;

async function hmacKey(secret) { return crypto.subtle.importKey("raw", enc.encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign", "verify"]); }
async function signToken(payload, secret) {
  const body = b64url(enc.encode(JSON.stringify(payload)));
  const sig = b64url(await crypto.subtle.sign("HMAC", await hmacKey(secret), enc.encode(body)));
  return `${body}.${sig}`;
}
async function verifyToken(token, secret) {
  try {
    const [body, sig] = (token || "").split(".");
    if (!body || !sig) return null;
    const ok = await crypto.subtle.verify("HMAC", await hmacKey(secret), unb64url(sig), enc.encode(body));
    if (!ok) return null;
    const payload = JSON.parse(new TextDecoder().decode(unb64url(body)));
    return payload.exp > Date.now() ? payload : null;
  } catch { return null; }
}

/* ───────── iTrackGPS client ───────── */
const cache = new Map(); // url → { at, data }
async function itrack(env, path, params = {}, ttlMs = 0) {
  const url = new URL(env.ITRACK_BASE + path);
  Object.entries(params).forEach(([k, v]) => v != null && v !== "" && url.searchParams.set(k, v));
  const key = url.toString();
  const hit = cache.get(key);
  if (ttlMs && hit && Date.now() - hit.at < ttlMs) return hit.data;
  const res = await fetch(key, { headers: { apikey: env.ITRACK_API_KEY, accept: "application/json" } });
  const text = await res.text();
  let data;
  try { data = JSON.parse(text); } catch { data = text; }
  if (res.status === 401) throw new HttpError(502, "Platform rejected the API key");
  if (res.status === 404) return []; // unit/driver not on account or no data
  if (!res.ok) throw new HttpError(502, `Platform error ${res.status}: ${typeof data === "object" ? data.error || data.message : text}`.slice(0, 200));
  if (!Array.isArray(data) && typeof data !== "object") data = []; // "No Record Found"
  if (ttlMs) cache.set(key, { at: Date.now(), data });
  return data;
}
class HttpError extends Error { constructor(status, msg) { super(msg); this.status = status; } }

/* ───────── date helpers (iTrack wants MMddyyHHmmss for units/events, DD-MM-YYYY for trips) ───────── */
const p2 = (n) => String(n).padStart(2, "0");
function toItrackStamp(iso) { // app sends ISO-ish "YYYY-MM-DDTHH:mm:ss" (unit local time)
  const d = new Date(iso);
  if (isNaN(d)) throw new HttpError(400, "Bad date");
  return `${p2(d.getMonth() + 1)}${p2(d.getDate())}${String(d.getFullYear()).slice(-2)}${p2(d.getHours())}${p2(d.getMinutes())}${p2(d.getSeconds())}`;
}
function toItrackDay(iso) { const d = new Date(iso); return `${p2(d.getDate())}-${p2(d.getMonth() + 1)}-${d.getFullYear()}`; }
function parseItrackTime(s) { // "YYYY/MM/DD hh:mm:ss AM/PM"
  if (!s) return null;
  const m = /^(\d{4})\/(\d{2})\/(\d{2}) (\d{1,2}):(\d{2}):(\d{2}) ?(AM|PM)?$/i.exec(s.trim());
  if (!m) return null;
  let h = +m[4]; if (m[7]) { if (m[7].toUpperCase() === "PM" && h < 12) h += 12; if (m[7].toUpperCase() === "AM" && h === 12) h = 0; }
  return new Date(+m[1], +m[2] - 1, +m[3], h, +m[5], +m[6]).getTime();
}
function relative(ms) {
  if (!ms) return "no fix";
  const s = Math.max(0, Math.round((Date.now() - ms) / 1000));
  if (s < 60) return `${s} s ago`; if (s < 3600) return `${Math.round(s / 60)} min ago`;
  if (s < 86400) { const h = Math.floor(s / 3600); return `${h} h ${Math.round((s % 3600) / 60)} m ago`; }
  return `${Math.round(s / 86400)} d ago`;
}

/* ───────── mapping: iTrack unit record → app Unit (README §5.3) ───────── */
function mapUnit(rec, cfg) {
  const r = Array.isArray(rec) ? rec[rec.length - 1] : rec; // latest record
  const at = parseItrackTime(r?.time);
  const speed = Math.round(Number(r?.speed) || 0);
  const ign = /on|true|1/i.test(String(r?.ignitionStatus ?? ""));
  const ageMin = at ? (Date.now() - at) / 60000 : Infinity;
  const status = ageMin > 180 ? "OFFLINE" : speed > 0 ? "MOVING" : ign ? "IDLE" : "STOPPED";
  return {
    id: cfg.esn, esn: cfg.esn,
    name: cfg.name || r?.nickName || cfg.esn, plate: cfg.plate || "", group: cfg.group || "My vehicles",
    driver: cfg.driverName || "", driverId: cfg.driverId || null,
    status, speed, heading: Math.round(Number(r?.heading) || 0),
    lat: Number(r?.latitude) || 0, lng: Number(r?.longitude) || 0,
    address: r?.geolocation || "", ignition: ign,
    battery: r?.battery != null ? Math.round(Number(r.battery)) : null, batteryUnit: r?.batterUnit || r?.batteryUnit || "",
    satellites: r?.satelliteCount != null ? Number(r.satelliteCount) : null,
    lastReportAt: at, lastReport: relative(at),
    odometer: r?.odometer != null ? Math.round(Number(r.odometer) / (r?.odometerUnit === "m" ? 1000 : 1)) : null,
    engineHours: r?.engineHours ?? null, event: r?.eventName || "", unitType: r?.unitType || "",
    hasDashcam: !!cfg.hasDashcam, raw: undefined,
  };
}
const mapPoint = (r) => ({ lat: Number(r.latitude), lng: Number(r.longitude), speed: Math.round(Number(r.speed) || 0), heading: Math.round(Number(r.heading) || 0), at: parseItrackTime(r.time), event: r.eventName || "", address: r.geolocation || "" });

function classifyEvent(name = "") {
  const n = name.toLowerCase();
  if (/geofence|geo-fence|zone/.test(n)) return "GEOFENCE";
  if (/speed/.test(n)) return "SPEED";
  if (/idle|idling/.test(n)) return "IDLE";
  if (/impact|crash|harsh|collision|accident/.test(n)) return "IMPACT";
  if (/ignition|engine/.test(n)) return "IGNITION";
  if (/batter|power|voltage/.test(n)) return "BATTERY";
  if (/maint|service|dtc/.test(n)) return "MAINTENANCE";
  return "IGNITION";
}

/* ───────── auth ───────── */
function loadUsers(env) { try { return JSON.parse(env.USERS_JSON || "[]"); } catch { return []; } }
async function requireUser(req, env) {
  const auth = req.headers.get("authorization") || "";
  const token = auth.startsWith("Bearer ") ? auth.slice(7) : "";
  const payload = await verifyToken(token, env.SESSION_SECRET);
  if (!payload) throw new HttpError(401, "Session expired — please sign in again");
  const user = loadUsers(env).find((u) => u.userId.toLowerCase() === payload.sub.toLowerCase());
  if (!user) throw new HttpError(401, "Unknown user");
  return user;
}
const unitFor = (user, esn) => { const u = (user.units || []).find((x) => String(x.esn) === String(esn)); if (!u) throw new HttpError(404, "Unit not on this account"); return u; };

/* ───────── routes ───────── */
async function handle(req, env, ctx) {
  const url = new URL(req.url);
  const path = url.pathname.replace(/\/+$/, "") || "/";
  const q = Object.fromEntries(url.searchParams);

  if (req.method === "OPTIONS") return new Response(null, { status: 204, headers: cors() });
  if (path === "/" || path === "/health") return json({ ok: true, service: "curtrack-broker", time: new Date().toISOString() });

  // The app's updater reads version.json here (UPDATE_URL) — proxied from the GitHub repo, so a push updates every phone.
  if (path === "/app/version.json") {
    const r = await fetch(env.APP_VERSION_URL, { cf: { cacheTtl: 300 } });
    return new Response(await r.text(), { status: r.status, headers: { "content-type": "application/json", "cache-control": "public, max-age=300" } });
  }

  if (path === "/auth/login" && req.method === "POST") {
    const body = await req.json().catch(() => ({}));
    const { userId = "", password = "", portalId = "" } = body;
    const user = loadUsers(env).find((u) => u.userId.toLowerCase() === String(userId).trim().toLowerCase());
    const hash = user ? await pbkdf2(String(password), user.salt) : await pbkdf2("x", "00");
    if (!user || !timingSafeEqual(hash, user.passwordHash)) return err(401, "Incorrect user ID or password");
    if (portalId && user.portalId && String(portalId) !== String(user.portalId)) return err(401, "Wrong portal ID for this user");
    const hours = Number(env.SESSION_HOURS || 720);
    const exp = Date.now() + hours * 3600 * 1000;
    const token = await signToken({ sub: user.userId, exp }, env.SESSION_SECRET);
    return json({
      token, expiresAt: exp,
      user: { userId: user.userId, firstName: user.firstName || user.userId, portalId: user.portalId || "189" },
      units: (user.units || []).map((u) => ({ esn: String(u.esn), name: u.name, plate: u.plate || "", group: u.group || "My vehicles", driverId: u.driverId || null, hasDashcam: !!u.hasDashcam })),
      driverIds: user.driverIds || [],
      capabilities: { commands: false, geofences: false, reports: false, platformUrl: env.PLATFORM_URL },
    });
  }

  const user = await requireUser(req, env);

  if (path === "/units") {
    const results = await Promise.all((user.units || []).map(async (cfg) => {
      try { return mapUnit(await itrack(env, `/v2/units/${encodeURIComponent(cfg.esn)}`, {}, 10000), cfg); }
      catch (e) { return { ...mapUnit(null, cfg), status: "OFFLINE", error: e.message }; }
    }));
    return json({ units: results, at: Date.now() });
  }

  let m;
  if ((m = path.match(/^\/units\/([^/]+)$/))) {
    const cfg = unitFor(user, decodeURIComponent(m[1]));
    return json(mapUnit(await itrack(env, `/v2/units/${encodeURIComponent(cfg.esn)}`, {}, 5000), cfg));
  }
  if ((m = path.match(/^\/units\/([^/]+)\/history$/))) {
    const cfg = unitFor(user, decodeURIComponent(m[1]));
    if (!q.from || !q.to) return err(400, "from and to are required");
    const recs = await itrack(env, `/v2/units/${encodeURIComponent(cfg.esn)}`, { startdate: toItrackStamp(q.from), enddate: toItrackStamp(q.to) }, 30000);
    const points = (Array.isArray(recs) ? recs : [recs]).filter((r) => r && r.latitude != null).map(mapPoint);
    return json({ esn: cfg.esn, points });
  }
  if ((m = path.match(/^\/units\/([^/]+)\/events$/))) {
    const cfg = unitFor(user, decodeURIComponent(m[1]));
    const recs = await itrack(env, "/eventapi", { ESN: cfg.esn, EventName: q.name, EventId: q.id, StartDate: q.from && toItrackStamp(q.from), EndDate: q.to && toItrackStamp(q.to) }, 30000);
    return json({ esn: cfg.esn, events: (Array.isArray(recs) ? recs : []).map(mapPoint) });
  }
  if (path === "/alerts") {
    // No alert feed in the API: poll a list of event names per unit and merge, newest first.
    const names = String(env.ALERT_EVENTS || "").split(",").map((s) => s.trim()).filter(Boolean);
    const from = q.from || new Date(Date.now() - 7 * 86400000).toISOString();
    const to = q.to || new Date(Date.now() + 86400000).toISOString();
    const out = [];
    await Promise.all((user.units || []).flatMap((cfg) => names.map(async (name) => {
      try {
        const recs = await itrack(env, "/eventapi", { ESN: cfg.esn, EventName: name, StartDate: toItrackStamp(from), EndDate: toItrackStamp(to) }, 60000);
        (Array.isArray(recs) ? recs : []).forEach((r) => {
          const at = parseItrackTime(r.time);
          out.push({ id: `${cfg.esn}-${name}-${at}`, unitId: String(cfg.esn), unit: cfg.name || cfg.esn, title: r.eventName || name, kind: classifyEvent(r.eventName || name),
            at, relative: relative(at), description: [r.geolocation, r.speed != null ? `${Math.round(Number(r.speed))} km/h` : ""].filter(Boolean).join(" · "),
            lat: Number(r.latitude) || null, lng: Number(r.longitude) || null, highPriority: /speed|impact|geofence|tow|crash|harsh/i.test(r.eventName || name) });
        });
      } catch { /* one event name failing must not break the feed */ }
    })));
    out.sort((a, b) => (b.at || 0) - (a.at || 0));
    return json({ alerts: out.slice(0, 200), eventNames: names });
  }
  if (path === "/trips") {
    const driverId = q.driverId || (user.driverIds || [])[0];
    if (!driverId) return json({ trips: [], note: "No driver configured for this account" });
    if (!(user.driverIds || []).map(String).includes(String(driverId))) return err(403, "Driver not on this account");
    const from = q.from || new Date(Date.now() - 7 * 86400000).toISOString();
    const to = q.to || new Date(Date.now() + 86400000).toISOString();
    const recs = await itrack(env, "/tripapi", { driverId, startDate: toItrackDay(from), endDate: toItrackDay(new Date(new Date(to).getTime() + 86400000).toISOString()) }, 30000);
    const trips = (Array.isArray(recs) ? recs : []).map((t) => ({
      id: String(t.tripId), driverId: String(t.driverId), esn: String(t.gpsEsn || ""), startAt: t.startTime, endAt: t.endTime,
      fromAddress: t.startAddress || "", toAddress: t.endAddress || "", startLat: t.startLatitude, startLng: t.startLongitude, endLat: t.endLatitude, endLng: t.endLongitude,
      km: Number(t.tripKm) || 0, durationMinutes: Number(t.durationMinutes) || 0, status: t.status,
    }));
    return json({ trips });
  }
  if ((m = path.match(/^\/drivers\/([^/]+)$/))) {
    const id = decodeURIComponent(m[1]);
    if (!(user.driverIds || []).map(String).includes(String(id))) return err(403, "Driver not on this account");
    return json(await itrack(env, "/driverapi", { driverId: id }, 30000));
  }
  if (path === "/route") {
    // In-app directions: OSRM (public demo server, OpenStreetMap data). from/to = "lat,lng"; mode = driving | walking
    const [flat, flng] = String(q.from || "").split(",").map(Number); const [tlat, tlng] = String(q.to || "").split(",").map(Number);
    if (![flat, flng, tlat, tlng].every(Number.isFinite)) return err(400, "from and to must be lat,lng");
    const profile = q.mode === "walking" ? "foot" : "driving";
    const u = `${env.OSRM_BASE || "https://router.project-osrm.org"}/route/v1/${profile}/${flng},${flat};${tlng},${tlat}?overview=full&geometries=geojson&steps=true`;
    const r = await fetch(u, { headers: { "user-agent": "curtrack-broker" } });
    const j = await r.json().catch(() => ({}));
    if (!r.ok || j.code !== "Ok" || !j.routes?.length) return err(502, `Routing unavailable (${j.code || r.status})`);
    const route = j.routes[0];
    return json({
      distanceM: Math.round(route.distance), durationS: Math.round(route.duration),
      points: route.geometry.coordinates.map(([lng, lat]) => ({ lat, lng })),
      steps: (route.legs?.[0]?.steps || []).map((st) => ({ instruction: describeStep(st), distanceM: Math.round(st.distance), durationS: Math.round(st.duration), lat: st.maneuver?.location?.[1], lng: st.maneuver?.location?.[0] })).filter((st) => st.distanceM > 0 || /arrive/i.test(st.instruction)),
      attribution: "Routing © OSRM · map data © OpenStreetMap contributors",
    });
  }
  return err(404, "No such endpoint");
}

function describeStep(st) {
  const m = st.maneuver || {}; const name = st.name ? ` onto ${st.name}` : "";
  const mod = (m.modifier || "").replace("slight ", "slightly ").replace("sharp ", "sharply ");
  switch (m.type) {
    case "depart": return `Head ${mod || "out"}${name}`;
    case "arrive": return `Arrive at your vehicle${mod ? ` (on the ${mod})` : ""}`;
    case "turn": case "end of road": case "fork": case "merge": case "on ramp": case "off ramp": return `Turn ${mod || ""}${name}`.replace(/\s+/g, " ");
    case "continue": case "new name": return `Continue${name}`;
    case "roundabout": case "rotary": return `At the roundabout take exit ${m.exit || ""}${name}`;
    default: return `${m.type ? m.type[0].toUpperCase() + m.type.slice(1) : "Continue"}${name}`;
  }
}

const cors = () => ({ "access-control-allow-origin": "*", "access-control-allow-headers": "authorization, content-type", "access-control-allow-methods": "GET, POST, OPTIONS" });

export default {
  async fetch(req, env, ctx) {
    try {
      const res = await handle(req, env, ctx);
      Object.entries(cors()).forEach(([k, v]) => res.headers.set(k, v));
      return res;
    } catch (e) {
      const status = e instanceof HttpError ? e.status : 500;
      return json({ statuscode: status, error: e.message || "Internal error" }, status, cors());
    }
  },
};
