# CurTrack broker (Cloudflare Worker)

Thin server between the CurTrack app and the iTrackGPS API. Holds the dealer API key, maps app users to their units
(ESN) and drivers, issues session tokens, and returns data in the app's shape. Also serves `/app/version.json` for the
in-app updater.

## Endpoints
| Method | Path | Notes |
|---|---|---|
| POST | `/auth/login` | `{userId, password, portalId}` → `{token, expiresAt, user, units[], driverIds[], capabilities}` |
| GET | `/units` | latest position of every unit on the account (Bearer token) |
| GET | `/units/{esn}` | one unit |
| GET | `/units/{esn}/history?from&to` | trail points (`from`/`to` ISO local time) |
| GET | `/units/{esn}/events?name&from&to` | event history for one event name |
| GET | `/alerts?from&to` | merged event feed across units (names in `ALERT_EVENTS`) |
| GET | `/trips?driverId&from&to` | trips for a driver on the account |
| GET | `/drivers/{id}` | driver record |
| GET | `/app/version.json` | updater manifest (proxied from the repo) |

## Deploy
```
cd broker && npm install
export CLOUDFLARE_API_TOKEN=… CLOUDFLARE_ACCOUNT_ID=…
npx wrangler deploy
echo -n "<value>" | npx wrangler secret put ITRACK_API_KEY   # likewise SESSION_SECRET, USERS_JSON
```
Live at https://curtrack-broker.curtrack01.workers.dev

## Secrets
Set in Cloudflare (the deploy workflow pushes them from GitHub secrets of the same name):
- `ITRACK_API_KEY` — iTrackGPS key
- `SESSION_SECRET` — random string for signing session tokens
- `USERS_JSON` — the user list; generate entries with `node scripts/make-user.mjs <userId> <password> <firstName>`

## Users
`USERS_JSON` example (one app login with one unit):
```json
[{"userId":"curtrack","firstName":"Curtrack","portalId":"189","salt":"…","passwordHash":"…",
  "units":[{"esn":"1363024147","name":"My car","plate":"","group":"My vehicles","hasDashcam":false}],"driverIds":[]}]
```
To add a customer: run `make-user.mjs`, add their units' ESNs, update the `USERS_JSON` secret, redeploy.
