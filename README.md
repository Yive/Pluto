# Pluto
Maybe an optimised fork of Pufferfish. Mileage may vary per setup.

## Support
I don't intend to provide much support for this project. I'll fix any bugs relating to my patches and maybe add certain patch requests, but this is mostly just so I can use it as a base for my private forks. If you want to use it, just make sure you test before uploading to production.

## Downloads
You can download pre-compiled paperclip jars [here](https://ci.yive.dev/job/Pluto/job/1.20.2/)

## API
You can find the javadocs [here](https://repo.yive.dev/javadoc/snapshots/dev/yive/pluto/pluto-api/1.20.2-R0.1-SNAPSHOT).

Maven:
```xml
<repositories>
    <repository>
        <id>yive-repo</id>
        <url>https://repo.yive.dev/snapshots</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>dev.yive</groupId>
        <artifactId>pluto</artifactId>
        <version>1.20.2-R0.1-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```
Gradle:
```groovy
repositories {
    maven {
        url = 'https://repo.yive.dev/snapshots'
    }
}

dependencies {
    compileOnly 'dev.yive:pluto:1.20.2-R0.1-SNAPSHOT'
}
```

## Building

```bash
./gradlew applyPatches
./gradlew createReobfPaperclipJar
```