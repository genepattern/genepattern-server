package org.genepattern.webservice.jaxb.task;

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

public class TASK extends MarshallableObject implements Element {

	private String _Id;

	private String _NAME;

	private String _DESCRIPTION;

	private String _CLASSNAME;

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

	public String getNAME() {
		return _NAME;
	}

	public void setNAME(String _NAME) {
		this._NAME = _NAME;
		if (_NAME == null) {
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

	public String getCLASSNAME() {
		return _CLASSNAME;
	}

	public void setCLASSNAME(String _CLASSNAME) {
		this._CLASSNAME = _CLASSNAME;
		if (_CLASSNAME == null) {
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

	public void validateThis() throws LocalValidationException {
		if (_Id == null) {
			throw new MissingAttributeException("id");
		}
	}

	public void validate(Validator v) throws StructureValidationException {
	}

	public void marshal(Marshaller m) throws IOException {
		XMLWriter w = m.writer();
		w.start("TASK");
		w.attribute("id", _Id.toString());
		if (_NAME != null) {
			w.leaf("NAME", _NAME.toString());
		}
		if (_DESCRIPTION != null) {
			w.leaf("DESCRIPTION", _DESCRIPTION.toString());
		}
		if (_CLASSNAME != null) {
			w.leaf("CLASSNAME", _CLASSNAME.toString());
		}
		if (_ANALYSISPARAMETERS != null) {
			w.leaf("ANALYSISPARAMETERS", _ANALYSISPARAMETERS.toString());
		}
		w.end("TASK");
	}

	public void unmarshal(Unmarshaller u) throws UnmarshalException {
		XMLScanner xs = u.scanner();
		Validator v = u.validator();
		xs.takeStart("TASK");
		while (xs.atAttribute()) {
			String an = xs.takeAttributeName();
			if (an.equals("id")) {
				if (_Id != null) {
					throw new DuplicateAttributeException(an);
				}
				_Id = xs.takeAttributeValue();
				continue;
			}
			throw new InvalidAttributeException(an);
		}
		if (xs.atStart("NAME")) {
			xs.takeStart("NAME");
			String s;
			if (xs.atChars(XMLScanner.WS_COLLAPSE)) {
				s = xs.takeChars(XMLScanner.WS_COLLAPSE);
			} else {
				s = "";
			}
			try {
				_NAME = String.valueOf(s);
			} catch (Exception x) {
				throw new ConversionException("NAME", x);
			}
			xs.takeEnd("NAME");
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
		if (xs.atStart("CLASSNAME")) {
			xs.takeStart("CLASSNAME");
			String s;
			if (xs.atChars(XMLScanner.WS_COLLAPSE)) {
				s = xs.takeChars(XMLScanner.WS_COLLAPSE);
			} else {
				s = "";
			}
			try {
				_CLASSNAME = String.valueOf(s);
			} catch (Exception x) {
				throw new ConversionException("CLASSNAME", x);
			}
			xs.takeEnd("CLASSNAME");
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
		xs.takeEnd("TASK");
	}

	public static TASK unmarshal(InputStream in) throws UnmarshalException {
		return unmarshal(XMLScanner.open(in));
	}

	public static TASK unmarshal(XMLScanner xs) throws UnmarshalException {
		return unmarshal(xs, newDispatcher());
	}

	public static TASK unmarshal(XMLScanner xs, Dispatcher d)
			throws UnmarshalException {
		return ((TASK) d.unmarshal(xs, (TASK.class)));
	}

	public boolean equals(Object ob) {
		if (this == ob) {
			return true;
		}
		if (!(ob instanceof TASK)) {
			return false;
		}
		TASK tob = ((TASK) ob);
		if (_Id != null) {
			if (tob._Id == null) {
				return false;
			}
			if (!_Id.equals(tob._Id)) {
				return false;
			}
		} else {
			if (tob._Id != null) {
				return false;
			}
		}
		if (_NAME != null) {
			if (tob._NAME == null) {
				return false;
			}
			if (!_NAME.equals(tob._NAME)) {
				return false;
			}
		} else {
			if (tob._NAME != null) {
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
		if (_CLASSNAME != null) {
			if (tob._CLASSNAME == null) {
				return false;
			}
			if (!_CLASSNAME.equals(tob._CLASSNAME)) {
				return false;
			}
		} else {
			if (tob._CLASSNAME != null) {
				return false;
			}
		}
		if (_ANALYSISPARAMETERS != null) {
			if (tob._ANALYSISPARAMETERS == null) {
				return false;
			}
			if (!_ANALYSISPARAMETERS.equals(tob._ANALYSISPARAMETERS)) {
				return false;
			}
		} else {
			if (tob._ANALYSISPARAMETERS != null) {
				return false;
			}
		}
		return true;
	}

	public int hashCode() {
		int h = 0;
		h = ((127 * h) + ((_Id != null) ? _Id.hashCode() : 0));
		h = ((127 * h) + ((_NAME != null) ? _NAME.hashCode() : 0));
		h = ((127 * h) + ((_DESCRIPTION != null) ? _DESCRIPTION.hashCode() : 0));
		h = ((127 * h) + ((_CLASSNAME != null) ? _CLASSNAME.hashCode() : 0));
		h = ((127 * h) + ((_ANALYSISPARAMETERS != null) ? _ANALYSISPARAMETERS
				.hashCode() : 0));
		return h;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<<TASK");
		if (_Id != null) {
			sb.append(" id=");
			sb.append(_Id.toString());
		}
		if (_NAME != null) {
			sb.append(" NAME=");
			sb.append(_NAME.toString());
		}
		if (_DESCRIPTION != null) {
			sb.append(" DESCRIPTION=");
			sb.append(_DESCRIPTION.toString());
		}
		if (_CLASSNAME != null) {
			sb.append(" CLASSNAME=");
			sb.append(_CLASSNAME.toString());
		}
		if (_ANALYSISPARAMETERS != null) {
			sb.append(" ANALYSISPARAMETERS=");
			sb.append(_ANALYSISPARAMETERS.toString());
		}
		sb.append(">>");
		return sb.toString();
	}

	public static Dispatcher newDispatcher() {
		return ANALYSISTASKINFO.newDispatcher();
	}

}