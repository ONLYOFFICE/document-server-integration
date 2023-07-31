FROM ruby:3.2.2-alpine3.18 AS example-base
WORKDIR /srv
COPY . .
RUN \
	apk update && \
	apk add --no-cache \
		build-base \
		gcompat \
		git \
		make \
		nodejs && \
	gem install bundler -v 2.3.7

FROM example-base AS example-dev
RUN make dev
CMD ["make", "server-dev"]

FROM example-base AS example-prod
RUN make prod
CMD ["make", "server-prod"]

FROM nginx:1.23.4-alpine3.17 AS proxy
COPY proxy/nginx.conf /etc/nginx/nginx.conf
