package test.adoc2odt;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Description;
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;
import org.xmlunit.util.Predicate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public abstract class OdtFileMatcher extends OdtMatcher {
    private final ZipFile expected;

    public OdtFileMatcher(File expected) throws IOException {
        this.expected = new ZipFile(expected);
    }

    public static OdtFileMatcher hasContentEquivalentTo(File zipFile) throws IOException {
        return new OdtContentMatcher(zipFile);
    }

    public static OdtFileMatcher hasMetadataEquivalentTo(File zipFile) throws IOException {
        return new OdtMetadataMatcher(zipFile);
    }

    public boolean check(File fileToMatch) throws IOException {
        ZipFile file = new ZipFile(fileToMatch);
        Enumeration<? extends ZipEntry> en = expected.entries();
        while(en.hasMoreElements()) {
            ZipEntry zipEntry = en.nextElement();
            InputStream inputStream = getInputStream(file, zipEntry.getName());

            if (inputStream == NOT_FOUND) {
                reason = String.format("file not found: %s", zipEntry.getName());
                return false;
            }

            if(mustCheck(zipEntry.getName())) {

                if (zipEntry.getName().endsWith(".xml") && zipEntry.getSize() > 0) {

                    Diff diff = DiffBuilder.compare(Input.from(expected.getInputStream(zipEntry)))
                            .withTest(Input.from(inputStream))
                            .withNodeFilter(nodeFilter())
                            .build();

                    if (diff.hasDifferences()) {
                        reason = String.format("in file %s: %s", zipEntry.getName(), diff.getDifferences());
                        UnzipUtility.unzip(fileToMatch);
                        return false;
                    }

                } else if (!IOUtils.contentEquals(inputStream, expected.getInputStream(zipEntry))) {
                    reason = String.format("binary file differ: %s", zipEntry.getName());
                    UnzipUtility.unzip(fileToMatch);
                    return false;
                }
            }
        }

        file.close();
        return true;
    }

    static Predicate<Node> NO_FILTER = new Predicate<Node>() {
        @Override
        public boolean test(Node toTest) {
            return true;
        }
    };

    Predicate<Node> nodeFilter() {
        return NO_FILTER;
    }

    abstract boolean mustCheck(String zipEntry);

    @Override
    protected boolean matchesSafely(File fileToMatch) {
        try {
            return check(fileToMatch);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a file matching ").appendValue(expected.getName());
    }
}