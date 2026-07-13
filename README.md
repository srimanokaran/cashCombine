# cashCombine

Personal finance ledger: import bank CSVs, detect duplicates, categorise with rules, override manually.

## Run locally

### Backend (Spring Boot + SQLite)

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
