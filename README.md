# SpeedtestApplication

## Project setup

### Patching submodules

Firstly, you should make sure that all submodules are cloned.

```
git submodule init
git submodule update
```

This project patches `swaggerApi` submodule, so to untrack changes there execute

```
echo "AUTOBUILD" >> ./.git/modules/swaggerApi/info/exclude
cd swaggerApi
find AUTOBUILD/javaBalancerClient/src/ -type f | xargs git update-index --skip-worktree
```

Finally, apply the patch

```
cd swaggerApi
git apply ../swaggerApi.patch
```

### Local configuration

Make sure that `local.properties` contains `sdk.dir` property set to your Android SDK absolute path.

## Build

You can build debug apk with Gradle by executing `./gradlew assembleDebug` on Linux and `gradlew assembleDebug` on Windows.
