.DEFAULT_GOAL := help

ADDRESS := $(ADDRESS)
PORT := $(PORT)

.PHONY: help
help: #          Show help message for each of the Makefile recipes.
	@grep -E "^[a-z-]+: #" $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ": # "}; {printf "%s: %s\n", $$1, $$2}'

.PHONY: dev
dev: #           Install development dependencies.
	@composer install

.PHONY: prod
prod: #          Install production dependencies.
	@composer install --no-dev

ifeq ($(ADDRESS),)
server-dev: \
	export ADDRESS := localhost
else
server-dev: \
	export ADDRESS := $(ADDRESS)
endif

ifeq ($(PORT),)
server-dev: \
	export PORT := 9000
else
server-dev: \
	export PORT := $(PORT)
endif

.PHONY: server-dev
server-dev: #    Start the development server on localhost at $PORT (default: 9000).
	@php --server $(ADDRESS):$(PORT)

ifeq ($(ADDRESS),)
server-prod: \
	export ADDRESS := 0.0.0.0
else
server-prod: \
	export ADDRESS := $(ADDRESS)
endif

ifeq ($(PORT),)
server-prod: \
	export PORT := 9000
else
server-prod: \
	export PORT := $(PORT)
endif

.PHONY: server-prod
server-prod: #   Start the production server on 0.0.0.0 at $PORT (default: 9000).
	@php-fpm --fpm-config php-fpm.conf

.PHONY: compose-prod
compose-prod: #  Up containers in a production environment.
	@docker-compose build
	@docker-compose up --detach

.PHONY: lint
lint: #          Lint the source code for the style.
	@./vendor/bin/phpcs src index.php

.PHONY: test
test: #          Run tests recursively.
	@./vendor/bin/phpunit \
		--test-suffix "Tests.php" \
		--display-incomplete \
		--display-deprecations \
		--display-errors \
		--display-notices \
		--display-warnings \
		src
