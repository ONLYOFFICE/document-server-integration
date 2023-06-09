.DEFAULT_GOAL := help

.PHONY: help
help: #     Show help message for each of the Makefile recipes.
	@grep -E "^[a-z]+: #" $(MAKEFILE_LIST) | \
		sort | \
		awk 'BEGIN {FS = ": # "}; {printf "%s: %s\n", $$1, $$2}'

.PHONY: install
install: #  Install dependencies and initialize the project.
	@bundle install
	@bundle exec tapioca init
