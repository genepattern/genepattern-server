package org.genepattern.data.matrix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A one-dimensional matrix used to hold class assignment information.
 * 
 * @author Joshua Gould
 */
public class ClassVector {
	Map classNumber2IndicesMap;

	int[] assignments;

	Map classNumber2LabelMap;

	int classCount;

	/**
	 * Constructs a new class vector from the array of class assignments
	 * 
	 * @param x
	 *            the class assignments
	 */
	public ClassVector(String[] x) {
		this.assignments = new int[x.length];
		this.classNumber2IndicesMap = new HashMap();
		this.classNumber2LabelMap = new HashMap();
		int maxClassNumber = 0;
		Map className2ClassNumberMap = new HashMap();
		for (int i = 0; i < x.length; i++) {
			Integer classNumberInteger = (Integer) className2ClassNumberMap
					.get(x[i]);
			if (classNumberInteger == null) {
				classNumberInteger = new Integer(maxClassNumber++);
				className2ClassNumberMap.put(x[i], classNumberInteger);
				classNumber2IndicesMap.put(classNumberInteger, new ArrayList());
				classNumber2LabelMap.put(classNumberInteger, x[i]);
			}
			assignments[i] = classNumberInteger.intValue();
			List indices = (List) this.classNumber2IndicesMap
					.get(classNumberInteger);
			indices.add(new Integer(i));
		}
		this.classCount = maxClassNumber;
	}

	/**
	 * Constructs a new class vector from the array of class assignments
	 * 
	 * @param x
	 *            the class assignments
	 * @param classes
	 *            ordered array of class names. The 0th entry will be given the
	 *            assignment 0, the 1st entry the assignment 1, etc.
	 */
	public ClassVector(String[] x, String[] classes) {
		this.assignments = new int[x.length];
		this.classNumber2IndicesMap = new HashMap();
		this.classNumber2LabelMap = new HashMap();
		int maxClassNumber = classes.length;
		Map className2ClassNumberMap = new HashMap();
		for (int i = 0; i < classes.length; i++) {
			Integer classNumberInteger = new Integer(i);
			className2ClassNumberMap.put(classes[i], classNumberInteger);
			classNumber2IndicesMap.put(classNumberInteger, new ArrayList());
			classNumber2LabelMap.put(classNumberInteger, classes[i]);
		}
		for (int i = 0; i < x.length; i++) {
			Integer classNumberInteger = (Integer) className2ClassNumberMap
					.get(x[i]);
			if (classNumberInteger == null) {
				classNumberInteger = new Integer(maxClassNumber++);
				className2ClassNumberMap.put(x[i], classNumberInteger);
				classNumber2IndicesMap.put(classNumberInteger, new ArrayList());
				classNumber2LabelMap.put(classNumberInteger, x[i]);
			}
			assignments[i] = classNumberInteger.intValue();
			List indices = (List) this.classNumber2IndicesMap
					.get(classNumberInteger);
			indices.add(new Integer(i));
		}
		this.classCount = maxClassNumber;
	}

	private ClassVector(Map classNumber2IndicesMap, int[] assignments,
			Map classNumber2LabelMap, int classCount) {
		this.classNumber2IndicesMap = classNumber2IndicesMap;
		this.assignments = assignments;
		this.classNumber2LabelMap = classNumber2LabelMap;
		this.classCount = classCount;
	}

	/**
	 * Constructs a new class vector that is the union of this class vector with
	 * the given class vector
	 * 
	 * @param classVector
	 *            the class vector
	 * @return the union
	 */
	public ClassVector union(ClassVector classVector) {
		int[][] lookup = new int[getClassCount()][classVector.getClassCount()];
		int classNumber = 0;
		for (int i = 0; i < getClassCount(); i++) {
			for (int j = 0; j < classVector.getClassCount(); j++) {
				lookup[i][j] = classNumber++;
			}
		}
		String[] assignments = new String[size()];
		for (int i = 0; i < size(); i++) {
			assignments[i] = "Class "
					+ lookup[getAssignment(i)][classVector.getAssignment(i)];
		}
		return new ClassVector(assignments);
	}

	/**
	 * Gets the number of assignments in this class vector
	 * 
	 * @return the number of assignments
	 */
	public int size() {
		return assignments.length;
	}

	/**
	 * Constructs and returns a new class vector that contains the indicated
	 * cells. Indices can be in arbitrary order.
	 * 
	 * @param includeSlicedClassOnly
	 *            Whether the returned class vector should have a class count
	 *            equal to the class count in the this class vector, even if the
	 *            returned class vector only less classes than this class
	 *            vector.
	 * @param order
	 *            the indices
	 * @return the new class vector
	 */
	public ClassVector slice(int[] order, boolean includeSlicedClassOnly) {
		if (includeSlicedClassOnly) {
			String[] x = new String[order.length];
			for (int i = 0, length = order.length; i < length; i++) {
				x[i] = this.getClassName(this.assignments[order[i]]);
			}
			return new ClassVector(x);
		}

		int[] newAssignments = new int[order.length];
		Map newClassNumber2IndicesMap = new HashMap();
		for (int i = 0, length = order.length; i < length; i++) {
			newAssignments[i] = this.assignments[order[i]];
			Integer assignment = new Integer(newAssignments[i]);
			List newIndices = (List) newClassNumber2IndicesMap.get(assignment);
			if (newIndices == null) {
				newIndices = new ArrayList();
				newClassNumber2IndicesMap.put(assignment, newIndices);

			}
			newIndices.add(new Integer(i));
		}
		return new ClassVector(newClassNumber2IndicesMap, newAssignments,
				classNumber2LabelMap, classCount);
	}

