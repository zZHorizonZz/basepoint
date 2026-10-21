package dev.horizon.basepoint.mariadb;

import dev.horizon.basepoint.contract.AggregateContractTest;
import dev.horizon.basepoint.contract.CompoundQueryContractTest;
import dev.horizon.basepoint.contract.DecoratorContractTest;
import dev.horizon.basepoint.contract.DeleteContractTest;
import dev.horizon.basepoint.contract.FilterContractTest;
import dev.horizon.basepoint.contract.IncludeContractTest;
import dev.horizon.basepoint.contract.InsertContractTest;
import dev.horizon.basepoint.contract.JsonContractTest;
import dev.horizon.basepoint.contract.LockContractTest;
import dev.horizon.basepoint.contract.PagingContractTest;
import dev.horizon.basepoint.contract.TestDatabase;
import dev.horizon.basepoint.contract.TextFilterContractTest;
import dev.horizon.basepoint.contract.UpdateContractTest;
import dev.horizon.basepoint.contract.UpsertContractTest;
import org.junit.jupiter.api.Nested;

class MariaDbContractTest {

  private static final TestDatabase DATABASE = new MariaDbTestDatabase();

  @Nested
  class Filter extends FilterContractTest {

    @Override
    protected TestDatabase database() {
      return DATABASE;
    }
  }

  @Nested
  class TextFilter extends TextFilterContractTest {

    @Override
    protected TestDatabase database() {
      return DATABASE;
    }
  }

  @Nested
  class Paging extends PagingContractTest {

    @Override
    protected TestDatabase database() {
      return DATABASE;
    }
  }

  @Nested
  class Aggregate extends AggregateContractTest {

    @Override
    protected TestDatabase database() {
      return DATABASE;
    }
  }

  @Nested
  class Include extends IncludeContractTest {

    @Override
    protected TestDatabase database() {
      return DATABASE;
    }
  }

  @Nested
  class CompoundQuery extends CompoundQueryContractTest {

    @Override
    protected TestDatabase database() {
      return DATABASE;
    }
  }

  @Nested
  class Insert extends InsertContractTest {

    @Override
    protected TestDatabase database() {
      return DATABASE;
    }
  }

  @Nested
  class Upsert extends UpsertContractTest {

    @Override
    protected TestDatabase database() {
      return DATABASE;
    }
  }

  @Nested
  class Update extends UpdateContractTest {

    @Override
    protected TestDatabase database() {
      return DATABASE;
    }
  }

  @Nested
  class Delete extends DeleteContractTest {

    @Override
    protected TestDatabase database() {
      return DATABASE;
    }
  }

  @Nested
  class Lock extends LockContractTest {

    @Override
    protected TestDatabase database() {
      return DATABASE;
    }
  }

  @Nested
  class Decorator extends DecoratorContractTest {

    @Override
    protected TestDatabase database() {
      return DATABASE;
    }
  }

  @Nested
  class Json extends JsonContractTest {

    @Override
    protected TestDatabase database() {
      return DATABASE;
    }
  }
}
