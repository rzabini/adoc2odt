package test.adoc2odt;

import org.apache.commons.io.input.NullInputStream;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;

public abstract class OdtMatcher extends TypeSafeMatcher<File> {
    public static final NullInputStream NOT_FOUND = new NullInputStream(0);
    protected Object reason;

    protected InputStream getInputStream(ZipFile file, String name) throws IOException {
        if (file.getEntry(name) != null)
            return file.getInputStream(file.getEntry(name));
        else
            return NOT_FOUND;
    }

    @Override
    protected void describeMismatchSafely(final File item, final Description mismatchDescription) {
        mismatchDescription.appendText(" there was a mismatch: ").appendValue(reason);
    }
}
