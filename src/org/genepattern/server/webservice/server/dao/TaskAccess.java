/* Auto generated file */

package org.genepattern.server.webservice.server.dao;

import java.math.BigInteger;

public class TaskAccess {

	/** auto generated
	 * @es_generated
	 */
	private BigInteger accessId;
	/** auto generated
	 * @es_generated
	 */
	private String name;
	/** auto generated
	 * @es_generated
	 */
	private String description;

	/** auto generated
	 * @es_generated
	 */
	public TaskAccess() {
		super();
	}

	/** auto generated
	 * @es_generated
	 */
	public TaskAccess(BigInteger accessId) {
		super();
		this.accessId = accessId;
	}

	/** auto generated
	 * @es_generated
	 */
	public TaskAccess(BigInteger accessId, String name, String description) {
		super();
		this.accessId = accessId;
		this.name = name;
		this.description = description;
	}

	/** auto generated
	 * @es_generated
	 */
	public boolean equals(Object value) {
		//TODO Implement equals() using Business key equality.	
		return super.equals(value);
	}

	/** auto generated
	 * @es_generated
	 */
	public int hashCode() {
		//TODO Implement hashCode() using Business key equality.	
		return super.hashCode();
	}

	/** auto generated
	 * @es_generated
	 */
	public String toString() {
		//TODO Implement toString().	
		return super.toString();
	}

	/** auto generated
	 * @es_generated
	 */
	public BigInteger getAccessId() {
		return this.accessId;
	}

	/** auto generated
	 * @es_generated
	 */
	public void setAccessId(BigInteger value) {
		this.accessId = value;
	}

	/** auto generated
	 * @es_generated
	 */
	public String getName() {
		return this.name;
	}

	/** auto generated
	 * @es_generated
	 */
	public void setName(String value) {
		this.name = value;
	}

	/** auto generated
	 * @es_generated
	 */
	public String getDescription() {
		return this.description;
	}

	/** auto generated
	 * @es_generated
	 */
	public void setDescription(String value) {
		this.description = value;
	}
}
