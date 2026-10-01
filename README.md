# AI Expense Ledger

A group expense tracker with an AI assistant. Built with Spring Boot, React and PostgreSQL.

- Log shared expenses and split them between group members
- See who owes whom, calculated from an append-only ledger
- Ask the AI about your spending, or scan a receipt to fill in an expense

## Run locally

Requirements: Java 17+, Maven, Node 18+, Docker.

```bash
docker compose up -d                  # start PostgreSQL
cd backend && mvn spring-boot:run     # start the API on http://localhost:8080
cd frontend && npm install && npm run start   # start the UI on http://localhost:3000
```

Set `OPENAI_API_KEY` before starting the backend to enable the AI features.

## Deploy

Build and run the production image:

```bash
docker build -t ai-expense-ledger .
docker run -p 8080:8080 -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... -e OPENAI_API_KEY=... ai-expense-ledger
```

`render.yaml` deploys the app to Render.
