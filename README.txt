KEYCARD READER + KEYCARDS — SOURCE PROJECT

FEATURES
- /cardify <CODE> <SPECIALCODE> converts exactly one PAPER in your main hand into a keycard.
- KeyCard stores the visual code and selects one of the ten whitelisted item models.
- KeyCardSerial stores a per-card special code. Different serials are different item components, so cards with different special codes do not stack even when their display names and textures match.
- The command requires exactly one paper item, preventing an entire paper stack from accidentally receiving one shared identity.
- The anvil display name does not select the texture. It can be changed freely, while the card model and serial are preserved.
- The anvil name color is forced to match the card's final color code: B = dark blue, G = dark green, P = dark purple, R = dark red, Y = gold/yellow.
- Plain paper and cards missing a valid visual code or special code cannot be renamed into a valid card.
- The dropper reader front uses full-bright vertex lighting so its texture is not darkened by surrounding block light.

USAGE
1. Hold exactly one PAPER in your main hand.
2. Enter a visual code plus a unique special code, for example:
   /cardify KC-B-P VaultCard_0001
   /cardify KC-B-P VaultCard_0002
   /cardify KC-W-B StaffCard_0001
3. Supported visual codes: KC-B-B, KC-B-G, KC-B-P, KC-B-R, KC-B-Y, KC-W-B, KC-W-G, KC-W-P, KC-W-R, KC-W-Y.
4. The special code must be 4–64 characters and contain only letters, numbers, underscores or hyphens. Use a different special code for every physical card that must not stack with another.
5. Rename the card in an anvil if desired. Its texture still comes from the visual code, not the display name; the name color is forced to match the card.

STACKING / SECURITY NOTES
Minecraft compares item components when deciding whether items can stack. Since KeyCardSerial is stored in CustomData, cards with different special codes do not stack, regardless of display name. If two cards have the same visual code AND the same special code, Minecraft may stack them; therefore issue a unique special code per card.
This is not cryptographic anti-cheat protection. Players with commands, creative-mode item editing, or tools that can alter item components may forge or duplicate data. Strong multiplayer enforcement requires server-side validation against a server-controlled registry of issued card IDs.

BUILD / COMPATIBILITY
Targets Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.164.0+26.3, Java 25 and Fabric Loom 1.17.21.
Run `gradle build` with a compatible JDK and internet access. Output should be in `build/libs/`.
This environment has Java 21 and no Gradle executable, so a successful build and in-game test could not be performed here. Compile and test with the exact target versions before deploying.