	/**
	 * Constructs and returns a new class vector that contains the indicated
	 * cells. Indices can be in arbitrary order. The class count in the returned
	 * class vector will be the same as this class vector.
	 * 
	 * @param order
	 *            the indices
	 * @return the new class vector
	 */
	public ClassVector slice(int[] order) {
		return slice(order, false);
	}

	public String toAssignmentString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0, length = assignments.length; i < length; i++) {
			if (i > 0) {
				sb.append(" ");
			}
			sb.append(assignments[i]);
		}
		return sb.toString();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0, length = assignments.length; i < length; i++) {
			if (i > 0) {
				sb.append(" ");
			}
			sb.append(getClassName(assignments[i]));
		}
		return sb.toString();
	}

	/**
	 * Gets the number of different possible values taken by the class
	 * assignments. Note that this can be greater than the actual number of
	 * classes contained in this class vector.
	 * 
	 * @return The number of classes.
	 */
	public int getClassCount() {
		return classCount;
	}

	/**
	 * Gets the class name for the specified class number
	 * 
	 * @param classNumber
	 *            The class number
	 * @return The class name.
	 */
	public String getClassName(int classNumber) {
		return (String) classNumber2LabelMap.get(new Integer(classNumber));
	}

	/**
	 * Gets the class assignment
	 * 
	 * @param index
	 *            The index
	 * @return The assignment
	 */
	public int getAssignment(int index) {
		return assignments[index];
	}

	/**
	 * Allocates a new array containing the class assignments
	 * 
	 * @return The assignments
	 */
	public int[] getAssignments() {
		return (int[]) assignments.clone();
	}

	/**
	 * Creates a array containing all one versus all class vectors. For example
	 * if this class vector contained the classes 'A', 'B', and 'C', the
	 * returned array would contain 3 elements: 'A' vs all, 'B' vs all, and 'C'
	 * vs all
	 * 
	 * @return all one versus all class vectors
	 */
	public ClassVector[] getOneVersusAll() {
		int numClasses = this.getClassCount();
		ClassVector[] array = new ClassVector[numClasses];
		for (int i = 0; i < numClasses; i++) {
			String[] x = new String[this.size()];
			for (int j = 0; j < x.length; j++) {
				int assignment = this.getAssignment(j);
				if (assignment == i) {
					x[j] = this.getClassName(assignment);
				} else {
					x[j] = "Rest";
				}
			}

			ClassVector oneVersusAll = new ClassVector(x, new String[] {
					getClassName(i), "Rest" });
			array[i] = oneVersusAll;
		}
		return array;
	}

	/**
	 * Creates a array containing all pairwise class vectors. For example if
	 * this class vector contained the classes 'A', 'B', and 'C', the returned
	 * array would contain 2 elements: 'A' vs 'B' and 'B' vs 'C'
	 * 
	 * @return all pairwise class vectors
	 */
	public ClassVector[] getAllPairs() {
		int n = getClassCount();
		// int r = 3;
		// BigInteger nFact = factorial(n);
		// BigInteger rFact = factorial(r);
		// BigInteger nminusrFact = factorial(n - r);
		// BigInteger totalBigInt = nFact.divide(rFact.multiply(nminusrFact));
		// int total = totalBigInt.intValue();
		List list = new ArrayList();

		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				int[] iIndices = getIndices(i);
				int[] jIndices = getIndices(j);
				String[] x = new String[iIndices.length + jIndices.length];
				for (int k = 0; k < iIndices.length; k++) {
					x[k] = getClassName(i);
				}
				for (int k = 0; k < jIndices.length; k++) {
					x[k + iIndices.length] = getClassName(j);
				}
				ClassVector pair = new ClassVector(x, new String[] {
						getClassName(i), getClassName(j) });
				list.add(pair);
			}
		}
		return (ClassVector[]) list.toArray(new ClassVector[0]);
	}

	/**
	 * Gets the indices in the assignments array that have the specified class
	 * number.
	 * 
	 * @param classNumber
	 *            The class number
	 * @return The indices
	 */
	public int[] getIndices(int classNumber) {
		List indices = (List) classNumber2IndicesMap.get(new Integer(
				classNumber));
		if (indices == null) {
			return new int[0];
		}
		int[] _indices = new int[indices.size()];
		for (int i = 0, length = _indices.length; i < length; i++) {
			_indices[i] = (((Integer) indices.get(i))).intValue();
		}

		return _indices;
	}

}
