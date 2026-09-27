COMMENTCENSOR_REF ?= 48d702a6ba4ace9af0bf996fad2fff9a012f25f9
COMMENTCENSOR_ENV = build/commentcensor
COMMENTCENSOR = $(COMMENTCENSOR_ENV)/bin/commentcensor

SWIFT_DIR = ../wikilayer-client-swift
TEST_RESOURCES = src/test/resources

.DEFAULT_GOAL := build

.PHONY: install format comments lint test-build test docs build sync-yaml publish publish-local publish-check

install:
	python3 -m venv $(COMMENTCENSOR_ENV)
	$(COMMENTCENSOR_ENV)/bin/pip install --quiet --upgrade git+https://github.com/botforge-pro/commentcensor.git@$(COMMENTCENSOR_REF)

format:
	./gradlew ktlintFormat

comments:
	$(COMMENTCENSOR) .

lint: comments
	./gradlew ktlintCheck detekt

test-build:
	./gradlew compileDebugUnitTestKotlin

test:
	./gradlew testDebugUnitTest

docs:
	./gradlew dokkaGeneratePublicationHtml

build: lint test-build test docs
	./gradlew assembleRelease

publish:
	@test -n "$(CI)" || { echo "publish runs in the release workflow, not locally" >&2; exit 1; }
	./gradlew publishAndReleaseToMavenCentral

publish-local:
	./gradlew publishToMavenLocal -PunsignedLocalPublish

publish-check:
	./gradlew publishToMavenLocal

sync-yaml:
	mkdir -p $(TEST_RESOURCES)
	cp $(SWIFT_DIR)/Tests/WikilayerClientTests/Resources/*.yaml $(TEST_RESOURCES)/
