package test.adoc2odt;

import java.io.File;
import java.io.IOException;

public class OdtContentMatcher extends OdtFileMatcher {
    public OdtContentMatcher(File zipFile) throws IOException {
        super(zipFile);
    }

    @Override
    boolean mustCheck(String zipEntry) {
            return zipEntry.matches("content.xml");
    }
}
