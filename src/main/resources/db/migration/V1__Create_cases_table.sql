create table cases(
    case_id uuid primary key,
    tenant_id uuid not null,
    creator_id uuid not null,
    title text not null,
    created_at timestamptz not null
);

create index on cases (tenant_id, created_at);
