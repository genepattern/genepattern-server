package org.genepattern.server.jaxb.analysis.parameter;

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


public class ATTRIBUTE
    extends MarshallableObject
    implements Element
{

    private String _Key;
    private String _Content;

    public String getKey() {
        return _Key;
    }

    public void setKey(String _Key) {
        this._Key = _Key;
        if (_Key == null) {
            invalidate();
        }
    }

    public String getContent() {
        return _Content;
    }

    public void setContent(String _Content) {
        this._Content = _Content;
        if (_Content == null) {
            invalidate();
        }
    }

    public void validateThis()
        throws LocalValidationException
    {
        if (_Key == null) {
            throw new MissingAttributeException("key");
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
        w.start("ATTRIBUTE");
        w.attribute("key", _Key.toString());
        if (_Content!= null) {
            w.chars(_Content.toString());
        }
        w.end("ATTRIBUTE");
    }

    public void unmarshal(Unmarshaller u)
        throws UnmarshalException
    {
        XMLScanner xs = u.scanner();
        Validator v = u.validator();
        xs.takeStart("ATTRIBUTE");
        while (xs.atAttribute()) {
            String an = xs.takeAttributeName();
            if (an.equals("key")) {
                if (_Key!= null) {
                    throw new DuplicateAttributeException(an);
                }
                _Key = xs.takeAttributeValue();
                continue;
            }
            throw new InvalidAttributeException(an);
        }
        {
            String s;
            if (xs.atChars(XMLScanner.WS_COLLAPSE)) {
                s = xs.takeChars(XMLScanner.WS_COLLAPSE);
            } else {
                s = "";
            }
            try {
                _Content = String.valueOf(s);
            } catch (Exception x) {
                throw new ConversionException("content", x);
            }
        }
        xs.takeEnd("ATTRIBUTE");
    }

    public static ATTRIBUTE unmarshal(InputStream in)
        throws UnmarshalException
    {
        return unmarshal(XMLScanner.open(in));
    }

    public static ATTRIBUTE unmarshal(XMLScanner xs)
        throws UnmarshalException
    {
        return unmarshal(xs, newDispatcher());
    }

    public static ATTRIBUTE unmarshal(XMLScanner xs, Dispatcher d)
        throws UnmarshalException
    {
        return ((ATTRIBUTE) d.unmarshal(xs, (ATTRIBUTE.class)));
    }

    public boolean equals(Object ob) {
        if (this == ob) {
            return true;
        }
        if (!(ob instanceof ATTRIBUTE)) {
            return false;
        }
        ATTRIBUTE tob = ((ATTRIBUTE) ob);
        if (_Key!= null) {
            if (tob._Key == null) {
                return false;
            }
            if (!_Key.equals(tob._Key)) {
                return false;
            }
        } else {
            if (tob._Key!= null) {
                return false;
            }
        }
        if (_Content!= null) {
            if (tob._Content == null) {
                return false;
            }
            if (!_Content.equals(tob._Content)) {
                return false;
            }
        } else {
            if (tob._Content!= null) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = 0;
        h = ((127 *h)+((_Key!= null)?_Key.hashCode(): 0));
        h = ((127 *h)+((_Content!= null)?_Content.hashCode(): 0));
        return h;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("<<ATTRIBUTE");
        if (_Key!= null) {
            sb.append(" key=");
            sb.append(_Key.toString());
        }
        if (_Content!= null) {
            sb.append(" content=");
            sb.append(_Content.toString());
        }
        sb.append(">>");
        return sb.toString();
    }

    public static Dispatcher newDispatcher() {
        return ANALYSISPARAMETERS.newDispatcher();
    }

}
