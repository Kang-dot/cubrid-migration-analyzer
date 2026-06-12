# CUBRID SQL Analyzer

**CUBRID SQL Analyzer** is a command-line tool for checking the compatibility of
Oracle database objects and SQL statements with CUBRID.

The analyzer reads DDL metadata from an Oracle database or DML statements from
XML mapper files, validates them with the embedded CUBRID parser or a CUBRID
database, and generates a report containing success and failure details. Failed
statements are also assigned an estimated migration cost based on configurable
rules.

## Key Features

* **Source Analysis**
  * Loads database objects from an Oracle JDBC connection.
  * Extracts SQL statements from XML mapper files.
* **CUBRID Validation**
  * Validates statements with the embedded CUBRID parser.
  * Supports validation against a CUBRID database through JDBC.
* **Execution Modes**
  * Interactive console mode.
  * Non-interactive command-line mode.
  * Terminal UI (TUI) mode.
* **Analysis Reports**
  * Summarizes analyzed, successful, and failed statements.
  * Records detailed failure information.
  * Estimates migration cost using rules defined in `settings/cost.json`.

## Project Structure

| Path | Description |
| --- | --- |
| `src/com/cubrid/sqlanalyzer/command` | CLI/TUI entry points, argument handling, services, and result views |
| `src/com/cubrid/sqlanalyzer/core` | Analysis configuration, execution plans, parsers, and cost calculation |
| `src/com/cubrid/sqlanalyzer/dmlparser` | XML mapper and DML statement parsing |
| `src/com/cubrid/sqlanalyzer/xmlmetadata` | XML source metadata loading |
| `src/dist` | Distribution scripts and default settings |
| `test` | Unit tests |
| `jni` | Native SQL validator library source and build files |
| `pl_server` | PL/CSQL parser dependency used during analysis |
| `submodule/cubrid-migration` | CUBRID Migration Toolkit modules used by the analyzer |

## Build from Source

### 1. Prerequisites

Install or prepare the following:

* Linux x86_64
* JDK 21 or later
* Apache Maven 3.9 or later
* Git
* GNU Make and GCC
* A CUBRID installation with the `CUBRID` environment variable configured
* `pl_server.jar` at `pl_server/pl_server.jar`

### 2. Clone Submodules

```bash
git submodule update --init --recursive
```

### 3. Build

Use `build.sh` as the single build entry point. For the initial build, include
`--with-submodules` to install the required CUBRID Migration Toolkit modules
into the project-local Maven repository:

```bash
bash build.sh --with-submodules
```

Subsequent builds reuse the installed CMT artifacts:

```bash
bash build.sh
```

Maven goals and options can be passed directly to the script:

```bash
bash build.sh clean package
```

| Option | Description |
| --- | --- |
| `-s`, `--with-submodules` | Builds and installs the required CMT submodules before SQL Analyzer |
| `-h`, `--help` | Displays the build script usage |

The default Maven arguments are:

```text
-DskipTests package
```

The Linux distribution archive is generated at:

```text
target/sql-analyzer-0.0.1-SNAPSHOT-linux-x86_64.tar.gz
```

The unpacked distribution files are available under `target/analyzer`.

## Running the Analyzer

Extract the distribution and run the launcher:

```bash
tar -xzf target/sql-analyzer-0.0.1-SNAPSHOT-linux-x86_64.tar.gz
cd sql-analyzer-0.0.1-SNAPSHOT
./analyzer.sh
```

Running without arguments starts the interactive console. The analyzer can also
be started with command-line options:

```bash
# Analyze SQL mapper XML files with the embedded parser
./analyzer.sh -sx -xd /path/to/sqlmap -tp

# Start the terminal UI
./analyzer.sh -tui -sx -xd /path/to/sqlmap -tp

# Analyze an Oracle database with the embedded parser
./analyzer.sh -so \
  -oj 'jdbc:oracle:thin:@//localhost:1521/xe|user|password' \
  -tp
```

