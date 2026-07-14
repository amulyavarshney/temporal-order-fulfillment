# Temporal Order Fulfillment Application Makefile

.PHONY: help build clean test package worker api up down logs demo demo-approve java-version ui ui-build ui-preview

help: ## Show available targets
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  %-18s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

build: ## Compile all modules
	mvn -q -DskipTests compile

clean: ## Clean build artifacts
	mvn -q clean
	rm -rf .data ui/dist

test: ## Run unit and workflow tests
	mvn -q test

package: ## Package executable Spring Boot jars
	mvn -q -DskipTests package

worker: package ## Run the Temporal worker locally
	java -jar order-worker/target/order-worker-1.0.0.jar

api: package ## Run the REST API locally
	java -jar order-api/target/order-api-1.0.0.jar

ui: ## Run the sample UI (Vite dev server)
	cd ui && npm install && npm run dev

ui-build: ## Build the sample UI for GitHub Pages
	cd ui && npm ci && npm run build
	rm -rf docs && mkdir -p docs && cp -R ui/dist/. docs/ && cp docs/index.html docs/404.html && touch docs/.nojekyll

ui-preview: ui-build ## Preview the production UI build locally
	cd ui && npm run preview

up: ## Start full stack with Docker Compose
	docker compose up --build -d
	@echo "API:        http://localhost:8080"
	@echo "Worker:     http://localhost:8081/actuator/health"
	@echo "Temporal UI:http://localhost:8233"

down: ## Stop Docker Compose stack
	docker compose down

logs: ## Tail compose logs
	docker compose logs -f

demo: ## Submit a sample low-value order via API
	@curl -s -X POST http://localhost:8080/api/orders \
	  -H 'Content-Type: application/json' \
	  -d '{"order":{"items":[{"itemName":"Pima Cotton T-Shirt","itemPrice":49.99,"quantity":2}],"payment":{"creditCard":{"number":"4111111111111111","expiration":"12/28"}}}}' | tee /tmp/order-demo.json
	@echo
	@ORDER_ID=$$(python3 -c "import json; print(json.load(open('/tmp/order-demo.json'))['orderId'])"); \
	  echo "Polling $$ORDER_ID"; \
	  sleep 2; \
	  curl -s http://localhost:8080/api/orders/$$ORDER_ID; echo

demo-approve: ## Submit a high-value order and approve it
	@curl -s -X POST http://localhost:8080/api/orders \
	  -H 'Content-Type: application/json' \
	  -d '{"order":{"items":[{"itemName":"Wool Suit","itemPrice":599.99,"quantity":20}],"payment":{"creditCard":{"number":"4111111111111111","expiration":"12/28"}}}}' | tee /tmp/order-hi.json
	@echo
	@ORDER_ID=$$(python3 -c "import json; print(json.load(open('/tmp/order-hi.json'))['orderId'])"); \
	  sleep 1; \
	  curl -s -X POST http://localhost:8080/api/orders/$$ORDER_ID/approve; echo; \
	  sleep 2; \
	  curl -s http://localhost:8080/api/orders/$$ORDER_ID; echo

java-version: ## Show Java and Maven versions
	@java -version
	@mvn -version

verify: test ## Alias for test
