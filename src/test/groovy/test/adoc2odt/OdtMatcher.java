package test.adoc2odt;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class OdtMatcher extends TypeSafeMatcher<File> {
    public static final NullInputStream NOT_FOUND = new NullInputStream(0);
    private final ZipFile expected;

    private Object reason;

    public OdtMatcher(String expected) throws IOException {
        this.expected = new ZipFile(expected);
    }

    public OdtMatcher(File expected) throws IOException {
        this.expected = new ZipFile(expected);
    }

    public static OdtMatcher isEquivalentTo(String zipFile) throws IOException {
        return new OdtMatcher(zipFile);
    }
    public static OdtMatcher isEquivalentTo(File zipFile) throws IOException {
        return new OdtMatcher(zipFile);
    }

    public boolean check(ZipFile file) throws IOException {

        Enumeration<? extends ZipEntry> en = expected.entries();
        while(en.hasMoreElements()) {
            ZipEntry zipEntry = en.nextElement();
            InputStream inputStream = getInputStream(file, zipEntry.getName());

            if (inputStream == NOT_FOUND) {
                reason = String.format("file not found: %s", zipEntry.getName());
                return false;
            }

            if (zipEntry.getName().endsWith(".xml") && zipEntry.getSize() > 0) {

                if (zipEntry.getName().equalsIgnoreCase("content.xml")) {
                    Diff diff = DiffBuilder.compare(Input.from(expected.getInputStream(zipEntry)))
                            .withTest(Input.from(inputStream))
                            .build();

                    if (diff.hasDifferences()) {
                        reason = String.format("in file %s: %s", zipEntry.getName(), diff.getDifferences());
                        return false;
                    }
                }
            }
            /*else
                if (!IOUtils.contentEquals(inputStream, expected.getInputStream(zipEntry))) {
                    reason = String.format("binary file differ: %s", zipEntry.getName());
                    return false;
                }*/
        }

        return true;
    }

    private InputStream getInputStream(ZipFile file, String name) throws IOException {
        if (file.getEntry(name) != null)
            return file.getInputStream(file.getEntry(name));
        else
            return NOT_FOUND;
    }

    @Override
    protected boolean matchesSafely(File item) {
        ZipFile file = null;
        try {
            file = new ZipFile(item);
            return check(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            if (file != null)
                try {
                    file.close();
                } catch (IOException e) {

                }
        }

    }

    @Override
    protected void describeMismatchSafely(final File item, final Description mismatchDescription) {
        mismatchDescription.appendText(" there was a mismatch: ").appendValue(reason);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a file matching ").appendValue(expected.getName());
    }
}