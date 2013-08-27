package org.genepattern.server.job.input.choice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.*;


/**
 * jUnit tests for the ChoiceInfoHelper class.
 * @author pcarr
 *
 */
public class TestDynamicChoiceInfoParser {
    private static final String gseaGeneSetsDatabaseChoicesFromManifest="noGeneSetsDB=;c1.all.v4.0.symbols.gmt=c1.all.v4.0.symbols.gmt [Positional];c2.all.v4.0.symbols.gmt=c2.all.v4.0.symbols.gmt [Curated];c2.cgp.v4.0.symbols.gmt=c2.cgp.v4.0.symbols.gmt [Curated];c2.cp.v4.0.symbols.gmt=c2.cp.v4.0.symbols.gmt [Curated];c2.cp.biocarta.v4.0.symbols.gmt=c2.cp.biocarta.v4.0.symbols.gmt [Curated];c2.cp.kegg.v4.0.symbols.gmt=c2.cp.kegg.v4.0.symbols.gmt [Curated];c2.cp.reactome.v4.0.symbols.gmt=c2.cp.reactome.v4.0.symbols.gmt [Curated];c3.all.v4.0.symbols.gmt=c3.all.v4.0.symbols.gmt [Motif];c3.mir.v4.0.symbols.gmt=c3.mir.v4.0.symbols.gmt [Motif];c3.tft.v4.0.symbols.gmt=c3.tft.v4.0.symbols.gmt [Motif];c4.all.v4.0.symbols.gmt=c4.all.v4.0.symbols.gmt [Computational];c4.cgn.v4.0.symbols.gmt=c4.cgn.v4.0.symbols.gmt [Computational];c4.cm.v4.0.symbols.gmt=c4.cm.v4.0.symbols.gmt [Computational];c5.all.v4.0.symbols.gmt=c5.all.v4.0.symbols.gmt [Gene Ontology];c5.bp.v4.0.symbols.gmt=c5.bp.v4.0.symbols.gmt [Gene Ontology];c5.cc.v4.0.symbols.gmt=c5.cc.v4.0.symbols.gmt [Gene Ontology];c5.mf.v4.0.symbols.gmt=c5.mf.v4.0.symbols.gmt [Gene Ontology];c6.all.v4.0.symbols.gmt=c6.all.v4.0.symbols.gmt [Oncogenic Signatures];c7.all.v4.0.symbols.gmt=c7.all.v4.0.symbols.gmt [Immunologic signatures]";
    private static List<Choice> gseaGeneSetsDatabaseChoices;
    
