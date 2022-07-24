# Changelog

## Version 2.5.1

### Changes

- Added ancient city pin

### Bug fixes

- Fixed mangrove roots did not show up in foliage

## Version 2.5.0

### Changes

- Added support for 1.19 worlds

## Version 2.4.1

### Bug fixes

- Fixed broken "open world" dialog #75
- Fixed an exception while rendering old chunks introduced by the negative y coordinates in 1.18

## Version 2.4.0

### Changes

- Added support for 1.18 worlds
- Added a force-reload action that bypasses the cache. Shortcut `Ctrl+Shift+R`
- Newer fancier heightmap and biome view
- Changed the default settings for some of the pins (maps and treasures are now hidden by default)
- Added name and color information to banner pins (not pretty yet, but better than nothing)

### Bug fixes

- Fixed banner pin positions (#40, #70, thanks @Reispfannenfresser)
- Fixed that changing the world would not cancel the old render tasks (a114616e9de4c54bc6a0f1167088868f16b1d788)

## Version 2.3.0

### Changes

- Minecraft 1.17 support
- GUI rendering engine improvements
	- The way downscaled images are handled got changed. The resulting code is a lot simpler,
		which results in less bugs and memory consumption.
	- The difference is especially noticable in worlds with empty region files/chunks. This
		occurs when using the `/locate` command, or when interacting with cartographers in game.
- Added pin for Lodestones

### Bug fixes

- Some random `NullPointerException`s
- Java >=16 support

## Version 2.2.1

### Changes

- Minor changes to the resource generation—CI is now slightly faster and should fail less often
- Updated screenshots in README

### Bug fixes

- Minor improvements in the open dialog
- Fixed pins for multiple maps on the exact same place

## Version 2.2.0

### Changes

- New "open" dialog
- Dragging the map is now done with the left mouse button, like in any other reasonable map viewer.
- Show a timestamp of when the world was last rendered
- The GUI now has command line options
	- Optionally specify a path as command line argument, which will be loaded on startup
		- This allows associating files and folders with BlockMap: Right click a Minecraft save folder -> Open with -> BlockMap \o/
	- `-v` or `--verbose` to enable debug messages
- Custom URI scheme handler
	- If you have an URL pointing at a BlockMap server location (usually some `index.json`, prefix it with `blockmap:`
	- The browser (or any other application with URL scheme handling support) will now prompt and ask to open said world in BlockMap
	- More features, like linking a specific location may be added in the future
- Switching into the Nether and back adapts the view to the fact that the Nether is smaller than other dimensions (#51)
- Better caching of several things
	- Recently opened worlds are now saved and displayed first when loading
	- Player skins and UUID won't be fetched every time from Mojang servers
- Server command line changes (`render-many`)
	- Removed most command line options and moved them into the configuration file itself
	- Added fine-grained control about which pins to include when rendering, and which not (for both file size and privacy/cheating reasons)

### Bug fixes

- BlockMap accidentially opened the region files with RW permission, even if it does not modify them.
- Player pins are no longer blurry (#48, #49)
- Maps from 1.16 worlds load properly now
- URLs with missing trailing slash (as `https://blockmap.exmaple.com`) don't throw an exception anymore (#54)

## Version 2.1.0

### Changes

- Minecraft 1.16 support
	- Renderer supports the new save format
	- Added all new nether blocks to the color maps
	- Added biome colors for the new Nether biomes
	- Added new village pins: `bee_nest`, `nether_portal`
	- Added new Nether pins: `bastion_remnant`, `nether_fossil`
- Reordered a few of the pins (among other tweaks)

### Bug fixes

- Fixed village pins showing up in the wrong dimension (#47)
- Fixed a few minor errors in the color maps

## Version 2.0.0

***breaking*** news for ***breaking*** changes! Long overdue, a rewrite/overhaul of quite a few components. No Minecraft 1.16 support yet.

### Changes

- GUI redesign:
	- **Autocomplete!**
	- Better support for viewing worlds on servers.
	- Rewrite of the map rendering engine
		- Much cleaner code, probably removed a lot of bugs
		- Removed caching (may increase memory usage in large worlds)
	- Removed saving support. Use the CLI instead.
- ***breaking*** World representation has been changed. Previously, every folder containing region files would count as world. Now, a world always is a folder containing a `level.dat` together with a dimension (defaulting to the overworld).
	- For the CLI, a few paths will have to be changed. `--dimension` is now always used, but defaults to `OVERWORLD`.
	- For the GUI, nothing much will change except you cannot open a single region folder anymore (which probably nobody did anyways)
- New server mode using `render-many`:
	- **It is not declared stable yet, to allow breaking changes even on minor version bumps.**
	- Declare a configuration file with all your worlds and how you want to render them. An example configuration can be found [here](server-settings.json).
	- Call `blockmap render-many` and pass the configuration file as argument. You can add dynamic server information like online plyers etc. via more command line options.
	- An output directory will be created with all the rendered files. You can view them in BlockMap.
	- Host that folder using the web server of your choice (e.g. [`miniserve`](https://github.com/svenstaro/miniserve)). Clients will now be able to view your worlds across the Internet.
	- Call this on a scheduled basis. Subsequent runs will update the folder without re-rendering everything.
- Other CLI changes:
	- ***breaking*** The `--lazy` option has been removed and is no enabled by default. Use `--force` to disable it manually.
	- Proper exit code handling. When using BlockMap in scripts, you'll know when it fails.
- Java 13 support

### Bug fixes

- Color map selection in the GUI has gone wrong a few times
- Some bounds checks when rendering part of the world are notoriously wrong

### Backwards-compatibility

All changes that potentially require manual intervention are marked with "***breaking***" in the changelog above.

## Version 1.6.2
### Changes

- Added a color map that shows only rails (#38, thanks @gobo7793)
- Fixed a bug (#36, thanks @jedenastka)

## Version 1.6.1
### Changes

- Fixed a bug

## Version 1.6.0
### Changes
- Added proper caves view
- Renamed old cave view into X-ray view
- Performance optimizations
- Update for Minecraft 1.15
- Changed image filtering in HTML view (#32, thanks @rasmusolle)
- Fixed compiling under Java 13
- Internal changes and documentation

## Version 1.5.1
### Changes

- Fixed a bug

## Version 1.5.0
### Changes

- All biome colors (especially the water color) are up to date now
- There are proper Windows releases now. This should fix some class path issues.
- The application has been into separate CLI and GUI binaries. The command line interface is platform independent, but not the GUI.

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
