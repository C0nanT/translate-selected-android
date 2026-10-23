.PHONY: test hooks android android-apk

ANDROID_HOME ?= $(HOME)/Android/Sdk
ADB := $(ANDROID_HOME)/platform-tools/adb
APK := app/build/outputs/apk/debug/app-debug.apk

# unit tests (Gradle, no device): core (pure JVM) + app
test:
	ANDROID_HOME=$(ANDROID_HOME) ./gradlew :core:test :app:testDebugUnitTest

# enables the pre-push hook versioned in hooks/ (once per clone; safe to repeat)
hooks:
	git config core.hooksPath hooks
	chmod +x hooks/pre-push

# builds the debug APK and installs it on the tablet (USB, USB debugging on)
android: android-apk
	$(ADB) install -r $(APK)

# only builds the APK (lands in $(APK))
android-apk:
	ANDROID_HOME=$(ANDROID_HOME) ./gradlew assembleDebug
