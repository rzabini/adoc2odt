package adoc2odt;

import org.asciidoctor.ast.*;
import org.jruby.Ruby;
import org.jruby.RubyArray;

import java.util.List;

public class TableImpl extends AbstractBlockImpl implements TableNode {

    private final TableNode delegate;

    public TableImpl(TableNode delegate, Ruby runtime) {
        super(delegate, runtime);
        this.delegate = delegate;
    }


    @Override
    public TableRows rows() {
        return delegate.rows();
    }

    @Override
    public List<RubyArray> headerRows(){
        return (List) rows().head().getList();
    }

    @Override
    public List<RubyArray> footerRows(){
        return (List) rows().foot().getList();
    }

    @Override
    public List<RubyArray> bodyRows(){
        return (List) rows().body().getList();
    }

    public RubyArray columns() {
        return delegate.columns();
    }

    @Override
    public List getColumnList(){
        return columns().getList();
    }

}
