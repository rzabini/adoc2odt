package test.adoc2odt

import adoc2odt.Adoc2Odt
import adoc2odt.AdocParser
import adoc2odt.OdtDocument
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.util.zip.ZipFile

import static spock.util.matcher.HamcrestSupport.expect
import static test.adoc2odt.OdtMatcher.isEquivalentTo

class AdocSpecification extends Specification {

    @Rule
    TemporaryFolder tempFolder = new TemporaryFolder(new File('build'))
    File resultFile

    def setup() {
        resultFile = new File(tempFolder.root, 'result.odt')
    }


    def "can convert the empty document"() {
        given:
        AdocParser parser = buildParser("adoc/empty.adoc", "odt/emptyTest.odt")

        when:
        parser.parse()

        then:
        expect this.resultFile, isEquivalentTo("src/test/resources/odt/emptyTest.odt")
    }

    def "can convert a single line document"() {
        given:
        AdocParser parser = buildParser("adoc/singleLine.adoc", "odt/emptyTest.odt")

        when:
        parser.parse()

        then:
        expect resultFile, isEquivalentTo("src/test/resources/odt/singleLine.odt")
    }


    private AdocParser buildParser(String adocFilePath, String odtTemplatePath) {
        new AdocParser(readResource(adocFilePath),
                new Adoc2Odt(
                        new OdtDocument(
                                new ZipFile(getClass().getClassLoader().getResource(odtTemplatePath).getFile()),
                                new FileOutputStream(resultFile))))

    }

    private InputStream readResource(String path) {
        getClass().getClassLoader().getResourceAsStream(path)
    }

}

