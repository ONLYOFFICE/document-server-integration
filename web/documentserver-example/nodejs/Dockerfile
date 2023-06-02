FROM node:20.2.0-alpine3.17 as example
WORKDIR /srv
COPY . .
RUN npm install
CMD ["node", "bin/www"]

FROM nginx:1.23.4-alpine3.17 as proxy
COPY nginx.conf /etc/nginx/nginx.conf
