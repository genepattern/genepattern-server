package org.genepattern.webservice.jaxb.task;

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
import javax.xml.bind.RootElement;
import javax.xml.bind.StructureValidationException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidatableObject;
import javax.xml.bind.Validator;
import javax.xml.marshal.XMLScanner;
import javax.xml.marshal.XMLWriter;


public class ANALYSISTASKINFO
    extends MarshallableRootElement
    implements RootElement
{

    private List _TASK = PredicatedLists.createInvalidating(this, new TASKPredicate(), new ArrayList());
    private PredicatedLists.Predicate pred_TASK = new TASKPredicate();

    public List getTASK() {
        return _TASK;
    }

    public void deleteTASK() {
        _TASK = null;
        invalidate();
    }

    public void emptyTASK() {
        _TASK = PredicatedLists.createInvalidating(this, pred_TASK, new ArrayList());
    }

    public void validateThis()
        throws LocalValidationException
    {
        if (_TASK == null) {
            throw new MissingContentException("TASK");
        }
    }

    public void validate(Validator v)
        throws StructureValidationException
    {
        for (Iterator i = _TASK.iterator(); i.hasNext(); ) {
            v.validate(((ValidatableObject) i.next()));
        }
    }

    public void marshal(Marshaller m)
        throws IOException
    {
        XMLWriter w = m.writer();
        w.start("ANALYSISTASKINFO");
        for (Iterator i = _TASK.iterator(); i.hasNext(); ) {
            m.marshal(((MarshallableObject) i.next()));
        }
        w.end("ANALYSISTASKINFO");
    }

    public void unmarshal(Unmarshaller u)
        throws UnmarshalException
    {
        XMLScanner xs = u.scanner();
        Validator v = u.validator();
        xs.takeStart("ANALYSISTASKINFO");
        while (xs.atAttribute()) {
            String an = xs.takeAttributeName();
            throw new InvalidAttributeException(an);
        }
        {
            List l = PredicatedLists.create(this, pred_TASK, new ArrayList());
            while (xs.atStart("TASK")) {
                l.add(((TASK) u.unmarshal()));
            }
            _TASK = PredicatedLists.createInvalidating(this, pred_TASK, l);
        }
        xs.takeEnd("ANALYSISTASKINFO");
    }

    public static ANALYSISTASKINFO unmarshal(InputStream in)
        throws UnmarshalException
    {
        return unmarshal(XMLScanner.open(in));
    }

    public static ANALYSISTASKINFO unmarshal(XMLScanner xs)
        throws UnmarshalException
    {
        return unmarshal(xs, newDispatcher());
    }

    public static ANALYSISTASKINFO unmarshal(XMLScanner xs, Dispatcher d)
        throws UnmarshalException
    {
        return ((ANALYSISTASKINFO) d.unmarshal(xs, (ANALYSISTASKINFO.class)));
    }

    public boolean equals(Object ob) {
        if (this == ob) {
            return true;
        }
        if (!(ob instanceof ANALYSISTASKINFO)) {
            return false;
        }
        ANALYSISTASKINFO tob = ((ANALYSISTASKINFO) ob);
        if (_TASK!= null) {
            if (tob._TASK == null) {
                return false;
            }
            if (!_TASK.equals(tob._TASK)) {
                return false;
            }
        } else {
            if (tob._TASK!= null) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = 0;
        h = ((127 *h)+((_TASK!= null)?_TASK.hashCode(): 0));
        return h;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("<<ANALYSISTASKINFO");
        if (_TASK!= null) {
            sb.append(" TASK=");
            sb.append(_TASK.toString());
        }
        sb.append(">>");
        return sb.toString();
    }

    public static Dispatcher newDispatcher() {
        Dispatcher d = new Dispatcher();
        d.register("ANALYSISTASKINFO", (ANALYSISTASKINFO.class));
        d.register("TASK", (TASK.class));
        d.freezeElementNameMap();
        return d;
    }


    private static class TASKPredicate
        implements PredicatedLists.Predicate
    {


        public void check(Object ob) {
            if (!(ob instanceof TASK)) {
                throw new InvalidContentObjectException(ob, (TASK.class));
            }
        }

    }

}
