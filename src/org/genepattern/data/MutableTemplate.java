/*
 * WHITEHEAD INSTITUTE
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2001 by the
 * Whitehead Institute for Biomedical Research.  All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever.  The Whitehead Institute can not be responsible for its
 * use, misuse, or functionality.
 */

package org.genepattern.data;

import java.util.ArrayList;

import org.genepattern.util.XMath;

/**
 * A Template object that is mutable i.e has add() methods. Generally used only
 * by classes and methods that make new Templates.
 * 
 * Alos, factor methoids for temnplates kept here so that we can control
 * generation (acnd acces s non publ;ic fields needed for generation)
 * 
 * @author Aravind Subramanian
 * @version %I%, %G%
 */

public class MutableTemplate extends KlassTemplate {

	/** Generated serialized version UID */
	private static final long serialVersionUID = 3328727228371242727L;

	/**
	 * Constructor Creates a new MutableTemplate.
	 * 
	 * @param name
	 *            name of new MutableTemplate.
	 */
	public MutableTemplate(String name) {
		super(name, 10, 10, true);
	}

	/** Create an empty MutableTemplate */
	public MutableTemplate() {
		this("");
	}

	/** Creates a new MutableTemplate from another Template */
	public MutableTemplate(Template template) {
		this(template.getName() + "_copy");
		final int limit = template.getItemCount();
		for (int i = 0; i < limit; i++) {
			this.add(template.getItem(i));
		}
		final int cnt = template.getKlassCount();
		for (int i = 0; i < cnt; i++) {
			add(new MutableKlass(template.getKlassLabel(i)));
		}
		this.assignItems();
	}

	/**
	 * Add specified Klass to this MutableTemplate.
	 */
	public void add(MutableKlass aKlass) {
		//        checkImmutable();
		klasses.add(aKlass);
	}

	/**
	 * Add specified TemplateItem to this MutableTemplate
	 */
	public void add(Template.Item aItem) {
		//        checkImmutable();
		if (isNumeric()) {
			try {
				Float.parseFloat(aItem.getId());
			} catch (NumberFormatException e) {
				throw new NumberFormatException(
						"MutableTemplate is numeric but TemplateItem asked to be added was not Float-parsable "
								+ e.toString());
			}
		}

		items.add(aItem);
	}

	/**
	 * Set this Template as numeric. Making this private for now - would seem to
	 * be sufficient as numeric Templates are made from expression values of
	 * genes and these are made using the factory createTemplate() methods in
	 * this class. But, if it turns out to be neccessary to make numeric
	 * Templates externally, for instance, after parsing a Cls file, this method
	 * can be made public.
	 *  
	 */
	private void setNumeric(boolean numeric) {
		is_numeric = numeric;
	}

	/**
	 * Factory method. Make a new Template from specified float[]. The template
	 * is set to numeric meaning that its guaranteed that all its TemplateItems
	 * are Float objects. Klass labels are automatically generated, one for each
	 * unique float By default they are: class_floatvalue template made is
	 * always numeric
	 */
	public static synchronized Template createTemplate(String aName, float[] x) {

		// what if 2 elements have the same value??
		// so num of classes may not be eq to num of items
		// is that ok in the context of numeric templates??
		// currently - make a new Klass only for the unique floats

		ArrayList uniq = new ArrayList();

		MutableTemplate template = new MutableTemplate(aName);

		for (int i = 0; i < x.length; i++) {
			Float fl = new Float(x[i]);
			if (!uniq.contains(fl)) {
				uniq.add(fl);
				MutableKlass klass = new MutableKlass("class_" + fl);
				template.add(klass);
			}

			Template.Item item = new Template.Item(SampleLabel.ANY_SAMPLE,
					Float.toString(x[i]), i);
			template.add(item);
		}

		template.setNumeric(true);
		template.assignItems();
		//        template.makeImmutable();
		return (Template) template;

	}

