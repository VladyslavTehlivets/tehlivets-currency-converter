create table members (
    id uuid primary key,
    verified_email text not null unique,
    created_at timestamptz not null default now()
);

create table provider_identities (
    provider text not null,
    subject text not null,
    member_id uuid not null references members (id) on delete cascade,
    linked_at timestamptz not null default now(),
    primary key (provider, subject),
    unique (member_id, provider)
);

create table sessions (
    id uuid primary key,
    member_id uuid not null references members (id) on delete cascade,
    issued_at timestamptz not null,
    expires_at timestamptz not null
);

create table sign_in_nonces (
    id uuid primary key,
    nonce text not null unique,
    expires_at timestamptz not null,
    used_at timestamptz
);
