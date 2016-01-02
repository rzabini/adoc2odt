package adoc2odt;

import org.asciidoctor.ast.AbstractBlockImpl;
import org.jruby.Ruby;
import org.jruby.RubyArray;

import java.util.List;

public class TableCellImpl extends AbstractBlockImpl implements TableCell {

    private final TableCell delegate;

    public TableCellImpl(TableCell delegate, Ruby runtime) {
        super(delegate, runtime);
        this.delegate = delegate;
    }

    @Override
    public Object content() {
        return delegate.getContent();
    }

    @Override
    public List<String> lines() {
        return ((RubyArray)delegate.getContent()).getList();
    }

}
