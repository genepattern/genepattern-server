/**
 * GenomeSpace integration library - this package contains the classes necessary to integrate
 * GenePattern with GenomeSpace.
 * 
 * Because we still want GP to function in a Java 5 environment, 
 * methods which require access to the GS CDK will be implemented in a different package
 * which must not be loaded at runtime unless genomeSpaceEnabled is set to true.
 * 
 */
package org.genepattern.server.genomespace;
