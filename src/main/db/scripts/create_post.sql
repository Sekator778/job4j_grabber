create table if not exists post
(
    id      serial primary key,
    name    varchar(90)  not null unique,
    text    text         not null,
    link    varchar(256) not null,
    created timestamp    not null
);