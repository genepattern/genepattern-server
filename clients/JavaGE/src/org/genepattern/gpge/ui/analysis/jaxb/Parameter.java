package org.genepattern.gpge.ui.analysis.jaxb;

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

public class Parameter extends MarshallableObject implements Element {

	private String _Name;

	private String _Value;

	private String _Description;

	private List _Attribute = PredicatedLists.createInvalidating(this,
			new AttributePredicate(), new ArrayList());

	private PredicatedLists.Predicate pred_Attribute = new AttributePredicate();

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

	public String getDescription() {
		return _Description;
	}

	public void setDescription(String _Description) {
		this._Description = _Description;
		if (_Description == null) {
			invalidate();
		}
	}

	public List getAttribute() {
		return _Attribute;
	}

	public void deleteAttribute() {
		_Attribute = null;
		invalidate();
	}

	public void emptyAttribute() {
		_Attribute = PredicatedLists.createInvalidating(this, pred_Attribute,
				new ArrayList());
	}

	public void validateThis() throws LocalValidationException {
		if (_Name == null) {
			throw new MissingAttributeException("name");
		}
	}

	public void validate(Validator v) throws StructureValidationException {
		for (Iterator i = _Attribute.iterator(); i.hasNext();) {
			v.validate(((ValidatableObject) i.next()));
		}
	}

	public void marshal(Marshaller m) throws IOException {
		XMLWriter w = m.writer();
		w.start("parameter");
		w.attribute("name", _Name.toString());
		if (_Value != null) {
			w.attribute("value", _Value.toString());
		}
		if (_Description != null) {
			w.leaf("description", _Description.toString());
		}
		if (_Attribute.size() > 0) {
			for (Iterator i = _Attribute.iterator(); i.hasNext();) {
				m.marshal(((MarshallableObject) i.next()));
			}
		}
		w.end("parameter");
	}

	public void unmarshal(Unmarshaller u) throws UnmarshalException {
		XMLScanner xs = u.scanner();
		Validator v = u.validator();
		xs.takeStart("parameter");
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
		if (xs.atStart("description")) {
			xs.takeStart("description");
			String s;
			if (xs.atChars(XMLScanner.WS_COLLAPSE)) {
				s = xs.takeChars(XMLScanner.WS_COLLAPSE);
			} else {
				s = "";
			}
			try {
				_Description = String.valueOf(s);
			} catch (Exception x) {
				throw new ConversionException("description", x);
			}
			xs.takeEnd("description");
		}
		{
			List l = PredicatedLists.create(this, pred_Attribute,
					new ArrayList());
			while (xs.atStart("attribute")) {
				l.add(((Attribute) u.unmarshal()));
			}
			_Attribute = PredicatedLists.createInvalidating(this,
					pred_Attribute, l);
		}
		xs.takeEnd("parameter");
	}

	public static Parameter unmarshal(InputStream in) throws UnmarshalException {
		return unmarshal(XMLScanner.open(in));
	}

	public static Parameter unmarshal(XMLScanner xs) throws UnmarshalException {
		return unmarshal(xs, newDispatcher());
	}

	public static Parameter unmarshal(XMLScanner xs, Dispatcher d)
			throws UnmarshalException {
		return ((Parameter) d.unmarshal(xs, (Parameter.class)));
	}

	public boolean equals(Object ob) {
		if (this == ob) {
			return true;
		}
		if (!(ob instanceof Parameter)) {
			return false;
		}
		Parameter tob = ((Parameter) ob);
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
		if (_Description != null) {
			if (tob._Description == null) {
				return false;
			}
			if (!_Description.equals(tob._Description)) {
				return false;
			}
		} else {
			if (tob._Description != null) {
				return false;
			}
		}
		if (_Attribute != null) {
			if (tob._Attribute == null) {
				return false;
			}
			if (!_Attribute.equals(tob._Attribute)) {
				return false;
			}
		} else {
			if (tob._Attribute != null) {
				return false;
			}
		}
		return true;
	}

	public int hashCode() {
		int h = 0;
		h = ((127 * h) + ((_Name != null) ? _Name.hashCode() : 0));
		h = ((127 * h) + ((_Value != null) ? _Value.hashCode() : 0));
		h = ((127 * h) + ((_Description != null) ? _Description.hashCode() : 0));
		h = ((127 * h) + ((_Attribute != null) ? _Attribute.hashCode() : 0));
		return h;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<<parameter");
		if (_Name != null) {
			sb.append(" name=");
			sb.append(_Name.toString());
		}
		if (_Value != null) {
			sb.append(" value=");
			sb.append(_Value.toString());
		}
		if (_Description != null) {
			sb.append(" description=");
			sb.append(_Description.toString());
		}
		if (_Attribute != null) {
			sb.append(" attribute=");
			sb.append(_Attribute.toString());
		}
		sb.append(">>");
		return sb.toString();
	}

	public static Dispatcher newDispatcher() {
		return AnalysisData.newDispatcher();
	}

	private static class AttributePredicate implements
			PredicatedLists.Predicate {

		public void check(Object ob) {
			if (!(ob instanceof Attribute)) {
				throw new InvalidContentObjectException(ob, (Attribute.class));
			}
		}

	}

}