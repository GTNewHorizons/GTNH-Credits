# Credits Editor - Specification

## 1. Overview

The Credit Editor is a standalone Swing application for creating and maintaining the
`credits.json` and `lang/en_US.lang` files of a GTNH Credits resource pack. It targets
team editors who work with data arriving from varied and partially structured sources.

**In scope for this version:**
- Editing `credits.json` (schema version 2)
- Editing `lang/en_US.lang` (reference language)
- TSV bulk import of persons
- Undo/redo history

**Out of scope (future versions):**
- Direct Discord / GitHub API connections
- Reading `mod.mcmeta` files
- Editing lang files other than `en_US.lang`


## 2. Data Model

### 2.1 Relationship overview

```
Person (1)
  └── category entry (0..n)
        ├── category id  →  Category
        └── roles (0..n)  →  Role
```

One person can belong to zero or more categories. Within each category membership,
the person can carry zero or more roles. Roles are scoped to a category membership,
not to the person globally.

### 2.2 Categories

| Property     | Type        | Source       | Notes                                |
|--------------|-------------|--------------|--------------------------------------|
| `id`         | key string  | credits.json | 1-32 chars, letter start/end         |
| `class`      | string list | credits.json | subset of `person`, `role`, `detail` |
| display name | string      | lang         | key `credits.category.{key}`         |
| description  | string      | lang         | key `credits.category.{key}.detail`  |

The `description` field uses paragraphs separated by `\n`. The editor always writes this
syntax (not the indexed `.detail.N` syntax).

The lang key is derived from `id` by sanitization: lowercase, replace every non-alphanumeric
character with `_`, collapse consecutive `_` into one.

### 2.3 Persons

| Property   | Type                       | Source       | Notes                            |
|------------|----------------------------|--------------|----------------------------------|
| `name`     | string                     | credits.json | May contain Minecraft `§x` codes |
| `category` | list of membership entries | credits.json | Ordered; see relationship above  |

Each membership entry is a `(catId, list of roles)` pair.

### 2.4 Roles

Roles are free-form strings associated with a person within a specific category membership.
They do not exist as independent entities in the JSON, but the editor manages them globally
to support reuse and refactoring.

| Property     | Type   | Source       | Notes                                 |
|--------------|--------|--------------|---------------------------------------|
| raw value    | string | credits.json | As stored in the JSON                 |
| lang key     | string | derived      | `credits.person.role.{sanitized}`     |
| display name | string | lang         | Optional; falls back to derived value |

The sanitization algorithm is the same as for category ids.


## 3. Resource Management

The existing `ResourceManager` handles file access (directory or zip).
GUI behaviors:

| Action                | Behaviour                                                         |
|-----------------------|-------------------------------------------------------------------|
| File > Open Resources | File/directory chooser dialog                                     |
| File > New Resources  | Creation dialog (name and type: directory or .zip)                |
| File > Save Resources | Writes `credits.json` + `lang/en_US.lang`; greyed out when clean  |
| Window close          | If unsaved changes: confirmation dialog (Save / Discard / Cancel) |

The dirty indicator appears in the window title: `GTNH Credits Editor - name [*]`.


## 4. Main Window Layout

```
+------------------+-----------------------------------+---------------------+
|  Categories      |  Persons                          |  Detail             |
|  (left panel)    |  (centre panel)                   |  (right panel)      |
|                  |  [filter...] [role v] [Select all]|                     |
|  [+] [-] [Roles] |  +-------------------------------+|  Contextual:        |
|  > team          |  | name (rendered) | memberships ||  - Selected         |
|  > dev           |  | ...             | ...         ||    category         |
|  > contrib       |  | ...             | ...         ||  - Selected         |
|  > support       |  +-------------------------------+|    person(s)        |
|  > thanks        |  [Import TSV]  [Add person]       |                     |
+------------------+-----------------------------------+---------------------+
```

The **memberships** column adapts to the current category filter:

- **"All persons" selected:** shows all category memberships as a compact summary,
  e.g. `dev (gtnh-creator, core-mods), contrib`.
