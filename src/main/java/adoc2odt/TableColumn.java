package adoc2odt;

import org.asciidoctor.ast.AbstractBlock;

public interface TableColumn extends AbstractBlock {


    long getIndex();
}

