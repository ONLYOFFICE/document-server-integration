.DEFAULT_GOAL := help

.PHONY: help
help: #  Show help message for each of the Makefile recipes.
	@grep -E "^[a-z]+: #" $(MAKEFILE_LIST) | \
		sort | \
		awk 'BEGIN {FS = ": # "}; {printf "%s: %s\n", $$1, $$2}'

.PHONY: dev
dev: #   Install development dependencies.
	@pip install --editable .[development]

.PHONY: lint
lint: #  Lint the source code for style and check for types.
	@flake8
	@mypy .

.PHONY: prod
prod: #  Install production dependencies.
	@pip install .
