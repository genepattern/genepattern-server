package org.genepattern.util;

/**
 *  A function of one variable.
 *
 * @author    Joshua Gould
 */
public interface Function {

   /**
    *  Evalutes the function at x.
    *
    * @param  x
    * @return    double f(x)
    */
   public double evaluate(double x);
}
