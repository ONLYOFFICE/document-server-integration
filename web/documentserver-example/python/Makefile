.DEFAULT_GOAL := help

.PHONY: help
help: #          Show help message for each of the Makefile recipes.
	@grep -E "^[a-z-]+: #" $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ": # "}; {printf "%s: %s\n", $$1, $$2}'

.PHONY: dev
dev: #           Install development dependencies.
	@pip install --editable .[development]

.PHONY: prod
prod: #          Install production dependencies.
	@pip install .

.PHONY: server-dev
server-dev: #    Start the development server on localhost at $PORT (default: 8000).
	@python manage.py runserver

.PHONY: server-prod
server-prod: \
	export DEBUG := false
server-prod: #   Start the production server on 0.0.0.0 at $PORT (default: 8000).
	@python manage.py runserver

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
	@flake8
	@mypy .

.PHONY: test
test: #          Recursively run the tests.
	@python -m unittest ./src/**/*_tests.py
