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
import javax.xml.bind.PredicatedLists.Predicate;
import javax.xml.bind.StructureValidationException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidatableObject;
import javax.xml.bind.Validator;
import javax.xml.marshal.XMLScanner;
import javax.xml.marshal.XMLWriter;


public class Job
    extends MarshallableObject
    implements Element
{

    private String _Id;
    private String _TaskId;
    private String _Status;
    private String _DateSubmitted;
    private String _DateCompleted;
    private String _InputFilename;
    private String _ResultFilename;
    private List _Parameter = PredicatedLists.createInvalidating(this, new ParameterPredicate(), new ArrayList());
    private PredicatedLists.Predicate pred_Parameter = new ParameterPredicate();

    public String getId() {
        return _Id;
    }

    public void setId(String _Id) {
        this._Id = _Id;
        if (_Id == null) {
            invalidate();
        }
    }

    public String getTaskId() {
        return _TaskId;
    }

    public void setTaskId(String _TaskId) {
        this._TaskId = _TaskId;
        if (_TaskId == null) {
            invalidate();
        }
    }

    public String getStatus() {
        return _Status;
    }

    public void setStatus(String _Status) {
        this._Status = _Status;
        if (_Status == null) {
            invalidate();
        }
    }

    public String getDateSubmitted() {
        return _DateSubmitted;
    }

    public void setDateSubmitted(String _DateSubmitted) {
        this._DateSubmitted = _DateSubmitted;
        if (_DateSubmitted == null) {
            invalidate();
        }
    }

    public String getDateCompleted() {
        return _DateCompleted;
    }

    public void setDateCompleted(String _DateCompleted) {
        this._DateCompleted = _DateCompleted;
        if (_DateCompleted == null) {
            invalidate();
        }
    }

    public String getInputFilename() {
        return _InputFilename;
    }

    public void setInputFilename(String _InputFilename) {
        this._InputFilename = _InputFilename;
        if (_InputFilename == null) {
            invalidate();
        }
    }

    public String getResultFilename() {
        return _ResultFilename;
    }

    public void setResultFilename(String _ResultFilename) {
        this._ResultFilename = _ResultFilename;
        if (_ResultFilename == null) {
            invalidate();
        }
    }

    public List getParameter() {
        return _Parameter;
    }

    public void deleteParameter() {
        _Parameter = null;
        invalidate();
    }

    public void emptyParameter() {
        _Parameter = PredicatedLists.createInvalidating(this, pred_Parameter, new ArrayList());
    }

    public void validateThis()
        throws LocalValidationException
    {
        if (_Id == null) {
            throw new MissingAttributeException("id");
        }
        if (_TaskId == null) {
            throw new MissingAttributeException("task_id");
        }
        if (_Status == null) {
            throw new MissingAttributeException("status");
        }
        if (_DateSubmitted == null) {
            throw new MissingAttributeException("date_submitted");
        }
    }

    public void validate(Validator v)
        throws StructureValidationException
    {
        for (Iterator i = _Parameter.iterator(); i.hasNext(); ) {
            v.validate(((ValidatableObject) i.next()));
        }
    }

    public void marshal(Marshaller m)
        throws IOException
    {
        XMLWriter w = m.writer();
        w.start("Job");
        w.attribute("id", _Id.toString());
        w.attribute("task_id", _TaskId.toString());
        w.attribute("status", _Status.toString());
        w.attribute("date_submitted", _DateSubmitted.toString());
        if (_DateCompleted!= null) {
            w.attribute("date_completed", _DateCompleted.toString());
        }
        if (_InputFilename!= null) {
            w.attribute("input_filename", _InputFilename.toString());
        }
        if (_ResultFilename!= null) {
            w.attribute("result_filename", _ResultFilename.toString());
        }
        if (_Parameter.size()> 0) {
            for (Iterator i = _Parameter.iterator(); i.hasNext(); ) {
                m.marshal(((MarshallableObject) i.next()));
            }
        }
        w.end("Job");
    }

    public void unmarshal(Unmarshaller u)
        throws UnmarshalException
    {
        XMLScanner xs = u.scanner();
        Validator v = u.validator();
        xs.takeStart("Job");
        while (xs.atAttribute()) {
            String an = xs.takeAttributeName();
            if (an.equals("id")) {
                if (_Id!= null) {
                    throw new DuplicateAttributeException(an);
                }
                _Id = xs.takeAttributeValue();
                continue;
            }
            if (an.equals("task_id")) {
                if (_TaskId!= null) {
                    throw new DuplicateAttributeException(an);
                }
                _TaskId = xs.takeAttributeValue();
                continue;
            }
            if (an.equals("status")) {
                if (_Status!= null) {
                    throw new DuplicateAttributeException(an);
                }
                _Status = xs.takeAttributeValue();
                continue;
            }
            if (an.equals("date_submitted")) {
                if (_DateSubmitted!= null) {
                    throw new DuplicateAttributeException(an);
                }
                _DateSubmitted = xs.takeAttributeValue();
                continue;
            }
            if (an.equals("date_completed")) {
                if (_DateCompleted!= null) {
                    throw new DuplicateAttributeException(an);
                }
                _DateCompleted = xs.takeAttributeValue();
                continue;
            }
            if (an.equals("input_filename")) {
                if (_InputFilename!= null) {
                    throw new DuplicateAttributeException(an);
                }
                _InputFilename = xs.takeAttributeValue();
                continue;
            }
            if (an.equals("result_filename")) {
                if (_ResultFilename!= null) {
                    throw new DuplicateAttributeException(an);
                }
                _ResultFilename = xs.takeAttributeValue();
                continue;
            }
            throw new InvalidAttributeException(an);
        }
        {
            List l = PredicatedLists.create(this, pred_Parameter, new ArrayList());
            while (xs.atStart("parameter")) {
                l.add(((Parameter) u.unmarshal()));
            }
            _Parameter = PredicatedLists.createInvalidating(this, pred_Parameter, l);
        }
        xs.takeEnd("Job");
    }

    public static Job unmarshal(InputStream in)
        throws UnmarshalException
    {
        return unmarshal(XMLScanner.open(in));
    }

    public static Job unmarshal(XMLScanner xs)
        throws UnmarshalException
    {
        return unmarshal(xs, newDispatcher());
    }

    public static Job unmarshal(XMLScanner xs, Dispatcher d)
        throws UnmarshalException
    {
        return ((Job) d.unmarshal(xs, (Job.class)));
    }

    public boolean equals(Object ob) {
        if (this == ob) {
            return true;
        }
        if (!(ob instanceof Job)) {
            return false;
        }
        Job tob = ((Job) ob);
        if (_Id!= null) {
            if (tob._Id == null) {
                return false;
            }
            if (!_Id.equals(tob._Id)) {
                return false;
            }
        } else {
            if (tob._Id!= null) {
                return false;
            }
        }
        if (_TaskId!= null) {
            if (tob._TaskId == null) {
                return false;
            }
            if (!_TaskId.equals(tob._TaskId)) {
                return false;
            }
        } else {
            if (tob._TaskId!= null) {
                return false;
            }
        }
        if (_Status!= null) {
            if (tob._Status == null) {
                return false;
            }
            if (!_Status.equals(tob._Status)) {
                return false;
            }
        } else {
            if (tob._Status!= null) {
                return false;
            }
        }
        if (_DateSubmitted!= null) {
            if (tob._DateSubmitted == null) {
                return false;
            }
            if (!_DateSubmitted.equals(tob._DateSubmitted)) {
                return false;
            }
        } else {
            if (tob._DateSubmitted!= null) {
                return false;
            }
        }
        if (_DateCompleted!= null) {
            if (tob._DateCompleted == null) {
                return false;
            }
            if (!_DateCompleted.equals(tob._DateCompleted)) {
                return false;
            }
        } else {
            if (tob._DateCompleted!= null) {
                return false;
            }
        }
        if (_InputFilename!= null) {
            if (tob._InputFilename == null) {
                return false;
            }
            if (!_InputFilename.equals(tob._InputFilename)) {
                return false;
            }
        } else {
            if (tob._InputFilename!= null) {
                return false;
            }
        }
        if (_ResultFilename!= null) {
            if (tob._ResultFilename == null) {
                return false;
            }
            if (!_ResultFilename.equals(tob._ResultFilename)) {
                return false;
            }
        } else {
            if (tob._ResultFilename!= null) {
                return false;
            }
        }
        if (_Parameter!= null) {
            if (tob._Parameter == null) {
                return false;
            }
            if (!_Parameter.equals(tob._Parameter)) {
                return false;
            }
        } else {
            if (tob._Parameter!= null) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = 0;
        h = ((127 *h)+((_Id!= null)?_Id.hashCode(): 0));
        h = ((127 *h)+((_TaskId!= null)?_TaskId.hashCode(): 0));
        h = ((127 *h)+((_Status!= null)?_Status.hashCode(): 0));
        h = ((127 *h)+((_DateSubmitted!= null)?_DateSubmitted.hashCode(): 0));
        h = ((127 *h)+((_DateCompleted!= null)?_DateCompleted.hashCode(): 0));
        h = ((127 *h)+((_InputFilename!= null)?_InputFilename.hashCode(): 0));
        h = ((127 *h)+((_ResultFilename!= null)?_ResultFilename.hashCode(): 0));
        h = ((127 *h)+((_Parameter!= null)?_Parameter.hashCode(): 0));
        return h;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("<<Job");
        if (_Id!= null) {
            sb.append(" id=");
            sb.append(_Id.toString());
        }
        if (_TaskId!= null) {
            sb.append(" task_id=");
            sb.append(_TaskId.toString());
        }
        if (_Status!= null) {
            sb.append(" status=");
            sb.append(_Status.toString());
        }
        if (_DateSubmitted!= null) {
            sb.append(" date_submitted=");
            sb.append(_DateSubmitted.toString());
        }
        if (_DateCompleted!= null) {
            sb.append(" date_completed=");
            sb.append(_DateCompleted.toString());
        }
        if (_InputFilename!= null) {
            sb.append(" input_filename=");
            sb.append(_InputFilename.toString());
        }
        if (_ResultFilename!= null) {
            sb.append(" result_filename=");
            sb.append(_ResultFilename.toString());
        }
        if (_Parameter!= null) {
            sb.append(" parameter=");
            sb.append(_Parameter.toString());
        }
        sb.append(">>");
        return sb.toString();
    }

    public static Dispatcher newDispatcher() {
        return AnalysisData.newDispatcher();
    }


    private static class ParameterPredicate
        implements PredicatedLists.Predicate
    {


        public void check(Object ob) {
            if (!(ob instanceof Parameter)) {
                throw new InvalidContentObjectException(ob, (Parameter.class));
            }
        }

    }

}
