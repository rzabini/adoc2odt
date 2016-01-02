package adoc2odt;

import org.asciidoctor.ast.AbstractBlock;
import org.jruby.RubyArray;

import java.util.List;

public interface TableCell extends AbstractBlock {


    List<String> lines();
}

