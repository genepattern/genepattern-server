/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.output;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

/**
 * PathMatcher composed of a list of PathMatchers.
 * @author pcarr
 *
 */
public class CompositePathMatcher implements PathMatcher {
    private List<PathMatcher> matchers=new ArrayList<PathMatcher>();

    public void add(PathMatcher pathMatcher) {
        matchers.add(pathMatcher);
    }

    @Override
    public boolean matches(Path path) {
        if (matchers==null) {
            return false;
        }
        for(final PathMatcher matcher : matchers) {
            if (matcher.matches(path)) {
                return true;
            }
        }
        return false;
    }
}
