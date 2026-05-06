alter table public.workflow
    add column if not exists disable_update_by_creator boolean default false;

