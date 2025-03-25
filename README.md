# Legacy Item DFU
This library contains a data fixer for being able to convert items from the legacy NBT format of 1.8 directly to the 1.21+ components format with maximal accuracy and no data fixing hacks. The datafixers used here are adapted versions of the ones in Minecraft as the game's DFU is unable to handle certain items properly without brittle Mixins. This is intended primarily for working with the items in the Hypixel API.

## Getting Started
First, add the following Maven repository to your `build.gradle` file.

```groovy
repositories {
	exclusiveContent {
		forRepository {
			maven { url "https://maven.azureaaron.net/releases" }
		}

		filter {
			includeGroup "net.azureaaron"
		}
	}
}
```

Second, add the library as a dependency.

```groovy
dependencies {
	implementation("net.azureaaron:legacy-item-dfu:<insert latest version>")
}
```

## Usage
This library is used like Minecraft's regular DFU, except you pass the custom `LEGACY_ITEM_STACK` TypeReference, pretty simple.

## Requirements
- Minecraft 1.21.5+