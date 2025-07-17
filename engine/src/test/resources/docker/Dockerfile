FROM docker.io/library/postgres:16-alpine

RUN apk add git build-base autoconf automake libtool pkgconf pcre2-dev
RUN git clone https://github.com/idsm-src/pgsparql.git
RUN cd pgsparql && autoreconf -i && CFLAGS="-O2 -Wall" ./configure && make install

COPY dbinit.sql /docker-entrypoint-initdb.d/dbinit.sql
COPY direct.sql /docker-entrypoint-initdb.d/direct.sql
COPY indirect.sql /docker-entrypoint-initdb.d/indirect.sql
