# Steam Deck Native Controls for Minecraft (Java Edition)

This mod gives Minecraft Java Edition native support for the Steam Deck's inputs, bypassing all of Steam Input.
## Features
* Gyro and flick stick camera controls as designed by [Jibb Smart](http://gyrowiki.jibbsmart.com/)
* Twin-stick aiming using left joystick and right trackpad
* Mouse controls for GUIs
* On-screen keyboard using trackpads

## Control mapping

The following is the default config. It can be changed in the config file. More instructions / GUI coming soon...

### Non-GUI mode
![non-GUI controls](./docs/nongui.svg)
### GUI mode
![GUI controls](./docs/gui.svg)

## Installation
This is a [Forge](https://minecraftforge.net/) mod. Place the JAR in the `mods/` directory.

You will also need to configure Steam Input to use an empty configuration in order to avoid conflicts.

### Touchscreen

In order to enable this mod's touchscreen controls, you need to create a file `/etc/udev/rules.d/51-deck-touchscreen.rules` with contents

```
SUBSYSTEMS=="input", ATTRS{name}=="FTS3528:00 2808:1015", ATTRS{phys}=="i2c-FTS3528:00", RUN="/usr/bin/setfacl -m u:deck:rw %N"
```

## How it works

This mod opens the Steam Deck's hidraw device and reads raw input reports (independently / in parallel with anything Steam does). These inputs are injected into Minecraft by calling into the existing keyboard and mouse handler code.
