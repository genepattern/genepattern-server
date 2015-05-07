/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.dm;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A Node in a Graph.
 * @author pcarr
 *
 * @param <T>
 */
public class Node<T> {
    private Node<T> parent = null;
    private T val;
    private SortedSet<Node<T>> children ;
    private Comparator<Node<T>> comparator;

    public Node(Comparator<Node<T>> comparator, T val) {
        this.val = val;
        this.comparator = comparator;
    }
    public Node(Comparator<Node<T>> comparator, Node<T> parent, T val) {
        this.comparator = comparator;
        this.parent = parent;
        this.val = val;
    }
    
    public boolean isRoot() {
        return parent == null;
    }
    
    public Node<T> getParent() {
        return parent;
    }
    
    public Node<T> addChild(T childObj) {
        if (children == null) {
            children = new TreeSet<Node<T>>(comparator);
        }
        Node<T> child = new Node<T>(comparator, this, childObj);
        children.add( child );
        return child;
    }
    
    public Node<T> addChild(Node<T> child) {
        if (children == null) {
            children = new TreeSet<Node<T>>(comparator);
        }
        child.parent = this;
        children.add( child );
        return child;
    }
    
    public Collection<Node<T>> getChildren() {
        if (children == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableCollection(children);
    }
    
    public T getValue() {
        return val;
    }

}
