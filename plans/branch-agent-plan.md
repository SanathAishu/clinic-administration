# Branch and Agent Plan

Base branch: mongo-docs (all branches cut from here).

## Agent assignments

Agent A - feat/domain-foundation
- Scope: shared base model, auditing, id strategy, common enums/DTOs, core entities (organization, user, role, permission).
- Output: common package structure and repositories/services in server/src/main/java/com/clinic/mgmt/common and server/src/main/java/com/clinic/mgmt/identity.

Agent B - feat/security-rbac
- Scope: Spring Security setup, JWT auth, login/refresh endpoints, RBAC checks, user/role/permission CRUD endpoints.
- Output: security config + auth controllers/services + seed alignment.

Agent C - feat/clinical-crud
- Scope: CRUD for patients, appointments, visits, treatments/procedures (per requirements).
- Output: controllers/services/repos + validation per domain module under com.clinic.mgmt.<module>.

Agent D - feat/reporting
- Scope: reporting endpoints with aggregation pipelines, filters, DTOs.
- Output: reporting service + controller + tests + sample pipeline docs.

## Merge order
1) feat/domain-foundation (shared contracts)
2) feat/security-rbac (depends on identity models)
3) feat/clinical-crud (uses shared base + security)
4) feat/reporting (depends on domain models)
