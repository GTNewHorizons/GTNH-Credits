# GTNH Credits

A Minecraft 1.7.10 Forge mod that adds a **Credits** screen to the
main menu, giving GT New Horizons one official place to recognize
everyone who contributed to the pack.

It is an implementation for
the [Centralized Credits Page #23582](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/issues/23582) proposal.

## Access

- **Credits** button on the main menu
- **ESC** / **Back** returns to the menu

## Installation and setup

Build the mod:

```sh
./gradlew build
```

Copy the release jar from `build/libs/` (the jar without `-dev` or `-sources` in
its name) into the Minecraft instance's `.minecraft/mods/` directory, then
restart Minecraft.

### Custom Main Menu integration

[Custom Main Menu](https://github.com/GTNewHorizons/Custom-Main-Menu) 1.14.0+ adds a
`sendIMC` action type. When a button with that action is clicked, CMM fires a
`ActionIMCEvent` on `MinecraftForge.EVENT_BUS`. GTNH-Credits subscribes to this event and
opens the Credits screen when it receives `modid = "gtnhcredits"` and
`message = "openCredits"`. GTNH-Credits has a compile-only dependency on CMM for
`ActionIMCEvent`; CMM has no dependency on GTNH-Credits.

#### Button configuration

Add the following entry to the `"buttons"` object in
`.minecraft/config/CustomMainMenu/mainmenu.json`:

```json
{
  "credits": {
    "text": "Credits",
    "posX": 0,
    "posY": -160,
    "width": 150,
    "height": 20,
    "alignment": "column_bottom",
    "action": {
      "type": "sendIMC",
      "modid": "gtnhcredits",
      "message": "openCredits"
    }
  }
}
```

GTNH installations commonly provide responsive menu variants alongside
`mainmenu.json`:

- `mainmenu_auto.json`
- `mainmenu_small.json`
- `mainmenu_normal.json`
- `mainmenu_large.json`

CMM selects one of these files according to the current GUI scale. Add the
same `credits` entry to every variant that exists; editing only
`mainmenu.json` will not affect the menu while a scale-specific variant is
active.

Using the mod's lang key for the button label is also valid:

```json
{
  "text": "gui.main_menu.button.credits"
}
```

#### Vanilla button configuration

When CMM is managing the main menu, the built-in Credits button added by this mod to
the vanilla `GuiMainMenu` is redundant. It can be disabled in
`config/gtnh-credits/gtnh-credits.cfg`:

```text
menu_button {
    # Show the Credits button in the vanilla main menu.
    B:enabled=false
}
```

The default is `false`. If CMM is not installed or has no Credits entry, set
`B:enabled=true` to add the mod's button to the vanilla main menu instead.

## Credits content and translations

The mod owns the format, renderer, editor, and interface text. The modpack owns
its credits document and English category names, descriptions, and role labels.
Community translations are maintained through GTNH-Translations / ParaTranz.

At runtime the mod reads `gtnhcredits:credits.json` through Minecraft's resource
manager. A standard resource pack supplies:

```text
assets/gtnhcredits/credits.json
assets/gtnhcredits/lang/en_US.lang
```

For GTNH's existing TX Loader installation, put the same content in the modpack
repository at:

```text
config/txloader/load/gtnhcredits/credits.json
config/txloader/load/gtnhcredits/lang/en_US.lang
```

The renderer has no dependency on TX Loader. Use its normal `load/` directory so
translation and other resource packs can override content. Keep behavioral
settings in `config/gtnh-credits/gtnh-credits.cfg`.

The old standalone `config/gtnh-credits/credits.json` is no longer loaded. Its
presence produces a migration warning; it is never moved or deleted automatically.
See [Upgrading an existing pack](#upgrading-an-existing-pack) before upgrading.

The release jar contains UI labels and no production roster or credits wording.
If no credits resource is available, the screen displays an empty-state message.
Resource reloads refresh the document and the displayed translations.

### Upgrading an existing pack

1. Move the standalone credits JSON and its credits-owned English language keys
   into the TX Loader paths shown above. Leave behavioral settings in their cfg.
   Keep `gui.credits.*` interface translations in the mod and update the
   GTNH-Translations extraction source for pack-owned text.
2. Replace machine-name role ids with `{ "translationKey": "..." }` references
   using the defining mod's exact name keys. Set the document version to `3`
   when using references, then remove the duplicated machine names from the
   credits language files. Retain category and ordinary role ids.
3. Validate with the updated editor's `--validate /path/to/modpack` command.
   Supply `--instance=/path/to/minecraft` to check against installed language
   resources and saved GregTech defaults. Check the screen in English and another
   language, including resource reloads.
4. Ship the content move with the updated mod. Arrange removal of the obsolete
   standalone JSON through the pack updater; the mod only warns about that file.

Keep the preceding content and matching mod release for rollback. Older readers
cannot consume external-reference objects; restore the previous content and
reader together. Opening or validating with the updated editor does not rewrite
files. Saving emits version 3, expands legacy combined membership maps and omits
ignored version-2 `username` metadata, so retain any needed metadata separately.

### Document format

See [credits.schema.md](credits.schema.md) for the complete contract and
[credits.schema.json](credits.schema.json) for the machine-readable schema.
Version 2 remains readable; saving JSON produces version 3. Unsupported versions
are rejected explicitly. The schema also ships inside the editor distribution.

```json
{
  "version": 3,
  "category": [
    { "id": "development", "class": ["detail", "person", "role"] },
    { "id": "gt_multiblocks", "class": ["detail", "person", "role"] }
  ],
  "person": [
    {
      "name": "Example Contributor",
      "category": [
        { "development": "coordinator" },
        {
          "gt_multiblocks": {
            "translationKey": "gt.blockmachines.multimachine.nanochipassemblycomplex.name"
          }
        }
      ]
    }
  ]
}
```

Category markers are `detail` (description), `person` (people), and `role` (labels
beside people). The category title is always displayed; unknown markers are ignored.
People can have multiple category memberships, each with its own role labels.

### Credits-owned language keys

```properties
credits.category.development=Development
credits.category.development.detail=People who develop and maintain the pack.
credits.category.gt_multiblocks=GT Multiblocks
credits.category.gt_multiblocks.detail=Original machine authors and structure designers.
credits.person.role.coordinator=Coordinator
```

Category and local-role ids are stable identifiers. Their language-key suffixes
remove dots and hyphens, replace runs of spaces with `_`, and lowercase using
`Locale.ROOT`. Validation reports ids that would collide after this normalization.
Change wording in the language file while retaining the id.

Descriptions may use a plain `credits.category.<id>.detail` value with literal
`\n` paragraph separators, indexed `detail.<suffix>` entries, or both. Indexed
suffixes use natural numeric-aware ordering; plain paragraphs precede indexed ones.

### External translation references

A `{ "translationKey": "..." }` role uses that exact Minecraft translation key.
Its spelling and case are preserved and it receives no credits prefix. Machine
names stay owned and translated by their defining mod; do not duplicate them in
credits language files. The player's selected language and Minecraft's English
fallback are used. Unresolved references display the key and are logged.

UI translations such as `gui.credits.button.back` remain bundled with the mod.
Missing local category/role translations display the original id; content
validation requires the source English labels and requested descriptions.

Development examples live under [`fixtures/content/`](fixtures/content/).
Validation fixtures use [`src/test/resources/`](src/test/resources/).
Neither is included in release resources.

## GTNH Credits Editor

A standalone desktop tool for editing `credits.json` and the associated
`.lang` files lives in the [`creditsEditor/`](creditsEditor/README.md)
subproject.

Run it directly from the repository:

```sh
./gradlew :creditsEditor:run
```

Install it to `~/.local` (overridable with `-PPREFIX=...`):

```sh
./gradlew :creditsEditor:install
```

Once installed, launch it with `gtnh-credits-editor [<path>]`. See the
[Credits Editor README](creditsEditor/README.md) for full usage,
installation options, and supported path forms (directory or resource
pack zip).

## Build tasks

### Validate `credits.json`

```bash
./gradlew test
```

Runs schema, parser, semantic validation, editor, and round-trip tests against
development fixtures. Checks include supported versions, category references,
translation-key collisions, and external-reference preservation. Runs automatically as part of `check` and `build`.

### Generate schema documentation

```sh
./gradlew generateCreditsSchemaDoc
```

Regenerates [`credits.schema.md`](credits.schema.md) from
[`credits.schema.json`](credits.schema.json).
Runs automatically as part of `check` and `docs`.

```sh
./gradlew docs   # generate all project documentation
./gradlew check  # validate + generate docs + run all checks
```

## AI Transparency

This project uses Claude Code and other AI agents as a technical harness for planning, architecture, and code
generation.

As the Software Architect and Lead Developer, I manage a team of AI agents to handle implementation and slicing. While
they help with the heavy lifting, I manually review everything. I vet my own work and generated code alike because I
trust my first-pass code as little as I trust AI output.

I ensure all output complies with the Software Engineering fundamentals I mastered over 40+ years in the industry. My
standards are rooted in a career spanning the full IT stack from legacy mini-systems and network protocols to modern R&D
and Java development.

### Ownership & Accountability

I am accountable for every line of code associated with my name in the `git blame`. I do not use "co-authored-by"
metadata for AI agents because tools do not take responsibility; Engineers do.

If the code is incorrect, has bugs, or does not act as intended, the responsibility is mine. I put my real name on this
work and take full ownership of it within the limits of the [MIT License](LICENSE). This project is open-source; if you
find an issue, please open a ticket or propose a change.
