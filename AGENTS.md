# AGENTS.md

## Project overview

This is an Apache Camel Discord component built with Maven and Java 21.

- `lib/` contains the reusable `io.meyer1994:camel-discord` library.
- `example/` contains the standalone `io.meyer1994:camel-discord-example` bot.
- The root `pom.xml` is the Maven parent and reactor for both modules.

## Build commands from the repository root

Run these commands from the repository root:

```sh
# Clean all build output
mvn clean

# Compile all modules
mvn compile

# Run all tests
mvn test

# Package all modules
mvn package

# Install the library and example artifacts in the local Maven repository
mvn install
```

Target a module while also building its reactor dependencies with `-am`:

```sh
# Build and install only lib and its required projects
mvn -pl lib -am install

# Build and install example plus lib
mvn -pl example -am install

# Run tests for lib
mvn -pl lib -am test

# Run tests for example
mvn -pl example -am test
```

Useful lifecycle combinations include `clean compile`, `clean test`,
`clean package`, and `clean install`, for example:

```sh
mvn clean install
mvn -pl lib -am clean package
mvn -pl example -am clean package
```

## `lib` module

The library is the `camel-discord` JAR. From `lib/`, use:

```sh
mvn clean
mvn compile
mvn test
mvn package
mvn install
```

The library build also generates Camel component metadata during the Maven
build. Prefer the root command `mvn -pl lib -am <phase>` when working from a
fresh checkout so Maven can resolve the parent and reactor context together.

### How the library works

`DiscordComponent` creates `DiscordEndpoint` instances for `discord:` URIs.
Each endpoint can create a consumer or producer. Consumers register a
`DiscordHandler` with the configured JDA client; the handler converts selected
JDA events into Camel exchanges with Discord headers and event data. Producers
read the exchange body and headers to send messages, reply to messages, or add
reactions. Stopping a consumer removes its JDA listener.

## `example` module

The example depends on the library. From a fresh checkout, build it together
with `lib` from the repository root:

```sh
mvn -pl example -am clean install
```

From `example/`, the standard Maven lifecycle commands are:

```sh
mvn clean
mvn compile
mvn test
mvn package
mvn install
```

After installing the modules, run the Camel example with a Discord bot token:

```sh
DISCORD_TOKEN=YOUR_DISCORD_BOT_TOKEN mvn -pl example camel:run
```

Do not commit real Discord tokens. Use an environment variable or another
local secret-management mechanism when running the example.

## Development notes

- Use Java 21 or newer; the parent POM sets `maven.compiler.release` to `21`.
- Keep generated files under `lib/src/generated/` consistent with the source
  and Maven component metadata generation.
- Check the existing working tree before editing; preserve unrelated user
  changes.


## Commands

## jq

```sh
# Pretty-print JSON
cat file.json | jq

# Extract a field
jq '.name' file.json

# Extract nested fields
jq '.user.profile.email' file.json

# Select array elements
jq '.items[]' file.json

# Filter by value
jq '.users[] | select(.role == "admin")'

# Multiple conditions
jq '.users[] | select(.active and .age > 18)'

# Return multiple fields
jq '.users[] | {id, name, email}'

# Sort
jq 'sort_by(.timestamp)'

# Count items
jq '.items | length'

# Unique values
jq 'unique'

# Group by field
jq 'group_by(.status)'

# Search for text
jq '.. | strings | select(test("search_term"; "i"))'

# Find objects containing text
jq '.. | objects | select(tostring | test("search_term"; "i"))'

# Convert JSON to TSV
jq -r '.[] | [.id, .name] | @tsv'

# Convert JSON to CSV
jq -r '.[] | [.id, .name] | @csv'
```

## DuckDB

```sql
-- Query CSV directly
duckdb -c "SELECT * FROM 'data.csv' LIMIT 10;"

-- Query Parquet
duckdb -c "SELECT * FROM 'data.parquet';"

-- Search text
SELECT *
FROM read_csv_auto('data.csv')
WHERE description ILIKE '%search_term%';

-- Find schema
DESCRIBE SELECT * FROM read_csv_auto('data.csv');

-- Count rows
SELECT COUNT(*)
FROM read_csv_auto('data.csv');

-- Aggregate
SELECT status, COUNT(*)
FROM read_csv_auto('data.csv')
GROUP BY status
ORDER BY COUNT(*) DESC;

-- Join files
SELECT *
FROM read_csv_auto('users.csv') u
JOIN read_csv_auto('orders.csv') o
ON u.id = o.user_id;

-- Read all Parquet files
SELECT *
FROM read_parquet('logs/*.parquet');

-- Search JSON
SELECT *
FROM read_json_auto('events.json')
WHERE payload::VARCHAR ILIKE '%error%';

-- Export results
COPY (
    SELECT *
    FROM read_csv_auto('data.csv')
) TO 'output.parquet';

-- List tables
SHOW TABLES;

-- Show columns
DESCRIBE table_name;
```

## GitHub CLI (gh)

```sh
# Clone repository
gh repo clone owner/repo

# Search repositories
gh search repos "vector database language:go stars:>1000"

# Search code
gh search code "OpenAI client language:typescript"

# Search code in a repository
gh search code "TODO repo:owner/repo"

# Search issues
gh search issues "panic is:open repo:owner/repo"

# Search pull requests
gh search prs "authentication is:merged repo:owner/repo"

# View issue
gh issue view 123

# List issues
gh issue list

# View pull request
gh pr view 42

# Checkout pull request
gh pr checkout 42

# List pull requests
gh pr list

# View workflow runs
gh run list

# View workflow logs
gh run view RUN_ID --log

# List releases
gh release list

# View latest release
gh release view

# Open repository in browser
gh browse

# Repository metadata
gh repo view --json name,description,defaultBranchRef,licenseInfo,languages

# List repository contents
gh api repos/OWNER/REPO/contents/

# Read a file
gh api repos/OWNER/REPO/contents/path/to/file

# List workflow files
gh api repos/OWNER/REPO/actions/workflows

# Search commits
gh api search/commits -f q="bugfix repo:OWNER/REPO"

# Latest workflow logs
gh run view --log
```

## Combining Tools

```sh
# Search GitHub code and inspect with jq
gh search code "OpenAI repo:owner/repo" --json path,repository \
  | jq

# Search issues and extract title + URL
gh search issues "bug repo:owner/repo" --json title,url \
  | jq -r '.[] | "\(.title)\t\(.url)"'

# Export GitHub data and query with DuckDB
gh search issues "bug repo:owner/repo" --json number,title,state > issues.json

duckdb -c "
SELECT *
FROM read_json_auto('issues.json')
WHERE state = 'OPEN';
"

# Query GitHub API and filter with jq
gh api repos/OWNER/REPO/issues \
  | jq '.[] | {number, title, state}'

# Search every JSON file recursively
find . -name "*.json" -print0 \
  | xargs -0 jq '.. | strings | select(test("search_term"; "i"))'

# Query every CSV recursively
duckdb -c "
SELECT filename, *
FROM read_csv_auto('**/*.csv', filename=true)
WHERE * ILIKE '%search_term%';
"

# Query every Parquet file recursively
duckdb -c "
SELECT *
FROM read_parquet('**/*.parquet')
WHERE message ILIKE '%error%';
"
```