package org.genepattern.gpge.ui.analysis.jaxb;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.Dispatcher;
import javax.xml.bind.DuplicateAttributeException;
import javax.xml.bind.Element;
import javax.xml.bind.InvalidAttributeException;
import javax.xml.bind.InvalidContentObjectException;
import javax.xml.bind.LocalValidationException;
import javax.xml.bind.MarshallableObject;
import javax.xml.bind.Marshaller;
import javax.xml.bind.MissingAttributeException;
import javax.xml.bind.PredicatedLists;
import javax.xml.bind.StructureValidationException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidatableObject;
import javax.xml.bind.Validator;
import javax.xml.marshal.XMLScanner;
import javax.xml.marshal.XMLWriter;


public class Result
    extends MarshallableObject
    implements Element
{

    private String _SiteName;
    private String _TaskName;
    private List _Job = PredicatedLists.createInvalidating(this, new JobPredicate(), new ArrayList());
    private PredicatedLists.Predicate pred_Job = new JobPredicate();

    public String getSiteName() {
        return _SiteName;
    }

    public void setSiteName(String _SiteName) {
        this._SiteName = _SiteName;
        if (_SiteName == null) {
            invalidate();
        }
    }

    public String getTaskName() {
        return _TaskName;
    }

    public void setTaskName(String _TaskName) {
        this._TaskName = _TaskName;
        if (_TaskName == null) {
            invalidate();
        }
    }

    public List getJob() {
        return _Job;
    }

    public void deleteJob() {
        _Job = null;
        invalidate();
    }

    public void emptyJob() {
        _Job = PredicatedLists.createInvalidating(this, pred_Job, new ArrayList());
    }

    public void validateThis()
        throws LocalValidationException
    {
        if (_SiteName == null) {
            throw new MissingAttributeException("siteName");
        }
        if (_TaskName == null) {
            throw new MissingAttributeException("taskName");
        }
    }

    public void validate(Validator v)
        throws StructureValidationException
    {
        for (Iterator i = _Job.iterator(); i.hasNext(); ) {
            v.validate(((ValidatableObject) i.next()));
        }
    }

    public void marshal(Marshaller m)
        throws IOException
    {
        XMLWriter w = m.writer();
        w.start("Result");
        w.attribute("siteName", _SiteName.toString());
        w.attribute("taskName", _TaskName.toString());
        if (_Job.size()> 0) {
            for (Iterator i = _Job.iterator(); i.hasNext(); ) {
                m.marshal(((MarshallableObject) i.next()));
            }
        }
        w.end("Result");
    }

    public void unmarshal(Unmarshaller u)
        throws UnmarshalException
    {
        XMLScanner xs = u.scanner();
        Validator v = u.validator();
        xs.takeStart("Result");
        while (xs.atAttribute()) {
            String an = xs.takeAttributeName();
            if (an.equals("siteName")) {
                if (_SiteName!= null) {
                    throw new DuplicateAttributeException(an);
                }
                _SiteName = xs.takeAttributeValue();
                continue;
            }
            if (an.equals("taskName")) {
                if (_TaskName!= null) {
                    throw new DuplicateAttributeException(an);
                }
                _TaskName = xs.takeAttributeValue();
                continue;
            }
            throw new InvalidAttributeException(an);
        }
        {
            List l = PredicatedLists.create(this, pred_Job, new ArrayList());
            while (xs.atStart("Job")) {
                l.add(((Job) u.unmarshal()));
            }
            _Job = PredicatedLists.createInvalidating(this, pred_Job, l);
        }
        xs.takeEnd("Result");
    }

    public static Result unmarshal(InputStream in)
        throws UnmarshalException
    {
        return unmarshal(XMLScanner.open(in));
    }

    public static Result unmarshal(XMLScanner xs)
        throws UnmarshalException
    {
        return unmarshal(xs, newDispatcher());
    }

    public static Result unmarshal(XMLScanner xs, Dispatcher d)
        throws UnmarshalException
    {
        return ((Result) d.unmarshal(xs, (Result.class)));
    }

    public boolean equals(Object ob) {
        if (this == ob) {
            return true;
        }
        if (!(ob instanceof Result)) {
            return false;
        }
        Result tob = ((Result) ob);
        if (_SiteName!= null) {
            if (tob._SiteName == null) {
                return false;
            }
            if (!_SiteName.equals(tob._SiteName)) {
                return false;
            }
        } else {
            if (tob._SiteName!= null) {
                return false;
            }
        }
        if (_TaskName!= null) {
            if (tob._TaskName == null) {
                return false;
            }
            if (!_TaskName.equals(tob._TaskName)) {
                return false;
            }
        } else {
            if (tob._TaskName!= null) {
                return false;
            }
        }
        if (_Job!= null) {
            if (tob._Job == null) {
                return false;
            }
            if (!_Job.equals(tob._Job)) {
                return false;
            }
        } else {
            if (tob._Job!= null) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = 0;
        h = ((127 *h)+((_SiteName!= null)?_SiteName.hashCode(): 0));
        h = ((127 *h)+((_TaskName!= null)?_TaskName.hashCode(): 0));
        h = ((127 *h)+((_Job!= null)?_Job.hashCode(): 0));
        return h;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("<<Result");
        if (_SiteName!= null) {
            sb.append(" siteName=");
            sb.append(_SiteName.toString());
        }
        if (_TaskName!= null) {
            sb.append(" taskName=");
            sb.append(_TaskName.toString());
        }
        if (_Job!= null) {
            sb.append(" Job=");
            sb.append(_Job.toString());
        }
        sb.append(">>");
        return sb.toString();
    }

    public static Dispatcher newDispatcher() {
        return AnalysisData.newDispatcher();
    }


    private static class JobPredicate
        implements PredicatedLists.Predicate
    {


        public void check(Object ob) {
            if (!(ob instanceof Job)) {
                throw new InvalidContentObjectException(ob, (Job.class));
            }
        }

    }

}
