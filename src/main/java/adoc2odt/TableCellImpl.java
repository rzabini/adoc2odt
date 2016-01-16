package adoc2odt;

import org.asciidoctor.ast.AbstractBlockImpl;
import org.asciidoctor.ast.Document;
import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

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
    public IRubyObject inner_document() {
        return delegate.inner_document();
    }

    @Override
    public String style() {
        return delegate.style();
    }

    @Override
    public String text() {
        return delegate.text();
    }

    @Override
    public int rowspan() {
        return delegate.rowspan();
    }

    @Override
    public int colspan() {
        return delegate.colspan();
    }

    @Override
    public List<String> lines() {
        return null; //((RubyArray)delegate.getContent()).getList();
    }

}
