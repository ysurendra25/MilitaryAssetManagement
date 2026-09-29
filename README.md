# MAMS — Military Asset Management System

A role-based system for tracking military assets (weapons, vehicles, ammunition,
protective gear) across bases: purchases, transfers between bases, assignment to
personnel, expenditure, and a full audit trail of every transaction.

**Stack:** Spring Boot 3 (Java 21) · React 18 (Vite) · H2 relational database

---

## 1. Run it

Requires Java 21+ and Maven 3.9+.

```bash
./run.sh            # builds and starts on http://localhost:8080
```

The H2 database file is created and **seeded with deterministic demo data** on
first start (3 bases, 4 equipment types, ~100 transactions over 12 months).
Delete `backend/data/` to re-seed.

The React app is pre-built into the backend's static resources, so one service
serves everything. To rebuild the frontend (needs Node 18+):

```bash
cd frontend && npm install && npm run build
cp -r dist/* ../backend/src/main/resources/static/
```

For frontend development with hot reload: `npm run dev` (proxies `/api` to
`localhost:8080`).

### Login credentials

| Role | Username | Password | Scope |
|---|---|---|---|
| Admin | `admin` | `admin123` | Everything, all bases |
| Base Commander | `cmdr_alpha` | `commander123` | Base Alpha only |
| Base Commander | `cmdr_bravo` | `commander123` | Base Bravo only |
| Logistics Officer | `logistics01` | `logistics123` | Purchases & transfers only |

---

## 2. What it does

- **Dashboard** — Opening Balance, Closing Balance, Net Movement, Assigned and
  Expended, with filters for date range, base and equipment type. Clicking the
  Net Movement card opens a breakdown of Purchases / Transfers In / Transfers
  Out. Also shows a monthly net-movement chart and a per-equipment-type table.
- **Purchases** — record purchases (base, equipment, quantity, unit cost,
  supplier, date) and browse history with date/equipment filters.
- **Transfers** — move stock between bases with a stock check at the source;
  full timestamped transfer history.
- **Assignments & Expenditures** — assign assets to personnel (with return),
  and record expended stock with a mandatory reason.
- **Audit Log** — admin-only view of every transaction and every denied or
  failed attempt.

### Accounting model (the core design decision)

All balances are **derived from one ledger**: `opening_balances` plus every
purchase, transfer and expenditure ever recorded. Nothing is cached, so the
dashboard can never disagree with the history.

```
Net Movement = Purchases + Transfers In − Transfers Out        (the brief's formula)
Closing      = Opening + Net Movement − Expended
```

- An **assignment** moves stock to a person but keeps it on the base's books
  (an assigned weapon still belongs to the base). Only **expenditure** removes
  stock — which is why Closing subtracts Expended but not Assigned.
- For a single base, Transfers In/Out are that base's incoming/outgoing moves.
  Across **all bases** combined, internal transfers cancel out (every transfer
  out of one base is a transfer into another) — which is the accounting-honest
  answer.
- Opening balance for a period = stock at the day before the period starts,
  computed from the full history — so the numbers stay correct no matter what
  date range you filter on.

## 3. Architecture

```
mams/
├── backend/                        Spring Boot 3, Java 21
│   └── src/main/java/com/mams/
│       ├── config/                 AuthInterceptor (RBAC), WebConfig (CORS), SpaForwardFilter
│       ├── model/                  JPA entities: Base, EquipmentType, User,
│       │                           OpeningBalance, Purchase, Transfer,
│       │                           Assignment, Expenditure, AuditLog
│       ├── repository/             Spring Data JPA repositories
│       ├── service/                DashboardService (ledger math),
│       │                           TransactionService (validation + RBAC),
│       │                           AuditService, SeedRunner
│       ├── web/                    REST controllers (one per feature)
│       └── dto/                    Request/view records
├── frontend/                       React 18 + Vite SPA
│   └── src/pages/                  Login, Dashboard, Purchases, Transfers,
│                                   Assignments, AuditLog
├── tests/smoke_test.py             42 end-to-end checks
└── run.sh
```

**Why this stack:** Spring Boot gives mature, boring reliability for the
backend — JPA for the schema, an interceptor for auth, plain REST controllers.
React 18 with React Router gives a responsive SPA; the UI is deliberately plain
CSS (no UI framework) to keep the bundle small and the styling fully
controllable. H2 was chosen for the prototype: zero setup for reviewers, a
single file, real SQL (porting to PostgreSQL is a config change — the schema
and queries are standard). All reads go through view DTOs, and every write is
validated server-side.

## 4. RBAC

Enforced **server-side** in `AuthInterceptor` (roles) and `TransactionService`
(base scoping) — the frontend hides what a role can't use, but the server is
the authority:

| Endpoint group | Admin | Base Commander | Logistics Officer |
|---|---|---|---|
| Dashboard | ✔ | ✔ (own base only) | ✘ |
| Purchases | ✔ | ✔ (own base only) | ✔ |
| Transfers | ✔ | ✔ (own base involved) | ✔ |
| Assignments | ✔ | ✔ (own base only) | ✘ |
| Expenditures | ✔ | ✔ (own base only) | ✘ |
| Audit log | ✔ | ✘ | ✘ |

A commander who asks for another base's data gets their own base's numbers
back — the scope is forced server-side, not just hidden in the UI.

## 5. API logging

Every write (purchase, transfer, assignment, expenditure, return, login,
logout) is appended to `audit_logs`: timestamp, username, role, action,
details and status. **Denied attempts are logged too** — role denials (403)
and business-rule failures like insufficient stock (400). Audit writes use
`REQUIRES_NEW` transactions so audit rows survive even when the business
transaction they describe is rolled back.

## 6. API endpoints

```
POST   /api/auth/login              POST   /api/auth/logout
GET    /api/auth/me                 GET    /api/meta/bases
GET    /api/meta/equipment-types    GET    /api/dashboard?from&to&baseId&equipmentTypeId
GET    /api/purchases?from&to&baseId&equipmentTypeId
POST   /api/purchases
GET    /api/transfers?from&to&baseId&equipmentTypeId
POST   /api/transfers
GET    /api/assignments             POST   /api/assignments
POST   /api/assignments/{id}/return
GET    /api/expenditures           POST   /api/expenditures
GET    /api/audit?user&limit       (admin only)
```

## 7. Validation & edge cases

- Quantity must be positive; dates required; transfers need two different bases.
- Transfers and expenditures check stock at the source base first.
- Assignments check *unassigned* stock (stock − active assignments).
- Assignment must name personnel; expenditure must give a reason.
- Double-return of an assignment is refused.
- Bad credentials return 401 and are audited.

`tests/smoke_test.py` covers all of the above: **42/42 passing** from a fresh
database (auth, RBAC role rules, base scoping, dashboard math self-consistency,
validation, transactions, audit trail, filters).

## 8. Assumptions & limitations

- Transfers apply in one step (stock leaves and arrives together). A two-step
  dispatch → receive flow would be the next feature.
- Assignment dates within the same day are not ordered; stock checks count the
  day's earlier transactions conservatively.
- Costs are recorded in INR; the dashboard tracks quantities, not value.
- The seed generates demo data (Random(42), deterministic). At production scale
  the dashboard aggregation would move to SQL GROUP BY with indexes on the
  transaction date columns.
- Session-cookie auth (HttpSession + BCrypt password hashes) fits the
  prototype; production would add token rotation and HTTPS-only cookies.
