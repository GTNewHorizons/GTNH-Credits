# GTNH Credits: `credits.json` Schema Reference

_Auto-generated from [`credits.schema.json`](credits.schema.json)._

## Contents

- [Root Object](#root-object)
- [Category](#category)
- [Person](#person)
- [PersonCategoryEntry](#personcategoryentry)
- [Shared Types](#shared-types)
  - [`key`](#key)
  - [Role](#role)
  - [TranslationReference](#translationreference)

---

## Root Object

Schema for the GTNH credits data file. Persons reference categories by their id value. Category id uniqueness and person-to-category reference validity cannot be expressed in JSON Schema and are enforced by additional semantic checks in the build tooling.

| Property | Type | Required | Description |
|---|---|---|---|
| `$schema` | `string` | - | JSON Schema dialect URI. Present for editor tooling; ignored by the application. |
| `version` | `integer` | yes | Format version. Version 2 supports local role ids, ignored username metadata and combined category maps. Version 3 also supports exact external translation references and requires one category per membership object. Readers reject unsupported versions. |
| [`category`](#category) | [`Category`](#category)[] | yes | Ordered list of credit categories. Each category's id field is its stable identifier. |
| [`person`](#person) | [`Person`](#person)[] | - | List of credited persons. Optional; may be absent or empty. |

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
| `category` | [`PersonCategoryEntry`](#personcategoryentry) or `PersonCategoryEntry[]` | yes | Non-empty; each id must match a defined category *(build-enforced)* | One or more category memberships for this person. Each entry is either a plain category key (no roles) or a single-property object whose key is the category id and whose value is one or more role keys for that category. Category id uniqueness within the list and cross-reference validity are enforced by the build tooling. |

---

## PersonCategoryEntry

A single category membership for a person. Either a plain category key (no roles) or a single-property object mapping the category key to one or more role keys for that category.

| Property key | Value type | Description |
|---|---|---|
| (category id) | [`Role`](#role) or `Role[]` | Role labels for this category: credits-owned ids or exact external translation references. One role may be given directly; multiple roles as an array. |

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

### Role

A credits-owned role is a string id of type `key`. An external role is a `TranslationReference` object containing the exact Minecraft language key. Translation references require version 3.

### TranslationReference

An exact Minecraft translation key supplied by another mod or resource pack. Available in version 3. Preserves case and punctuation; never sanitized or prefixed.

| Property | Type | Required | Constraints | Description |
|---|---|---|---|---|
| `translationKey` | `string` | yes | 1-512 characters; no whitespace or control characters | Exact Minecraft translation key supplied by another mod or resource pack. Case and punctuation are preserved; no credits prefix is added. |

---

_JSON Schema draft: [draft-07](https://json-schema.org/draft-07/schema)._
