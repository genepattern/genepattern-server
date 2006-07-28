/* Auto generated file */

package org.genepattern.server.webservice.server.dao;


public class Suite {

	/** auto generated
	 * @es_generated
	 */
	private String lsid;
	/** auto generated
	 * @es_generated
	 */
	private String name;
	/** auto generated
	 * @es_generated
	 */
	private String author;
	/** auto generated
	 * @es_generated
	 */
	private String owner;
	/** auto generated
	 * @es_generated
	 */
	private String description;
	/** auto generated
	 * @es_generated
	 */
	private Integer accessId;

	/** auto generated
	 * @es_generated
	 */
	public Suite() {
		super();
	}

	/** auto generated
	 * @es_generated
	 */
	public Suite(String lsid) {
		super();
		this.lsid = lsid;
	}

	/** auto generated
	 * @es_generated
	 */
	public Suite(String lsid, String name, String author, String owner,
			String description, Integer accessId) {
		super();
		this.lsid = lsid;
		this.name = name;
		this.author = author;
		this.owner = owner;
		this.description = description;
		this.accessId = accessId;
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
	public String getLsid() {
		return this.lsid;
	}

	/** auto generated
	 * @es_generated
	 */
	public void setLsid(String value) {
		this.lsid = value;
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
	public String getAuthor() {
		return this.author;
	}

	/** auto generated
	 * @es_generated
	 */
	public void setAuthor(String value) {
		this.author = value;
	}

	/** auto generated
	 * @es_generated
	 */
	public String getOwner() {
		return this.owner;
	}

	/** auto generated
	 * @es_generated
	 */
	public void setOwner(String value) {
		this.owner = value;
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

	/** auto generated
	 * @es_generated
	 */
	public Integer getAccessId() {
		return this.accessId;
	}

	/** auto generated
	 * @es_generated
	 */
	public void setAccessId(Integer value) {
		this.accessId = value;
	}
}
