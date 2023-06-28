.DEFAULT_GOAL := help

.PHONY: help
help: #         Show help message for each of the Makefile recipes.
	@grep -E "^[a-z-]+: #" $(MAKEFILE_LIST) | \
		sort | \
		awk 'BEGIN {FS = ": # "}; {printf "%s: %s\n", $$1, $$2}'

.PHONY: dev
dev: #          Install development dependencies.
	@pip install --editable .[development]

.PHONY: dev-server
dev-server: \
	export DEBUG := true
dev-server: #   Start the development server on localhost at $PORT (default: 8000).
	@python manage.py runserver

.PHONY: lint
lint: #         Lint the source code for style and check for types.
	@pylint --recursive=y .
	@mypy .

.PHONY: prod
prod: #         Install production dependencies.
	@pip install .

.PHONY: prod-server
prod-server: #  Start the production server on 0.0.0.0 at $PORT (default: 8000).
	@python manage.py runserver

.PHONY: test
test: #         Recursively run the tests.
	@python -m unittest ./src/**/*_tests.py

.PHONY: up
up: #           Build and up docker containers.
	@docker-compose build
	@docker-compose up -d