For JDBC-based analysis, place the required Oracle and CUBRID JDBC drivers in a
driver repository and specify it with `-jr <directory>`.

## Configuration

The default runtime configuration files are located in `settings`:

* `settings/setting.conf`: source, target, UI, JDBC, and logging options
* `settings/cost.json`: base costs and heuristic migration cost rules
* `settings/logback.xml`: logging configuration

Run the analyzer with a specific settings file:

```bash
./analyzer.sh -conf settings/setting.conf
```

### `setting.conf` Options

#### Common Options

| Property | Values / Default | Description |
| --- | --- | --- |
| `arguments` | Command-line arguments | Provides the complete analyzer argument string, for example `-sx -xd ./sqlmap -tp` |
| `ui.mode` | `console` or `tui` / `console` | Selects the user interface mode |
| `tui.width` | Integer / `100` | Sets the initial TUI width |
| `tui.height` | Integer / `30` | Sets the initial TUI height |
| `jdbc.repository.dir` | Directory path | Specifies a directory containing Oracle and CUBRID JDBC drivers |
| `log.dir` | Directory path / `logs` | Specifies the runtime log directory |

When `arguments` is set, it takes precedence over the structured source,
target, and UI properties below. Command-line analyzer options also take
precedence over options loaded from the settings file.

#### Source Options

| Property | Values / Default | Description |
| --- | --- | --- |
| `source.type` | `oracle` or `xml` | Selects the analysis source |
| `source.jdbc` | `<jdbcUrl\|user\|password>` | Provides an Oracle connection as a single value |
| `source.jdbc.url` | Oracle JDBC URL | Provides the Oracle JDBC URL separately |
| `source.host` | Host name | Oracle host used to construct the JDBC URL |
| `source.port` | Port number | Oracle port used with `source.host` and `source.sid` |
| `source.sid` | Service name or SID | Oracle service name used to construct the JDBC URL |
| `source.username` | User name | Oracle database user |
| `source.password` | Password | Oracle database password |
| `xml.directory` | Directory path | Directory containing XML mapper files |
| `xml.charset` | Charset / `UTF-8` | Character encoding of the XML files |

For an Oracle source, use either `source.jdbc`, `source.jdbc.url`, or the
`source.host`/`source.port`/`source.sid` combination.

#### Target Options

| Property | Values / Default | Description |
| --- | --- | --- |
| `target.type` | `parser`, `jdbc`, or `cubrid` | Selects the embedded parser or CUBRID JDBC target |
| `target.jdbc` | `<jdbcUrl\|user\|password>` | Provides a CUBRID connection as a single value |
| `target.jdbc.url` | CUBRID JDBC URL | Provides the CUBRID JDBC URL separately |
| `target.username` | User name | CUBRID database user |
| `target.password` | Password | CUBRID database password |

### Configuration Examples

Analyze XML mapper files with the embedded parser:

```properties
ui.mode=console
source.type=xml
xml.directory=./sqlmap
xml.charset=UTF-8
target.type=parser
```

Analyze an Oracle database against a CUBRID database:

```properties
ui.mode=tui
jdbc.repository.dir=./jdbc

source.type=oracle
source.host=localhost
source.port=1521
source.sid=xe
source.username=oracle_user
source.password=oracle_password

target.type=jdbc
target.jdbc.url=jdbc:cubrid:localhost:33000:demodb:::
target.username=dba
target.password=
```

> **Security Note:** Database passwords in `setting.conf` are stored as plain
> text. Restrict access to the file and do not commit credentials to source
> control.

## Output

Analysis results are written to the `report` directory as UTF-8 text files:

```text
report/analyzer_result_<timestamp>.txt
```

Runtime logs are written to the `logs` directory by default.

## Tests

Run the unit test suite with the same build entry point:

```bash
bash build.sh test
```

Add `--with-submodules` when the CMT artifacts have not been installed yet or
when the submodule source has changed.
