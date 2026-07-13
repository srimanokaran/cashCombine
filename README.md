# cashCombine

Personal finance ledger: import bank CSVs, detect duplicates, categorise with rules, override manually.

## Run locally

### Backend (Spring Boot + SQLite)

Quiet API log (recommended while developing against the UI):

```bash
./scripts/run-backend
```

Example output:

```text
18:30:01 cashCombine ready → http://localhost:8080
18:30:01 Logging /api/* calls (4xx/5xx show rejection reason)
18:30:12 POST /api/accounts → 201 (18ms)
18:30:20 POST /api/accounts/.../import → 400 rejected: Invalid CSV format... (42ms)
```

Full Spring logging:

```bash
./gradlew bootRun
```

API: `http://localhost:8080`  
SQLite file: `cashcombine.db` in the project root (gitignored).

### Frontend (React SPA)

```bash
cd frontend
npm install
npm run dev
```

UI: `http://localhost:5173` (Vite proxies `/api` to the backend).

## Slice 1 flow

1. Create categories and contains-match rules
2. Create a CommBank account
3. Import a CSV
4. Review transactions and change categories as needed

See [domain-notes.md](./domain-notes.md) and [goal.md](./goal.md).
