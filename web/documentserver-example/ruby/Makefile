.DEFAULT_GOAL := help

ifeq ($(OS),Windows_NT)
	SORBET_SUPPORTED := 0
else
	NAME := $(shell uname -s)
	ifeq ($(NAME),Darwin)
		SORBET_SUPPORTED := 1
	else
		ARCH := $(shell uname -p)
		ifeq ($(ARCH),x86_64)
			SORBET_SUPPORTED := 1
		else
			SORBET_SUPPORTED := 0
		endif
	endif
endif

.PHONY: help
help: #          Show help message for each of the Makefile recipes.
	@grep -E "^[a-z-]+: #" $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ": # "}; {printf "%s: %s\n", $$1, $$2}'

.PHONY: dev
dev: #           Install development dependencies and initialize the project.
	@bundle install
	@bundle exec rake app:update:bin
ifeq ($(SORBET_SUPPORTED),1)
	@bundle exec tapioca init
endif

.PHONY: prod
prod: #          Install production dependencies.
	@bundle install --without development doc test
	@bundle exec rake app:update:bin

.PHONY: server-dev
server-dev: #    Start the development server on localhost at $PORT (default: 3000).
	@bundle exec rails server

.PHONY: server-prod
server-prod: #   Start the poruction server on 0.0.0.0 at $PORT (default: 3000).
	@bundle exec rails server --environment production

.PHONY: compose-dev
compose-dev: #   Up containers in a development environment.
	@docker-compose \
		--file compose-base.yml \
		--file compose-dev.yml \
		build
	@docker-compose \
		--file compose-base.yml \
		--file compose-dev.yml \
		up --detach

.PHONY: compose-prod
compose-prod: #  Up containers in a production environment.
	@docker-compose \
		--file compose-base.yml \
		--file compose-prod.yml \
		build
	@docker-compose \
		--file compose-base.yml \
		--file compose-prod.yml \
		up --detach

.PHONY: lint
lint: #          Lint the source code for style and check for types.
	@bundle exec rubocop
ifeq ($(SORBET_SUPPORTED),1)
	@bundle exec srb tc
endif

.PHONY: test
test: #          Recursively run the tests.
	@bundle exec rake test
