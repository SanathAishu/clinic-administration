# Admin UI Planning Template (Discrete Math)

No UI code until this plan is reviewed and approved.

## 1) Overview
- Purpose: admin console for tenant setup, reference data, and RBAC.
- Actors: Super Admin (platform), Tenant Admin, Tenant Staff (read-only, optional).
- Tenancy rule: super admin can select org_id; tenant admins are locked to JWT org_id.
- Permission rule: tenant permission listings exclude system scope unless include_system=true for super admins.
- Role rule: role organization_id is immutable.
- Data boundaries: all list/detail views respect org_id from JWT unless system.super_admin.

## 2) Route Tree (Tree)
Define the route hierarchy. Each node is a route; edges represent nesting.

- /
  - /login
  - /app
    - /app/dashboard
    - /app/organizations
      - /app/organizations/:id
    - /app/organization
    - /app/clinics
      - /app/clinics/:id
    - /app/branches
      - /app/branches/:id
    - /app/departments
      - /app/departments/:id
    - /app/lookup-values
      - /app/lookup-values/:id
    - /app/roles
      - /app/roles/:id
      - /app/roles/:id/permissions
    - /app/permissions
      - /app/permissions/:id
    - /app/users
      - /app/users/:id
      - /app/users/:id/roles
      - /app/users/:id/permissions
    - /app/audit
    - /app/profile

## 3) Navigation Graph (Directed Graph)
Nodes are screens. Edges are allowed transitions. Label edges with permissions or roles.

Adjacency list format:
- login -> dashboard (auth)
- dashboard -> organizations_list (system.super_admin, organizations.read)
- dashboard -> organization_self (organizations.read)
- organizations_list -> organization_detail (organizations.read)
- organization_detail -> clinics_list (clinics.read)
- organization_self -> clinics_list (clinics.read)
- dashboard -> clinics_list (clinics.read)
- clinics_list -> clinic_detail (clinics.read)
- clinic_detail -> branches_list (branches.read)
- clinic_detail -> departments_list (departments.read)
- branches_list -> branch_detail (branches.read)
- departments_list -> department_detail (departments.read)
- dashboard -> lookup_list (lookup_values.read)
- lookup_list -> lookup_detail (lookup_values.read)
- dashboard -> roles_list (roles.read)
- roles_list -> role_detail (roles.read)
- role_detail -> role_permissions (roles.update)
- dashboard -> permissions_list (permissions.read)
- permissions_list -> permission_detail (permissions.read)
- dashboard -> users_list (users.read)
- users_list -> user_detail (users.read)
- user_detail -> user_roles (users.assign_roles)
- user_detail -> user_permissions (users.read)
- dashboard -> audit_log (audit_log.read)
- dashboard -> profile (auth)

## 4) Component Trees (Trees)
For each screen, define a component tree (root layout -> sections -> widgets).

Screen: Login
- AuthLayout
  - BrandPanel
  - LoginForm
  - SupportLinks

Screen: Dashboard
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - KPIGrid
    - QuickActions
    - RecentActivity

Screen: Organizations List
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - FilterBar
    - OrganizationsTable
    - CreateOrganizationModal
    - Pagination

Screen: Organization Detail
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - OrganizationSummaryCard
    - OrganizationForm
    - DeactivateOrganizationPanel
    - LinkedClinicsPanel

Screen: Organization (Tenant Admin)
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - OrganizationSummaryCard
    - OrganizationForm

Screen: Clinics List
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - FilterBar
    - ClinicsTable
    - CreateClinicModal
    - Pagination

Screen: Clinic Detail
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - ClinicForm
    - BranchesInlineList
    - DepartmentsInlineList
    - DeactivateClinicPanel

Screen: Branches List
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - FilterBar
    - BranchesTable
    - CreateBranchModal
    - Pagination

Screen: Branch Detail
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - BranchForm
    - DeactivateBranchPanel

Screen: Departments List
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - FilterBar
    - DepartmentsTable
    - CreateDepartmentModal
    - Pagination

Screen: Department Detail
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - DepartmentForm
    - DeactivateDepartmentPanel

Screen: Lookup Values List
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - FilterBar
    - LookupValuesTable
    - CreateLookupValueModal
    - Pagination

Screen: Lookup Value Detail
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - LookupValueForm
    - DeactivateLookupValuePanel

Screen: Roles List
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - RolesTable
    - CreateRoleModal
    - Pagination

Screen: Role Detail
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - RoleSummaryCard
    - Tabs
      - DetailsTab
        - RoleForm
      - PermissionsTab
        - PermissionPicker
        - AssignedPermissionsTable
        - SavePermissionsBar

Screen: Role Permissions
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - PermissionPicker
    - AssignedPermissionsTable
    - SavePermissionsBar

Screen: Permissions List
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - FilterBar
    - IncludeSystemToggle
    - PermissionsTable
    - CreatePermissionModal
    - Pagination

