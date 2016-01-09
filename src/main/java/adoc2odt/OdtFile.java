package adoc2odt;

import com.gc.iotools.stream.os.OutputStreamToInputStream;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class OdtFile {


    private final Collection<String> fileEntryListFormMetadata = new ArrayList<String>();
    private final ZipOutputStream odtZipFile;
    private final File styleFile;
    private final Document metadata;

    public OdtFile(File outputFile, File styleFile) {
        try {
            odtZipFile = new ZipOutputStream(new FileOutputStream(outputFile));
            this.styleFile = styleFile;
            this.metadata = extractXMLFromZip(styleFile, "meta.xml");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    void copyZipFolderEntry(File zipFile, String folder, ZipOutputStream outputStream, Manifest manifest) throws IOException {
        ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry ze = null;
        while ((ze = zin.getNextEntry()) != null) {
            if (ze.getName().startsWith(folder)) {
                copyZipFileEntry(zin, ze, outputStream);
                manifest.addFileEntry(ze.getName());

            }
        }
    }


    void extractFileFromZip(File zipFile, String fileName, OutputStream outputStream) throws IOException {
        ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry ze = null;
        while ((ze = zin.getNextEntry()) != null) {
            if (ze.getName().equals(fileName)) {
                copy(zin, outputStream);
                break;
            }
        }
    }

    private void copyZipFileEntry(ZipInputStream zin, ZipEntry ze, ZipOutputStream outputStream) throws IOException {
        outputStream.putNextEntry(ze);
        copy(zin, outputStream);
        fileEntryListFormMetadata.add(ze.getName());
        outputStream.closeEntry();
    }

    private void copy(InputStream zin, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[8192];
        int len;
        while ((len = zin.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
        }
    }


    org.jdom2.Document extractXMLFromZip(File zipFile, String fileName) {
        try {
            final OutputStreamToInputStream<Document> out = new OutputStreamToInputStream<org.jdom2.Document>() {
                @Override
                protected org.jdom2.Document doRead(final InputStream istream) throws Exception {
          /*
           * Read the data from the InputStream "istream" passed as parameter.
           */
                    SAXBuilder saxBuilder = new SAXBuilder();
                    return saxBuilder.build(istream);
                }
            };
            try {
                extractFileFromZip(zipFile, fileName, out);
            } finally {
                // don't miss the close (or a thread would not terminate correctly).
                out.close();
            }


            return out.getResult();
        }
        catch (Exception e) {
            throw new RuntimeException(String.format("cannot extract xml: %s from zip: %s", fileName, zipFile.getAbsolutePath() ));
        }
    }

    public void copyZipFileEntry(File styleFile, String fileName) {
        try {
            ZipEntry ze = new ZipEntry(fileName);
            odtZipFile.putNextEntry(ze);
            extractFileFromZip(styleFile, fileName, odtZipFile);
            odtZipFile.closeEntry();
        }
        catch (Exception e) {
            throw new RuntimeException("cannot copy zip file entry: " + fileName, e);
        }
    }

    public void copyZipFolderEntry(File styleFile, String folder, Manifest manifest) {
        try {
            copyZipFolderEntry(styleFile, folder, odtZipFile, manifest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeManifest(Document document) {
        try {
            ZipEntry ze = new ZipEntry("META-INF/manifest.xml");

            odtZipFile.putNextEntry(ze);
            XMLOutputter xo = new XMLOutputter();
            xo.setFormat(Format.getPrettyFormat());
            xo.output(document, odtZipFile);
            odtZipFile.closeEntry();
        }
        catch (Exception e) {
            throw new RuntimeException("cannot write manifest", e);
        }
    }

    public void writeContent(Document odtDocument) {
        writeDocument(odtDocument, "content.xml");
    }

    public void writeDocument(Document odtDocument, String fileName) {
        try {
            ZipEntry ze = new ZipEntry(fileName);
            odtZipFile.putNextEntry(ze);
            XMLOutputter xo = new XMLOutputter();
            xo.output(odtDocument, odtZipFile);
            odtZipFile.closeEntry();
        }
        catch (Exception e) {
            throw new RuntimeException("cannot write document: " + fileName, e);
        }
    }

    public void storeFile(File file, String fileRelativePath) {
        try {
            ZipEntry ze = new ZipEntry(fileRelativePath);
            odtZipFile.putNextEntry(ze);
            Path path = file.toPath();
            Files.copy(path, odtZipFile);
            odtZipFile.closeEntry();
        }
        catch (Exception e) {
            throw new RuntimeException("cannot store file: " + file, e);
        }
    }

    public void copyFromStyle(String fileName) {
        copyZipFileEntry(styleFile, fileName);
    }

    public void copyPicturesFolderFromStyle(Manifest manifest) {
        copyZipFolderEntry(styleFile, "Pictures", manifest);
    }

    public void setMetaProperty(String attributeNamespace, String attributeName, String attributeValue) {
        Element root = metadata.getRootElement();
        Element meta = root.getChild("meta", root.getNamespace());
        Element attributeElement = meta.getChild(attributeName, root.getNamespace(attributeNamespace));
        if (attributeElement == null) {
            attributeElement = new Element(attributeName, root.getNamespace(attributeNamespace));
            meta.addContent(attributeElement);
        }

        attributeElement.setText(attributeValue);
    }



    public void writeMetadata() {
        writeDocument(metadata, "meta.xml");
    }

    public void close() {
        try {
            odtZipFile.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
