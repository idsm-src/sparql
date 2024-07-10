FROM docker.io/library/postgres:16-alpine
COPY dbinit.sql /docker-entrypoint-initdb.d/dbinit.sql

RUN apk add git build-base autoconf automake libtool pkgconf pcre2-dev
RUN git clone https://github.com/idsm-src/pgsparql.git
RUN cd pgsparql && autoreconf -i && CFLAGS="-O2 -Wall" ./configure && make install
