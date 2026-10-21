// node scripts/make-user.mjs <userId> <password> [firstName] [esn:name,esn:name]
// Prints a USERS_JSON entry with a PBKDF2-SHA256 hash (100k iterations) — same scheme the worker verifies.
import { webcrypto as crypto } from "node:crypto";
const [userId, password, firstName = "", unitsArg = ""] = process.argv.slice(2);
if (!userId || !password) { console.error("usage: make-user.mjs <userId> <password> [firstName] [esn:name,...]"); process.exit(1); }
const salt = [...crypto.getRandomValues(new Uint8Array(16))].map((b) => b.toString(16).padStart(2, "0")).join("");
const key = await crypto.subtle.importKey("raw", new TextEncoder().encode(password), "PBKDF2", false, ["deriveBits"]);
const bits = await crypto.subtle.deriveBits({ name: "PBKDF2", hash: "SHA-256", salt: Uint8Array.from(salt.match(/../g).map((h) => parseInt(h, 16))), iterations: 100000 }, key, 256);
const passwordHash = [...new Uint8Array(bits)].map((b) => b.toString(16).padStart(2, "0")).join("");
const units = unitsArg ? unitsArg.split(",").map((p) => { const [esn, name] = p.split(":"); return { esn, name: name || esn, plate: "", group: "My vehicles", hasDashcam: false }; }) : [];
console.log(JSON.stringify({ userId, firstName: firstName || userId, portalId: "189", salt, passwordHash, units, driverIds: [] }, null, 2));
