KEYCARD READER + KEYCARDS — SOURCE PROJECT FOR MINECRAFT JAVA 26.3 / FABRIC

FEATURES INTENDED
- Includes all ten transparent keycard PNGs and the existing CIT .properties rules.
- /cardify adds the custom data boolean KeyCard=true to the item in your main hand.
- Anvil mixin blocks renaming PAPER unless the input stack has KeyCard=true and applies the matching native item model for the ten exact keycard names.
- Client renderer displays the custom front only on a placed dropper whose block entity custom name is exactly "Keycard-Reader", for all six facings.

IMPORTANT LIMITATIONS
- This is a SOURCE PROJECT, not a compiled or tested .jar. It has not been built against Minecraft 26.3 in this environment. Minecraft 26.3's API changes mean compilation and in-game testing may require adjustments.
- Keycard textures are implemented with native Minecraft item models and selected by the anvil mixin; no CIT mod is required for these ten keycard names. The original OptiFine CIT files remain included.
- The dropper front is rendered by this mod; it does not globally replace vanilla dropper textures.
- /cardify currently tags any item in the main hand. Only PAPER is blocked by the anvil mixin when it lacks KeyCard=true; other item types keep vanilla anvil behavior.

BUILD
Requires JDK 25, Gradle 9.6.0, internet access for Gradle/Maven dependency downloads, and Fabric Loom 1.17.
From this folder run: gradle build
Output (if the build succeeds): build/libs/keycard-reader-1.1.0.jar

INSTALL
1. Build the project first; this zip itself is not installable in the mods folder.
2. Install Fabric Loader for Minecraft 26.3, Fabric API 26.3, and CIT Resewn: Continued compatible with 26.3.
3. Put the resulting keycard-reader-1.1.0.jar in %appdata%\.minecraft\mods.
4. Launch the Fabric 26.3 profile.
5. Hold an item and run /cardify. For a keycard, use paper, then rename it at an anvil to one of: KC-B-B, KC-B-G, KC-B-P, KC-B-R, KC-B-Y, KC-W-B, KC-W-G, KC-W-P, KC-W-R, KC-W-Y.
6. Rename a dropper item to exactly KeyCard einlegen before placing it.
