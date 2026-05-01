# LLM Wiki Platform — Frontend

**Stack:** React 18 + TypeScript + Vite + Ant Design + D3.js
**Locale:** zh_CN

## Structure

```
frontend/
├── src/
│   ├── pages/              # Route-level page components
│   │   ├── Dashboard.tsx   # Statistics overview
│   │   ├── Pages.tsx       # Document list
│   │   ├── PageDetail.tsx  # Single document view
│   │   ├── Search.tsx      # Search with results
│   │   ├── Approvals.tsx   # Approval queue
│   │   └── Graph.tsx       # Knowledge graph visualization
│   ├── components/
│   │   └── Layout.tsx      # App shell (header, sidebar, content)
│   ├── api.ts              # Axios client (all API calls)
│   ├── App.tsx             # Router config + ConfigProvider
│   ├── main.tsx            # Entry point
│   └── vite-env.d.ts       # Vite type declarations
├── public/
├── index.html
├── vite.config.ts
├── tsconfig.json
├── package.json
└── AGENTS.md               # This file
```

## Where to Look

| Task | Path |
|------|------|
| Add/change page | `src/pages/{Name}.tsx` |
| Add/change layout | `src/components/Layout.tsx` |
| Add API endpoint call | `src/api.ts` |
| Add/change route | `src/App.tsx` |
| Modify build config | `vite.config.ts` |

## Conventions

- **Components:** Functional components + React hooks only (no classes)
- **UI library:** Ant Design components for all UI (Table, Form, Button, Layout, etc.)
- **Graph viz:** D3.js inside `Graph.tsx`, integrated via `useRef` + `useEffect`
- **API client:** Single `api.ts` with Axios instance. All pages import from here. No fetch calls elsewhere.
- **Proxy:** Vite dev server proxies `/api/*` to `localhost:8080` (defined in `vite.config.ts`)
- **Routing:** `react-router-dom` v6 with `BrowserRouter`, `Routes`, `Route`
- **Locale:** Ant Design `ConfigProvider` wraps the app with `zhCN` locale
- **Styling:** Ant Design theme tokens + CSS modules where needed. No global CSS files.

## Anti-Patterns

- Don't put API calls or Axios logic inside page components. Use `api.ts` as the single source of truth for all HTTP requests.
- Don't bypass Ant Design with raw HTML elements for UI primitives (buttons, tables, forms, modals). Stick to Ant Design components for consistency.
- Don't hardcode API base URLs (`http://localhost:8080`) anywhere. Vite proxy handles this in dev; production uses the same relative `/api/` path.
- Don't add global CSS or override Ant Design classes without using the theme token system.
- Don't use `any` types in API responses — define and export TypeScript interfaces from `api.ts`.
