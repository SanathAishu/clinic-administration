# Branch and Agent Plan

Base branch: mongo-docs (all branches cut from here).

## Status
- Done: Agent A (domain-foundation), Agent B (security-rbac), Agent C (clinical-crud), Agent D (reporting), Agent E (tenant-admin-api), Agent F (reference-masterdata), Agent G (rbac-tests), Agent H (db-maintenance), Agent I (admin-ui), Agent J (api-docs).

## Work Notes
- Agent D (reporting): Reporting controller/service/DTOs added; report aggregation samples documented in `docs/mongodb-functions.md`.
- Agent E (tenant-admin-api): OrganizationController wired with tenant scoping; super-admin-only create/delete enforced; audit logging added.
- Agent F (reference-masterdata): Clinics/branches/departments module scaffolded (domain/dto/repo/service/controller); departments schema now requires `organization_id`.
- Agent G (rbac-tests): Added TenantGuard, PermissionService, and ReportingService validation tests (include_system filtering, role-permission cleanup).
- Agent H (db-maintenance): Added runbook for schema/backfill/cleanup scripts and linked it from docs index.
- Agent I (admin-ui): Completed admin UI planning artifacts in `clinic-administration/docs/ui/plan.md`.
- Agent J (api-docs): Added report response examples and revenue-analysis `group_by` note in `docs/api-endpoints.md`.

## Admin UI Tech Stack (React + TypeScript)
- Build: Vite + React 18 + TypeScript
- Routing: React Router
- Data: TanStack Query + Axios
- State: Zustand (auth + tenant context)
- Forms: React Hook Form + Zod
- Styling: Tailwind CSS + Radix UI + CSS variables; fonts: Space Grotesk (headings), IBM Plex Sans (body)
- Tests: Vitest + React Testing Library
- Planning rule: no UI code until the layout/component plan is approved (trees + graphs).

## Completed Wave (5 agents)

Agent D - feat/reporting [done]
- Goal: implement reporting endpoints with Mongo aggregation pipelines.
- Do: add reporting package + controller/service/DTOs; enforce tenant scope via TenantGuard; support filters from docs/api-endpoints.md.
- Done when: endpoints compile, sample pipelines documented, minimal tests added.

Agent E - feat/tenant-admin-api [done]
- Goal: tenant (organization) management for super admins + tenant admins.
- Do: add OrganizationController; allow super admin full CRUD; allow tenant admin update only for current org (TenantGuard).
- Done when: endpoints wired, RBAC annotations set, audit logging added.

Agent F - feat/reference-masterdata [done]
- Goal: clinics/branches/departments CRUD (tenant-scoped).
- Do: create domain/repo/service/controller; enforce org_id immutability; add schema validators + indexes; add RBAC guards.
- Done when: CRUD endpoints live + schema updated in docs/db/mongodb_schema/10_core.js.

Agent G - feat/rbac-tests [done]
- Goal: automated tests for tenant scoping + permission scope.
- Do: add Spring Boot tests for include_system filtering, super admin bypass, org_id mismatch, and role-permission cascade on deactivate.
- Done when: tests pass locally.

Agent J - feat/api-docs [done]
- Goal: API documentation completeness.
- Do: update docs/api-endpoints.md with org/clinic/branch/department + reporting endpoints and tenant-scope notes; document include_system usage.
- Done when: docs cover new endpoints and filters.

## Merge order
1) feat/domain-foundation [done]
2) feat/security-rbac [done]
3) feat/clinical-crud [done]
4) feat/reporting [done]
5) feat/tenant-admin-api + feat/reference-masterdata [done]
6) feat/rbac-tests + feat/api-docs [done]
7) feat/db-maintenance + feat/admin-ui [done]