- **Specific category selected:** shows only the roles the person holds in that category,
  e.g. `gtnh-creator, core-mods`; persons not in the category are hidden.

**Default proportions:** left 20%, center 50%, right 30%. All panes are resizable via `JSplitPane`.

**Menu bar:**

```
File        Edit
  Open        Roles...
  New         --------
  -----       Undo  Ctrl+Z
  Save        Redo  Ctrl+Y
  -----
  Quit
```


## 5. Categories Panel (left)

### 5.1 Category List

The list reflects the ordered array of categories in `credits.json`. Order here directly
controls the in-game display order.

Each entry renders the category's **display name** (lang) using the `MinecraftTextRenderer`
(WYSIWYG, see section 10). The lang key suffix (e.g. `team`) is shown in small gray text
below the rendered name. If no lang entry exists for the name, the raw id is shown instead.

The top of the list always contains a special **"All persons"** entry that is not part of
the data model and cannot be moved or deleted.

### 5.2 Reordering

The list supports two equivalent reorder mechanisms for the selected category:

- **Drag-and-drop** within the list.
- **Up / Down buttons** in the toolbar below the list, for keyboard-driven precision.

Both produce the same `MoveCategoryOrder` undo entry.

### 5.3 Toolbar (bottom of a panel)

```
[+]  [-]  [^]  [v]  [Roles...]
```

| Button     | Effect                                                     |
|------------|------------------------------------------------------------|
| `+`        | Creates a new category; opens its form in the detail panel |
| `-`        | Deletes the selected category (see 5.4)                    |
| `^`        | Moves selected category one position up                    |
| `v`        | Moves selected category one position down                  |
| `Roles...` | Opens the role editor (section 8)                          |

Right-clicking on a list entry also provides: Duplicate, Delete.

### 5.4 Category Deletion

If any persons have entries in this category, a warning dialog appears:
`"N person(s) have entries in this category. Delete anyway?"`.
Deletion removes the category from all affected persons.


## 6. Persons Panel (center)

### 6.1 Filter Bar

```
[ Name...        ] [ Role v ] [ Select all ]
```

- **Text field:** real-time filter on name (case-insensitive match).
- **Role dropdown:** filters to persons having a given role in any category.
  Values: `(All roles)` + alphabetically sorted list of existing roles.
- **"Select all" button:** selects every entry currently visible in the list.
- Filters are combinatorial: name AND role AND category (chosen in the left panel).

### 6.2 Person List

Two-column `JTable`:

| Column      | "All persons" view                       | Specific category view              |
|-------------|------------------------------------------|-------------------------------------|
| Name        | Minecraft-rendered name                  | Minecraft-rendered name             |
| Memberships | All `category (role, role, ...)` entries | Roles in the selected category only |

Each person appears once in the list regardless of how many categories they belong to.
When a specific category is selected, persons with no membership in that category are hidden.

Behaviours:
- Column header click to sort.
- Multi-selection: `Ctrl+click` (toggle), `Shift+click` (range), `Ctrl+A` (all visible).
- Drag-and-drop: the selection can be dragged onto a category in the left panel to add
  all selected persons to that category (with no roles; roles can be assigned afterward).

### 6.3 Actions

| Element                 | Location        | Effect                                    |
|-------------------------|-----------------|-------------------------------------------|
| `Add person`            | Bottom of panel | Creates a new person and selects it       |
| `Import TSV`            | Bottom of panel | Opens the import dialog (section 9)       |
| Right-click (selection) | List            | Context menu (section 6.4)                |
| `Delete` key            | Focused list    | Deletes selected person(s) (confirmation) |

### 6.4 Context Menu

- Assign to category... (dropdown of categories; adds membership with no roles)
- Remove from the category... (dropdown of categories where they currently appear)
- Add a role in a category... (pick category first, then role with autocomplete)
- Remove role... (list of roles common to the entire selection, across all their categories)
- Delete person(s)

When multiple persons are selected, all operations apply to every person in the selection.


## 7. Detail Panel (right, contextual)

Content adapts to the current selection.

### 7.1 No Selection / "All Persons"

Empty panel with a hint label.

### 7.2 Category Selected

