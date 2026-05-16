# Credits Editor - Remaining Features

## Infrastructure

- [x] **CompoundCommand** (`command/impl/CompoundCommand.java`)
  Wraps a `List<Command>` as a single undo entry. `execute()` runs all children
  in order; `undo()` reverses them. Needed by BulkPersonView, ImportTsv, and
  RenameRole.

## Multi-selection support

- [x] **PersonPanel multi-selection** (`ui/panel/PersonPanel.java`)
  Switch from `SINGLE_SELECTION` to `MULTIPLE_INTERVAL_SELECTION`. Update
  `EditorView` selection callback to distinguish single vs. multi. When multiple
  persons are selected, show `BulkPersonView` in the detail panel.

## Detail views

- [x] **BulkPersonView** (`ui/detail/BulkPersonView.java`, spec 7.4)
  Detail panel card for multi-person selection. Shows "N persons selected" with
  bulk operation buttons: assign to category, add role in category, remove from
  category, delete all. Each button dispatches a `CompoundCommand`.

- [x] **DetailPanel integration** (`ui/panel/DetailPanel.java`)
  Add a `showBulkPersons(List<DocumentPerson>)` card that displays the
  `BulkPersonView`.

## Role management

- [x] **RoleEditorPanel** (`ui/roleeditor/RoleEditorPanel.java`, spec 8)
  JTable showing all roles: raw value, lang key, display name (editable),
  categories, person count. Uses `RoleIndex` as the data source. Inline display
  name editing via `MinecraftTextEditor`. Accessible from `Edit > Roles...` menu
  and from a `Roles...` button on the category panel toolbar.

- [x] **RenameRoleCommand** (`command/impl/RenameRoleCommand.java`, spec 8.2)
  Replaces all occurrences of a role string across all persons. Renames or
  removes the lang key accordingly. Wrapped as a single `CompoundCommand` undo
  entry.

- [x] **RoleRefactorDialog** (integrated into `RoleEditorPanel`, spec 8.2)
  Rename action with input dialog and confirmation showing affected person count.
  Implemented as part of RoleEditorPanel rather than as a separate dialog class.

## TSV import

- [x] **ImportTsvDialog** (`ui/dialog/ImportTsvDialog.java`, spec 9)
  JDialog with file chooser, target category dropdown, preview table, and import
  button. Preview table shows parsed TSV lines with a computed action column
  (Create / Add / Complete / No change).

- [x] **ImportTsvCommand** (compound, integrated into `ImportTsvDialog`, spec 9 + 12.2)
  Atomic undo entry built from Add/AddMembership/AddPersonRole commands
  following the merge rules in spec 9.3.

## Suggested implementation order

1. CompoundCommand
2. PersonPanel multi-selection
3. BulkPersonView + DetailPanel integration
4. RoleEditorPanel + RenameRoleCommand + RoleRefactorDialog
5. ImportTsvDialog + ImportTsvCommand

---

# Translation Editing Support

Add per-language editing for credits content (category names, descriptions,
indexed detail paragraphs, role display names). The active editing locale is
global (one selector for the whole window), defaults to the editor UI locale,
and resolves keys through a three-tier fallback chain:

1. value from the active locale's `<locale>.lang` file, if present and
   non-empty;
2. value from `en_US.lang`, if present and non-empty;
3. the existing sanitized-key fallback used when no lang entry exists.

When the active locale is not English, every Minecraft text field exposes a
small "EN" toggle (visual sibling of the existing raw/rendered `<>` toggle on
`AbstractMcEditor`). Toggling on swaps the pane to the read-only English
value and reveals a "Copy to <locale>" button next to the toggle, which seeds
the editing locale's value with the English text as a starting point.

New languages can be added and removed from the UI. English (`en_US`) is the
reference and cannot be removed.

## Phase A: Data layer (no UI change)

- [x] **A.1 Multi-locale ResourceManager** (`ResourceManager.java`)
  Replace the single `langDoc` field with `Map<String, LangDocument>` keyed
  by lang file basename (`en_US`, `fr_FR`, ...). `loadDocuments()`
  enumerates every `*.lang` under `assets/gtnhcredits/lang/` and parses each.
  Add `availableLocales()`, `langDoc(String locale)`,
  `addLocale(String locale)`, `removeLocale(String locale)`. Keep the
  no-arg `getLangDoc()` returning the English document during the transition
  so existing call sites compile unchanged.
  Done when: existing tests pass with the new API and a new unit test covers
  loading a directory containing `en_US.lang` plus `fr_FR.lang`.

- [x] **A.2 LangResolver service** (`service/LangResolver.java`)
  New read-only service constructed with a live `Map<String, LangDocument>`
  reference, no `DocumentBus` dependency (B.1 wires the bus to it later).
  Method `resolve(String key, String activeLocale)` returns
  `Optional<String>`: the active locale's value if present and non-empty;
  else the `en_US` value if present and non-empty; else `Optional.empty()`.
  Empty-string values are treated as absent at every tier so a translator
  leaving an entry blank in a non-default locale falls through. Tier-3
  placeholder selection is the caller's responsibility: each call site
  picks `cat.id`, the role string, empty UI text, or whatever fits its
  context via `Optional.orElse(...)`.
  Done when: a unit test exercises all locale tiers including the
  empty-string vs missing-key distinction, the active==default short
  circuit, and live-map updates being observed.

