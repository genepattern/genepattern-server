package org.genepattern.server;

public class UserPassword {
	Integer id;
	String username;
	String password;

	public Integer getId() {
		return this.id;
    }

	public void setId(Integer value) {
		this.id = value;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	

}