```
ID           : [team                    ]  (plain text, read-only after creation)
Name (en_US) : [ Core Team          [<>] ]  <- MinecraftTextEditor
Classes      : [person] [role] [detail]      <- toggle buttons

Description (en_US):                         <- MinecraftTextAreaEditor
+------------------------------------------+
| The core team is responsible for...  [<>]|
| Their work spans...                      |
+------------------------------------------+
(each line in the editor = one paragraph)
```

- **ID** is read-only after creation. Renaming an ID is a refactoring operation with lang
  key and reference impact; deferred to a future version or via explicit confirmation dialog.
- **Name** uses `MinecraftTextEditor` (single-line, section 10.2). The lang key suffix
  (e.g. `credits.category.team`) is shown as non-editable gray text below the field.
- **Toggle buttons** for classes: pressed = class present in the JSON.
  If `role` is active without `person`, an inline warning is shown.
- **Description** uses `MinecraftTextAreaEditor` (multi-line, section 10.3). It is only
  visible when the `detail` class is active. The `[<>]` toggle is at the top-right of
  the area and switches the entire area between raw and rendered mode.
- Every change is applied to the model immediately (no explicit Apply button) and pushed
  onto the undo stack.

### 7.3 Single Person Selected

The person has one name and a list of category memberships, each holding its own roles.
The roles within a membership also use `MinecraftTextEditor` for their display names
(rendered in read-only context via `MinecraftTextRenderer`).

```
Name:  [ DreamMaster XXL          [<>] ]  <- MinecraftTextEditor
       (raw: §cDreamMaster§f§lXXL)         <- shown when in raw mode

Category memberships:
  team   :  gtnh-creator  [x]
  dev    :  gtnh-creator  [x]
            [+ add role to dev]
  [+ add to category]
```

- The name field is a `MinecraftTextEditor`; `[<>]` toggles raw/rendered mode.
- Roles are shown via `MinecraftTextRenderer` (their display name from lang, or derived).
- Each role entry has an `[x]` button to remove that role from this membership.
- Each membership line has an `[x]` button to remove the entire category membership.
- `[+ add role to dev]` adds a role to the dev membership (autocomplete from existing
  roles in that category across all persons).
- `[+ add to category]` creates a new category membership (dropdown of categories
  this person does not already belong to).

### 7.4 Multiple Persons Selected

```
N persons selected

Bulk operations:
  [Assign to category...]
  [Add role in category...]
  [Remove from category...]
  [Delete all]
```

"Add role in category..." first asks for the target category, then the role to add.
Only persons already in the target category receive the role; others are skipped with
a summary at the end.


## 8. Role Editor

Opened via `Edit > Roles...` or the `Roles...` button at the bottom of the left panel.
Opens as a side panel (replacing the detail panel) or as a non-modal window.

### 8.1 Main View

`JTable` with columns:

| Column       | Content                                       | Editable    |
|--------------|-----------------------------------------------|-------------|
| Role (raw)   | Value as stored in the JSON                   | No          |
| Lang key     | `credits.person.role.{sanitized}`             | No          |
| Display name | Value from lang (or derived if absent)        | Yes, inline |
| Categories   | List of category ids that reference this role | No          |
| Persons      | Number of persons carrying this role          | No          |

- Column-sortable; default sort: alphabetical on "Role (raw)".
- Alphabetical sort helps identify duplicates and near-miss spellings.

### 8.2 Role Refactoring

1. Select one or more rows in the table.
2. Right-click > **"Rename to..."** (or dedicated button).
3. Dialog: `Rename [source-role] to:` with a text field and autocomplete of existing roles.
4. Confirmation: `"N person(s) will be updated. Continue?"`.
5. Effect:
   - All occurrences of the source role in the JSON are replaced with the target role.
   - If the target role did not exist, the source lang key is renamed to the target lang key.
   - If the target role already existed, the source lang key is removed (target has its own).
   - The entire operation is a single undo entry.

### 8.3 Editing the Display Name (lang)

The "Display name" column uses `MinecraftTextRenderer` in read-only mode. Double-click
activates an inline `MinecraftTextEditor` (section 10.2) in that cell, because role
display names can contain `§` formatting codes just like person names.

