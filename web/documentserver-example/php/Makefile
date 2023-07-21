.DEFAULT_GOAL := help

.PHONY: help
help: #  Show help message for each of the Makefile recipes.
	@grep -E "^[a-z-]+: #" $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ": # "}; {printf "%s: %s\n", $$1, $$2}'

.PHONY: dev
dev: #   Install development dependencies.
	@composer install

.PHONY: prod
prod: #  Install production dependencies.
	@composer install --no-dev

.PHONY: lint
lint: #  Lint the source code for the style.
	@./vendor/bin/phpcs .

.PHONY: test
test: #  Run tests recursively.
	@./vendor/bin/phpunit --test-suffix "Tests.php" common
