FROM node:buster
LABEL maintainer Ascensio System SIA <support@onlyoffice.com>

ENV LANG=en_US.UTF-8 \
    LANGUAGE=en_US:en \
    LC_ALL=en_US.UTF-8 \
    NODE_ENV=production-linux \
    NODE_CONFIG_DIR=/etc/onlyoffice/documentserver-example/

WORKDIR /var/www/onlyoffice/documentserver-example/
COPY . /var/www/onlyoffice/documentserver-example/

RUN groupadd --system --gid 1001 ds && \
    useradd --system -g ds --create-home --shell /sbin/nologin --uid 1001 ds && \
    chown -R ds:ds /var/www/onlyoffice/documentserver-example/ && \
    mkdir -p /var/lib/onlyoffice/documentserver-example/ && \
    chown -R ds:ds /var/lib/onlyoffice/ && \
    mv files /var/lib/onlyoffice/documentserver-example/ && \
    mkdir -p /etc/onlyoffice/documentserver-example/ && \
    chown -R ds:ds /etc/onlyoffice/ && \
    mv config/* /etc/onlyoffice/documentserver-example/ && \
    npm install

EXPOSE 3000

USER ds

ENTRYPOINT /var/www/onlyoffice/documentserver-example/docker-entrypoint.sh npm start
