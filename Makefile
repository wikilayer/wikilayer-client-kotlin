.DEFAULT_GOAL := build

.PHONY: format lint test-build test docs build

format:
	./gradlew ktlintFormat

lint:
	./gradlew ktlintCheck detekt

test-build:
	./gradlew compileDebugUnitTestKotlin

test:
	./gradlew testDebugUnitTest

docs:
	./gradlew dokkaGeneratePublicationHtml

build: lint test docs
	./gradlew assembleRelease
