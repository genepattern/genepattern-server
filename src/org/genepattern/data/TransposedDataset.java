package org.genepattern.data;

public class TransposedDataset implements Dataset {
	Dataset dataset;

	public static DatasetMutable transpose(DatasetMutable dataset) {
		return new DatasetMutable(dataset.getName(), dataset.getMatrix().getTransposedMatrix(), dataset.getColumnPanel(), dataset.getRowPanel());
	}
	
	
	public Id getId() {
		return dataset.getId();	
	}
	
	public boolean isMutable() {
		return dataset.isMutable();	
	}
	
	public TransposedDataset(Dataset d) {
		dataset = d;	
	}
	
	public DataModel getDataModel() {
		return dataset.getDataModel();	
	}
	
	public String getName() {
		return dataset.getName();
	}


	public int getRowCount() {
		return dataset.getColumnCount();
	}


	public int getColumnCount() {
		return dataset.getRowCount();
	}


	public FloatVector getRow(int row) {
		return dataset.getColumn(row);
	}


	public FloatVector getColumn(int column) {
		return dataset.getRow(column);
	}


	public float getElement(int row, int column) {
		return dataset.getElement(column, row);
	}


	public Matrix getMatrix() {
		return dataset.getMatrix().getTransposedMatrix();
	}


	public NamesPanel getRowPanel() {
		return dataset.getColumnPanel();
	}


	public NamesPanel getColumnPanel() {
		return dataset.getRowPanel();
	}


	public String getRowName(int row) {
		return dataset.getColumnName(row);
	}


	public String[] getRowNames() {
		return dataset.getColumnNames();
	}


	public String getColumnName(final int col) {
		return dataset.getRowName(col);
	}


	public String[] getColumnNames() {
		return dataset.getRowNames();
	}


	public Object clone() {
		try {
			return super.clone();
		} catch(CloneNotSupportedException e) {
			return null;
		}
	}

}

