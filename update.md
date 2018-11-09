# Update to a new Minecraft version

1. Download the new json file (see `Downloader#downloadMinecraft` for link) and update it according to the documentation
2. Generate the block states `gradle generateBlockStates`and copy them into the enum (`BlockState.java`)
3. Update `block-color-instructions.json` to contain all the new blocks, remove all removed blocks
4. Compile all block data
5. Run all tests
6. To make sure everything works, run `gradle beforeCommit`
7. Commit
