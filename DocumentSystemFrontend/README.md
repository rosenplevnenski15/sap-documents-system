# Document System Frontend

## Local run

```bash
npm install
npm run dev
```

Set optional API URL:

```bash
VITE_API_URL=http://localhost:8080 npm run dev
```

## Docker

Use the root compose file:

```bash
docker compose up --build
```

Frontend: `http://localhost:3000`
Backend: `http://localhost:8080`