Screen: Permission Detail
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - PermissionForm
    - DeactivatePermissionPanel

Screen: Users List
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - FilterBar
    - UsersTable
    - CreateUserModal
    - Pagination

Screen: User Detail
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - UserSummaryCard
    - Tabs
      - ProfileTab
        - UserForm
      - RolesTab
        - RoleAssignmentTable
      - PermissionsTab
        - EffectivePermissionsTable
      - StatusTab
        - StatusChangePanel

Screen: User Roles
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - RoleAssignmentTable
    - SaveRolesBar

Screen: User Permissions
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - EffectivePermissionsTable

Screen: Audit Log
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - FilterBar
    - AuditTable
    - ExportButton

Screen: Profile
- AppShell
  - TopBar
  - Sidebar
  - Content
    - PageHeader
    - ProfileForm
    - ChangePasswordForm
    - SessionTokensPanel

## 5) Data Flow Graph (Directed Graph)
Nodes: API endpoints, query cache, stores, components.
Edges: data read/write or mutation.

Example nodes:
- API: POST /api/v1/auth/login
- API: GET /api/v1/organizations
- Cache: organizationsQuery
- Store: authStore, tenantContext
- UI: OrganizationsTable

Example edges:
- POST /api/v1/auth/login -> authStore
- authStore -> tenantContext
- GET /api/v1/organizations -> organizationsQuery
- organizationsQuery -> OrganizationsTable
- POST /api/v1/organizations -> organizationsQuery (invalidate)

Full data flow graph:
- POST /api/v1/auth/login -> authStore (token, user, org_id)
- POST /api/v1/auth/refresh-token -> authStore (token refresh)
- GET /api/v1/auth/me -> authStore (user, permissions)
- POST /api/v1/auth/logout -> authStore (clear)
- authStore -> tenantContext (default org_id)
- tenantContext -> list filters (organization_id for super admin)
- GET /api/v1/organizations -> organizationsQuery -> OrganizationsTable
- POST/PUT/DELETE /api/v1/organizations -> organizationsQuery (invalidate)
- GET /api/v1/clinics -> clinicsQuery -> ClinicsTable
- POST/PUT/DELETE /api/v1/clinics -> clinicsQuery (invalidate)
- GET /api/v1/branches -> branchesQuery -> BranchesTable
- POST/PUT/DELETE /api/v1/branches -> branchesQuery (invalidate)
- GET /api/v1/departments -> departmentsQuery -> DepartmentsTable
- POST/PUT/DELETE /api/v1/departments -> departmentsQuery (invalidate)
- GET /api/v1/lookup-values -> lookupValuesQuery -> LookupValuesTable
- POST/PUT/DELETE /api/v1/lookup-values -> lookupValuesQuery (invalidate)
- GET /api/v1/roles -> rolesQuery -> RolesTable
- GET /api/v1/roles/{id}/permissions -> rolePermissionsQuery -> AssignedPermissionsTable
- PUT/POST/DELETE /api/v1/roles/{id}/permissions -> rolePermissionsQuery (invalidate)
- GET /api/v1/permissions -> permissionsQuery -> PermissionsTable
- POST/PUT/DELETE /api/v1/permissions -> permissionsQuery (invalidate)
- GET /api/v1/users -> usersQuery -> UsersTable
- GET /api/v1/users/{id}/permissions -> userPermissionsQuery -> EffectivePermissionsTable
- PUT /api/v1/users/{id}/roles -> usersQuery (invalidate)
- POST /api/v1/users/{id}/status -> usersQuery (invalidate)
- GET /api/v1/audit -> auditQuery -> AuditTable
- POST /api/v1/auth/change-password -> ProfileForm (success toast, optional re-login)

## 6) Permission Gating Graph (Bipartite Graph)
Left nodes: permissions. Right nodes: routes/components.
Edge exists if a permission is required to view or use a route/component.

- system.super_admin -> /app/organizations
- system.super_admin -> IncludeSystemToggle (permissions list)
- organizations.read -> /app/organizations
- organizations.read -> /app/organizations/:id
- organizations.read -> /app/organization
- organizations.create -> CreateOrganizationModal
- organizations.update -> OrganizationForm
- organizations.delete -> DeactivateOrganizationPanel
- clinics.read -> /app/clinics
- clinics.read -> /app/clinics/:id
- clinics.create -> CreateClinicModal
- clinics.update -> ClinicForm
- clinics.delete -> DeactivateClinicPanel
- branches.read -> /app/branches
- branches.read -> /app/branches/:id
- branches.create -> CreateBranchModal
- branches.update -> BranchForm
- branches.delete -> DeactivateBranchPanel
- departments.read -> /app/departments
- departments.read -> /app/departments/:id
- departments.create -> CreateDepartmentModal
- departments.update -> DepartmentForm
- departments.delete -> DeactivateDepartmentPanel
- lookup_values.read -> /app/lookup-values
- lookup_values.create -> CreateLookupValueModal
- lookup_values.update -> LookupValueForm
- lookup_values.delete -> DeactivateLookupValuePanel
- roles.read -> /app/roles
- roles.read -> /app/roles/:id
- roles.create -> CreateRoleModal
- roles.update -> RoleForm
- roles.update -> RolePermissionsTab
- roles.delete -> RoleDeleteAction
- permissions.read -> /app/permissions
- permissions.read -> /app/permissions/:id
- permissions.create -> CreatePermissionModal
- permissions.update -> PermissionForm
- permissions.delete -> DeactivatePermissionPanel
- users.read -> /app/users
- users.read -> /app/users/:id
- users.update -> UserForm
- users.create -> CreateUserModal
- users.assign_roles -> RolesTab
- users.status -> StatusChangePanel
- audit_log.read -> /app/audit
- authenticated -> /app/profile