	/**
	 * Factory method. Make a new Template from specified String[].
	 * 
	 * Klass labels are automatically generated one for each unique String[] By
	 * default they are: class_strvalue
	 * 
	 * if there are only numbers in the passed string[][ then the template is
	 * made numeric else its not.
	 *  
	 */
	// make a new Klass for every unique String[] element
	public static synchronized Template createTemplate(String aName, String[] x) {
		//System.out.println("array is x=" + x.length);
		MutableTemplate template = new MutableTemplate(aName);

		ArrayList uniq = new ArrayList();

		for (int i = 0; i < x.length; i++) {

			if (!uniq.contains(x[i])) {
				uniq.add(x[i]);
				MutableKlass klass = new MutableKlass("class_" + x[i]);
				template.add(klass);
			}

			Template.Item item = new Template.Item(SampleLabel.ANY_SAMPLE,
					x[i], i);
			template.add(item);

		}

		template.assignItems();
		if (XMath.isNumeric(x))
			template.setNumeric(true);
		else
			template.setNumeric(false);
		//        template.makeImmutable();
		//template.printf();
		return (Template) template;

	}

	/**
	 * Klass labels are automatically generated. By default they are: class_#
	 * template made is always numeric
	 * 
	 * @param aName
	 *            name of the Template
	 * @param countbyclass ->
	 *            number of of items in each class. Assigned in order.
	 *  
	 */
	public static synchronized Template createTemplate(String aName,
			int[] countbyclass) {
		String[] names = new String[countbyclass.length];
		for (int i = 0; i < names.length; i++)
			names[i] = "class_" + i;
		return createTemplate(aName, countbyclass, names);
	}

	public static synchronized Template createTemplate(String aName,
			int[] countbyclass, String[] classnames) {
		if (countbyclass.length != classnames.length)
			throw new IllegalArgumentException("Number of classes: "
					+ countbyclass.length
					+ " not equal to number of classnames: "
					+ classnames.length);

		MutableTemplate template = new MutableTemplate(aName);
		int pos = 0;
		for (int i = 0; i < countbyclass.length; i++) {
			int cnt = countbyclass[i];
			MutableKlass klass = new MutableKlass(classnames[i]);
			template.add(klass);
			for (int j = 0; j < cnt; j++) {
				Template.Item item = new Template.Item(SampleLabel.ANY_SAMPLE,
						Integer.toString(i), pos);
				template.add(item);
				pos++;
			}

		}

		template.assignItems();
		template.setNumeric(true);
		//        template.makeImmutable();
		return (Template) template;
	}

	/**
	 * for biphasic templates
	 */
	public static synchronized Template createBiphasicTemplate(String aName,
			int minuscnt, int pluscnt) {

		MutableTemplate template = new MutableTemplate(aName);
		MutableKlass minusklass = new MutableKlass("class_-1");
		template.add(minusklass);
		int pos = 0;
		for (int i = 0; i < minuscnt; i++) {
			Template.Item item = new Template.Item(SampleLabel.ANY_SAMPLE,
					"-1", pos);
			template.add(item);
			pos++;
		}

		MutableKlass plusklass = new MutableKlass("class_+1");
		template.add(plusklass);
		for (int i = 0; i < pluscnt; i++) {
			Template.Item item = new Template.Item(SampleLabel.ANY_SAMPLE, "1",
					pos);
			template.add(item);
			pos++;
		}

		template.assignItems();
		template.setNumeric(true);
		//        template.makeImmutable();
		return (Template) template;
	}

	/**
	 * takes a template with 2 or more classes and uses it to make a 2 class
	 * template with one being items from refclass and everyting else (the all)
	 * being in the other class the elements are thus now: 0 all else: 1
	 */
	// it dont matter if templae is numeric or symbollic - it should simpy be
	// biphasic
	public static Template createBiphasicTemplate(String newname,
			Template.Klass refclass, Template template) {

		int minusclasscnt = 0;
		int plusclasscnt = 0;
		for (int i = 0; i < template.getKlassCount(); i++) {
			Template.Klass tc = template.getKlass(i);
			if (tc == refclass)
				minusclasscnt = tc.getSize();
			else
				plusclasscnt += tc.getSize();
		}

		return createBiphasicTemplate(newname, minusclasscnt, plusclasscnt);
	}

