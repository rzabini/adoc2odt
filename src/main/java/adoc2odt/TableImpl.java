package adoc2odt;

import org.asciidoctor.ast.*;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyObject;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.ArrayList;
import java.util.List;

public class TableImpl extends AbstractBlockImpl implements TableNode {

    private final TableNode delegate;
    private final SafeNodeConverter nodeConverter;

    public TableImpl(TableNode delegate, Ruby runtime, SafeNodeConverter safeNodeConverter) {
        super(delegate, runtime);
        this.delegate = delegate;
        this.nodeConverter = safeNodeConverter;
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

    @Override
    public List<TableRow> getBodyRows() {
        List<TableRow> rows = new ArrayList<TableRow>();

        for (RubyArray rowAsRubyArray : (List<RubyArray>)rows().body().getList()) {
            TableRow listOfCells = new TableRow();
            for (IRubyObject rubyObject : rowAsRubyArray.toJavaArray())
                listOfCells.add(nodeConverter.createTableCellASTNode(rubyObject));
            rows.add(listOfCells);
        }
        return rows;
    }

    public RubyArray columns() {
        return delegate.columns();
    }

    @Override
    public List getColumnList(){
        return columns().getList();
    }

}
