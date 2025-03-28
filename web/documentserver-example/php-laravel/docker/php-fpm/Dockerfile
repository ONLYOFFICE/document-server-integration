FROM php:8.2-fpm

COPY ./docker/php-fpm/conf.d/uploads.ini /usr/local/etc/php/conf.d/uploads.ini

RUN apt-get update && apt-get install -y \
    build-essential \
    git \
    curl \
    libpng-dev \
    libjpeg-dev \
    libfreetype6-dev \
    libonig-dev \
    libxml2-dev \
    libgd-dev \
    zip \
    unzip

RUN apt-get clean && rm -rf /var/lib/apt/lists/*

RUN docker-php-ext-configure gd --enable-gd --with-freetype --with-jpeg
RUN docker-php-ext-install pdo_mysql mbstring exif pcntl bcmath gd

COPY --from=composer:latest /usr/bin/composer /usr/bin/composer

WORKDIR /var/www

COPY . .

RUN chgrp -R www-data ./storage
RUN chmod -R 775 ./storage

RUN composer install
RUN php artisan key:generate