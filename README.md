# Lewd Crossing

A Minecraft mod for Fabric, in two versions: **1.21.1** and **26.3**. Adults
only: on first launch, the game asks whether the player is 18 or older.

Companions to meet and win over, a collection of items and machines that bend
the game's mechanics, and a strange dimension, the void, beneath the
laboratories of the containment sites.

## Content

- **Three companions**: Jenny, Ellie and FapGPT (the lab's holographic AI).
  Each has her own personality, lines, mood and an affection meter that
  gradually unlocks new interactions (talk, hug, outfits, requests).
- **Animated scenes**, with or without the player taking part, and an
  arousal bar.
- **Items and toys**: handcuffs, whip, dance pole, glory wall, dildo pogo,
  Cummy piston, shape and inversion potions, capture capsule, camera and
  OnlyFaps.
- **Cum**: vials, traces on the ground and on the girls, cauldron, cheese,
  "cum" dishes, sticky arrows and splash vials, jar, compressor and Cum
  Crystal.
- **The lab and the void**: terminal (shop, radio, OnlyFaps), vending
  machine, elevator, wall screens, void suit and void swimming, lidar, rifts,
  and an unsettling merchant behind a tree.
- **Structures**: houses in five biomes, containment sites.

## Installation

Download the jar for your Minecraft version from the repository's
**Releases** page and put it in the instance's `mods/` folder.

| Dependency | Minecraft 1.21.1 | Minecraft 26.3 |
|---|---|---|
| Fabric Loader | 0.17 or newer | 0.19 or newer |
| Fabric API | for 1.21.1 | for 26.3 |
| GeckoLib | 4.9 or newer | 5.5 or newer |
| Java | 21 | 25 |

JEI is supported when installed (the mod's brewing recipes), but not
required. The mod must be installed on both the client and the server.

## Files not included

Some files are **not included** in this repository, because I am not sure I
have the right to redistribute them:

- the 3D models, animations and textures of Jenny and Ellie, and those
  derived from them (FapGPT, the first-person hands);
- Gaster's model;
- the Deltarune assets used in Gaster's shop (background, heart, font,
  purchase sound).

To play, use the jar from the Releases page.

## Repository layout

```
resources/          assets and data shared by both versions
versions/1.21.1/    code and build for Minecraft 1.21.1
versions/26.3/      code and build for Minecraft 26.3
```

Each version has its own code, because the Minecraft API changed too much
between the two. The resources are shared: each version reads `resources/`,
and `versions/26.3/` only adds the files whose format changed in 26.3.

## Credits

See `CREDITS.txt`, also included in the jar: SchnurriTV's Jenny Mod,
Skyguy555's "Stylized Gaster" model, Deltarune assets (Toby Fox) in Gaster's
shop, and recoloured game textures (Mojang).

## License

The code is under the MIT license (`LICENSE`). Third-party assets listed in
`CREDITS.txt` remain the property of their authors.