If the value is left empty, the lang key is deleted (display falls back to the derived value).


## 9. TSV Import

### 9.1 File Format

```
PersonName\tRole1\tRole2\t...
```

- First column: person name (required).
- Subsequent columns: roles within the target category (optional, variable count per line).
- Encoding: UTF-8, tab separator.
- Empty lines and lines starting with `#` are ignored.

### 9.2 Import Dialog

1. **File chooser** (or path input field).
2. **Target category:** dropdown of existing categories (required).
   All imported roles are placed under this category for each person.
3. **Preview table:** shows each TSV line with an "Action" column:
   - `Create` - person does not exist yet.
   - `Add` - person exists; the category membership will be created with its roles.
   - `Complete` - person already in this category; only the missing roles will be added.
   - `No change` - everything already present.
4. **Import button:** applies all changes. The entire import is a single undo entry.

### 9.3 Merge Rules

- Person does not exist: create with name and `{category: roles}` entry.
- Person exists but not in the target category: add the category membership with roles.
- Person exists in the target category: add only the missing roles to that membership.
- Roles already present for this person in this category are not duplicated.


## 10. Formatted Text Components

The editor is a **standalone Swing application** with no Minecraft or MinecraftForge
on its classpath. There is no `EnumChatFormatting`, no Minecraft renderer, no Minecraft
font. Every aspect of `§` code handling must be implemented from scratch in pure Java.

Minecraft console formatting codes (`§x`) may appear in virtually every user-visible
string in the data model:

- Category display name (`credits.category.{key}`)
- Category description / detail paragraphs (`credits.category.{key}.detail`)
- Person name
- Role display name (`credits.person.role.{sanitized}`)

Three reusable parts, all in `ui/component/`, handle display and editing of these
strings. They depend only on `java.*`, `javax.swing.*`, and `java.awt.*`.

### 10.1 McFormatCode (parser, internal)

A small value type and parser that splits a raw string into a list of styled segments.
Each segment carries a character range and the active formatting state at that position
(foreground color, bold, italic, underline, strikethrough, obfuscated).

This is the shared parsing logic used by all three parts. It has no Swing dependency
and can be unit-tested independently.

Supported codes and their effect on rendering state:

| Code        | Effect                                    |
|-------------|-------------------------------------------|
| `§0` - `§9` | Set foreground colour (Minecraft palette) |
| `§a` - `§f` | Set foreground colour (Minecraft palette) |
| `§l`        | Bold on                                   |
| `§o`        | Italic on                                 |
| `§n`        | Underline on                              |
| `§m`        | Strikethrough on                          |
| `§k`        | Obfuscated on (random character cycling)  |
| `§r`        | Reset all formatting to default           |

The 16-color Minecraft palette is defined as a static lookup table of `java.awt.Color`
constants inside `McFormatCode`. No Minecraft class is referenced.

### 10.2 MinecraftTextRenderer (read-only)

A `JComponent` that receives a raw string, parses it with `McFormatCode`, and paints
each segment using Java2D (`Graphics2D`) with the appropriate `Font` and `Color`.

- Bold, italic, underline, and strikethrough are composed of the standard `Font` API
  and `Graphics2D.drawLine`.
- Obfuscation (`§k`): a `javax.swing.Timer` fires every ~100 ms and replaces each
  obfuscated character with a random character of the same width from the same font.
  The timer is started when the component becomes visible and stopped when hidden, to
  avoid background CPU usage.
- Used as a list/table cell renderer and in preview labels throughout the UI.

### 10.3 MinecraftTextEditor (editable, single-line)

An editable field for single-line strings (person names, category display names, role
display names). Implemented as a `JPanel` containing:

- A `JTextField` (raw mode, always present as the data source)
- A `MinecraftTextRenderer` (rendered mode)
- A `[<>]` / `[Aa]` toggle button

**Raw mode:** the `JTextField` is visible; the user types `§` codes directly.

**Rendered mode:** the `MinecraftTextRenderer` is visible; the `JTextField` is hidden
but remains the source of truth and receives key events forwarded from the panel.

