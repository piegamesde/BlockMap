#!/bin/bash
# Stop this script on the first failure (e.g. cannot create the output folder)
set -e

# This script renders one or more worlds with different settings. It is an example for how to use BlockMap in scripts. Adapt these variables to point to the required data for
# this to work. Further below are the actual render commands. Feel free to adapt them to your needs.
#
# The current configuration will render each dimension at least once, the overworld even multiple times (ocean ground view and cave view). All images are written to a different
# subfolder for each setting.

WORLD_FOLDER_OVERWORLD=/path/to/world/region
WORLD_FOLDER_NETHER=/path/to/world/DIM1/region
WORLD_FOLDER_END=/path/to/world/DIM-1/region
OUTPUT_DIR=/path/to/output
BLOCKMAP_FILE=/path/to/BlockMap.jar

mkdir -p $OUTPUT_DIR

# A simple and plain overworld view
mkdir -p $OUTPUT_DIR/overworld
java -jar $BLOCKMAP_FILE -v render -l -o=$OUTPUT_DIR/overworld $WORLD_FOLDER_OVERWORLD
# The ocean grounds of the overworld
mkdir -p $OUTPUT_DIR/overworld_ocean
java -jar $BLOCKMAP_FILE -v render -l -o=$OUTPUT_DIR/overworld_ocean -c=OCEAN_GROUND $WORLD_FOLDER_OVERWORLD
# All caves up to height 30
mkdir -p $OUTPUT_DIR/overworld_cave
java -jar $BLOCKMAP_FILE -v render -l -o=$OUTPUT_DIR/overworld_cave -c=CAVES --max-height=30 $WORLD_FOLDER_OVERWORLD
# The nether up to height 64
mkdir -p $OUTPUT_DIR/overworld_nether
java -jar $BLOCKMAP_FILE -v render -l -o=$OUTPUT_DIR/nether --max-height=64 $WORLD_FOLDER_NETHER
# A plain view of the end
mkdir -p $OUTPUT_DIR/overworld_end
java -jar $BLOCKMAP_FILE -v render -l -o=$OUTPUT_DIR/end $WORLD_FOLDER_END

# Write an index file to the root directory. It is a simple name=path properties file, but in JSON.
cat << EOF > $OUTPUT_DIR/index.json
{
	"Overworld": "./overworld/rendered.json.gz",
	"Overworld (Ocean)": "./overworld_ocean/rendered.json.gz",
	"Overworld (Cave)": "./overworld_cave/rendered.json.gz",
	"Nether": "./nether/rendered.json.gz",
	"End": "./end/rendered.json.gz"
}
EOF
