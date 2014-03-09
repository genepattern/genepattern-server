package org.genepattern.server.job.input.choice;


import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for the ChoiceInfoHelper class.
 * @author pcarr
 *
 */
public class TestChoiceInfoHelper {
    @Test
    public void testInitChoicesFromManifestEntry() {
        //<actualValue>=<displayValue>;
        final String choicesString="=NONE;0=ZERO;1=ONE;2=TWO";
        final List<Choice> choices=ChoiceInfoHelper.initChoicesFromManifestEntry(choicesString);
        final String actualChoicesString=ChoiceInfoHelper.initManifestEntryFromChoices(choices);
        Assert.assertEquals(choicesString, actualChoicesString);
    }
    
    @Test
    public void testInitManifestEntryFromChoice_nullValue() {
        final Choice choice=new Choice(null);
        final String manifestEntry=ChoiceInfoHelper.initManifestEntryFromChoice(choice);
        Assert.assertEquals("", manifestEntry);
        
        //now, initialize the choice
        final Choice fromManifestEntry=ChoiceInfoHelper.initChoiceFromManifestEntry(manifestEntry);
        Assert.assertEquals("choice from manifest entry", choice, fromManifestEntry);
    }
    
    @Test
    public void testInitManifestEntryFromChoice_noLabel() {
        final Choice choice=new Choice("value");
        final String manifestEntry=ChoiceInfoHelper.initManifestEntryFromChoice(choice);
        Assert.assertEquals("value", manifestEntry);
        
        //now, initialize the choice
        final Choice fromManifestEntry=ChoiceInfoHelper.initChoiceFromManifestEntry(manifestEntry);
        Assert.assertEquals("choice from manifest entry", choice, fromManifestEntry);

    }
    
    @Test
    public void testInitManifestEntryFromChoice_emptyLabel() {
        final Choice choice=new Choice("", "value");
        final String manifestEntry=ChoiceInfoHelper.initManifestEntryFromChoice(choice);
        Assert.assertEquals("value=", manifestEntry);
        
        //now, initialize the choice
        final Choice fromManifestEntry=ChoiceInfoHelper.initChoiceFromManifestEntry(manifestEntry);
        Assert.assertEquals("choice from manifest entry", choice, fromManifestEntry);

    }

    @Test
    public void testInitManifestEntryFromChoice_withLabel() {
        final String label="displayValue";
        final String value="actualValue";
        final Choice choice=new Choice(label, value);
        final String manifestEntry=ChoiceInfoHelper.initManifestEntryFromChoice(choice);
        Assert.assertEquals("actualValue=displayValue", manifestEntry);

        //now, initialize the choice
        final Choice fromManifestEntry=ChoiceInfoHelper.initChoiceFromManifestEntry(manifestEntry);
        Assert.assertEquals("choice from manifest entry", choice, fromManifestEntry);
    }

    @Test
    public void testInitManifestEntryFromChoice_emptyValue() {
        final String displayValue="displayValue";
        final String actualValue="";
        final Choice choice=new Choice(displayValue, actualValue);
        final String manifestEntry=ChoiceInfoHelper.initManifestEntryFromChoice(choice);
        Assert.assertEquals("=displayValue", manifestEntry);

        //now, initialize the choice
        final Choice fromManifestEntry=ChoiceInfoHelper.initChoiceFromManifestEntry(manifestEntry);
        Assert.assertEquals("choice from manifest entry", choice, fromManifestEntry);
    }

    @Test
    public void testInitManifestEntryFromChoice_emptyValue_emptyDisplay() {
        final String displayValue="";
        final String actualValue="";
        final Choice choice=new Choice(displayValue, actualValue);
        final String manifestEntry=ChoiceInfoHelper.initManifestEntryFromChoice(choice);
        Assert.assertEquals("", manifestEntry);

        //now, initialize the choice
        final Choice fromManifestEntry=ChoiceInfoHelper.initChoiceFromManifestEntry(manifestEntry);
        Assert.assertEquals("choice from manifest entry", choice, fromManifestEntry);
    }

}
