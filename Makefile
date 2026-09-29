SWIFT_DIR = ../wikilayer-client-swift
TEST_RESOURCES = src/test/resources

.DEFAULT_GOAL := build

.PHONY: install-tools format comments lint test-build test docs build sync-yaml publish publish-local publish-check

install-tools:
	python3 -m pip install --quiet --upgrade git+https://github.com/botforge-pro/commentcensor.git

format:
	./gradlew ktlintFormat

comments:
	commentcensor .

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