Both views stay in sync on every keystroke. Default mode: **Rendered**.

### 10.4 MinecraftTextAreaEditor (editable, multi-line)

An editable area for multi-paragraph content (category descriptions). Each line in the
editor is one paragraph. Paragraphs are serialized with `\n` as separator.

**Raw mode:** a `JTextArea` with `§` codes typed literally. Each newline = one paragraph.

**Rendered mode:** a scrollable `JPanel` containing one `MinecraftTextRenderer` per
paragraph, rebuilt on each edit event.

The same `[<>]` / `[Aa]` toggle switches modes. Default mode: **Rendered**.

### 10.5 List and table cell rendering

In read-only list and table cells (category names, person names, role display names),
the cell renderer is a `MinecraftTextRenderer`. Lang key suffixes shown as secondary
text (e.g., the category id below the display name) are plain `String` painted without
any formatting-code parsing.


## 11. Lang File Management

### 11.1 File structure and ownership

The `en_US.lang` file belongs to the broader GTNH Credits resource pack and contains keys
from multiple concerns: GUI strings (`gui.*`), credits categories, credit detail text,
person roles, and possibly others added in the future. The editor owns only a specific
subset of keys.

The file is a structured text document, not a flat map. It contains:
- **Key-value lines:** `key=value`
- **Comment lines:** starting with `#`
- **Blank lines:** used to separate logical sections

All three must be preserved on save. The editor must not reorder, merge, or reformat
sections it does not own.

### 11.2 Keys owned by the editor

The editor reads and writes only these key prefixes:

| Key prefix                        | Managed by                            |
|-----------------------------------|---------------------------------------|
| `credits.category.{key}`          | "Name" field in category detail       |
| `credits.category.{key}.detail`   | "Description" area in category detail |
| `credits.person.role.{sanitized}` | "Display name" column in role editor  |

All other keys (including `gui.*` and any other prefix) are treated as read-only foreign
content. The editor must never alter, remove, or reorder them.

### 11.3 Section grouping on writing

Editor-owned keys must be written in coherent groups, separated from foreign content by
blank lines. The expected layout in the file is:

```
# --- GUI strings (foreign, untouched) ---
gui.credits.title=Credits
...

# --- Category names and details (editor-owned) ---
credits.category.team=Core Team
credits.category.team.detail=The core team is responsible for...

credits.category.dev=Developers
credits.category.dev.detail=...

# --- Role display names (editor-owned) ---
credits.person.role.dev=Developer
credits.person.role.gtnh_creator=§6§lGTNH §f§lCreator
```

When the editor writes the file, it must:
1. Output all foreign lines (key-value, comment, blank) exactly as read, in their
   original order.
2. Replace the content of its own sections in place. If no editor-owned section exists
   yet, append it at the end, preceded by a blank line and an identifying comment.
3. Within each editor-owned section, maintain one blank line between per-category groups
   (name and detail together).
4. Remove orphaned editor-owned keys (for deleted categories or roles) without leaving
   stray blank lines.

### 11.4 Key sanitization

`KeySanitizer` in the editor implements this algorithm independently (no dependency on
the main mod). The algorithm must produce the same output as the mod does at runtime so
that the game finds lang keys written by the editor:

1. Delete all `.` (dot) characters.
2. Delete all `-` (hyphen) characters.
3. Replace runs of one or more spaces with a single `_`.
4. Lowercase the result.

Examples: category id `"Core.Mod-Team"` -> lang key suffix `coremodteam`;
role `"gtnh-creator"` -> `gtnhcreator`.


## 12. Undo / Redo

Implemented with the **Command pattern**.

### 12.1 Atomic Commands

| Command           | Undo                           |
|-------------------|--------------------------------|
| AddCategory       | RemoveCategory                 |
| RemoveCategory    | RestoreCategory (with persons) |
| EditCategoryField | Restore previous value         |
| MoveCategoryOrder | Restore previous position      |
| AddPerson         | RemovePerson                   |
| RemovePerson      | RestorePerson                  |
| EditPersonName    | Restore previous name          |
| AddMembership     | RemoveMembership               |
| RemoveMembership  | AddMembership (with roles)     |
| AddPersonRole     | RemovePersonRole               |
| RemovePersonRole  | AddPersonRole                  |
| RenameRole        | Reverse the rename             |
| EditRoleLang      | Restore previous value         |

