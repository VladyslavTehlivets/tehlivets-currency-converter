create table members (
    id uuid primary key,
    verified_email text not null,
    created_at timestamptz not null
);

create index members_by_verified_email on members (verified_email);

create table provider_identities (
    provider text not null,
    subject text not null,
    member_id uuid not null references members (id) on delete cascade,
    verified_email text not null,
    linked_at timestamptz not null,
    primary key (provider, subject),
    unique (member_id, provider)
);

create table sessions (
    id uuid primary key,
    member_id uuid not null references members (id) on delete cascade,
    issued_at timestamptz not null,
    expires_at timestamptz not null
);

create table session_nonces (
    nonce text primary key,
    expires_at timestamptz not null
);
