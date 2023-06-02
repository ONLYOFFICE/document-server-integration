FROM ruby:3.2.2-alpine3.18 as example
WORKDIR /srv
COPY . .
RUN \
	apk update && \
	apk add --no-cache \
		build-base \
		git \
		nodejs \
		sqlite-dev && \
	gem install bundler -v 2.3.7 && \
	bundle install
CMD ["rails", "server", "-b", "0.0.0.0", "-p", "80", "-u", "webrick"]

FROM nginx:1.23.4-alpine3.17 as proxy
COPY nginx.conf /etc/nginx/nginx.conf