## 7) Screen Catalog (Tables)
List each screen with:
- Route
- Actor(s)
- Required permissions
- Primary actions
- Data sources

| Route | Actors | Required permissions | Primary actions | Data sources |
| --- | --- | --- | --- | --- |
| /login | All | none | sign in | POST /api/v1/auth/login |
| /app/dashboard | Super Admin, Tenant Admin | authenticated | navigate to modules | GET /api/v1/auth/me |
| /app/organizations | Super Admin | organizations.read, system.super_admin | create, filter, deactivate | GET/POST /api/v1/organizations |
| /app/organizations/:id | Super Admin | organizations.read | update, deactivate | GET/PUT /api/v1/organizations/{id} |
| /app/organization | Tenant Admin | organizations.read | update own org | GET/PUT /api/v1/organizations/{id} |
| /app/clinics | Super Admin, Tenant Admin | clinics.read | create, filter, deactivate | GET/POST /api/v1/clinics |
| /app/clinics/:id | Super Admin, Tenant Admin | clinics.read | update, deactivate | GET/PUT /api/v1/clinics/{id} |
| /app/branches | Super Admin, Tenant Admin | branches.read | create, filter, deactivate | GET/POST /api/v1/branches |
| /app/branches/:id | Super Admin, Tenant Admin | branches.read | update, deactivate | GET/PUT /api/v1/branches/{id} |
| /app/departments | Super Admin, Tenant Admin | departments.read | create, filter, deactivate | GET/POST /api/v1/departments |
| /app/departments/:id | Super Admin, Tenant Admin | departments.read | update, deactivate | GET/PUT /api/v1/departments/{id} |
| /app/lookup-values | Super Admin, Tenant Admin | lookup_values.read | create, filter, deactivate | GET/POST /api/v1/lookup-values |
| /app/lookup-values/:id | Super Admin, Tenant Admin | lookup_values.read | update, deactivate | GET/PUT /api/v1/lookup-values/{id} |
| /app/roles | Super Admin, Tenant Admin | roles.read | create, filter | GET/POST /api/v1/roles |
| /app/roles/:id | Super Admin, Tenant Admin | roles.read | update role, edit permissions | GET/PUT /api/v1/roles/{id} |
| /app/roles/:id/permissions | Super Admin, Tenant Admin | roles.update | add/remove permissions | GET/PUT /api/v1/roles/{id}/permissions |
| /app/permissions | Super Admin, Tenant Admin | permissions.read | create, filter, include_system (super admin) | GET/POST /api/v1/permissions |
| /app/permissions/:id | Super Admin, Tenant Admin | permissions.read | update, deactivate | GET/PUT /api/v1/permissions/{id} |
| /app/users | Super Admin, Tenant Admin | users.read | create, filter | GET/POST /api/v1/users |
| /app/users/:id | Super Admin, Tenant Admin | users.read | update profile, status | GET/PUT /api/v1/users/{id} |
| /app/users/:id/roles | Super Admin, Tenant Admin | users.assign_roles | assign roles | PUT /api/v1/users/{id}/roles |
| /app/users/:id/permissions | Super Admin, Tenant Admin | users.read | view effective permissions | GET /api/v1/users/{id}/permissions |
| /app/audit | Super Admin, Tenant Admin | audit_log.read | filter, export | GET /api/v1/audit |
| /app/profile | All authenticated | authenticated | update profile, change password | POST /api/v1/auth/change-password |

## 8) Invariants (Must Hold)
- Every route is covered by a component tree.
- Every action has a permission edge.
- Tenant admins never see system-scope permissions.
- Super admin views show include_system=true for permissions list.
- Role organization_id never changes via UI.

## 9) Open Questions
- Should tenant admins use /app/organization or reuse /app/organizations/:id with a lock?
- Should audit log be visible to tenant admins or super admin only?
- Do we need bulk import for clinics/branches/departments?
- Should lookup values be global or scoped per organization?

## 10) Approval Checklist
- Route tree complete
- Navigation graph complete
- Component trees complete
- Data flow graph complete
- Permission gating graph complete
