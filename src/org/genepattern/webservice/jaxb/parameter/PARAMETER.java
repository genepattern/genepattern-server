package org.genepattern.webservice.jaxb.parameter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.ConversionException;
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

public class PARAMETER extends MarshallableObject implements Element {

	private String _Name;

	private String _Value;

	private String _DESCRIPTION;

	private List _ATTRIBUTE = PredicatedLists.createInvalidating(this,
			new ATTRIBUTEPredicate(), new ArrayList());

	private PredicatedLists.Predicate pred_ATTRIBUTE = new ATTRIBUTEPredicate();

	public String getName() {
		return _Name;
	}

	public void setName(String _Name) {
		this._Name = _Name;
		if (_Name == null) {
			invalidate();
		}
	}

	public String getValue() {
		return _Value;
	}

	public void setValue(String _Value) {
		this._Value = _Value;
		if (_Value == null) {
			invalidate();
		}
	}

	public String getDESCRIPTION() {
		return _DESCRIPTION;
	}

	public void setDESCRIPTION(String _DESCRIPTION) {
		this._DESCRIPTION = _DESCRIPTION;
		if (_DESCRIPTION == null) {
			invalidate();
		}
	}

	public List getATTRIBUTE() {
		return _ATTRIBUTE;
	}

	public void deleteATTRIBUTE() {
		_ATTRIBUTE = null;
		invalidate();
	}

	public void emptyATTRIBUTE() {
		_ATTRIBUTE = PredicatedLists.createInvalidating(this, pred_ATTRIBUTE,
				new ArrayList());
	}

	public void validateThis() throws LocalValidationException {
		if (_Name == null) {
			throw new MissingAttributeException("name");
		}
	}

	public void validate(Validator v) throws StructureValidationException {
		for (Iterator i = _ATTRIBUTE.iterator(); i.hasNext();) {
			v.validate(((ValidatableObject) i.next()));
		}
	}

	public void marshal(Marshaller m) throws IOException {
		XMLWriter w = m.writer();
		w.start("PARAMETER");
		w.attribute("name", _Name.toString());
		if (_Value != null) {
			w.attribute("value", _Value.toString());
		}
		if (_DESCRIPTION != null) {
			w.leaf("DESCRIPTION", _DESCRIPTION.toString());
		}
		if (_ATTRIBUTE.size() > 0) {
			for (Iterator i = _ATTRIBUTE.iterator(); i.hasNext();) {
				m.marshal(((MarshallableObject) i.next()));
			}
		}
		w.end("PARAMETER");
	}

	public void unmarshal(Unmarshaller u) throws UnmarshalException {
		XMLScanner xs = u.scanner();
		Validator v = u.validator();
		xs.takeStart("PARAMETER");
		while (xs.atAttribute()) {
			String an = xs.takeAttributeName();
			if (an.equals("name")) {
				if (_Name != null) {
					throw new DuplicateAttributeException(an);
				}
				_Name = xs.takeAttributeValue();
				continue;
			}
			if (an.equals("value")) {
				if (_Value != null) {
					throw new DuplicateAttributeException(an);
				}
				_Value = xs.takeAttributeValue();
				continue;
			}
			throw new InvalidAttributeException(an);
		}
		if (xs.atStart("DESCRIPTION")) {
			xs.takeStart("DESCRIPTION");
			String s;
			if (xs.atChars(XMLScanner.WS_COLLAPSE)) {
				s = xs.takeChars(XMLScanner.WS_COLLAPSE);
			} else {
				s = "";
			}
			try {
				_DESCRIPTION = String.valueOf(s);
			} catch (Exception x) {
				throw new ConversionException("DESCRIPTION", x);
			}
			xs.takeEnd("DESCRIPTION");
		}
		{
			List l = PredicatedLists.create(this, pred_ATTRIBUTE,
					new ArrayList());
			while (xs.atStart("ATTRIBUTE")) {
				l.add(((ATTRIBUTE) u.unmarshal()));
			}
			_ATTRIBUTE = PredicatedLists.createInvalidating(this,
					pred_ATTRIBUTE, l);
		}
		xs.takeEnd("PARAMETER");
	}

	public static PARAMETER unmarshal(InputStream in) throws UnmarshalException {
		return unmarshal(XMLScanner.open(in));
	}

	public static PARAMETER unmarshal(XMLScanner xs) throws UnmarshalException {
		return unmarshal(xs, newDispatcher());
	}

	public static PARAMETER unmarshal(XMLScanner xs, Dispatcher d)
			throws UnmarshalException {
		return ((PARAMETER) d.unmarshal(xs, (PARAMETER.class)));
	}

	public boolean equals(Object ob) {
		if (this == ob) {
			return true;
		}
		if (!(ob instanceof PARAMETER)) {
			return false;
		}
		PARAMETER tob = ((PARAMETER) ob);
		if (_Name != null) {
			if (tob._Name == null) {
				return false;
			}
			if (!_Name.equals(tob._Name)) {
				return false;
			}
		} else {
			if (tob._Name != null) {
				return false;
			}
		}
		if (_Value != null) {
			if (tob._Value == null) {
				return false;
			}
			if (!_Value.equals(tob._Value)) {
				return false;
			}
		} else {
			if (tob._Value != null) {
				return false;
			}
		}
		if (_DESCRIPTION != null) {
			if (tob._DESCRIPTION == null) {
				return false;
			}
			if (!_DESCRIPTION.equals(tob._DESCRIPTION)) {
				return false;
			}
		} else {
			if (tob._DESCRIPTION != null) {
				return false;
			}
		}
		if (_ATTRIBUTE != null) {
			if (tob._ATTRIBUTE == null) {
				return false;
			}
			if (!_ATTRIBUTE.equals(tob._ATTRIBUTE)) {
				return false;
			}
		} else {
			if (tob._ATTRIBUTE != null) {
				return false;
			}
		}
		return true;
	}

	public int hashCode() {
		int h = 0;
		h = ((127 * h) + ((_Name != null) ? _Name.hashCode() : 0));
		h = ((127 * h) + ((_Value != null) ? _Value.hashCode() : 0));
		h = ((127 * h) + ((_DESCRIPTION != null) ? _DESCRIPTION.hashCode() : 0));
		h = ((127 * h) + ((_ATTRIBUTE != null) ? _ATTRIBUTE.hashCode() : 0));
		return h;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<<PARAMETER");
		if (_Name != null) {
			sb.append(" name=");
			sb.append(_Name.toString());
		}
		if (_Value != null) {
			sb.append(" value=");
			sb.append(_Value.toString());
		}
		if (_DESCRIPTION != null) {
			sb.append(" DESCRIPTION=");
			sb.append(_DESCRIPTION.toString());
		}
		if (_ATTRIBUTE != null) {
			sb.append(" ATTRIBUTE=");
			sb.append(_ATTRIBUTE.toString());
		}
		sb.append(">>");
		return sb.toString();
	}

	public static Dispatcher newDispatcher() {
		return ANALYSISPARAMETERS.newDispatcher();
	}

	private static class ATTRIBUTEPredicate implements
			PredicatedLists.Predicate {

		public void check(Object ob) {
			if (!(ob instanceof ATTRIBUTE)) {
				throw new InvalidContentObjectException(ob, (ATTRIBUTE.class));
			}
		}

	}

}