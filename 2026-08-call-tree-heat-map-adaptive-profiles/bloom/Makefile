COMPOSE := docker compose

# Adaptive Profiles spots a new version through the service_git_ref label, so it
# tracks the checkout: committing (or just editing) makes the next `up` a
# genuine new version rather than a hand-typed one. Override to force a value.
GIT_SHA := $(shell git rev-parse --short HEAD 2>/dev/null || echo unknown)
GIT_DIRTY := $(shell git rev-parse HEAD >/dev/null 2>&1 && { git diff --quiet HEAD 2>/dev/null || printf -- -dirty; })
SERVICE_GIT_REF ?= $(GIT_SHA)$(GIT_DIRTY)
export SERVICE_GIT_REF

# Limit any `up` to one service, e.g. `make up-cloud SERVICE=pricing`.
SERVICE ?=

.PHONY: build up up-cloud down clean logs ps smoke version

build:
	$(COMPOSE) build

# Brings up everything: Bloom, Postgres, the k6 load generator, Pyroscope,
# Tempo and Grafana, all local. Grafana is on http://localhost:3000.
up:
	$(COMPOSE) up -d --build $(SERVICE)

# Same stack, but the apps ship profiles and traces to the Grafana Cloud stack
# configured in .env.cloud.
up-cloud:
	$(COMPOSE) --env-file .env.cloud up -d --build $(SERVICE)

# What the next `up` will tag profiles with.
version:
	@echo $(SERVICE_GIT_REF)

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
