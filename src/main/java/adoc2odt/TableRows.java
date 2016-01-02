package adoc2odt;

import org.asciidoctor.ast.AbstractBlock;
import org.jruby.RubyArray;

public interface TableRows extends AbstractBlock {
    RubyArray head();
    RubyArray foot();
    RubyArray body();
}

