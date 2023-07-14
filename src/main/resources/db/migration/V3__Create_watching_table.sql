create table watching(
    case_id uuid not null,
    user_id uuid not null,
    created_at timestamptz not null,
    primary key (case_id, user_id)
);
