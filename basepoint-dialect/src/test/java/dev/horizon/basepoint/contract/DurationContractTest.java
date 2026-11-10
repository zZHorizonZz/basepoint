package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.expression.Expression.add;
import static dev.horizon.basepoint.expression.Expression.column;
import static dev.horizon.basepoint.expression.Expression.now;
import static dev.horizon.basepoint.expression.Expression.subtract;
import static dev.horizon.basepoint.filter.Filter.gt;
import static dev.horizon.basepoint.filter.Filter.lt;
import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.basepoint.CollectionReference;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class DurationContractTest extends ContractTest {

  private CollectionReference events;

  @BeforeEach
  void insertEvents() throws SQLException {
    events = dialect.collection("events");
    assertThat(execute(events.insert(List.of(Map.of("id", 1, "happened", subtract(now(), Duration.ofDays(1))),
        Map.of("id", 2, "happened", subtract(now(), Duration.ofDays(10))),
        Map.of("id", 3, "happened", add(now(), Duration.ofHours(2))),
        Map.of("id", 4, "happened", subtract(now(), Duration.ofMinutes(90).plusMillis(500)))))))
        .isEqualTo(4);
  }

  @Test
  @DisplayName("Finds the rows of the last week")
  void lastWeek() throws SQLException {
    assertThat(ids(events.query().columns("id").where(gt("happened", subtract(now(), Duration.ofDays(7))))))
        .containsExactly(1, 3, 4);
  }

  @Test
  @DisplayName("Finds the rows before and after a time")
  void beforeAndAfter() throws SQLException {
    assertThat(ids(events.query().columns("id").where(lt("happened", now())))).containsExactly(1, 2, 4);
    assertThat(ids(events.query().columns("id").where(gt("happened", add(now(), Duration.ofHours(1))))))
        .containsExactly(3);
  }

  Stream<Named<Duration>> farDurations() {
    return Stream.of(Named.of("400 days, beyond an int of microseconds", Duration.ofDays(400)),
        Named.of("60 years", Duration.ofDays(365 * 60)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("farDurations")
  @DisplayName("Moves a timestamp far into the past")
  void moveFar(Duration far) throws SQLException {
    assertThat(ids(events.query().columns("id").where(gt("happened", subtract(now(), far))))).containsExactly(1, 2, 3,
        4);
    assertThat(ids(events.query().columns("id").where(lt("happened", subtract(now(), far))))).isEmpty();
  }

  @Test
  @DisplayName("Moves the timestamp of a row by parts of a second")
  void moveFraction() throws SQLException {
    assertThat(ids(events.query().columns("id").where(gt(add(column("happened"), Duration.ofMillis(1500)),
        add(column("happened"), Duration.ofSeconds(1)))))).containsExactly(1, 2, 3, 4);
    assertThat(ids(events.query().columns("id").where(gt(add(column("happened"), Duration.ofMillis(500)),
        add(column("happened"), Duration.ofSeconds(1)))))).isEmpty();
  }

  @Test
  @DisplayName("Moves the timestamp of a row back by parts of a second")
  void moveBack() throws SQLException {
    assertThat(ids(events.query().columns("id").where(lt(subtract(column("happened"), Duration.ofMillis(1)),
        column("happened"))))).containsExactly(1, 2, 3, 4);
  }

  @Test
  @DisplayName("Gives the current time, which the rows of now() find")
  void readClock() throws SQLException {
    execute(events.insert(Map.of("id", 5, "happened", now())));

    assertThat(ids(events.query().columns("id").where(gt("happened", subtract(now(), Duration.ofMinutes(1))))
        .where(lt("happened", add(now(), Duration.ofMinutes(1)))))).containsExactly(5);
  }
}
