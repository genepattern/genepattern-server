/*
 * Datasets.java
 *
 * Created on October 31, 2002, 10:41 AM
 */

package org.genepattern.data;

import gnu.trove.TObjectIntHashMap;

import java.util.Iterator;

import org.genepattern.util.AbstractReporter;

/**
 * Utility class that has static methods for minipulating <code>Dataset</code>
 * objects or creating new ones from old.
 * 
 * @author kohm
 */
public class Datasets {

	/** private constructor to prevent creation of new instance of Datasets */
	private Datasets() {
	}

	/**
	 * returns a new dataset that is sorted by either the rows or the columns
	 * 
	 * @param dataset
	 *            is the original (this method could have been nonstatic and
	 *            this would not be needed)
	 * @param labels
	 *            has the new order defined by the order of the array of labels
	 * @param panels
	 *            some lists of data that needs the same ordering
	 */
	//    public static final Dataset getSortedDataset (final Dataset dataset,
	//    final LabelSet labels, final java.util.List panels, final String name) {
	//        final String[] labs = labels.getLabels ();
	//        final boolean use_rows = labels.usingDatasetRows();
	public static final Dataset getSortedDataset(final Dataset dataset,
			final String[] labs, final boolean use_rows,
			final java.util.List panels, final String name) {
		System.out.println("Original dataset:");
		datasetDump(dataset);
		//        System.out.println("labels: use_rows="+use_rows+" row="+labels.row+"
		// transposed="+labels.transposed);
		System.out.println("labs=" + labs.length);
		//        CGenome genome = new CGenome (dataset,
		//                (use_rows ? CGenome.ROW_LABELS : CGenome.COLUMN_LABELS));
		//        final int[] rows_cols2include = genome.createIndices (labs);
		final int[] rows_cols2include = createIndices(dataset, labs, use_rows);
		// order the panels
		if (panels != null) {
			java.util.List list;
			for (final Iterator iter = panels.iterator(); iter.hasNext();) {
				list = (java.util.List) iter.next();
				Object[] new_order = new Object[list.size()];
				for (int i = 0; i < rows_cols2include.length; i++) {
					new_order[i] = list.get(rows_cols2include[i]);
				}
				// remove the old values and it's order
				list.clear();
				// reset the order of the list
				list.addAll(java.util.Arrays.asList(new_order));
				new_order = null;
			}
		}
		// order the data in the new dataset
		if (dataset.isMutable()) // FIXME should create a new DefaultDataset
			AbstractReporter.getInstance().logWarning(
					"Creating a DatasetView from Dataset " + dataset.getName()
							+ " which is muttable!");
		DatasetView newdataset;
		if (use_rows) {
			newdataset = new DatasetView(dataset, name, rows_cols2include, null/*
																			    * all
																			    * cols
																			    */);
			//            newdataset.matchParentsColumns();
			//            newdataset.useOnlyRows(rows_cols2include);
			//            newdataset = CDataset.buildDatasetWithRows (dataset,
			// rows_cols2include,
			//            dataset.getName ()+labels.getLabelExtension());
		} else {
			newdataset = new DatasetView(dataset, name, null/** all rows */
			, rows_cols2include);
			//            newdataset.matchParentsRows();
			//            newdataset.useOnlyColumns(rows_cols2include);
			//// newdataset = CDataset.buildDatasetWithColumns (dataset,
			// rows_cols2include,
			//// dataset.getName ()+labels.getLabelExtension());
		}
		//        newdataset.setName(dataset.getName ()+labels.getLabelExtension());

		System.out.println("rows_cols2include=" + rows_cols2include.length);
		System.out.println("new dataset:");
		datasetDump(newdataset);

		return newdataset;
	}

	/** debug helper method that dumps some info from the Dataset */
	public static final void datasetDump(final Dataset dataset) {
		System.out
				.println(" *** Dataset Dump *** \nName: " + dataset.getName());
		Matrix matrix = dataset.getMatrix();
		System.out.println("Matrix #rows=" + matrix.getRowCount()
				+ " #columns=" + matrix.getColumnCount());
		String[] rows = dataset.getRowNames(), cols = dataset.getColumnNames();
		System.out.println("NamesPanels #rows=" + rows.length + " #columns="
				+ cols.length);
		System.out.println("is mutable=" + dataset.isMutable() + " Id="
				+ dataset.getId());
		System.out.println(" ***  *** ");
		//        System.out.println(dataset.getInfo ());
		//System.out.println(dataset.getDefaultInfo ());
		System.out.println();
	}

	/** Returns an array that just contains the indices of the features in labels */
	public static final int[] createIndices(final Dataset dataset,
			final String[] labels, final boolean use_rows) {
		final int num_labels = labels.length;
		final TObjectIntHashMap map = new TObjectIntHashMap(num_labels);
		final int[] inds = new int[num_labels];
		final int rc_count = (use_rows ? dataset.getRowCount() : dataset
				.getColumnCount());
		// assumes that all names are unique
		for (int i = 0; i < rc_count; i++) {
			map.put((use_rows ? dataset.getRowName(i) : dataset
					.getColumnName(i)), i);
		}

		int curInd = 0;
		for (int i = 0; i < num_labels; ++i) {
			final int mapInd = map.get(labels[i]);

			// Check if the feature is in the reference feature set
			if (mapInd >= 0) {
				inds[curInd] = mapInd;
				curInd++;
			}
		}

		// Check if any features in labels were not found
		if (curInd < num_labels) {
			int[] tmpInds = new int[curInd]; // create truncated array
			System.arraycopy(inds, 0, tmpInds, 0, curInd);
			return tmpInds;
		} else
			return inds;
	}
	//    /**
	//     * This method creates an immutable dataset from the input dataset's rows
	//     * If the input dataset is mutable creates a new DefaultDataset otherwise
	// creates
	//     * an immutable DatasetView of the original
	//     */
	//    public static final Dataset buildDatasetWithColumns (final Dataset
	// original, final int[] cols2include, final String new_name) {
	//        DatasetView newdataset = new DatasetView(dataset);
	//        if(use_rows) {
	//            newdataset.matchParentsColumns();
	//            newdataset.useOnlyRows(rows_cols2include);
	//// newdataset = CDataset.buildDatasetWithRows (dataset,
	// rows_cols2include,
	//// dataset.getName ()+labels.getLabelExtension());
	//        } else {
	//            newdataset.matchParentsRows();
	//            newdataset.useOnlyColumns(rows_cols2include);
	//// newdataset = CDataset.buildDatasetWithColumns (dataset,
	// rows_cols2include,
	//// dataset.getName ()+labels.getLabelExtension());
	//        }
	//        newdataset.setName();
	//        return null;
	//    }
}