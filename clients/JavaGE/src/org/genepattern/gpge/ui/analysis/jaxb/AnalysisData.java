package org.genepattern.gpge.ui.analysis.jaxb;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.Dispatcher;
import javax.xml.bind.InvalidAttributeException;
import javax.xml.bind.InvalidContentObjectException;
import javax.xml.bind.LocalValidationException;
import javax.xml.bind.MarshallableObject;
import javax.xml.bind.MarshallableRootElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PredicatedLists;
import javax.xml.bind.RootElement;
import javax.xml.bind.StructureValidationException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidatableObject;
import javax.xml.bind.Validator;
import javax.xml.marshal.XMLScanner;
import javax.xml.marshal.XMLWriter;


public class AnalysisData
    extends MarshallableRootElement
    implements RootElement
{

    private List _Result = PredicatedLists.createInvalidating(this, new ResultPredicate(), new ArrayList());
    private PredicatedLists.Predicate pred_Result = new ResultPredicate();
    private List _History = PredicatedLists.createInvalidating(this, new HistoryPredicate(), new ArrayList());
    private PredicatedLists.Predicate pred_History = new HistoryPredicate();

    public List getResult() {
        return _Result;
    }

    public void deleteResult() {
        _Result = null;
        invalidate();
    }

    public void emptyResult() {
        _Result = PredicatedLists.createInvalidating(this, pred_Result, new ArrayList());
    }

    public List getHistory() {
        return _History;
    }

    public void deleteHistory() {
        _History = null;
        invalidate();
    }

    public void emptyHistory() {
        _History = PredicatedLists.createInvalidating(this, pred_History, new ArrayList());
    }

    public void validateThis()
        throws LocalValidationException
    {
    }

    public void validate(Validator v)
        throws StructureValidationException
    {
        for (Iterator i = _Result.iterator(); i.hasNext(); ) {
            v.validate(((ValidatableObject) i.next()));
        }
        for (Iterator i = _History.iterator(); i.hasNext(); ) {
            v.validate(((ValidatableObject) i.next()));
        }
    }

    public void marshal(Marshaller m)
        throws IOException
    {
        XMLWriter w = m.writer();
        w.start("AnalysisData");
        if (_Result.size()> 0) {
            for (Iterator i = _Result.iterator(); i.hasNext(); ) {
                m.marshal(((MarshallableObject) i.next()));
            }
        }
        if (_History.size()> 0) {
            for (Iterator i = _History.iterator(); i.hasNext(); ) {
                m.marshal(((MarshallableObject) i.next()));
            }
        }
        w.end("AnalysisData");
    }

    public void unmarshal(Unmarshaller u)
        throws UnmarshalException
    {
        XMLScanner xs = u.scanner();
        Validator v = u.validator();
        xs.takeStart("AnalysisData");
        while (xs.atAttribute()) {
            String an = xs.takeAttributeName();
            throw new InvalidAttributeException(an);
        }
        {
            List l = PredicatedLists.create(this, pred_Result, new ArrayList());
            while (xs.atStart("Result")) {
                l.add(((Result) u.unmarshal()));
            }
            _Result = PredicatedLists.createInvalidating(this, pred_Result, l);
        }
        {
            List l = PredicatedLists.create(this, pred_History, new ArrayList());
            while (xs.atStart("History")) {
                l.add(((History) u.unmarshal()));
            }
            _History = PredicatedLists.createInvalidating(this, pred_History, l);
        }
        xs.takeEnd("AnalysisData");
    }

    public static AnalysisData unmarshal(InputStream in)
        throws UnmarshalException
    {
        return unmarshal(XMLScanner.open(in));
    }

    public static AnalysisData unmarshal(XMLScanner xs)
        throws UnmarshalException
    {
        return unmarshal(xs, newDispatcher());
    }

    public static AnalysisData unmarshal(XMLScanner xs, Dispatcher d)
        throws UnmarshalException
    {
        return ((AnalysisData) d.unmarshal(xs, (AnalysisData.class)));
    }

    public boolean equals(Object ob) {
        if (this == ob) {
            return true;
        }
        if (!(ob instanceof AnalysisData)) {
            return false;
        }
        AnalysisData tob = ((AnalysisData) ob);
        if (_Result!= null) {
            if (tob._Result == null) {
                return false;
            }
            if (!_Result.equals(tob._Result)) {
                return false;
            }
        } else {
            if (tob._Result!= null) {
                return false;
            }
        }
        if (_History!= null) {
            if (tob._History == null) {
                return false;
            }
            if (!_History.equals(tob._History)) {
                return false;
            }
        } else {
            if (tob._History!= null) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = 0;
        h = ((127 *h)+((_Result!= null)?_Result.hashCode(): 0));
        h = ((127 *h)+((_History!= null)?_History.hashCode(): 0));
        return h;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("<<AnalysisData");
        if (_Result!= null) {
            sb.append(" Result=");
            sb.append(_Result.toString());
        }
        if (_History!= null) {
            sb.append(" History=");
            sb.append(_History.toString());
        }
        sb.append(">>");
        return sb.toString();
    }

    public static Dispatcher newDispatcher() {
        Dispatcher d = new Dispatcher();
        d.register("AnalysisData", (AnalysisData.class));
        d.register("History", (History.class));
        d.register("Job", (Job.class));
        d.register("Result", (Result.class));
        d.register("attribute", (Attribute.class));
        d.register("parameter", (Parameter.class));
        d.freezeElementNameMap();
        return d;
    }


    private static class ResultPredicate
        implements PredicatedLists.Predicate
    {


        public void check(Object ob) {
            if (!(ob instanceof Result)) {
                throw new InvalidContentObjectException(ob, (Result.class));
            }
        }

    }


    private static class HistoryPredicate
        implements PredicatedLists.Predicate
    {


        public void check(Object ob) {
            if (!(ob instanceof History)) {
                throw new InvalidContentObjectException(ob, (History.class));
            }
        }

    }

}
