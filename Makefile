COMMENTCENSOR_VERSION ?= v0.3.2
COMMENTCENSOR_ENV = build/commentcensor
COMMENTCENSOR = $(COMMENTCENSOR_ENV)/bin/commentcensor

SWIFT_DIR = ../wikilayer-client-swift
TEST_RESOURCES = src/test/resources

.DEFAULT_GOAL := build

.PHONY: install format comments lint test-build test docs build sync-yaml

install:
	python3 -m venv $(COMMENTCENSOR_ENV)
	$(COMMENTCENSOR_ENV)/bin/pip install --quiet --upgrade git+https://github.com/botforge-pro/commentcensor.git@$(COMMENTCENSOR_VERSION)

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

sync-yaml:
	mkdir -p $(TEST_RESOURCES)
	cp $(SWIFT_DIR)/Tests/WikilayerClientTests/Resources/*.yaml $(TEST_RESOURCES)/
