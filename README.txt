KEYCARD READER + KEYCARDS — SOURCE PROJECT

FEATURES
- /cardify opens a menu (card name, colour by clicking a card icon, special code). Confirming converts the PAPER in your main hand into a keycard.
- KeyCard stores the visual code and selects one of the ten whitelisted item models.
- KeyCardSerial stores a per-card special code. Different serials are different item components, so cards with different special codes do not stack even when their display names and textures match.
- The command requires exactly one paper item, preventing an entire paper stack from accidentally receiving one shared identity.
- The anvil display name does not select the texture. It can be changed freely, while the card model and serial are preserved.
- The anvil name color is forced to match the card's final color code: B = dark blue, G = dark green, P = dark purple, R = dark red, Y = gold/yellow.
- Plain paper and cards missing a valid visual code or special code cannot be renamed into a valid card.
- The dropper reader front uses full-bright vertex lighting so its texture is not darkened by surrounding block light.
- Opening a named reader (dropper "KeyCard einlegen") shows the keycard(s) in the hopper directly below it in a small panel to the right of the inventory; hover an icon for the tooltip.

USAGE
1. Hold PAPER in your main hand and run /cardify.
2. In the menu enter a card name (optional, defaults to the code), click the card colour you want and enter a unique special code (4-51 characters: letters, numbers, _ or -).
3. Click "Erstellen". Codes: KC-<Black|White>-<Blue|Green|Yellow|Red|Purple>, e.g. KC-B-P = black/purple.
4. Rename the card in an anvil if desired. Its texture still comes from the code; the name colour is forced to match the card.

STACKING / SECURITY NOTES
Minecraft compares item components when deciding whether items can stack. Since KeyCardSerial is stored in CustomData, cards with different special codes do not stack, regardless of display name. If two cards have the same visual code AND the same special code, Minecraft may stack them; therefore issue a unique special code per card.
This is not cryptographic anti-cheat protection. Players with commands, creative-mode item editing, or tools that can alter item components may forge or duplicate data. Strong multiplayer enforcement requires server-side validation against a server-controlled registry of issued card IDs.

BUILD / COMPATIBILITY
Targets Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.164.0+26.3, Java 25 and Fabric Loom 1.17.21.
Run `gradle build` with a compatible JDK and internet access. Output should be in `build/libs/`.
This environment has Java 21 and no Gradle executable, so a successful build and in-game test could not be performed here. Compile and test with the exact target versions before deploying.

HEX COLOURS
In the /cardify menu, row "Eigene": enter a hex colour for the card body and one for the stripe (e.g. 222222 and FF8800), click the custom card. Code format: KC-#RRGGBB-#RRGGBB. The name uses the stripe colour.
