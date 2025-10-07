# GymRoutines Agent Guide

## Android debug build setup
1. Download the Android command-line tools into a fresh SDK directory:
   ```bash
   mkdir -p "$HOME/android-sdk" && cd "$HOME/android-sdk"
   curl -Lo commandlinetools-linux.zip \
     https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
   unzip -q commandlinetools-linux.zip -d cmdline-tools-temp
   mkdir -p cmdline-tools
   mv cmdline-tools-temp/cmdline-tools cmdline-tools/latest
   rm -rf cmdline-tools-temp commandlinetools-linux.zip
   ```
2. Point the environment at the new SDK location for the current shell:
   ```bash
   export ANDROID_SDK_ROOT="$HOME/android-sdk"
   export ANDROID_HOME="$ANDROID_SDK_ROOT"
   ```
3. Accept licenses and install the pieces Gradle expects:
   ```bash
   cmdline-tools/latest/bin/sdkmanager --sdk_root="$ANDROID_SDK_ROOT" --licenses
   cmdline-tools/latest/bin/sdkmanager --sdk_root="$ANDROID_SDK_ROOT" \
     "platform-tools" "platforms;android-34" "build-tools;34.0.0"
   ```
4. Create `local.properties` (if it does not exist) so Gradle can find the SDK:
   ```bash
   cat <<'PROP' > local.properties
   sdk.dir=/root/android-sdk
   PROP
   ```
5. Run the debug build as usual:
   ```bash
   ./gradlew assembleDebug --console=plain
   ```

These steps typically take 5–7 minutes end-to-end the first time and allow repeatable debug builds in the container.
