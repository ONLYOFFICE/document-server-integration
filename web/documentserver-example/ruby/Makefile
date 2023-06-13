.DEFAULT_GOAL := help

.PHONY: help
help: #         Show help message for each of the Makefile recipes.
	@grep -E "^[a-z-]+: #" $(MAKEFILE_LIST) | \
		sort --dictionary-order | \
		awk 'BEGIN {FS = ": # "}; {printf "%s: %s\n", $$1, $$2}'

.PHONY: dev
dev: #          Install development dependencies and initialize the project.
	@bundle install
	@rake app:update:bin
	@bundle exec tapioca init

.PHONY: dev-server
dev-server: #   Start the development server on localhost at $PORT (default: 3000).
	@rails server --using webrick

.PHONY: lint
lint: #         Lint the source code for style and check for types.
	@bundle exec rubocop
	@bundle exec srb tc

.PHONY: prod
prod: #         Install production dependencies.
	@bundle install --without development doc test
	@rake app:update:bin

.PHONY: prod-server
prod-server: #  Start the poruction server on 0.0.0.0 at $PORT (default: 3000).
	@rails server --environment production --using webrick

.PHONY: up
up: #           Build and up docker containers.
	@docker-compose build
	@docker-compose up -d