    @BeforeClass
    public static void initClass() {
        gseaGeneSetsDatabaseChoices=new ArrayList<Choice>();
        //new Choice( <label>, <value> )
        gseaGeneSetsDatabaseChoices.add(new Choice("", "noGeneSetsDB"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c1.all.v4.0.symbols.gmt [Positional]", "c1.all.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c2.all.v4.0.symbols.gmt [Curated]", "c2.all.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c2.cgp.v4.0.symbols.gmt [Curated]", "c2.cgp.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c2.cp.v4.0.symbols.gmt [Curated]", "c2.cp.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c2.cp.biocarta.v4.0.symbols.gmt [Curated]", "c2.cp.biocarta.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c2.cp.kegg.v4.0.symbols.gmt [Curated]", "c2.cp.kegg.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c2.cp.reactome.v4.0.symbols.gmt [Curated]", "c2.cp.reactome.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c3.all.v4.0.symbols.gmt [Motif]", "c3.all.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c3.mir.v4.0.symbols.gmt [Motif]", "c3.mir.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c3.tft.v4.0.symbols.gmt [Motif]", "c3.tft.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c4.all.v4.0.symbols.gmt [Computational]", "c4.all.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c4.cgn.v4.0.symbols.gmt [Computational]", "c4.cgn.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c4.cm.v4.0.symbols.gmt [Computational]", "c4.cm.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c5.all.v4.0.symbols.gmt [Gene Ontology]", "c5.all.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c5.bp.v4.0.symbols.gmt [Gene Ontology]", "c5.bp.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c5.cc.v4.0.symbols.gmt [Gene Ontology]", "c5.cc.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c5.mf.v4.0.symbols.gmt [Gene Ontology]", "c5.mf.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c6.all.v4.0.symbols.gmt [Oncogenic Signatures]", "c6.all.v4.0.symbols.gmt"));
        gseaGeneSetsDatabaseChoices.add(new Choice("c7.all.v4.0.symbols.gmt [Immunologic signatures]", "c7.all.v4.0.symbols.gmt"));
    }

    @Test
    public void testParseChoiceEntry_null() {
        try {
            ChoiceInfoHelper.initChoiceFromManifestEntry(null);
            Assert.fail("null arg should cause IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
    }
    
    @Test
    public void testParseChoiceEntry_empty() throws Exception {
        Choice choice=ChoiceInfoHelper.initChoiceFromManifestEntry("");
        Choice expected=new Choice("");
        Assert.assertEquals("expected.label should be empty", "", expected.getLabel());
        Assert.assertEquals("expected.value should be empty", "", expected.getValue());
        Assert.assertEquals("choice from '='", expected, choice);
    }
    
    @Test
    public void testParseChoiceEntry_spaces() throws Exception {
        Choice choice=ChoiceInfoHelper.initChoiceFromManifestEntry(" ");
        Choice expected=new Choice(" ");
        Assert.assertEquals("don't trim space characters", expected, choice);
    }

    @Test
    public void testParseChoiceEntry_spaces02() throws Exception {
        Choice choice=ChoiceInfoHelper.initChoiceFromManifestEntry("  =  ");
        Choice expected=new Choice("  ");
        Assert.assertEquals("don't trim space characters", expected, choice);
    }
    
    @Test
    public void testParseChoiceEntry_equalsOnly() throws Exception {
        Choice choice=ChoiceInfoHelper.initChoiceFromManifestEntry("=");
        Choice expected=new Choice("");
        Assert.assertEquals("choice from '='", expected, choice);
    }
    
    @Test
    public void testParseChoiceEntry_emptyLhs() throws Exception {
        Choice choice=ChoiceInfoHelper.initChoiceFromManifestEntry("=NonBlankDisplayValue");
        Choice expected=new Choice("NonBlankDisplayValue", "");
        Assert.assertEquals(expected, choice);
    }
    
    @Test
    public void testParseChoiceEntry_emptyRhs() throws Exception {
        Choice choice=ChoiceInfoHelper.initChoiceFromManifestEntry("NonBlankActualValue=");
        Choice expected=new Choice("", "NonBlankActualValue");
        Assert.assertEquals(expected, choice);
    }
    
    @Test
    public void testInitChoicesFromManifestEntry() {
        final List<Choice> choices=ChoiceInfoHelper.initChoicesFromManifestEntry(gseaGeneSetsDatabaseChoicesFromManifest);
        Assert.assertEquals(gseaGeneSetsDatabaseChoices, choices);
    }
    
    /**
     * Test the 'gene.sets.database' drop-down for GSEA v. 14.
     * It has an empty display value, 'noGeneSetsDB'
     */
    @Test
    public void testLegacyChoiceGsea14() {
        //
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(TestDynamicChoiceInfoParser.class, "GSEA_v14.zip");
        final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);
        final ParameterInfoRecord pinfoRecord=paramInfoMap.get("gene.sets.database");
        
        final DynamicChoiceInfoParser choiceInfoParser=new DynamicChoiceInfoParser();
        final ChoiceInfo choiceInfo=choiceInfoParser.initChoiceInfo(pinfoRecord.getFormal());
        
        Assert.assertNotNull("Expecting a non-null choiceInfo", choiceInfo);
        final List<Choice> choices=choiceInfo.getChoices();
        Assert.assertThat("", choices, is(gseaGeneSetsDatabaseChoices));
    }

}
