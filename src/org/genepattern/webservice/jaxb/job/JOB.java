package org.genepattern.analysis.jaxb.job;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.ConversionException;
import javax.xml.bind.Dispatcher;
import javax.xml.bind.DuplicateAttributeException;
import javax.xml.bind.Element;
import javax.xml.bind.InvalidAttributeException;
import javax.xml.bind.LocalValidationException;
import javax.xml.bind.MarshallableObject;
import javax.xml.bind.Marshaller;
import javax.xml.bind.MissingAttributeException;
import javax.xml.bind.StructureValidationException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Validator;
import javax.xml.marshal.XMLScanner;
import javax.xml.marshal.XMLWriter;


public class JOB
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
    private String _ANALYSISPARAMETERS;

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

    public String getANALYSISPARAMETERS() {
        return _ANALYSISPARAMETERS;
    }

    public void setANALYSISPARAMETERS(String _ANALYSISPARAMETERS) {
        this._ANALYSISPARAMETERS = _ANALYSISPARAMETERS;
        if (_ANALYSISPARAMETERS == null) {
            invalidate();
        }
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
    }

    public void marshal(Marshaller m)
        throws IOException
    {
        XMLWriter w = m.writer();
        w.start("JOB");
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
        if (_ANALYSISPARAMETERS!= null) {
            w.leaf("ANALYSISPARAMETERS", _ANALYSISPARAMETERS.toString());
        }
        w.end("JOB");
    }

    public void unmarshal(Unmarshaller u)
        throws UnmarshalException
    {
        XMLScanner xs = u.scanner();
        Validator v = u.validator();
        xs.takeStart("JOB");
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
        if (xs.atStart("ANALYSISPARAMETERS")) {
            xs.takeStart("ANALYSISPARAMETERS");
            String s;
            if (xs.atChars(XMLScanner.WS_COLLAPSE)) {
                s = xs.takeChars(XMLScanner.WS_COLLAPSE);
            } else {
                s = "";
            }
            try {
                _ANALYSISPARAMETERS = String.valueOf(s);
            } catch (Exception x) {
                throw new ConversionException("ANALYSISPARAMETERS", x);
            }
            xs.takeEnd("ANALYSISPARAMETERS");
        }
        xs.takeEnd("JOB");
    }

    public static JOB unmarshal(InputStream in)
        throws UnmarshalException
    {
        return unmarshal(XMLScanner.open(in));
    }

    public static JOB unmarshal(XMLScanner xs)
        throws UnmarshalException
    {
        return unmarshal(xs, newDispatcher());
    }

    public static JOB unmarshal(XMLScanner xs, Dispatcher d)
        throws UnmarshalException
    {
        return ((JOB) d.unmarshal(xs, (JOB.class)));
    }

    public boolean equals(Object ob) {
        if (this == ob) {
            return true;
        }
        if (!(ob instanceof JOB)) {
            return false;
        }
        JOB tob = ((JOB) ob);
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
        if (_ANALYSISPARAMETERS!= null) {
            if (tob._ANALYSISPARAMETERS == null) {
                return false;
            }
            if (!_ANALYSISPARAMETERS.equals(tob._ANALYSISPARAMETERS)) {
                return false;
            }
        } else {
            if (tob._ANALYSISPARAMETERS!= null) {
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
        h = ((127 *h)+((_ANALYSISPARAMETERS!= null)?_ANALYSISPARAMETERS.hashCode(): 0));
        return h;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("<<JOB");
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
        if (_ANALYSISPARAMETERS!= null) {
            sb.append(" ANALYSISPARAMETERS=");
            sb.append(_ANALYSISPARAMETERS.toString());
        }
        sb.append(">>");
        return sb.toString();
    }

    public static Dispatcher newDispatcher() {
        return ANALYSISJOBINFO.newDispatcher();
    }

}
