install: # Install all the dependencies
	@composer install
	@npm run build

compose-start: # Start docker services and install all the dependencies (e.g. php, nodejs)
	@docker compose build
	@docker compose up -d
	@docker compose exec example composer install
	@docker compose exec node npm install
	@docker compose exec node npm run build

compose-stop: # Stop docker services
	@docker compose down