	/**
	 * only -1 and 1. However order may bot be all -1 and then all -1. Order is
	 * as specified alternate b/w 0 and 1
	 * 
	 * order -> 2, 4, 5 => 2 0's, 4 +1's and 5 0's order -> 2, 4, 5, 6 => 2 0's,
	 * 4 1's, 5 0's and 6 1's
	 *  
	 */
	public static synchronized Template createBiphasicTemplate(String aName,
			int[] order) {

		MutableTemplate template = new MutableTemplate(aName);
		int pos = 0;

		MutableKlass klass0 = new MutableKlass("class_0");
		template.add(klass0);

		MutableKlass klass1 = new MutableKlass("class_1");
		template.add(klass1);

		for (int i = 0; i < order.length; i++) {
			MutableKlass klass;
			String val;
			if (i % 2 == 0) {
				//klass = klass0;
				val = "0";
			} else {
				//klass = klass1;
				val = "1";
			}

			for (int j = 0; j < order[i]; j++) {
				Template.Item item = new Template.Item(SampleLabel.ANY_SAMPLE,
						val, pos);
				template.add(item);
				pos++;
			}

		}

		template.assignItems();
		template.setNumeric(true);
		//        template.makeImmutable();
		return (Template) template;
	}

	/**
	 * to shuffle a template:
	 * 
	 * @todo check java's Random numb gen alg
	 * 
	 * NOte: IMP to make new items and classes and NOT add orig templates items
	 * and classes (i.e clone dont use refs)
	 */
	//public static Template createShuffledTemplate(Template origtemplate,
	// Random rnd) {
	public static Template createShuffledTemplate(Template origtemplate) {
		//        ClassTemplate shuffled = new ClassTemplate();

		MutableTemplate mt = new MutableTemplate(origtemplate.getName());

		/*
		 * This doesnt work - need to pick from existing template randomly into
		 * a new one WITHOUT repetition HMM - need to check - with or without
		 * replacement ???
		 */
		/*
		 * for (int i=0; i < origtemplate.getItemCount(); i++) { int rndindex =
		 * rnd.nextInt(origtemplate.getItemCount()); // a rnd number from
		 * 0(incl) to num of items(exclusive) Template.Item item =
		 * origtemplate.getItem(rndindex); MutableTemplate.Item newitem = new
		 * MutableTemplate.Item(item.id, item.pos); mt.add(newitem); }
		 *  
		 */

		int[] inds = XMath.randomizeWithoutReplacement(origtemplate
				.getItemCount());
		for (int i = 0; i < origtemplate.getItemCount(); i++) {
			Template.Item item = origtemplate.getItem(inds[i]);
			Template.Item newitem = new Template.Item(SampleLabel.ANY_SAMPLE,
					item.id, item.pos);
			mt.add(newitem);
		}

		for (int i = 0; i < origtemplate.getKlassCount(); i++) {
			Template.Klass cl = origtemplate.getKlass(i);
			MutableKlass newcl = new MutableKlass(cl.getName());
			mt.add(newcl);
		}

		mt.assignItems();
		if (origtemplate.isNumeric())
			mt.setNumeric(true);
		//        mt.makeImmutable();
		return (Template) mt;
	}

	/**
	 * @todo Its debateable whther the same rnd should be used for generating
	 *       all the shuffled templates?? Check with pt.
	 */
	/*
	 * public static Template[] createShuffledTemplates(Template origtemplate,
	 * int numb) { Random rnd = new java.util.Random(); // Note using the same
	 * one Template[] newtemplates = new Template[numb]; for (int i=0; i < numb;
	 * i++) { newtemplates[i] = createShuffledTemplate(origtemplate, rnd); }
	 * return newtemplates; }
	 */

	/**
	 * Deduces a Template from the description field of Hmm cant do.
	 */
	/*
	 * public static synchronized Template createTemplate(Dataset ds) {
	 * 
	 *  }
	 */

} // End MutableTemplate