- [x] **A.3 Atomic multi-locale save** (`ResourceManager.writeLang()`,
  `ui/EditorSession.java`)
  Iterate over every entry in the locale map and write each `<locale>.lang`
  whose `LangDocument.isDirty()` is true. Each write merges only owned-prefix
  keys onto the destination file's existing content, so foreign keys (other
  mods', GUI strings) are preserved exactly. Removed locales have their owned
  keys stripped from the on-disk file but the file is left in place when it
  carries any foreign content. `EditorSession.save()` keeps a single entry
  point; the iteration lives inside `ResourceManager`.
  Done when: an integration test mutates two locales, calls save, reopens,
  and observes both files updated and the dirty bits cleared.

## Phase B: Bus and active-locale state

- [x] **B.1 EditingLocale on DocumentBus** (`bus/DocumentBus.java`)
  Add a private `String activeLocale` field defaulting to `"en_US"`,
  `activeLocale()` getter, `setActiveLocale(String)` setter, and a new
  `TOPIC_LOCALE` topic fired by the setter. `langDoc()` keeps its current
  meaning (English document); new code uses `langDoc(String)`.
  Done when: the bus exposes the topic and a no-op default works without any
  UI change.

- [x] **B.2 Default editing locale = UI locale** (`ui/EditorSession.java`,
  `ui/I18n.java`)
  After `ResourceManager.loadDocuments()` returns, resolve
  `Locale.getDefault()` to a lang basename. Match `language_COUNTRY` first
  (`fr_FR`), then any locale that starts with the language tag (`fr_*`),
  else fall back to `en_US`. Apply the result to `bus.setActiveLocale(...)`.
  Done when: launching with `LANG=fr_FR.UTF-8` against a resource pack that
  includes `fr_FR.lang` selects `fr_FR` automatically; launching with a
  locale not present on disk falls through to `en_US`.

## Phase C: Detail-view editor wrapper

- [x] **C.1 LocalizedMcEditor wrapper** (`ui/component/mc/LocalizedMcEditor.java`)
  Composes a `MinecraftTextEditor` or `MinecraftTextAreaEditor` with an
  extra "EN" toggle button placed in the editor's top bar next to the
  existing raw/rendered toggle. While the EN toggle is on, the pane shows
  the read-only `en_US` value (resolved through `LangResolver`) and a
  "Copy to <locale>" button appears beside the toggle. The Copy button
  loads the English value into the pending value of the editor and toggles
  EN view back off. The EN toggle and Copy button are hidden when the
  active locale is `en_US`. Reads go through `LangResolver`, writes go
  through a locale-targeted `EditFieldCommand`.
  Done when: the wrapper renders correctly in single- and multi-line modes,
  the EN toggle round-trips, and "Copy to <locale>" replaces the editing
  locale's pending value with the English source.

- [x] **C.2 CategoryDetailView locale awareness**
  (`ui/detail/CategoryDetailView.java`)
  Replace the `MinecraftTextEditor displayNameEditor` with a
  `LocalizedMcEditor`. Subscribe to `TOPIC_LOCALE` and rebuild the field's
  value from the resolver when fired.
  Done when: switching locale updates the displayed name in place without
  losing pending edits in the previously active locale.

- [x] **C.3 CategoryDescriptionSection locale awareness**
  (`ui/detail/CategoryDescriptionSection.java`,
  `ui/detail/CategoryDetailView.java`)
  Replace the section's `MinecraftTextAreaEditor` with a `LocalizedMcEditor`
  for the `credits.category.{id}.detail` field. Wire the English-value
  supplier and the active-locale setter, route writes through the active
  locale's `LangDocument`, and rebuild the field on `TOPIC_LOCALE`.
  The editor reads and writes the canonical `.detail` key only; indexed
  paragraphs (`.detail.0`, `.detail.1`, ...) are a manual-editing
  convenience normalized away inside libCredits (see L.1) so the editor
  never sees the indexed form.
  Done when: switching locale updates the displayed detail in place
  without losing pending edits in the previously active locale, and the
  EN toggle round-trips for the detail field the same way it does for
  the display name.

- [x] **L.1 libCredits detail-key normalization**
  (`libCredits/.../lang/LangDocument.java`)
  Hide the indexed-paragraph syntax from all callers. On read, a query
  for `credits.category.{id}.detail` returns the plain value joined with
  any `credits.category.{id}.detail.{n}` siblings (natural-sorted by
  suffix), separated by `\n`, as a single string. On write, `set` for
  the `.detail` key writes only the plain key and removes every
  `.detail.{n}` sibling. Different locales may legitimately use
  different paragraph counts; the indexed form remains a
  text-editor-friendly sugar in the on-disk file, never an editor or
  caller concern.
  Done when: a unit test loads a lang document mixing plain and indexed
  detail paragraphs, calls `get` and observes the joined value, calls
  `set` with a new value and observes the indexed siblings removed
  and the plain key updated, and the editor's category detail field
  reflects both forms transparently.

