# Codex Instructions (Project Root)

## Audit Logging (Required)
- Record all creates/updates/deletes in the `audit_log` collection.
- The audit payload must include target record id(s), action (create/update/delete), and resource.
- Include `actor_user_id` for all mutating requests.
- Do not rely on database triggers for auditing in this project.

## MongoDB (Required)
- Update schema definitions in `docs/db/mongodb_schema/*.js` and keep `docs/db/mongodb_schema.js` loading the full set.
- Use `ensureCollection` and `ensureIndex` helpers with strict validators and explicit enums.
- Keep identifiers as UUID strings and avoid mixed-type ids.
- Require `created_at` and `updated_at` unless a collection is explicitly immutable.
- Document schema changes in the most relevant split file; avoid duplicating shared fragments.
- Collection naming: use lowercase snake_case; avoid abbreviations unless widely standard.
- Index naming: prefix `uk_` for unique and `idx_` for non-unique; include key fields in the name.
- Prefer transactions for multi-collection writes (billing, inventory, ledger updates).
- Use `docs/mongodb-functions.md` for query patterns and report aggregations.

## MongoDB Quickstart (Local)
- Start local MongoDB: `docker-compose up -d` (container: `clinic_admin_mongo`, port `27017`).
- Load schema: `mongosh "mongodb://mongo:mongo@localhost:27017/clinic_admin?authSource=admin" docs/db/mongodb_schema.js`.
- Seed baseline RBAC + org: `ORG_ID=... ORG_NAME="Clinic Admin" ORG_CODE=CLINIC mongosh "mongodb://mongo:mongo@localhost:27017/clinic_admin?authSource=admin" docs/db/seed_permissions.js`.

## Discrete Mathematics Principles (No Compromise)
- Model domain concepts using sets, relations, and invariants.
- Encode invariants with explicit constraints (FKs, CHECKs, uniqueness) and validate preconditions in functions.
- Preserve referential integrity and avoid orphaned records.
- Use deterministic, well-defined state transitions and document them in function logic.
- When modeling rules, prefer explicit predicates over implicit assumptions.

## Operations Research Algorithms (Required)
- Define decision variables, constraints, and objective function (min/max) explicitly.
- Choose the algorithm class deliberately (graph, DP, greedy, LP/MILP, heuristic) and note complexity.
- Prefer exact solvers when feasible; if using heuristics, document parameters and stopping criteria.
- Use deterministic tie-breaking and avoid randomness unless seeded and justified.
- Add small correctness checks and edge-case tests that validate constraints and feasibility.
- Selection checklist: decision type (binary/integer/continuous), constraint structure (network/flow/assignment), size limits, and solver availability.
