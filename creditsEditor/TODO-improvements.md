# Credits Editor: Code Health Improvements

Each task is sized to be one focused PR or commit. Pick the next unchecked item,
implement it, verify, check it off, and commit. Do not batch unrelated items.

Priority order (top to bottom). Within a priority block items are independent
and can be taken in any order.

Running verification for every task:
- `./gradlew spotlessApply :creditsEditor:test :creditsEditor:build` stays green
- `./gradlew :creditsEditor:run` still opens, edits, undoes, and saves a credits.json

---

## P1 Correctness and error handling

- [x] **1.1 Replace silent `catch (IOException ignored)` in EditorSession**
  File: `src/main/java/net/noiraude/creditseditor/ui/EditorSession.java:128`
  Current: resource close failure is swallowed.
  Action: use `System.getLogger(EditorSession.class.getName()).log(WARNING, "...", ex)`. Do not surface a dialog (close-time noise is annoying), just log.
  Done when: the catch contains a logger call and no `ignored` variable.

- [x] **1.2 Replace silent catches in AppIcons**
  File: `src/main/java/net/noiraude/creditseditor/ui/AppIcons.java:32,47`
  Action: log at WARNING level. Icon load failures should not be invisible.
  Done when: both catches log and name their exception.

- [x] **1.3 Replace silent `BadLocationException` catches in McWysiwygPane**
  File: `src/main/java/net/noiraude/creditseditor/ui/component/McWysiwygPane.java:161,180,315,376,415`
  Action: log at WARNING. These indicate document-model invariant violations and should be visible in diagnostics.
  Done when: five catches each have a logger call with distinct messages identifying the call site.

- [x] **1.4 Add ErrorPresenter helper and stop leaking `ex.getMessage()` in MainWindow**
  File: `src/main/java/net/noiraude/creditseditor/ui/MainWindow.java:102,105,140`
  Current: raw `ex.getMessage()` is concatenated into user dialogs; may leak paths or class names.
  Action: create `ui/ErrorPresenter.java` that maps known exception types (IOException, JsonParseException, etc.) to friendly strings, with a "Details..." toggle to reveal raw text.
  Done when: MainWindow calls `ErrorPresenter.show(parent, title, ex)` for every error path.

- [x] **1.5 Null-safe `getCause().getMessage()` in TsvPreviewController**
  File: `src/main/java/net/noiraude/creditseditor/ui/dialog/TsvPreviewController.java:99-103`
  Action: fall back to `ex.getMessage()` when `getCause()` is null.
  Done when: no NPE possible; add a unit test using a fresh `ExecutionException` with null cause.

- [x] **1.6 Add EDT assertions to Swing mutation sites**
  Files: `ui/dialog/TsvPreviewController.java`, `ui/EditorView.java`, `ui/panel/DetailPanel.java`
  Action: add `assert SwingUtilities.isEventDispatchThread()` at the top of each public mutation method. Enable `-ea` in the `run` gradle task so dev runs fail loudly on EDT violations.
  Done when: assertion present in every Swing-touching entry point, and the `run` task passes `-ea`.

- [x] **1.7 Fix McFormatToolbar stale-read on caret move**
  Files: `ui/component/McWysiwygPane.java`, `ui/component/McFormatToolbar.java`
  Discovered while writing task 3.5 (McFormatToolbarTest): `McFormatToolbar.connectTo` adds its `CaretListener` after `McWysiwygPane`'s own `syncPendingFromCaret` listener. Swing fires `CaretListener`s in reverse insertion order, so the toolbar reads `pendingCodes` before the pane has updated it. Result: the toolbar display lags one caret move behind in production.
  Action: either fire `syncPendingFromCaret` before installing the public caret listener API (e.g., do the sync eagerly inside `addCaretListener` override or expose a direct sync entry point the toolbar calls first), or expose a method on `McWysiwygPane` that returns the caret style by reading the document directly rather than via the cached `pendingCodes`.
  Done when: a headless test places the caret, then calls `connectToolbar`, then moves the caret once more; the resulting modifier button state matches the post-move run without any workaround nudge.

## P2 Test coverage

Current: 11 test files for 59 production classes. Zero UI tests.

- [x] **2.1 EditorSessionTest**
  Cover: dirty-flag transitions on edits and saves; new-file-from-nonexistent-zip path; save-failure does not clear dirty flag.
  Done when: test class exists with at least 4 cases and passes.

- [x] **2.2 ResourceManagerTest**
  Cover: directory mode opens missing `assets/gtnhcredits/lang/` without throwing; zip mode same; non-existent `.zip` path creates a pack with correct `pack.mcmeta` for MC 1.7.10 (pack_format=1).
  Done when: test class covers both modes.

- [x] **2.3 McWysiwygPaneTest**
  Cover: §-code round-trip against `McText`; pending-codes carry when caret is between styled runs; caret position preserved after style toggle.
  Done when: at least 3 cases pass headlessly.

