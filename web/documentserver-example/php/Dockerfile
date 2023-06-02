FROM php:8.1.19-fpm-alpine3.16 as example
WORKDIR /srv
COPY . .
RUN \
	chown -R nobody:nobody . && \
	curl -sS https://getcomposer.org/installer | php -- \
		--install-dir=/usr/bin \
		--filename=composer && \
	composer install
USER nobody
CMD ["php-fpm"]

FROM nginx:1.23.4-alpine3.17 as proxy
COPY nginx.conf /etc/nginx/nginx.conf