### 12.2 Compound Commands

- **ImportTSV:** atomic group of multiple Add/AddMembership/AddPersonRole commands.
- **BulkAssign:** atomic group for batch operations on a multi-selection.

### 12.3 Behaviour

- Shortcuts: `Ctrl+Z` (undo), `Ctrl+Y` or `Ctrl+Shift+Z` (redo).
- `Edit > Undo` and `Edit > Redo` menu items display the command name:
  `Undo: Assign to category dev` / `Redo: Import TSV (23 persons)`.
- The undo stack is cleared when a new resource is opened.
- After a save, a "clean marker" is placed at the current stack position.
  Undoing below this marker re-shows `[*]` in the title (modified relative to disk).


## 13. Validation

### 13.1 Real-time Validation (on input)

| Field       | Constraint                               | Indicator     |
|-------------|------------------------------------------|---------------|
| Category ID | Key format: letter start/end, 1-32 chars | Red border    |
| Category ID | Unique within the file                   | Orange border |
| Person name | Non-empty                                | Red border    |

### 13.2 Validation on Save

If blocking errors exist, the save is refused and a dialog lists all issues.
Warnings (duplicate persons, categories without persons) are shown but do not block saving.


## 14. Keyboard Shortcuts

| Shortcut       | Action                       |
|----------------|------------------------------|
| `Ctrl+S`       | Save                         |
| `Ctrl+O`       | Open resources               |
| `Ctrl+Z`       | Undo                         |
| `Ctrl+Y`       | Redo                         |
| `Ctrl+Shift+Z` | Redo (alternative)           |
| `Ctrl+A`       | Select all (in focused list) |
| `Delete`       | Delete selection             |
| `F2`           | Rename / edit selected item  |
| `Escape`       | Cancel current input         |


## 15. Package Architecture

Responsibilities are split strictly across packages. No package may depend on a package
that sits above it in the hierarchy below. UI packages must not contain logic; service
packages must not contain Swing imports.

### 15.1 libCredits subproject (extended)

`libCredits/` is a dependency shared between the Minecraft mod and the editor.
It has **zero dependency on Minecraft or MinecraftForge** and must stay that way.
It depends only on Gson and the Java standard library.

Existing:
- `net.noiraude.libcredits.model` -- immutable data model (`CreditsData`, `CreditsCategory`,
  `CreditsPerson`). The immutable contract is intentional and must not change.
- `net.noiraude.libcredits.parser` -- `CreditsParser` reads an `InputStream` into
  `CreditsData`; throws `CreditsParseException` for any structural violation.
- `net.noiraude.libcredits.util` -- `FuzzyFinder`.

To add:
- `net.noiraude.libcredits.serializer` -- `CreditsSerializer` writes a `CreditsData` to
  an `OutputStream`. It is the only authority on JSON output format. It throws
  `IOException` on write failure.

`libCredits` is the sole authority on schema conformance. The editor must not
re-implement validation; it delegates to `CreditsParser` (round-trip check on save) and
treats any thrown exception as a blocking save error.

### 15.2 creditsEditor subproject packages