- [x] **2.4 EditorMenuBarTest**
  Cover: undo/redo/save enablement reflects `CommandStack` state and session dirty flag.
  Done when: test mutates state and asserts `JMenuItem.isEnabled()` matches expectation.

## P3 Refactoring

- [x] **3.1 Extract ListReorderHelper**
  Files: `command/impl/MoveCategoriesOrderCommand.java` (69 lines), `command/impl/MoveRolesOrderCommand.java` (74 lines)
  ~60% of the extract-and-reinsert logic is duplicated. Create `command/impl/ListReorderHelper.java` with a static method that takes indices and a mutable list.
  Done when: both commands delegate and shrink by roughly a third, existing command tests still pass.

- [ ] **3.2 Split MembershipRolePanel (540 lines)**
  File: `ui/detail/MembershipRolePanel.java`
  Split into `RoleListPanel` (list + toolbar) and `RoleDetailCard` (right card). Parent composes the two and mediates selection.
  Note: `MembershipTablePanelTest` (task 3.3, formerly 2.6) must target the post-split class structure rather than the pre-split monolith.
  Done when: each new class is under 300 lines and behavior is unchanged.

- [ ] **3.3 MembershipTablePanelTest** (formerly 2.6)
  Cover: add, remove, reorder rows, and that undo restores the previous table contents.
  Done when: test class passes using off-screen rendering.

- [ ] **3.4 Extract McDocumentModel from McWysiwygPane (436 lines)**
  File: `ui/component/McWysiwygPane.java`
  Pull §code state and `pendingCodes` carry into a separate `McDocumentModel`. The pane becomes a thin `JTextPane` subclass that delegates. Once the toolbar can query the model directly, remove the caret-listener-ordering workaround introduced by task 1.7 (commit aaddf20), since the root cause (toolbar reading cached `pendingCodes`) no longer exists.
  Note: `McFormatToolbarTest` (task 3.5, formerly 2.4) must be rewritten to target the model's new API rather than the caret listener ordering it currently asserts on.
  Done when: McWysiwygPane is under 250 lines; McDocumentModel has its own unit tests, and the §code round-trip and pending-codes carry cases from the existing `McWysiwygPaneTest` (task 2.3) are migrated to target `McDocumentModel` where the behavior now lives; the pendingCodes pre-sync hack from 1.7 is gone.

- [x] **3.5 McFormatToolbarTest** (formerly 2.4)
  Cover: toggle state sync when caret moves into pre-styled text (bold button should light up).
  Done when: headless test verifies button state after a synthetic caret move.

- [ ] **3.6 Introduce UiMetrics for scaled layout constants**
  Files: everything under `ui/component/`, `ui/panel/`, `ui/detail/`
  `UiScale.scaled(n)` is called ~50 times with bare integer literals. Create `ui/UiMetrics.java` with semantic constants (`GAP_SMALL`, `GAP_MEDIUM`, `TOOLBAR_ICON_SIZE`, etc.).
  Done when: no bare `scaled(<number>)` call outside `UiMetrics`.

## P4 Accessibility and internationalization

- [ ] **4.1 Add i18n infrastructure (bundle + helper)**
  Create `src/main/resources/messages_en.properties` (empty at first) and `ui/I18n.java` wrapper with `I18n.get(key, args...)`, lazy-loaded for `Locale.getDefault()`, falling back to the key name.
  Done when: the helper exists and is used from one call site as proof of concept.

- [ ] **4.2 Migrate EditorMenuBar strings to i18n**
  File: `ui/EditorMenuBar.java`
  Move every menu/item label into `messages_en.properties`. Keep current wording exactly.
  Done when: grep for string literals in the class shows none that are user-visible.

- [ ] **4.3 Migrate MainWindow dialog strings to i18n**
  Files: `ui/MainWindow.java`, `ui/ErrorPresenter.java`
  Done when: no user-visible literal remains in these two files.

- [ ] **4.4 Migrate panel strings to i18n**
  Files: `ui/panel/*.java`, `ui/detail/*.java`, `ui/dialog/*.java`
  Done when: grep finds no hardcoded English user strings in these packages.

- [ ] **4.5 Add mnemonics to EditorMenuBar**
  File: `ui/EditorMenuBar.java`
  Add `setMnemonic(...)` on each top-level menu and item. Put the mnemonic char into `messages_en.properties` as `menu.file.mnemonic=F`.
  Done when: Alt+F opens File, Alt+E opens Edit, etc.

- [ ] **4.6 Accessible names and tooltips on toolbar**
  File: `ui/component/McFormatToolbar.java`
  Every color swatch and modifier button gets `setAccessibleName()` and `setToolTipText()` including its shortcut.
  Done when: hovering a toolbar button shows a tooltip; screen reader reads a meaningful name.

