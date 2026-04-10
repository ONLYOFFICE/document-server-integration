FROM php:8.2.8-fpm-alpine3.18 AS example
WORKDIR /srv
COPY . .
COPY --from=composer:2 /usr/bin/composer /usr/local/bin/composer
RUN \
	chown -R www-data:www-data /srv && \
	apk update && \
	apk add --no-cache \
		make && \
	make prod
CMD ["make", "server-prod"]

FROM nginx:1.23.4-alpine3.17 AS proxy
COPY proxy/nginx.conf /etc/nginx/nginx.conf
