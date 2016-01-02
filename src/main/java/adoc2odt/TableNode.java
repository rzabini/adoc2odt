package adoc2odt;

import org.asciidoctor.ast.AbstractBlock;
import org.jruby.RubyArray;

import java.util.List;

public interface TableNode extends AbstractBlock {

    TableRows rows();

    List<RubyArray> headerRows();

    List<RubyArray> bodyRows();

    RubyArray columns();

    List getColumnList();

    List<RubyArray> footerRows();
}

