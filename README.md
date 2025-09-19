<div align="center">
  <h1>Pluto</h1>
  <h3>Possibly an optimised fork of Pufferfish</h3>
</div>

## Features
| Feature                                                          | Description                                                                                                                                                                                  |
|------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Fork of [Pufferfish](https://github.com/Yive/Tetraodontidae)** | Includes a bunch of performance patches.                                                                                                                                                     |
| **Decreased Memory Allocations**                                 | The JVM will have less pressure on the memory meaning less garbage collections, less time the CPU waits for memory & increased memory throughput                                             | 
| **Async Spawning Chunks**                                        | The natural mob spawner has gotten quite heavy in the latter versions of 1.21.x. Most of the logic is now calculated off the main thread. Mobs are still spawned via the main thread though. | 
| **Optimized Hoppers**                                            | Certain entities will search for hoppers allowing for a higher capacity of hoppers.                                                                                                          |
| **Optimisations for Farms**                                      | Certain farm related blocks can skip heavy tasks such as farmland searching for nearby water.                                                                                                |
| **Optimisations for Dropped Items**                              | Dropped items can be configured to no longer try to merge on every single tick.                                                                                                              |
| **Exploit Prevention**                                           | Configuration for preventing x-ray methods with certain blocks or lag machines with excessive minecarts/boats.                                                                               |
| **Full Spawner Configuration**                                   | Full configuration for mob spawners that would normally require using an API.                                                                                                                |
| **Tick Skipping**                                                | Whilst I don't recommend these, they unfortunately tend to be options in paid closed source forks. So now you've got access to some tick skipping methods for free.                          |
| **Optimised Bukkit APIs**                                        | Some areas of the Bukkit API have been optimised to avoid plugins causing performance issues.                                                                                                |

## Downloads
You can download pre-compiled paperclip jars [here](https://ci.yive.dev/job/Pluto/)

## API
You can find the javadocs [here](https://repo.yive.dev/javadoc/snapshots/dev/yive/pluto/pluto-api/1.21.8-R0.1-SNAPSHOT).

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
        <groupId>dev.yive.pluto</groupId>
        <artifactId>pluto-api</artifactId>
        <version>1.21.9-R0.1-SNAPSHOT</version>
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
    compileOnly 'dev.yive.pluto:pluto-api:1.21.9-R0.1-SNAPSHOT'
}
```
Paperweight + Gradle KTS:
```kts
repositories {
    maven("https://repo.yive.dev/snapshots")
}

dependencies {
    paperweight.devBundle("dev.yive.pluto", "1.21.9-R0.1-SNAPSHOT")
}
```

## Building

```bash
./gradlew applyPatches
./gradlew createMojmapPaperclipJar
```
