package test.adoc2odt;

import org.w3c.dom.Node;
import org.xmlunit.util.Predicate;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class OdtMetadataMatcher extends OdtFileMatcher {
    public OdtMetadataMatcher(File zipFile) throws IOException {
        super(zipFile);
    }

    static List<String> NODE_NAMES = Arrays.asList("title", "creator");

    @Override
    boolean mustCheck(String zipEntry) {
        return zipEntry.matches("meta.xml");
    }

    @Override
    Predicate<Node> nodeFilter() {
        return new Predicate<Node>() {
            @Override
            public boolean test(Node toTest) {
                return NODE_NAMES.contains(toTest.getNodeName());
            }
        };
    }
}
