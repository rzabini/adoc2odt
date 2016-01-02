package adoc2odt;

import org.asciidoctor.ast.AbstractBlockImpl;
import org.jruby.Ruby;

public class TableColumnImpl extends AbstractBlockImpl implements TableColumn {

    private final TableColumn delegate;

    public TableColumnImpl(TableColumn delegate, Ruby runtime) {
        super(delegate, runtime);
        this.delegate = delegate;
    }

    @Override
    public long getIndex() {
        return  (Long)delegate.getAttr("colnumber");
    }



}
