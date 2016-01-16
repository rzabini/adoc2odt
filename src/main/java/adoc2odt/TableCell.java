package adoc2odt;

import org.asciidoctor.ast.AbstractBlock;
import org.asciidoctor.ast.Document;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;

public interface TableCell extends AbstractBlock {


    List<String> lines();

    IRubyObject inner_document();

    String style();
    String text();

    int colspan();
    int rowspan();
}

