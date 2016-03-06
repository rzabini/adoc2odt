package test.adoc2odt;

import org.junit.rules.TemporaryFolder;

import java.io.File;

public class InspectableTemporaryFolder extends TemporaryFolder {

    public InspectableTemporaryFolder(File parentFolder) {
        super(parentFolder);
    }

    @Override
    public void delete() {

    }

    public void safeDelete() {
        super.delete();
    }
}
