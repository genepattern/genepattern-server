package org.genepattern.webservice.jaxb.parameter;

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

public class ANALYSISPARAMETERS extends MarshallableRootElement implements
		RootElement {

	private List _PARAMETER = PredicatedLists.createInvalidating(this,
			new PARAMETERPredicate(), new ArrayList());

	private PredicatedLists.Predicate pred_PARAMETER = new PARAMETERPredicate();

	public List getPARAMETER() {
		return _PARAMETER;
	}

	public void deletePARAMETER() {
		_PARAMETER = null;
		invalidate();
	}

	public void emptyPARAMETER() {
		_PARAMETER = PredicatedLists.createInvalidating(this, pred_PARAMETER,
				new ArrayList());
	}

	public void validateThis() throws LocalValidationException {
	}

	public void validate(Validator v) throws StructureValidationException {
		for (Iterator i = _PARAMETER.iterator(); i.hasNext();) {
			v.validate(((ValidatableObject) i.next()));
		}
	}

	public void marshal(Marshaller m) throws IOException {
		XMLWriter w = m.writer();
		w.start("ANALYSISPARAMETERS");
		if (_PARAMETER.size() > 0) {
			for (Iterator i = _PARAMETER.iterator(); i.hasNext();) {
				m.marshal(((MarshallableObject) i.next()));
			}
		}
		w.end("ANALYSISPARAMETERS");
	}

	public void unmarshal(Unmarshaller u) throws UnmarshalException {
		XMLScanner xs = u.scanner();
		Validator v = u.validator();
		xs.takeStart("ANALYSISPARAMETERS");
		while (xs.atAttribute()) {
			String an = xs.takeAttributeName();
			throw new InvalidAttributeException(an);
		}
		{
			List l = PredicatedLists.create(this, pred_PARAMETER,
					new ArrayList());
			while (xs.atStart("PARAMETER")) {
				l.add(((PARAMETER) u.unmarshal()));
			}
			_PARAMETER = PredicatedLists.createInvalidating(this,
					pred_PARAMETER, l);
		}
		xs.takeEnd("ANALYSISPARAMETERS");
	}

	public static ANALYSISPARAMETERS unmarshal(InputStream in)
			throws UnmarshalException {
		return unmarshal(XMLScanner.open(in));
	}

	public static ANALYSISPARAMETERS unmarshal(XMLScanner xs)
			throws UnmarshalException {
		return unmarshal(xs, newDispatcher());
	}

	public static ANALYSISPARAMETERS unmarshal(XMLScanner xs, Dispatcher d)
			throws UnmarshalException {
		return ((ANALYSISPARAMETERS) d
				.unmarshal(xs, (ANALYSISPARAMETERS.class)));
	}

	public boolean equals(Object ob) {
		if (this == ob) {
			return true;
		}
		if (!(ob instanceof ANALYSISPARAMETERS)) {
			return false;
		}
		ANALYSISPARAMETERS tob = ((ANALYSISPARAMETERS) ob);
		if (_PARAMETER != null) {
			if (tob._PARAMETER == null) {
				return false;
			}
			if (!_PARAMETER.equals(tob._PARAMETER)) {
				return false;
			}
		} else {
			if (tob._PARAMETER != null) {
				return false;
			}
		}
		return true;
	}

	public int hashCode() {
		int h = 0;
		h = ((127 * h) + ((_PARAMETER != null) ? _PARAMETER.hashCode() : 0));
		return h;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<<ANALYSISPARAMETERS");
		if (_PARAMETER != null) {
			sb.append(" PARAMETER=");
			sb.append(_PARAMETER.toString());
		}
		sb.append(">>");
		return sb.toString();
	}

	public static Dispatcher newDispatcher() {
		Dispatcher d = new Dispatcher();
		d.register("ANALYSISPARAMETERS", (ANALYSISPARAMETERS.class));
		d.register("ATTRIBUTE", (ATTRIBUTE.class));
		d.register("PARAMETER", (PARAMETER.class));
		d.freezeElementNameMap();
		return d;
	}

	private static class PARAMETERPredicate implements
			PredicatedLists.Predicate {

		public void check(Object ob) {
			if (!(ob instanceof PARAMETER)) {
				throw new InvalidContentObjectException(ob, (PARAMETER.class));
			}
		}

	}

}