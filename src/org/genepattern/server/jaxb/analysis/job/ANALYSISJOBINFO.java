package org.genepattern.server.jaxb.analysis.job;
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
import javax.xml.bind.MissingContentException;
import javax.xml.bind.PredicatedLists;
import javax.xml.bind.PredicatedLists.Predicate;
import javax.xml.bind.RootElement;
import javax.xml.bind.StructureValidationException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidatableObject;
import javax.xml.bind.Validator;
import javax.xml.marshal.XMLScanner;
import javax.xml.marshal.XMLWriter;


public class ANALYSISJOBINFO
    extends MarshallableRootElement
    implements RootElement
{

    private List _JOB = PredicatedLists.createInvalidating(this, new JOBPredicate(), new ArrayList());
    private PredicatedLists.Predicate pred_JOB = new JOBPredicate();

    public List getJOB() {
        return _JOB;
    }

    public void deleteJOB() {
        _JOB = null;
        invalidate();
    }

    public void emptyJOB() {
        _JOB = PredicatedLists.createInvalidating(this, pred_JOB, new ArrayList());
    }

    public void validateThis()
        throws LocalValidationException
    {
        if (_JOB == null) {
            throw new MissingContentException("JOB");
        }
    }

    public void validate(Validator v)
        throws StructureValidationException
    {
        for (Iterator i = _JOB.iterator(); i.hasNext(); ) {
            v.validate(((ValidatableObject) i.next()));
        }
    }

    public void marshal(Marshaller m)
        throws IOException
    {
        XMLWriter w = m.writer();
        w.start("ANALYSISJOBINFO");
        for (Iterator i = _JOB.iterator(); i.hasNext(); ) {
            m.marshal(((MarshallableObject) i.next()));
        }
        w.end("ANALYSISJOBINFO");
    }

    public void unmarshal(Unmarshaller u)
        throws UnmarshalException
    {
        XMLScanner xs = u.scanner();
        Validator v = u.validator();
        xs.takeStart("ANALYSISJOBINFO");
        while (xs.atAttribute()) {
            String an = xs.takeAttributeName();
            throw new InvalidAttributeException(an);
        }
        {
            List l = PredicatedLists.create(this, pred_JOB, new ArrayList());
            while (xs.atStart("JOB")) {
                l.add(((JOB) u.unmarshal()));
            }
            _JOB = PredicatedLists.createInvalidating(this, pred_JOB, l);
        }
        xs.takeEnd("ANALYSISJOBINFO");
    }

    public static ANALYSISJOBINFO unmarshal(InputStream in)
        throws UnmarshalException
    {
        return unmarshal(XMLScanner.open(in));
    }

    public static ANALYSISJOBINFO unmarshal(XMLScanner xs)
        throws UnmarshalException
    {
        return unmarshal(xs, newDispatcher());
    }

    public static ANALYSISJOBINFO unmarshal(XMLScanner xs, Dispatcher d)
        throws UnmarshalException
    {
        return ((ANALYSISJOBINFO) d.unmarshal(xs, (ANALYSISJOBINFO.class)));
    }

    public boolean equals(Object ob) {
        if (this == ob) {
            return true;
        }
        if (!(ob instanceof ANALYSISJOBINFO)) {
            return false;
        }
        ANALYSISJOBINFO tob = ((ANALYSISJOBINFO) ob);
        if (_JOB!= null) {
            if (tob._JOB == null) {
                return false;
            }
            if (!_JOB.equals(tob._JOB)) {
                return false;
            }
        } else {
            if (tob._JOB!= null) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = 0;
        h = ((127 *h)+((_JOB!= null)?_JOB.hashCode(): 0));
        return h;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("<<ANALYSISJOBINFO");
        if (_JOB!= null) {
            sb.append(" JOB=");
            sb.append(_JOB.toString());
        }
        sb.append(">>");
        return sb.toString();
    }

    public static Dispatcher newDispatcher() {
        Dispatcher d = new Dispatcher();
        d.register("ANALYSISJOBINFO", (ANALYSISJOBINFO.class));
        d.register("JOB", (JOB.class));
        d.freezeElementNameMap();
        return d;
    }


    private static class JOBPredicate
        implements PredicatedLists.Predicate
    {


        public void check(Object ob) {
            if (!(ob instanceof JOB)) {
                throw new InvalidContentObjectException(ob, (JOB.class));
            }
        }

    }

}
