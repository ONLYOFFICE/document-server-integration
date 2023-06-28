FROM python:3.11.4-alpine3.18 as example
WORKDIR /srv
COPY . .
RUN \
	apk update && \
	apk add --no-cache \
		libmagic \
		make && \
	make prod
CMD ["make", "prod-server"]

FROM nginx:1.23.4-alpine3.17 as proxy
COPY proxy/nginx.conf /etc/nginx/nginx.conf
