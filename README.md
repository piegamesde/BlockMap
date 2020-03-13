# BlockMap – A Minecraft 1.13-1.15 world viewer

[![Build Status](https://saibotk.de/buildstatus.php)]()

<img src="BlockMap-standalone/src/main/resources/de/piegames/blockmap/guistandalone/icon.png" width=150/>

*This started as fork of [TMCMR](https://github.com/TOGoS/TMCMR), but has been almost completely rewritten due to the update. If you want something stable that works with 1.12 and before worlds, go check it out.*

## Features:
- Really fast (>700 chunks/s on my system)
- Works with huge worlds
- Works on servers
- Rendering scale: 1 pixel : 1 block
- An interactive GUI viewer made with JavaFX
- Pins on the map show additional information like players and villages
- Different color maps and shaders that highlight exactly what you are looking for (including an underground caves and an ocean ground view)
- A command line interface to render your worlds from scripts
- The core rendering code as library to use in your own projects (releasing soon™)
- Gamma corrected rendering
- Works with both 1.13+ worlds (currently: up to 1.15)

## Gallery

![Four rendered region files, each with one with a different color map](screenshots/screenshot-1.png "All existing color maps")
![Four rendered region files, each one with a different shader](screenshots/screenshot-2.png "All existing shaders")
![Screenshot from the GUI](screenshots/screenshot-3.png "Screenshot from the GUI")
![Screenshot showing different Minecraft structures as pins on the map](screenshots/screenshot-4.png "Wohoo! Pins!")
![Gif of the GUI zooming out in a large world](screenshots/screenshot-0.gif "Works with very large worlds")

## Requirements:

- Java 12+
- Minecraft 1.13+ worlds. Chunks from before the release (even from 1.13 snapshots) will be ignored. Please optimize your worlds in Minecraft before rendering them

## Get it:

Download the latest version from the [Release page](https://github.com/piegamesde/BlockMap/releases).

## Use it (GUI):

The GUI version should just run by (double)clicking it. Otherwise run it through:

    java -jar BlockMap-gui-1.6.1.jar

to start.

### GUI controls:

- Mouse wheel to zoom in and out
- Drag with the right mouse button to pan the view
  - This will very likely change in the future
  - If you drag to the edge, the mouse will wrap around so you can drag indefinitely. Blender users will appreciate this
- When loading a world, you can select either a world folder, a region folder or a single region file
- You can only load remote worlds (from servers) if the server owner supports it.

## Use it (CLI):

If you want to use BlockMap through the command line without,

    # For general usage help
    java -jar BlockMap-cli-1.6.1.jar help
    # For help about rendering worlds to a folder
    java -jar BlockMap-cli-1.6.1.jar help render

will get you started. On Linux even with colors!

If your world has been created before the Minecraft 1.13 release, please optimize it. To do this, start Minecraft, select your world, go to "Edit" and then "Optimize World".

### Server usage:

The bash script [server.sh](server.sh) is an example of how this could be used in a server environment. Simply set the paths at the top of the file and call this script regularly on the server. It has a few different render settings pre-configured, but they are easy to adapt to your needs.

## Build it:

Due to technical, legal and performance reasons, some resources required to run and test BlockMap are not included in this repository, but generated locally. The Gradle task `regenerate` will download all required files (you only need an internet connection the first time and after a `clean`) and generate and compile a bunch of stuff. Without this, nothing will work. On a freshly cloned repository, use `initEclipse` or `initIdea` to transform the repository into a project you can simply open in your favorite IDE. (Warning: The `eclipse` and `idea` tasks have to be called each time some dependencies changed. Furthermore, they link to some folders in the build directory. This means that they won't work as intended until `regenerate` has been called.)

All screenshots from aboce are generated automatically through the Gradle task `generateScreenshots`. This way, they are always up to date with the latest version. Be aware that this task needs to generate a fairly large Minecraft world first and then render it, which takes both time and space and will cause Gradle to slow down a lot.

## Run it:

To run from code:
```sh
./gradlew regenerate
# For the CLI
./gradlew run
# For the GUI
./gradlew BlockMap-gui:run
```
If this fails, try `./gradlew run2`\*. If you want to create a release jar and run it, use `./gradlew :BlockMap-gui:runShadow` and `./gradlew :BlockMap-cli:runShadow`.

\* There is a bug in JavaFX that currently prevents the `run` task to work, so as a workaround use `./gradlew run2` for now.

## Update and Release BlockMap:

```sh
./gradlew regenerate
./gradlew generateScreenshots # Optional
./gradlew release
```

- Bump the version in `build.gradle`
- Update `README.md`
	- Bump the version information in file paths
	- Bump the Minecraft version (if applicable)
	- Check the feature list and read through it
- Update the `changelog.md`
- Regenerate all resources
	- If something in the UI changed, regenerate the screenshots
- Generate the release binaries (need to be called on each target platform)
	- This will create two executable fat (=containing all needed dependencies) jars in `./BlockMap-{gui,cli}/build/libs/fat/`.
- Release on GitHub
- Update all packaged versions (currently only AUR)

## Update to newer Minecraft version

- Update and start Minecraft. Create a new debug world. Copy it to `BlockMap/BlockMap-internal/src/test/resources`.
- Copy the current block color instructions in `BlockMap/BlockMap-internal/src/main/resources/` to match the new Minecraft version.
- Copy the current `ChunkRenderer` in `de.piegames.blockmap.renderer` to match the new Minecraft version.
- *Commit as update preparation*
- Start off updating `de.piegames.blockmap.MinecraftVersion`
- Update the Minecraft version of `ChunkRenderer`
- Update `de.piegames.blockmap.renderer.RegionRenderer` to use the new `ChunkRenderer`
- Run `./gradlew regenerate` and make it work
- Run all the tests and make them work
	- If Minecraft changed something on the save format, the `ChunkRenderer` will fail
	- If Minecraft added new blocks, the color map needs to be updated. The failing tests will tell which blocks are missing. Additional information can be retrieved from the default resource pack.
	- If Minecraft added or changed biomes, manual checking and updating is required
- Generate a Minecraft vanilla world and test the GUI
- Implement any new features of the update (e.g. new data that can be shown as GUI pin)
- Regenerate the screenshots
	- Optimize the BlockMapWorld in Minecraft
	- `./gradlew clear && ./gradlew regenerate && ./gradlew generateScreenshots`
- Release it 🎉

## Troubleshooting

If you cannot start the GUI and you get errors similar to

    java.lang.IllegalAccessError: superclass access check failed: class impl.org.controlsfx.behavior.RangeSliderBehavior (in unnamed module @0xa1d113b) cannot access class com.sun.javafx.scene.control.behavior.BehaviorBase (in module javafx.controls) because module javafx.controls does not export com.sun.javafx.scene.control.behavior to unnamed module @0xa1d113b
please try the *Quick start* section above. This is a known JavaFX bug, please try the workaround before reporting.

## Mod support:

Currently, no Minecraft mods are supported, but the rendering engine is built in an extensible way. Mod support will only be implemented on request.
