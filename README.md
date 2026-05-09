# GTNH Credits

A Minecraft 1.7.10 Forge mod that adds a **Credits** screen to the
main menu, giving GT New Horizons one official place to recognize
everyone who contributed to the pack.

It is an implementation for
the [Centralized Credits Page #23582](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/issues/23582) proposal.

## Access

- **Credits** button on the main menu
- **ESC** / **Back** returns to the menu

## Custom Main Menu integration

[Custom Main Menu](https://github.com/GTNewHorizons/Custom-Main-Menu) 1.14.0 adds a
`sendIMC` action type. When a button with that action is clicked, CMM fires a
`ActionIMCEvent` on `MinecraftForge.EVENT_BUS`. GTNH-Credits subscribes to this event and
opens the Credits screen when it receives `modid = "gtnhcredits"` and
`message = "openCredits"`. GTNH-Credits has a compile-only dependency on CMM for
`ActionIMCEvent`; CMM has no dependency on GTNH-Credits.

### Button configuration

Add an entry to the `"buttons"` object in your `mainmenu.json`:

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

Using a lang key for the button label is also valid:

```json
{
  "text": "menu.gtnh.credits"
}
```

### Disabling the vanilla button

When CMM is managing the main menu, the built-in Credits button added by this mod to
the vanilla `GuiMainMenu` is redundant. It can be disabled in
`config/gtnh-credits.cfg`:

```ini
[menu_button]
    # Show the Credits button in the vanilla main menu.
    enabled=false
```

The default is `false`. Set it to `true` to show the vanilla button regardless of
whether CMM is installed.

## Resource files

Credits data is driven by a `credits.json` for the semantic credits
content and associated `.lang` resource files for localized display
text. Both are loaded through Minecraft's standard resource manager,
so they can be included as part of the mod jar or overridden by a
resource pack.

### `credits.json`

ResourceLocation:
[`assets/gtnhcredits/credits.json`](src/main/resources/assets/gtnhcredits/credits.json)

Defines the credit categories, credited persons, and roles for those
categories that can be displayed in the credit screen. A category
groups persons under a common heading and controls what is rendered
via its `class` markers. A person belongs to one or more categories
and may carry one or more roles.

See **[credits.schema.md](credits.schema.md)** for the full format
reference.

#### Minimal example

```json
{
  "category": [
    {
      "id": "dev",
      "class": [
        "detail",
        "person",
        "role"
      ]
    }
  ],
  "person": [
    {
      "name": "Alice",
      "category": "dev",
      "role": "core-dev"
    }
  ]
}
```

#### Category `class` markers

| Value    | Effect                                                                                                              |
|:---------|:--------------------------------------------------------------------------------------------------------------------|
| `detail` | Renders the category description from the lang file; the category name is always shown as a centered title above it |
| `person` | Renders the list of persons belonging to this category, sorted alphabetically and deduplicated                      |
| `role`   | Renders persons' role alongside their name                                                                          |

A single value may be given as a bare string; multiple values as an
array. Unknown values are silently ignored.

### Lang files

Location:
[`assets/gtnhcredits/lang/<locale>.lang`](src/main/resources/assets/gtnhcredits/lang/en_US.lang)

Provides display text for categories, roles, and UI labels. All keys
are optional. If no translation is provided, the raw id is used as a
fallback.

Category and role ids are sanitized to form the translation key suffix:
dots and hyphens are removed, runs of spaces collapse to `_`, and the
result is lowercased. For example `"Core Team"` → `core_team`, `"Core.Mod"` → `coremod`.

| Key pattern                             | Purpose                                                                                           |
|:----------------------------------------|:--------------------------------------------------------------------------------------------------|
| `credits.category.<id>`                 | Display name for a category                                                                       |
| `credits.category.<id>.detail`          | Description text shown when the `detail` class is set. Use `\n` to separate paragraphs.           |
| `credits.category.<id>.detail.<suffix>` | Alternative: one key per paragraph, rendered in natural (numeric-aware) sort order of the suffix. |
| `credits.person.role.<role>`            | Display name for a role                                                                           |
| `gui.credits.filter.hint`               | Placeholder text for the person filter field                                                      |

The two `detail` forms are mutually exclusive per locale: if any `detail.<suffix>` key exists, the plain `detail` key is
ignored.

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

Runs `CreditsJsonValidationTest`, which checks schema compliance,
category id uniqueness, and that every person references a defined
category. Runs automatically as part of `check` and `build`.

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