- [ ] **4.7 Accessible names on lists**
  Files: `ui/panel/CategoryPanel.java`, `ui/panel/PersonPanel.java`, `ui/detail/MembershipTablePanel.java`
  Add `list.getAccessibleContext().setAccessibleName(...)` with localized labels.
  Done when: every `JList` and `JTable` in the app has an accessible name.

- [ ] **4.8 Replace hardcoded colors with FlatLaf lookups**
  Files: `ui/panel/DetailPanel.java:82` (Color.GRAY), `ui/component/McFormatToolbar.java:416-418` (RGB 210,210,210)
  Action: use `UIManager.getColor("...")` or extend `McSwingStyle` with semantic keys.
  Done when: no literal `Color.*` or `new Color(...)` for UI chrome remains in these files.

## P5 Packaging and distribution

- [ ] **5.1 Bump gson**
  File: root `build.properties` (or wherever `gsonVersion` is defined)
  2.8.9 to 2.11.0. Run full test suite.
  Done when: build green.

- [ ] **5.2 Migrate JUnit 4 to JUnit 5**
  Files: `creditsEditor/build.gradle.kts`, all `src/test/java/**/*Test.java`
  Swap dependency to `org.junit.jupiter:junit-jupiter:5.10.x`, add `useJUnitPlatform()` in the test task, rewrite imports and annotations. Verify banned-deps rule still respected (no jackson, no networknt).
  Done when: all tests pass under JUnit 5.

- [ ] **5.3 Install task writes a Linux `.desktop` file**
  File: `creditsEditor/build.gradle.kts:86-122`
  On Linux, also write `$PREFIX/share/applications/gtnh-credits-editor.desktop` and mirror icons to `$PREFIX/share/icons/hicolor/<size>/apps/gtnh-credits-editor.png`.
  Done when: after install, the app appears in the GNOME/KDE app launcher.

- [ ] **5.4 Add higher-resolution icons (256, 512)**
  Folder: `creditsEditor/src/main/resources/icons/`
  Add `icon256.png` and `icon512.png`; update `AppIcons.java` to include them in the image list.
  Done when: HiDPI taskbar and macOS dock use a sharp icon.

- [ ] **5.5 Add jpackage-based native installer task**
  File: `creditsEditor/build.gradle.kts`
  New `nativePackage` task using JDK 21 `jpackage` to produce `.msi`, `.dmg`, `.deb` artifacts (each on its matching host OS).
  Done when: `./gradlew :creditsEditor:nativePackage` on Linux produces a `.deb`.

- [ ] **5.6 CI step for creditsEditor**
  File: `.github/workflows/build-and-test.yml`
  Add an explicit job or step that runs `./gradlew :creditsEditor:test :creditsEditor:shadowJar` so PR status reflects the editor subproject specifically.
  Done when: new CI check appears on PRs and fails on a broken editor build.

## P6 UX polish

- [ ] **6.1 Progress dialog for large resource loads**
  File: `ui/EditorSession.java`, `src/main/java/net/noiraude/creditseditor/ResourceManager.java`
  Wrap the load in a `SwingWorker` driven by a modal indeterminate progress dialog. Keep synchronous API for tests.
  Done when: opening a 50MB zip does not freeze the UI.

- [ ] **6.2 Inline validation feedback in name fields**
  File: `service/KeySanitizer.java` + consumers in detail views
  Current: invalid characters are stripped silently. Add a small warning label under the field explaining which characters were removed, or show a toast with an undo hint.
  Done when: typing a space or dot into a name field produces visible feedback rather than silent mutation.

- [ ] **6.3 Keyboard shortcut help entry**
  File: `ui/dialog/AboutDialog.java` or new `ui/dialog/ShortcutsDialog.java`
  Add a Help menu entry listing all keyboard shortcuts.
  Done when: Help > Keyboard Shortcuts opens a dialog listing File, Edit, Format shortcuts.

---

## Execution order

Recommended: P1 first (real bugs, small patches), then P2 tests (makes later
refactors safe), then P3 refactors, then P4 i18n/a11y (larger sweep), then P6
UX polish, then P5 packaging last (release-adjacent).

Exception: tasks 3.3 (`MembershipTablePanelTest`, formerly 2.6) and 3.5
(`McFormatToolbarTest`, formerly 2.4) sit inside P3 instead of P2 because
they cover classes whose structure the surrounding refactors (3.2 and 3.4)
reshape. Run each after its related refactor, not before. Writing them
first is throwaway work.

---

## Notes for the next Claude session

- Before starting any item, re-read this file fresh. Items may have been reordered, added, or completed.
- Never batch unrelated items in one commit. One checkbox, one commit.
- Always run `./gradlew spotlessApply` before staging.
- Use `git mv` for renames.
- Conventional commit format: `type(scope): description`. For these tasks: `fix(editor)`, `test(editor)`, `refactor(editor)`, `feat(editor)`, `build(editor)`, `chore(editor)`.
- Do not add Co-Authored-By or any attribution trailers.
