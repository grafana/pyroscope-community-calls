COMPOSE := docker compose

.PHONY: build up up-local down clean logs ps load smoke

build:
	$(COMPOSE) build

up:
	$(COMPOSE) up -d --build

# Rehearsal mode: also starts a local Pyroscope on http://localhost:4040.
# Pair with `cp .env.local .env`.
up-local:
	$(COMPOSE) --profile local up -d --build

down:
	$(COMPOSE) --profile local --profile load down

clean:
	$(COMPOSE) --profile local --profile load down -v

logs:
	$(COMPOSE) logs -f --tail=100

ps:
	$(COMPOSE) ps

load:
	$(COMPOSE) run --rm k6 run /scripts/checkout-flow.js

smoke:
	@curl -sf -H 'X-Api-Key: bloom-demo-key' 'http://localhost:8080/shop/products?page=0&size=3' | head -c 400; echo
	@curl -sf -H 'X-Api-Key: bloom-demo-key' 'http://localhost:8080/shop/search?q=rose' | head -c 400; echo
	@curl -sf -H 'X-Api-Key: bloom-demo-key' -H 'Content-Type: application/json' \
		-d '{"customerId":"smoke-test","customerTier":"GOLD","items":[{"productId":1,"quantity":2},{"productId":2,"quantity":1}]}' \
		'http://localhost:8080/shop/checkout'; echo
