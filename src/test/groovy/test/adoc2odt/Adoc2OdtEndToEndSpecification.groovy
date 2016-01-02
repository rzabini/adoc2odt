package test.adoc2odt

import adoc2odt.Adoc2Odt
import adoc2odt.AdocParser
import groovy.util.slurpersupport.GPathResult
import org.asciidoctor.Asciidoctor
import org.hamcrest.MatcherAssert
import spock.lang.Specification

class Adoc2OdtEndToEndSpecification extends Specification {

    //@Rule
    //TemporaryFolder temporaryFolder = new TemporaryFolder(new File("build"))
    File temporaryFolder = new File("build")

    Asciidoctor asciidoctor = Asciidoctor.Factory.create()

    XmlSlurper parser = new XmlSlurper()


    def "Adoc2Odt translates a simple file"(){

        given:
            Adoc2Odt adoc2Odt = new Adoc2Odt(new File("build/adoc2odt.odt"), new File("build/resources/test/styles.odt"));
            AdocParser adocParser = new AdocParser(new File("src/test/resources/sample.adoc"), [:])

            File outputFile = new File(temporaryFolder, "adoc2odt.odt")

            adocParser.addListener(adoc2Odt)


        when:
            adocParser.parse()
        and:
            UnzipUtility.unzip(outputFile.absolutePath, temporaryFolder.absolutePath)
        and:

            GPathResult root = parser.parse(new File(temporaryFolder, "content.xml"))

        then:
            root.name() == 'document-content'
            root.body.text.h.size() == 3





    }

}
