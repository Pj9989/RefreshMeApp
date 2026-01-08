# RefreshMe

This repository contains the source code for the **RefreshMe** application. It is built using the Kotlin programming language and the Gradle build system.

## Getting Started

To build the project, make sure you have JDK 11 or later installed. Use the Gradle wrapper to build and test the project:

```bash
./gradlew build
```

This command will download dependencies and compile the source code.

## Important Note on Google Play Services

This project uses Google Play Services for features like Maps and authentication. To avoid a `DEVELOPER_ERROR` and ensure all features work correctly, please run the app on a device or emulator that has Google Play Services installed and is logged in with a Google account.

## Project Structure

- `build.gradle.kts`: Gradle build script written in Kotlin.
- `gradle.properties`: Configuration properties for the build.
- `settings.gradle.kts`: Defines the project structure and modules.
- `gradlew` and `gradlew.bat`: Scripts to run the Gradle wrapper on Unix/macOS and Windows, respectively.

## Contributing

Feel free to submit issues or pull requests. Please ensure that any code you contribute is well documented and tested.

## License

License details to be added.
