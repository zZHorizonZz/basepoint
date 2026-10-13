package dev.horizon.basepoint.it;

import static dev.horizon.basepoint.Filter.eq;
import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.basepoint.Dialect;
import dev.horizon.basepoint.Statement;
import dev.horizon.basepoint.mysql.MySqlDialect;
import dev.horizon.basepoint.postgres.PostgresDialect;
import dev.horizon.basepoint.sqlite.SqliteDialect;
import dev.horizon.basepoint.sqlserver.SqlServerDialect;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlaceholderTest {

  @Test
  void eachDialectHasThePlaceholdersOfItsDatabase() {
    assertThat(where(new PostgresDialect()).text()).endsWith("WHERE (\"t0\".\"state\" = $1)");
    assertThat(where(new SqlServerDialect()).text()).endsWith("WHERE ([t0].[state] = @p1)");
    assertThat(where(new MySqlDialect()).text()).endsWith("WHERE (`t0`.`state` = ?)");
    assertThat(where(new SqliteDialect()).text()).endsWith("WHERE (\"t0\".\"state\" = ?)");
    assertThat(where(new PostgresDialect()).params()).isEqualTo(List.of("OPEN"));
  }

  private static Statement where(Dialect dialect) {
    return dialect.collection("orders").query().where(eq("state", "OPEN")).build();
  }
}
