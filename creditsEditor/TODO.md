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