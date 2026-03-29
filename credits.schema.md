# GTNH Credits: `credits.json` Schema Reference

_Auto-generated from [`credits.schema.json`](credits.schema.json)._

## Contents

- [Root Object](#root-object)
- [Category](#category)
- [Person](#person)
- [Shared Types](#shared-types)
  - [`key`](#key)

---

## Root Object

Schema for the GTNH credits data file. Persons reference categories by their id value. Category id uniqueness and person-to-category reference validity cannot be expressed in JSON Schema and are enforced by additional semantic checks in the build tooling.

| Property | Type | Required | Description |
|---|---|---|---|
| `version` | `integer` | - | Integer >= 1; defaults to 1 if absent. |
| [`category`](#category) | `Category[]` | yes | Ordered list of credit categories. Each category's id field is its stable identifier. |
| [`person`](#person) | `Person[]` | - | List of credited persons. Optional; may be absent or empty. |

---

## Category

An entry in the category array. Its id is the stable identifier referenced by persons.

| Property | Type | Required | Constraints | Description |
|---|---|---|---|---|
| `id` | [`key`](#key) | yes | Unique across all categories *(build-enforced)* | Translation key suffix. The application prepends the namespace to form the full key and uses this value as the display fallback when no translation is found. Must be unique across all category objects; enforced by the build tooling. |
| `class` | `string` or `string[]` | - | Unique items if array | Semantic class markers for this category. Known values: `person` (render the persons list), `role` (include roles alongside each person), `detail` (render the category description from the lang file). Unknown values are silently ignored. |

---

## Person

An entry in the person array, representing a single credited individual.

| Property | Type | Required | Constraints | Description |
|---|---|---|---|---|
| `name` | `string` | yes | Printable UTF-8, 1-80 chars, no control characters | Display name. Full printable UTF-8; no control characters. May be visually truncated if it exceeds the available display width. |
| `category` | [`key`](#key) or `key[]` | yes | Non-empty, unique items if array, each must match a defined category id *(build-enforced)* | One or more category ids this person belongs to. Each value must match an id defined in the top-level category array; enforced by the build tooling. |
| `role` | [`key`](#key) or `key[]` | - | Non-empty if array form, unique items | Optional role or roles. Each value doubles as a translation key suffix and a human-readable fallback, same constraints as category id. |

---

## Shared Types

### `key`

A human-readable identifier used as a translation key suffix (after sanitization) and as fallback display text when no translation exists. Must start with a letter; may contain letters, digits, spaces, dots, underscores and hyphens; must end with a letter or digit. Before being appended to a translation key path the value is sanitized: dots and hyphens are removed, each run of spaces is collapsed to a single underscore, and the result is lowercased. The original unsanitized value is shown as fallback text when no translation is found.

| Constraint | Value |
|---|---|
| Type | `string` |
| Min length | 1 |
| Max length | 32 |
| Characters | Letters (A-Za-z), digits (0-9), space, `.` `_` `-`; must start with a letter; must end with a letter or digit |

---

_JSON Schema draft: [draft-07](https://json-schema.org/draft-07/schema)._
