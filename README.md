# BlockMap - A Minecraft 1.13 world viewer

*This started as fork of [TMCMR](https://github.com/TOGoS/TMCMR), but has been almost completely rewritten due to the update. If you want something stable that works with 1.12 and before worlds, go check it out.*

## Features:
- A little interactive GUI based on JavaFX
- Different color maps and shaders that highlight exactly what you are looking for (including an underground caves and an ocean ground view)
- A gui library to include maps into your own JavaFX applications (mostly done, but not released yet)
- A command line interface to render your worlds from scripts
- The core rendering code as library to use in your own projects (not released yet)
- Rendering scale: 1 pixel : 1 block
- Faster than MCEdit!
- Gamma corrected rendering

## Requirements:

- Java 8+
- The gui (library and standalone) rely on JavaFX. Make sure you have it on your class path!
  - Java 8 to Java 10 already include it if you are using the Oracle JDK
  - The OpenJDK 8 has it, but not OpenJDK 10
  - If all that does not work, try installing the [OpenJFX Early-Accesss Builds](http://jdk.java.net/openjfx/)
- Minecraft 1.13 worlds. Chunks from before the release (even from 1.13 snapshots) will be ignored. Chunks from before 1.9 will throw errors. Please optimize your worlds in Minecraft before rendering them

## Download:

Download the latest version from the [Release page](https://github.com/piegamesde/BlockMap/releases).

## Usage:

The GUI version should just run by (double)clicking it. Otherwise run it through:

    java -jar BlockMap-1.1.1.jar

to start. If you want to use BlockMap through the command line without GUI (not only for scripts),

    java -jar BlockMap-1.1.1.jar help

will get you started. On Linux even with colors!

If your world has been created before the Minecraft 1.13 release, please optimize it. To do this, start Minecraft, select your world, go to "Edit" and then "Optimize World".

**GUI controls:**

- Mouse wheel to zoom in and out
- Drag with the right mouse button to pan the view
  - This will very likely change in the future
  - If you drag to the edge, the mouse will wrap around so you can drag indefinitely. Blender users will appreciate this
- When loading a world, you can select either a world folder, a region folder or a single region file

## Mod support:

Currently, no Minecraft mods are supported, but the rendering engine is built in an extensible way. Mod support will only be implemented on request.

## Compile it yourself:

Due to technical, legal and performance reasons, some resources required to run and test BlockMap are not included in this repository, but generated locally. The Gradle task `regenerate` will download all required files (you only need an internet connection the first time and after a `clean`) and generate and compile a bunch of stuff. Without this, nothing will work. On a freshly cloned repository, use `initEclipse` or `initIdea` to transform the repository into a project you can simply open in your favorite IDE. (Warning: the `eclipse` and `idea` tasks you need to call when something on the dependencies changed link to some folders in the build directory. This means that they won't work as intended unless all resoruces have already been generated.)

The task `beforeCommit` will clean and regenerate all resources and run all the tests to check if everything works (poor man's CI). The task `github` will take the code and the resources into a fat standalone jar.

## Gallery

![Four rendered region files, each with one with a different color map](screenshots/screenshot-1.png "All existing color maps")
![Four rendered region files, each one with a different shader](screenshots/screenshot-2.png "All existing shaders")
![Screenshot from the GUI](screenshots/screenshot-3.png "Screenshot from the GUI")
