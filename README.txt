KEYCARD READER + KEYCARDS — SOURCE PROJECT

FEATURES
- /cardify <CODE> converts PAPER in your main hand into one of the ten supported keycards.
- The code is stored in item CustomData under KeyCard and the native item model is set from a strict allowlist.
- The anvil display name is independent of the card code: renaming the card does not choose its texture.
- Keycard display names are recolored on anvil result creation to match the card's final color code:
  B = dark blue, G = dark green, P = dark purple, R = dark red, Y = gold/yellow.
- Ordinary paper cannot be renamed in an anvil unless it has a valid keycard code.
- Existing dropper reader renderer and textures are retained.

USAGE
1. Hold PAPER in your main hand.
2. Enter one of:
   /cardify KC-B-B
   /cardify KC-B-G
   /cardify KC-B-P
   /cardify KC-B-R
   /cardify KC-B-Y
   /cardify KC-W-B
   /cardify KC-W-G
   /cardify KC-W-P
   /cardify KC-W-R
   /cardify KC-W-Y
3. Rename the card to any display name in an anvil. The texture comes from its KeyCard data/model, not from the editable name.
4. The anvil output name color is set to the card's color.

IMPORTANT SECURITY NOTE
The tag makes the card independent of its editable name, but Minecraft item data is not cryptographic security. Players with commands, creative-mode item editing, or tools that can alter item components may still forge tags. For a multiplayer server where cards must be unforgeable, validation must happen server-side against server-controlled card IDs/permissions.

BUILD / COMPATIBILITY
This is a source project, not a compiled JAR. The project targets Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.164.0+26.3, Java 25 and Fabric Loom 1.17.21.
Run `gradle build` in this directory with a compatible JDK and internet access. The output should be in `build/libs/`.
I could not verify a successful compile or in-game behavior in this environment. In particular, the Minecraft 26.3 API is a fast-moving target, so compile and test with the exact Minecraft/Fabric versions before using it on a server.

Existing texture/model assets and dropper-reader renderer are included.
