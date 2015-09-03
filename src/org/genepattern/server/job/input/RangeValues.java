package org.genepattern.server.job.input;

/**
 * Created by nazaire on 9/1/15.
 */
public class RangeValues<T extends Number>
{
    public static final String PROP_RANGE="range";

    private T min = null;
    private T max = null;

    public RangeValues() {}

    public RangeValues(T min, T max)
    {
        this.min = min;
        this.max = max;
    }

    public void setMin(T min) {
        this.min = min;
    }

    public void setMax(T max) {
        this.max = max;
    }

    public T getMin() {
        return min;
    }

    public T getMax() {
        return max;
    }
}
