package test.adoc2odt

import adoc2odt.Adoc2Odt
import adoc2odt.AdocParser
import adoc2odt.OdtDocument
import org.junit.Rule
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

import static spock.util.matcher.HamcrestSupport.expect
import static OdtFileMatcher.hasContentEquivalentTo
import static OdtFileMatcher.hasMetadataEquivalentTo
import static test.adoc2odt.OdtPictureMatcher.hasPictures


class AdocSpecification extends Specification {

    @Rule
    InspectableTemporaryFolder tempFolder = new InspectableTemporaryFolder(new File('build'))
    File resultFile

    def setup() {
        resultFile = new File(tempFolder.root, 'result.odt')
    }


    def "can convert documents"(name, outcome) {
        when:
        parse("${name}.adoc")
        then:
        expect resultFile, outcome
        tempFolder.safeDelete()

        where:
        name << ['empty', 'singleLine']
        outcome = hasContentEquivalentTo(file("${name}.odt"))
    }

    def "can convert images"() {
        when:
        parseString("image:images/logo.png[]")
        then:
        expect resultFile, hasPictures(['logo.png'])
        tempFolder.safeDelete()

    }

    def "converted document has metadata"() {
        when:
        parse("metadata.adoc", 'baseTemplate.odt')
        then:
        expect resultFile, hasMetadataEquivalentTo(file("metadata.odt"))
        tempFolder.safeDelete()
    }


    private parse(String adocFile) {
        parse(adocFile, 'emptyTemplate.odt')
    }

    private parseString(String adocString) {
        parseString(adocString, 'emptyTemplate.odt')
    }

    private parse(String adocFile, String odtTemplate) {
        buildParser(adocFile, "odt/template/$odtTemplate").parse()
    }

    private parseString(String adocString, String odtTemplate) {
        buildParser('adoc', "odt/template/$odtTemplate" , fromString(adocString)).parse()
    }

    private static File file(String fileName) {
        new File("src/test/resources/odt/${fileName}")
    }

    private AdocParser buildParser(String adocFileName, String odtTemplatePath) {
        buildParser('adoc', odtTemplatePath, readResource("adoc/$adocFileName"))
    }

    private AdocParser buildParser(String adocBasePath, String odtTemplatePath, InputStream resource) {
        new AdocParser(resource,
                new File(getClass().getClassLoader().getResource(adocBasePath).getFile()),
                new Adoc2Odt(
                        new OdtDocument(
                                new ZipFile(getClass().getClassLoader().getResource(odtTemplatePath).getFile()),
                                new FileOutputStream(resultFile))))

    }

    private InputStream readResource(String path) {
        getClass().getClassLoader().getResourceAsStream(path)
    }

    private static InputStream fromString(String string) {
        new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
    }

}

