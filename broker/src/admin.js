/* CurTrack client admin — single page served at /admin. Talks to the /admin/* API with the admin token (kept in this browser only). */
export const ADMIN_HTML = `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>CurTrack · Clients</title>
<link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
<style>
:root{--amber:#f2a13c;--teal:#57c9c1;--red:#e2705b;--bg:#0d1012;--bg2:#14181b;--card:#171c1f;--card2:#1d2327;--ink:#f2f5f4;--dim:rgba(242,245,244,.6);--faint:rgba(242,245,244,.1);--line:rgba(242,245,244,.09)}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--ink);font-family:"Space Grotesk",system-ui,sans-serif;font-size:14px}
.wrap{max-width:1040px;margin:0 auto;padding:24px 18px 80px}.kick{font:500 10px/1 "JetBrains Mono",monospace;letter-spacing:.24em;color:var(--dim);text-transform:uppercase}
h1{font-size:26px;letter-spacing:-.02em;margin:6px 0 18px}h2{font-size:15px;margin:0}
.card{background:var(--card);border:1px solid var(--line);border-radius:20px;padding:16px;margin-bottom:14px}
.row{display:flex;gap:10px;align-items:center;flex-wrap:wrap}.between{display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap}
input,textarea,select{background:var(--card2);border:1px solid var(--line);border-radius:14px;color:var(--ink);font:400 14px "Space Grotesk",sans-serif;padding:11px 13px;min-height:44px}
input:focus,textarea:focus{outline:1px solid var(--amber)}textarea{width:100%;min-height:120px;font-family:"JetBrains Mono",monospace;font-size:12px}
.btn{border:0;border-radius:16px;padding:12px 16px;font:600 14px "Space Grotesk",sans-serif;cursor:pointer;background:var(--card2);color:var(--ink);border:1px solid var(--line)}
.btn.pri{background:var(--amber);color:#14181b;border-color:transparent}.btn.red{color:var(--red)}.btn.sm{padding:8px 12px;font-size:12px;border-radius:12px}
.mono{font-family:"JetBrains Mono",monospace;font-size:12px}.dim{color:var(--dim);font-size:13px}.pill{font:500 10px/1 "JetBrains Mono",monospace;letter-spacing:.1em;padding:6px 9px;border-radius:999px;border:1px solid var(--line);color:var(--dim);text-transform:uppercase}
.pill.ok{color:#14181b;background:var(--teal);border-color:transparent}.pill.warn{color:var(--amber);border-color:var(--amber)}.pill.red{color:var(--red);border-color:var(--red)}
table{width:100%;border-collapse:collapse}td,th{text-align:left;padding:11px 8px;border-top:1px solid var(--line);vertical-align:top}th{font:500 9px/1 "JetBrains Mono",monospace;letter-spacing:.16em;color:var(--dim);text-transform:uppercase;border-top:0}
.code{font:500 22px/1 "JetBrains Mono",monospace;letter-spacing:.2em;color:var(--amber)}.toast{position:fixed;left:50%;bottom:24px;transform:translateX(-50%);background:var(--ink);color:var(--bg);padding:12px 16px;border-radius:14px;font-weight:600;display:none}
label{display:block;margin-bottom:4px}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:10px}.unitrow{display:grid;grid-template-columns:1.2fr 1fr .7fr auto;gap:8px;margin-top:8px}
@media(max-width:600px){.unitrow{grid-template-columns:1fr 1fr}}
</style></head><body><div class="wrap">
<div class="kick">CurTrack · portal 189</div><h1>Clients</h1>

<div class="card" id="auth"><div class="between"><div><h2>Admin token</h2><div class="dim">Stored only in this browser. Ask CurTrack support if you lose it.</div></div>
<div class="row"><input id="tok" type="password" placeholder="Admin token" style="min-width:260px"><button class="btn pri" onclick="saveTok()">Connect</button></div></div></div>

<div id="app" style="display:none">
<div class="card"><div class="between"><h2>Add or edit a client</h2><span class="dim">One client = one app login. Each client sees only their own units.</span></div>
<div class="grid" style="margin-top:12px">
<div><label class="kick">User ID</label><input id="f_id" placeholder="e.g. jdoe or 5999 123 4567"></div>
<div><label class="kick">Name</label><input id="f_name" placeholder="Shown in the app greeting"></div>
<div><label class="kick">Phone (WhatsApp)</label><input id="f_phone" placeholder="+599 9 …"></div>
<div><label class="kick">Email</label><input id="f_email" placeholder="optional"></div></div>
<div class="kick" style="margin-top:14px">Units (ESN / IMEI) — tap “check” to confirm the platform sees it</div>
<div id="units"></div>
<div class="row" style="margin-top:10px"><button class="btn sm" onclick="addUnitRow()">+ unit</button></div>
<div class="row" style="margin-top:14px"><button class="btn pri" onclick="saveClient()">Save client</button><button class="btn" onclick="clearForm()">Clear</button></div></div>

<div class="card"><div class="between"><h2>Bulk import</h2><span class="dim">One client per line: <span class="mono">userId, name, phone, esn:nickname; esn:nickname</span></span></div>
<textarea id="bulk" placeholder="jdoe, John Doe, +599 9 555 1234, 1363024147:Hilux; 1363024199:Van 2&#10;maria, Maria P, +599 9 555 9876, 1363024200:Corolla"></textarea>
<div class="row" style="margin-top:10px"><button class="btn pri" onclick="bulkImport()">Import all</button><span class="dim">Existing user IDs are updated, new ones created. Then press “Code” on each row to issue their first-login code.</span></div></div>

<div class="card"><div class="between"><h2 id="count">Clients</h2><button class="btn sm" onclick="load()">Refresh</button></div>
<div style="overflow-x:auto"><table id="tbl"><thead><tr><th>User ID</th><th>Name</th><th>Units</th><th>Status</th><th>Last login</th><th></th></tr></thead><tbody></tbody></table></div></div>

<div class="card" id="codebox" style="display:none;border-color:rgba(242,161,60,.6)"><div class="kick">First-login code · valid 7 days</div>
<div class="row" style="margin-top:8px"><span class="code" id="codeval"></span><button class="btn sm" onclick="copyMsg()">Copy WhatsApp message</button></div>
<div class="dim" id="codemsg" style="margin-top:8px;white-space:pre-wrap"></div></div>
</div>
<div class="toast" id="toast"></div>
<script>
const api = (p, opt={}) => fetch(p, { ...opt, headers: { "content-type": "application/json", authorization: "Bearer " + localStorage.tok, ...(opt.headers||{}) } }).then(async r => { const j = await r.json().catch(()=>({})); if (!r.ok) throw new Error(j.error || r.status); return j; });
const $ = (id) => document.getElementById(id);
function toast(m){ const t=$("toast"); t.textContent=m; t.style.display="block"; setTimeout(()=>t.style.display="none",2200); }
function saveTok(){ localStorage.tok = $("tok").value.trim(); boot(); }
async function boot(){ if(!localStorage.tok) return; try { await api("/admin/check"); $("auth").style.display="none"; $("app").style.display="block"; load(); } catch(e){ toast("Token not accepted"); } }
function unitRow(u={}){ const d=document.createElement("div"); d.className="unitrow"; d.innerHTML = '<input placeholder="ESN / IMEI" value="'+(u.esn||"")+'" class="u_esn"><input placeholder="Name shown in app" value="'+(u.name||"")+'" class="u_name"><input placeholder="Plate" value="'+(u.plate||"")+'" class="u_plate"><div class="row"><button class="btn sm" onclick="checkEsn(this)">check</button><button class="btn sm red" onclick="this.closest(\\'.unitrow\\').remove()">×</button></div>'; $("units").appendChild(d); }
function addUnitRow(){ unitRow(); }
async function checkEsn(btn){ const row=btn.closest(".unitrow"); const esn=row.querySelector(".u_esn").value.trim(); if(!esn) return; btn.textContent="…"; try { const r = await api("/admin/units/"+encodeURIComponent(esn)); if (r.found) { btn.textContent="✓ "+(r.nickName||"found"); if(!row.querySelector(".u_name").value) row.querySelector(".u_name").value = r.nickName||""; } else { btn.textContent="not found"; } } catch(e){ btn.textContent="error"; } }
function readUnits(){ return [...document.querySelectorAll(".unitrow")].map(r=>({ esn:r.querySelector(".u_esn").value.trim(), name:r.querySelector(".u_name").value.trim(), plate:r.querySelector(".u_plate").value.trim() })).filter(u=>u.esn); }
function clearForm(){ ["f_id","f_name","f_phone","f_email"].forEach(i=>$(i).value=""); $("units").innerHTML=""; unitRow(); }
async function saveClient(){ const id=$("f_id").value.trim(); if(!id) return toast("User ID is required"); try { await api("/admin/users/"+encodeURIComponent(id), { method:"PUT", body: JSON.stringify({ firstName:$("f_name").value, phone:$("f_phone").value, email:$("f_email").value, units: readUnits() }) }); toast("Saved "+id); load(); } catch(e){ toast(e.message); } }
function edit(u){ $("f_id").value=u.userId; $("f_name").value=u.firstName||""; $("f_phone").value=u.phone||""; $("f_email").value=u.email||""; $("units").innerHTML=""; (u.units.length?u.units:[{}]).forEach(unitRow); window.scrollTo({top:0,behavior:"smooth"}); }
async function del(id){ if(!confirm("Delete client "+id+"? They will no longer be able to log in.")) return; try { await api("/admin/users/"+encodeURIComponent(id), { method:"DELETE" }); toast("Deleted"); load(); } catch(e){ toast(e.message); } }
let lastCode=null;
async function code(u){ try { const r = await api("/admin/users/"+encodeURIComponent(u.userId)+"/code", { method:"POST" }); lastCode={ ...r, user:u }; $("codeval").textContent=r.code; $("codemsg").textContent = msgFor(u, r.code); $("codebox").style.display="block"; $("codebox").scrollIntoView({behavior:"smooth"}); load(); } catch(e){ toast(e.message); } }
function msgFor(u, c){ return "Hi "+(u.firstName||u.userId)+", welcome to the new CurTrack app.\\n\\n1. Install: https://github.com/curtrack01/curtrack-android/releases/latest\\n2. Open the app → \\"I have a code\\"\\n3. User ID: "+u.userId+"\\n   Code: "+c+" (valid 7 days)\\n4. Choose your own password.\\n\\nQuestions? +599 9 518 7297 · gps@curtrack.com"; }
function copyMsg(){ navigator.clipboard.writeText($("codemsg").textContent).then(()=>toast("Message copied — paste it in WhatsApp")); }
async function bulkImport(){ const lines=$("bulk").value.split("\\n").map(l=>l.trim()).filter(Boolean); let n=0; for (const l of lines){ const [id,name,phone,units=""]=l.split(",").map(s=>s.trim()); if(!id) continue; const us=units.split(";").map(s=>s.trim()).filter(Boolean).map(s=>{ const [esn,nm]=s.split(":"); return { esn:esn.trim(), name:(nm||"").trim() }; }); try { await api("/admin/users/"+encodeURIComponent(id), { method:"PUT", body: JSON.stringify({ firstName:name||"", phone:phone||"", units:us }) }); n++; } catch(e){ toast(id+": "+e.message); } } toast("Imported "+n+" client(s)"); $("bulk").value=""; load(); }
function fmt(t){ return t ? new Date(t).toLocaleString() : "—"; }
async function load(){ try { const r = await api("/admin/users"); const tb=$("tbl").querySelector("tbody"); tb.innerHTML=""; $("count").textContent = r.users.length+" client"+(r.users.length===1?"":"s");
  r.users.sort((a,b)=>a.userId.localeCompare(b.userId)).forEach(u=>{ const tr=document.createElement("tr"); const st = u.hasPassword ? '<span class="pill ok">active</span>' : u.activation ? '<span class="pill warn">code sent</span>' : '<span class="pill red">no login</span>';
    tr.innerHTML = '<td class="mono">'+u.userId+'</td><td>'+(u.firstName||"")+'<div class="dim">'+(u.phone||"")+'</div></td><td>'+(u.units.map(x=>'<div><span class="mono">'+x.esn+'</span> '+(x.name||"")+'</div>').join("")||'<span class="dim">none</span>')+'</td><td>'+st+'</td><td class="dim">'+fmt(u.lastLoginAt)+'</td><td><div class="row"><button class="btn sm">Edit</button><button class="btn sm">Code</button><button class="btn sm red">Delete</button></div></td>';
    const [b1,b2,b3]=tr.querySelectorAll("button"); b1.onclick=()=>edit(u); b2.onclick=()=>code(u); b3.onclick=()=>del(u.userId); tb.appendChild(tr); }); } catch(e){ toast(e.message); } }
unitRow(); boot();
</script></div></body></html>`;
