# Pluto
Maybe an optimised fork of Pufferfish. Mileage may vary per setup.

## Support
I don't provide support for this fork. This is mostly just so I can use it as a base for my private forks. If you want to use it, just make sure you test before uploading to production.

## API
You can find the javadocs [here](https://repo.yive.dev/javadoc/snapshots/dev/yive/pluto/pluto-api/1.19.4-R0.1-SNAPSHOT).

Maven:
```xml
<repository>
    <id>yive-repo</id>
    <url>https://repo.yive.dev/snapshots</url>
</repository>

<dependency>
    <groupId>dev.yive</groupId>
    <artifactId>pluto</artifactId>
    <version>1.20.1-R0.1-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```
Gradle:
```groovy
repositories {
    maven {
        url = 'https://repo.yive.dev/snapshots'
    }
}

dependencies {
    compileOnly 'dev.yive:pluto:1.20.1-R0.1-SNAPSHOT'
}
```

## Building
No downloads, you have to build it yourself.

```bash
./gradlew applyPatches
./gradlew createReobfPaperclipJar
```