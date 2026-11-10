package dev.horizon.basepoint.contract;

import static dev.horizon.basepoint.Expression.add;
import static dev.horizon.basepoint.Expression.now;
import static dev.horizon.basepoint.Expression.subtract;
import static dev.horizon.basepoint.Filter.gt;
import static dev.horizon.basepoint.Filter.lt;
import static org.assertj.core.api.Assertions.assertThat;

import dev.horizon.basepoint.CollectionReference;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class DurationContractTest extends ContractTest {

  private CollectionReference events;

  @BeforeEach
  void insertEvents() throws SQLException {
    events = dialect.collection("events");
    execute(events.insert(List.of(Map.of("id", 1, "happened", subtract(now(), Duration.ofDays(1))),
        Map.of("id", 2, "happened", subtract(now(), Duration.ofDays(10))),
        Map.of("id", 3, "happened", add(now(), Duration.ofHours(2))),
        Map.of("id", 4, "happened", subtract(now(), Duration.ofMinutes(90).plusMillis(500))))));
  }

  @Test
  void findsTheRowsOfTheLastWeek() throws SQLException {
    assertThat(ids(events.query().columns("id").where(gt("happened", subtract(now(), Duration.ofDays(7))))))
        .containsExactly(1, 3, 4);
  }

  @Test
  void findsTheRowsBeforeAndAfterATime() throws SQLException {
    assertThat(ids(events.query().columns("id").where(lt("happened", now())))).containsExactly(1, 2, 4);
    assertThat(ids(events.query().columns("id").where(gt("happened", add(now(), Duration.ofHours(1))))))
        .containsExactly(3);
  }

  @Test
  void movesByMoreThanTheRangeOfAMicrosecondCount() throws SQLException {
    assertThat(ids(events.query().columns("id").where(gt("happened", subtract(now(), Duration.ofDays(400))))))
        .containsExactly(1, 2, 3, 4);
  }

  @Test
  void movesByPartsOfASecond() throws SQLException {
    assertThat(ids(events.query().columns("id").where(gt(add(now(), Duration.ofMillis(1500)), add(now(), Duration
        .ofSeconds(1)))))).containsExactly(1, 2, 3, 4);
    assertThat(ids(events.query().columns("id").where(gt(add(now(), Duration.ofMillis(500)), add(now(), Duration
        .ofSeconds(1)))))).isEmpty();
  }
}
