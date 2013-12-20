package org.genepattern.server.job.input.collection;

import java.util.Collection;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.job.input.GroupId;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamValue;

/**
 * Utility methods for walking through all of the values for a given input parameter.
 * @author pcarr
 *
 */
public class ParamValueWalker {
    private static final Logger log = Logger.getLogger(ParamValueWalker.class);
    
    /**
     * Visit each value in the order in which the ParamValues were added to the Param.
     * @param param
     * @param visitor
     */
    public static void walkValuesInOrder(final Param param, final ParamValueVisitor visitor) {
        boolean withErrors=false;
        try {
            visitor.start();
            for(final Entry<GroupId,ParamValue> entry : param.getValuesAsEntries()) {
                visitor.visitValue(entry.getKey(), entry.getValue());
            }
        }
        catch (Throwable t) {
            log.error(t);
            withErrors=true;
        }
        visitor.finish(withErrors);
    }

    /**
     * Visit each value, by visiting each group in order. For each group, visit each of its values in order.
     * @param param
     * @param visitor
     */
    public static void walkValuesByGroup(final Param param, final GroupVisitor visitor) {
        boolean errors=false;
        try {
            visitor.start();
            for(final Entry<GroupId,Collection<ParamValue>> groupEntry : param.getGroupedValues().entrySet()) {
                final GroupId groupId=groupEntry.getKey();
                visitor.startGroup(groupId);
                for(final ParamValue paramValue : groupEntry.getValue()) {
                    visitor.visitValue(groupId, paramValue);
                }
                visitor.finishGroup(groupId);
            }
        }
        catch (Throwable t) {
            log.error(t);
            errors=true;
        }
        visitor.finish(errors);
    }
}
