FROM python:3.12.0b1-alpine3.18 as example
WORKDIR /srv
COPY . .
RUN \
	apk update && \
	apk add --no-cache libmagic && \
	pip install Django==3.1.3 && \
	pip install requests==2.25.0 && \
	pip install pyjwt==2.6.0 && \
	pip install python-magic==0.4.27
CMD ["python", "manage.py", "runserver", "0.0.0.0:80"]

FROM nginx:1.23.4-alpine3.17 as proxy
COPY nginx.conf /etc/nginx/nginx.conf
