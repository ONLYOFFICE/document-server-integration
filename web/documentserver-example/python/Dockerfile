FROM python:3.11.4-alpine3.18 AS example-base
WORKDIR /srv
COPY . .
RUN \
	apk update && \
	apk add --no-cache \
		libmagic \
		make

FROM example-base AS example-dev
RUN make dev
CMD ["make", "server-dev"]

FROM example-base AS example-prod
RUN make prod
CMD ["make", "server-prod"]

FROM nginx:1.23.4-alpine3.17 AS proxy
COPY proxy/nginx.conf /etc/nginx/nginx.conf
