# SpeedtestApplication

## Project setup

### Local configuration

Make sure that `local.properties` contains `sdk.dir` property set to your Android SDK absolute path.

## Generated code changes

Any commits in [balancer api](./balancerApi) are overwritten by GitHub Actions swagger-codegen workflow.
If you need to apply some changes in generated code, please update the [patch](./.swagger-codegen-config/balancerApi.patch) file.

## Build

You can build debug apk with Gradle by executing `./gradlew assembleDebug` on Linux and `gradlew assembleDebug` on Windows.
