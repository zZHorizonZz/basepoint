package dev.horizon.basepoint.contract;

import org.junit.jupiter.api.Nested;

public abstract class DialectContract {

  private final TestDatabase database;

  protected DialectContract(TestDatabase database) {
    this.database = database;
  }

  @Nested
  class Aggregate extends AggregateContractTest {

    @Override
    protected TestDatabase database() {
      return DialectContract.this.database;
    }
  }

  @Nested
  class Decorator extends DecoratorContractTest {

    @Override
    protected TestDatabase database() {
      return DialectContract.this.database;
    }
  }

  @Nested
  class Delete extends DeleteContractTest {

    @Override
    protected TestDatabase database() {
      return DialectContract.this.database;
    }
  }

  @Nested
  class Duration extends DurationContractTest {

    @Override
    protected TestDatabase database() {
      return DialectContract.this.database;
    }
  }

  @Nested
  class Filter extends FilterContractTest {

    @Override
    protected TestDatabase database() {
      return DialectContract.this.database;
    }
  }

  @Nested
  class FilterText extends FilterTextContractTest {

    @Override
    protected TestDatabase database() {
      return DialectContract.this.database;
    }
  }

  @Nested
  class Function extends FunctionContractTest {

    @Override
    protected TestDatabase database() {
      return DialectContract.this.database;
    }
  }

  @Nested
  class Include extends IncludeContractTest {

    @Override
    protected TestDatabase database() {
      return DialectContract.this.database;
    }
  }

  @Nested
  class Insert extends InsertContractTest {

    @Override
    protected TestDatabase database() {
      return DialectContract.this.database;
    }
  }

  @Nested
  class Json extends JsonContractTest {

    @Override
    protected TestDatabase database() {
      return DialectContract.this.database;
    }
  }

  @Nested
  class Lock extends LockContractTest {

    @Override
    protected TestDatabase database() {
      return DialectContract.this.database;
    }
  }

  @Nested
  class Name extends NameContractTest {

    @Override
    protected TestDatabase database() {
      return DialectContract.this.database;
    }
  }

  @Nested
  class Nesting extends NestingContractTest {

    @Override
    protected TestDatabase database() {
      return DialectContract.this.database;
    }
  }

  @Nested
  class Paging extends PagingContractTest {

    @Override
    protected TestDatabase database() {
      return DialectContract.this.database;
    }
  }

  @Nested
  class Relation extends RelationContractTest {

    @Override
    protected TestDatabase database() {
      return DialectContract.this.database;
    }
  }

  @Nested
  class Union extends UnionContractTest {

    @Override
    protected TestDatabase database() {
      return DialectContract.this.database;
    }
  }

  @Nested
  class Update extends UpdateContractTest {

    @Override
    protected TestDatabase database() {
      return DialectContract.this.database;
    }
  }

  @Nested
  class Upsert extends UpsertContractTest {

    @Override
    protected TestDatabase database() {
      return DialectContract.this.database;
    }
  }

  @Nested
  class Value extends ValueContractTest {

    @Override
    protected TestDatabase database() {
      return DialectContract.this.database;
    }
  }

  @Nested
  class With extends WithContractTest {

    @Override
    protected TestDatabase database() {
      return DialectContract.this.database;
    }
  }
}
