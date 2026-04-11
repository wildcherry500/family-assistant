# Family Assistant

A configurable personal intelligence platform that monitors your Gmail accounts
and turns incoming emails into structured, queryable events. Ask it for a daily
digest and get a plain-English summary of what matters — school deadlines,
appointments, permission slips, project updates, or anything else you care about.

Everything runs locally on your own machine. Your email never leaves your network.

## Two Modes

### Personal Mode

For a single person tracking multiple Gmail accounts (personal, work, side
projects). Events are categorized however you like — finance, health, projects,
etc.

### Family Mode

For a household. Multiple family members, multiple Gmail accounts, with
categories tuned for family life — school events, medical appointments, sports
schedules, and more.

## Quick Start

### 1. Choose your mode

Copy the template that matches your use case into the active config file:

**Personal mode:**

```bash
cp src/main/resources/application-personal.properties src/main/resources/application.properties
```

**Family mode:**

```bash
cp src/main/resources/application-family.properties src/main/resources/application.properties
```

Edit the file and fill in your real values (names, Gmail addresses, etc.).

### 2. Set environment variables

```bash
export GEMINI_API_KEY="your-gemini-api-key"
```

Optional:

```bash
export WEBHOOK_PORT=8080   # default is 8080
```

### 3. Set up Gmail OAuth

Place your Google OAuth `credentials.json` in `src/main/resources/`. On first
run, you'll be prompted to authorize each Gmail account in your browser. Tokens
are stored locally in `tokens/`.

### 4. Start the tunnel

Gmail push notifications need a public HTTPS endpoint. Start your Cloudflare
tunnel pointing at the webhook port:

```bash
cloudflared tunnel run family-assistant
```

### 5. Start the application

In a separate terminal:

```bash
mvn compile exec:exec
```

The assistant will:
- Start the processing pipeline
- Register a Gmail watch for each configured account (auto-renews every 5 days)
- Listen for push notifications on `POST /webhooks/gmail`
- Parse incoming emails into structured events
- Serve debug endpoints at `/debug/pstate` and `/debug/pstate/{familyId}`

## Switching Modes

Swap the properties file and restart:

```bash
cp src/main/resources/application-family.properties src/main/resources/application.properties
# edit with your values
mvn compile exec:exec
```

That's it. The pipeline reads its configuration on startup.

## Configuration Reference

See the template files for all available settings:

- `src/main/resources/application-personal.properties` — personal mode template
- `src/main/resources/application-family.properties` — family mode template

Environment variables override any property in the file. Use the `PA_` prefix:

| Property              | Env Var              |
|-----------------------|----------------------|
| `owner.name`          | `PA_OWNER_NAME`      |
| `gmail.accounts`      | `PA_GMAIL_ACCOUNTS`  |
| `event.categories`    | `PA_EVENT_CATEGORIES`|
| `timezone`            | `PA_TIMEZONE`        |
