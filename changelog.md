# Changelog

## Version 1.4.1
### Changes

- Minor bug fixes
- Updated to Minecraft 1.14.4

## Version 1.4.0
### Changes
- Updated to 1.14
	- 1.13 worlds and chunks still work
	- 1.14 villages are recognized
	- New biome colors and other details are still missing (help appreciated!)
- Improved GUI
	- Minor pin improvements
	- Added "about" dialog
	- Keyboard shortcuts
	- Splash screen
	- Improved startup performance
	- Everything is cached now, so it should be a lot faster
- New world save format and command line options
- Bug fixes
- New bugs

## Backwards-compatibility
The save format and command line have changed, and it is not backwards-compatible to BlockMap 1.3.0.
If you use BlockMap on a server, you'll need to update the server script and older clients won't be able
to open the worlds anymore.

## Version 1.3.0
### Changes
- Added Pins
	- They contain information about the world and the render process (did it fail?)
	- Save them together with your rendered world
	- Display them in the GUI
- Redesigned GUI layout
- Little performance improvements
- Updated to Java 11
- Updated libraries

## Version 1.2
### Changes
- Reworked the `RegionFolder` API. It remains not being great, but at least now with more features
- Rendered worlds can now be saved to a file using the `save` subcommand
- Rendered worlds can be loaded from the GUI, even on remote servers
- More automatically generated screenshots (and source code)
- Updated to a newer version of the NBT library, which got its region file API rewritten
- Fixed bugs

## Version 1.1.2
### Changes
- Added a grid overlay
- Updated to Minecraft 1.13.2
- Fixed some bugs

## Version 1.1.1
### Changes
- Support for changing the color map as well as the shader through CLI options
- Added an option to render chunks only if needed, called `--lazy`. (Implements #1)
- Fixed a few CLI bugs

### Known issues
- The about/help dialog still does nothing yet

## Version 1.1.0
### Changes
- Support for multiple color maps
- New color maps: No foliage, ocean ground, cave view
- Support for different shaders
- New height shading options: None, relief, biome color, height map
- GUI overhaul
- Made rendering of stacked semi-transparent blocks of the same color way faster
- Restructured gradle project structure

### Known bugs
- The about/help dialog does nothing yet

### Fixed bugs
- Crash when selecting folder and Minecraft ist not installed

## Version 1.0.1
### Changes
- CLI now actually works
- Added some simple relief shading to improve readability. The map does not look flat anymore.
- A toggle and more shaders are yet to come

### Known bugs
- There are some chunk-sized artifacts in the shading. This seems to come from chunks that have not been fully generated yet.
