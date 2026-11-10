# Get started

In this guide, you add Basepoint for PostgreSQL to a Maven project, build a query of a table, and run it with JDBC.

## Prerequisites

- Java 25 or later, and Maven.
- A GitHub account. GitHub Packages requires a token, also for public packages.
- One of these databases:

  | Database   | Minimum version |
  | ---------- | --------------- |
  | PostgreSQL | 13              |
  | MariaDB    | 11.4            |
  | SQL Server | 2017            |
  | SQLite     | 3.44            |

## 1. Add the dependency

1. Add the GitHub Packages repository and the module for your database to your `pom.xml`. This guide uses
   PostgreSQL:

   <!-- x-release-please-start-version -->
   ```xml
   <repositories>
     <repository>
       <id>github</id>
       <url>https://maven.pkg.github.com/zZHorizonZz/basepoint</url>
     </repository>
   </repositories>

   <dependencies>
     <dependency>
       <groupId>dev.horizon</groupId>
       <artifactId>basepoint-dialect-postgres</artifactId>
       <version>0.3.2-SNAPSHOT</version>
     </dependency>
   </dependencies>
   ```
   <!-- x-release-please-end -->

2. Create a personal access token with the `read:packages` scope, and add it to `~/.m2/settings.xml`:

   ```xml
   <settings>
     <servers>
       <server>
         <id>github</id>
         <username>YOUR_GITHUB_USERNAME</username>
         <password>YOUR_TOKEN</password>
       </server>
     </servers>
   </settings>
   ```

For MariaDB, SQL Server or SQLite, use `basepoint-dialect-mariadb`, `basepoint-dialect-sqlserver` or
`basepoint-dialect-sqlite`.

## 2. Build a query

1. Create a dialect and a reference to a table. JDBC uses `?` placeholders, so pass `ParameterStyle.QUESTION_MARK`:

   ```java
   Dialect dialect = new PostgresDialect(ParameterStyle.QUESTION_MARK);
   CollectionReference orders = dialect.collection("orders");
   ```

2. Build a query:

   ```java
   RenderedStatement statement = orders.query()
       .where(and(eq("state", "OPEN"), gt("total", 100)))
       .orderBy("id")
       .limit(20)
       .build();
   ```

`statement.text()` returns this SQL:

```sql
SELECT "t0".* FROM "orders" AS "t0" WHERE (("t0"."state" = ?) AND ("t0"."total" > ?)) ORDER BY "t0"."id" ASC LIMIT 20
```

`statement.params()` returns `[OPEN, 100]`.

## 3. Run the query

1. Prepare the SQL text, and bind each value in order:

   ```java
   try (PreparedStatement prepared = connection.prepareStatement(statement.text())) {
     for (int i = 0; i < statement.params().size(); i++) {
       prepared.setObject(i + 1, statement.params().get(i));
     }
     try (ResultSet rows = prepared.executeQuery()) {
       while (rows.next()) {
         System.out.println(rows.getInt("id"));
       }
     }
   }
   ```

The program prints the id of each open order with a total over 100, up to 20 of them.

## Next steps

- [Filter rows](filters/filter-rows.md)
- [Include related rows](queries/include-related-rows.md)
- [Dialect reference](dialects/dialect-reference.md)
