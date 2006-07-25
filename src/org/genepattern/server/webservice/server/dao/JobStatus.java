/* Auto generated file */

package org.genepattern.server.webservice.server.dao;


import java.util.Collection;

public class JobStatus {

	/** auto generated
	 * @es_generated
	 */
	private Integer statusId;
	/** auto generated
	 * @es_generated
	 */
	private String statusName;
	/** auto generated
	 * @es_generated
	 */

	public JobStatus() {
		super();
	}

	/** auto generated
	 * @es_generated
	 */
	public JobStatus(Integer statusId) {
		super();
		this.statusId = statusId;
	}

	/** auto generated
	 * @es_generated
	 */
	public JobStatus(Integer statusId, String statusName) {
		super();
		this.statusId = statusId;
		this.statusName = statusName;
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
	public Integer getStatusId() {
		return this.statusId;
	}

	/** auto generated
	 * @es_generated
	 */
	public void setStatusId(Integer value) {
		this.statusId = value;
	}

	/** auto generated
	 * @es_generated
	 */
	public String getStatusName() {
		return this.statusName;
	}

	/** auto generated
	 * @es_generated
	 */
	public void setStatusName(String value) {
		this.statusName = value;
	}


}
