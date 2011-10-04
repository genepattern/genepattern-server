import org.genepattern.server.dm.userupload.MigrationTool;

/**
 * GreenPepper test fixture for the MigrationTool class.
 * 
 * @author pcarr
 */
public class MigrationToolFixture {
    private String origValue;
    
    public void setOrigValue(String str) {
        this.origValue = str;
    }
    
    public String getNewValue() {
        String newValue = MigrationTool.migrateInputParameterValue(origValue);
        return newValue;
    }

}
