create table worksheets (
    id uuid primary key,
    key_hash bytea unique not null,
    created_at timestamptz not null default now()
);

create table dated_rates (
    id uuid primary key,
    worksheet_id uuid not null references worksheets (id) on delete cascade,
    source_currency char(3) not null,
    target_currency char(3) not null,
    rate numeric not null,
    rated_at timestamptz not null,
    unique (worksheet_id, source_currency, target_currency, rated_at)
);

create index dated_rates_by_direction on dated_rates (worksheet_id, source_currency, target_currency);
