COMPOSE := docker compose

.PHONY: build up down clean logs ps smoke

build:
	$(COMPOSE) build

# Brings up everything: Bloom, Postgres, the k6 load generator, Pyroscope,
# Tempo and Grafana. Grafana is on http://localhost:3000 (admin/admin).
up:
	$(COMPOSE) up -d --build

down:
	$(COMPOSE) down

clean:
	$(COMPOSE) down -v

logs:
	$(COMPOSE) logs -f --tail=100

ps:
	$(COMPOSE) ps

smoke:
	@curl -sf -H 'X-Api-Key: bloom-demo-key' 'http://localhost:8080/shop/products?page=0&size=3' | head -c 400; echo
	@curl -sf -H 'X-Api-Key: bloom-demo-key' 'http://localhost:8080/shop/search?q=rose' | head -c 400; echo
	@curl -sf -H 'X-Api-Key: bloom-demo-key' -H 'Content-Type: application/json' \
		-d '{"customerId":"smoke-test","customerTier":"GOLD","items":[{"productId":1,"quantity":2},{"productId":2,"quantity":1}]}' \
		'http://localhost:8080/shop/checkout'; echo
