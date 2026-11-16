# Java Spreadsheet - Deliverable 2

- **Student:** Rauf Muhammad
- **Course:** Software Engineering (300093)
- **Institution:** UPC BarcelonaTech, EETAC
- **Academic year:** 2026/2027

This application implements the spreadsheet content-management operations for
Deliverable 2. The domain model distinguishes numerical, text, formula and empty
contents. Formula strings are stored and displayed without calculation.

## Implemented features

- Validated cell addresses, including A1, Z10 and AA1.
- Setting, replacing, clearing and viewing original cell contents.
- A table of populated cells, ordered by row and column.
- UTF-8 S2V loading and saving with empty cell positions preserved.
- Function argument delimiter conversion for S2V serialization only.
- Save As, reuse of the current file, and confirmation before discarding edits.
- Explicit input, encoding and file errors; failed loads retain the current sheet.
- Separate domain, storage and console packages, with automated JUnit tests.

## Build and run

Use JDK 17 or later and Apache Maven. From the repository root:

```sh
mvn clean verify
java -cp target/classes org.example.spreadsheet.App
```

Maven downloads the required build and test dependencies on first use. Tests
require no assignment documents, external services or local development files.

## Console operations

Commands are followed by separate prompts for their arguments. Enter raw cell
contents and file paths without adding surrounding quotes.

| Command | Operation |
|---|---|
| `new` | Create an empty spreadsheet. |
| `set` | Enter an address and raw content; an empty content line clears the cell. |
| `show` | View one cell's original content. |
| `table` | List populated cell contents; display escapes do not change stored text. |
| `save` | Save to an entered path; an empty path reuses the current file if available. |
| `saveas` | Save to another entered path and make it the current file. |
| `load` | Load an S2V file after validating the complete file. |
| `help` | Display input conventions. |
| `exit` | Leave the application. |

Unsaved changes require an explicit `yes` before `new`, a successful `load`, or
`exit` discards them. An empty answer keeps the current sheet. Saving to an
existing regular file replaces its contents. Ending input reports unsaved changes
but does not save automatically.

For example, use `set`, enter `A1`, and enter `= 1 + 2`. `show` followed by `A1`
displays exactly `= 1 + 2`. Save the sheet, create a new sheet, load the saved file,
and view A1 again to inspect the restored formula source.

## S2V format and scope

Each line represents a row, with semicolons between cells. Leading and internal
empty cells and rows retain their positions. Output ends at each row's last
non-empty cell; trailing empty grid dimensions are not retained. An empty sheet
produces an empty file. LF, CRLF and CR input line endings are accepted.

Function argument semicolons are written as commas within formula fields and
restored on loading. For example, `=MAX(A1;B1)` is stored as `=MAX(A1,B1)`.
No function is evaluated. Literal text semicolons, embedded line breaks, NUL,
and ambiguous quoted formula arguments are rejected because the supplied format
does not define escaping. Invalid UTF-8 and unpaired Unicode surrogates produce
explicit errors. Saves use a temporary sibling file before destination replacement.

Addresses use uppercase letters and positive rows without leading zeros. Numeric
classification accepts signed dot-decimal literals and preserves their spelling;
other forms, such as scientific notation, remain text. These are documented model
conventions pending reconciliation with the group's final domain diagram.

Deliverable 2 does not include formula evaluation, an expression parser,
reference resolution, dependency tracking, cycle detection, function calculation
or range evaluation. Function and reference expressions remain raw cell contents.