- [x] **C.4 RoleDetailCard locale awareness**
  (`ui/detail/RoleDetailCard.java`, `ui/detail/MembershipRolePanel.java`)
  Same change for the role display-name editor inside the role detail card.
  Done when: editing a role's display name in `fr_FR` does not touch the
  `en_US` value, and the EN toggle exposes the English copy as expected.

## Phase D: Locale picker UI

- [x] **D.1 LocaleSelector component** (`ui/component/LocaleSelector.java`)
  Combo box that reads `ResourceManager.availableLocales()` and renders
  human labels combining the locale tag and `Locale.getDisplayLanguage` of
  the editor UI locale (e.g. `fr_FR -- French`). Selecting an entry calls
  `bus.setActiveLocale(...)`.
  Done when: opening a resource pack populates the combo box with all
  detected locales and selecting one fires `TOPIC_LOCALE`.

- [x] **D.2 Toolbar integration** (`ui/EditorView.java`)
  Place the `LocaleSelector` at the right edge of the existing top toolbar,
  near the Save button. Width budgeted via `UiMetrics`. Disabled when no
  session is loaded.
  Done when: the selector is visible at all times when a session is loaded
  and disabled otherwise.

- [x] **D.3 Add-language flow** (`ui/dialog/AddLocaleDialog.java`,
  `ui/EditorActions.java`, `ui/EditorMenuBar.java`)
  Action wired into the Edit menu and an inline "+" button on the
  `LocaleSelector`. Opens a dialog asking for a Minecraft lang code,
  validated against `[a-z]{2}_[A-Z]{2}` and a known-locale list. Calls
  `ResourceManager.addLocale(code)`, sets the active locale to the new
  one, and fires `TOPIC_LOCALE`. The new file is created on disk on the
  next save.
  Done when: adding `de_DE` produces an empty `de_DE.lang` on save and the
  selector immediately switches to it.

- [ ] **D.4 Remove-language flow** (`ui/EditorActions.java`,
  `ui/component/LocaleSelector.java`)
  Trash-can button on the `LocaleSelector`. Confirm dialog warning that the
  file will be deleted on the next save. Refuses removal of `en_US`. The
  active locale falls back to `en_US` if the removed locale was active.
  Done when: removing `fr_FR` deletes the file on save and the selector
  switches to `en_US`.

## Phase E: Commands

- [ ] **E.1 Locale-targeted EditFieldCommand**
  (`command/impl/EditFieldCommand.java`)
  Add a `String locale` field. Every detail-view write routes through it.
  Undo and redo restore the locale-specific value. Existing call sites
  pass the bus's `activeLocale()` at command-creation time.
  Done when: editing a category name in `fr_FR`, undoing, redoing, and
  switching locales preserves all values across both locales correctly.

- [ ] **E.2 AddLocaleCommand and RemoveLocaleCommand**
  (`command/impl/AddLocaleCommand.java`,
  `command/impl/RemoveLocaleCommand.java`)
  Undoable wrappers around `ResourceManager.addLocale` and `removeLocale`.
  `RemoveLocaleCommand` keeps the in-memory `LangDocument` so undo restores
  it verbatim.
  Done when: `Ctrl+Z` after removing a locale restores it with every key
  and value intact.

## Phase F: Tests, polish, i18n

- [ ] **F.1 LangResolverTest** (`src/test/.../service/LangResolverTest.java`)
  One case per fallback-tier transition (active hit, active empty -> EN
  hit, both empty -> sanitized key, missing key entirely).
  Done when: the test class runs headlessly and every tier is covered.

- [ ] **F.2 Multi-locale ResourceManagerTest**
  (`src/test/.../ResourceManagerTest.java`)
  Cover load with two locales, add a third, remove one, save round-trip,
  reload round-trip.
  Done when: every API path on the new `ResourceManager` surface is
  exercised at least once.

- [ ] **F.3 Title bar shows active locale** (`ui/MainWindow.java`)
  Append ` [fr_FR]` to the window title when the active locale is not
  `en_US`. Updates on `TOPIC_LOCALE`.
  Done when: switching locale updates the title without restarting the
  editor.

- [ ] **F.4 i18n strings for new UI**
  (`src/main/resources/messages_*.properties`)
  Add every new user-visible string (locale selector tooltip, EN-toggle
  label, "Copy to <locale>" button, add-locale dialog, remove-locale
  confirm) to `messages_en.properties` and the nine sibling locales
  already present.
  Done when: grep for hardcoded English strings in the new files returns
  nothing.

## Suggested implementation order

A.1 -> A.2 -> A.3 -> B.1 -> B.2 -> C.1 -> C.2 -> C.3 -> C.4 -> D.1 -> D.2 ->
D.3 -> D.4 -> E.1 -> E.2 -> F.1 -> F.2 -> F.3 -> F.4.

One commit per task. Run `./gradlew spotlessApply :creditsEditor:test
:creditsEditor:b
uild` before each commit.