```
net.noiraude.creditseditor
│
├── CreditsEditorApp              entry point, wires everything together
├── ResourceManager               file-system abstraction (directory / zip)
│
├── model/                        MUTABLE editor model (not libcredits model)
│   ├── EditorModel               root: ordered list of EditorCategory + list of EditorPerson
│   ├── EditorCategory            mutable: id, display name (raw), description (raw), classes
│   ├── EditorPerson              mutable: name (raw), ordered list of EditorMembership
│   └── EditorMembership          mutable: category id reference, ordered list of role strings
│
├── service/                      pure logic, no Swing
│   ├── CreditsService            load/save: converts EditorModel <-> libcredits CreditsData
│   │                             via CreditsParser and CreditsSerializer; propagates exceptions
│   ├── LangService               load/save en_US.lang; preserves foreign lines exactly;
│   │                             exposes typed read/write for editor-owned keys only
│   ├── KeySanitizer              standalone reimplementation of the sanitization algorithm
│   └── RoleIndex                 derived view: all roles across all persons, with usage counts
│
├── command/                      undo/redo infrastructure
│   ├── Command                   interface: execute(), undo(), getDisplayName()
│   ├── CommandStack              undo/redo stack; tracks dirty state vs. last save point
│   └── impl/                    one class per atomic command (see section 12.1)
│
└── ui/                           all Swing code
    ├── MainWindow                JFrame, menu bar, split-pane layout
    │
    ├── component/                reusable painted components
    │   ├── MinecraftTextRenderer read-only JComponent (list/table cells, previews)
    │   ├── MinecraftTextEditor   single-line editable field with raw/rendered toggle
    │   └── MinecraftTextAreaEditor  multi-line editable area with raw/rendered toggle
    │
    ├── panel/                    the three main panels of the window
    │   ├── CategoryPanel         left: list + toolbar
    │   ├── PersonPanel           centre: filter bar + table
    │   └── DetailPanel           right: context-switching container
    │
    ├── detail/                   views shown inside DetailPanel
    │   ├── CategoryDetailView    category form (id, name, classes, description)
    │   ├── PersonDetailView      single-person form (name, memberships)
    │   └── BulkPersonView        bulk-operation buttons for multi-selection
    │
    ├── roleeditor/
    │   └── RoleEditorPanel       role table + refactor action
    │
    └── dialog/
        ├── ImportTsvDialog       file chooser, target category, preview, import
        └── RoleRefactorDialog    rename-to input + confirmation
```

### 15.3 Dependency rules

```
ui  -->  command  -->  model
ui  -->  service  -->  model
ui  -->  service  -->  libcredits (CreditsData, CreditsParser, CreditsSerializer)
service  -->  ResourceManager
command  -->  model   (commands mutate EditorModel directly)
command  -->  service (CompoundImportTsvCommand calls CreditsService / LangService)
```

Forbidden:
- `service` must not import any `javax.swing.*`.
- `model` must not import anything outside `java.*` and `javax.annotation.*`.
- `command` must not import any `javax.swing.*`.
- `ui` communicates with services and the command stack only; it never mutates
  `EditorModel` fields directly.

### 15.4 Data flow on a load

```
ResourceManager.openRead("credits.json")
  --> CreditsParser.parse(InputStream)        [libcredits]
  --> CreditsService.toEditorModel(CreditsData)
  --> EditorModel (mutable, held in memory)

ResourceManager.openRead("lang/en_US.lang")
  --> LangService.load(InputStream)
  --> LangModel (editor-owned key map + foreign lines preserved)
```

### 15.5 Data flow on save

```
EditorModel
  --> CreditsService.toCreditsData(EditorModel)   [build immutable CreditsData]
  --> CreditsParser.parse(serialize then reparse)  [round-trip validation]
  --> CreditsSerializer.write(CreditsData, OutputStream)  [libcredits]
  --> ResourceManager.openWrite("credits.json")

LangModel
  --> LangService.write(LangModel, OutputStream)
  --> ResourceManager.openWrite("lang/en_US.lang")
```

If `CreditsParser` or `CreditsSerializer` throws, the save is aborted and the error
is surfaced to the user. No partial writes occur (write to temp, then rename, or write
to an in-memory buffer before committing).


## 16. Implementation Notes (to be refined)

The following points will be specified during implementation:

- **Name column rendering in JTable:** custom `MinecraftTextRenderer` for read-only display
  in the person list.
- **DnD between JList and JTable:** precise `TransferHandler` implementation.
- **Role autocomplete:** editable `JComboBox` or popup on `JTextField`.
- **Obfuscation animation** (`§k`): Swing timer at a short interval (~100 ms), active only
  when the component is visible.
- **credits.json serialization format:** compact vs. pretty-print, key ordering.
- **Person order in the JSON:** does the editor preserve insertion order or sort?
