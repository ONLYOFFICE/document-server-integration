install: # Install all the dependencies
	@composer install
	@npm install
	@npm run build
	@php artisan key:generate

compose-start: # Start docker services and install all the dependencies (e.g. php, nodejs)
	@docker compose build
	@docker compose up

compose-stop: # Stop docker services
	@docker compose down -v