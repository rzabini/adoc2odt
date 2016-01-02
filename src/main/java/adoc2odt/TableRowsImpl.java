package adoc2odt;

import org.asciidoctor.ast.AbstractBlockImpl;
import org.jruby.Ruby;
import org.jruby.RubyArray;

public class TableRowsImpl extends AbstractBlockImpl implements TableRows {

    private final TableRows delegate;

    public TableRowsImpl(TableRows delegate, Ruby runtime) {
        super(delegate, runtime);
        this.delegate = delegate;
    }


    @Override
    public RubyArray head() {
        return delegate.head();
    }

    @Override
    public RubyArray foot() {
        return delegate.foot();
    }

    @Override
    public RubyArray body() {
        return delegate.body();
    }



}
