package org.genepattern.server;

import java.util.Date;

public class User {
	Integer id;
	String username;
	String password;
    String email;
    Date lastLoginDate;
    String lastLoginIP;
    int totalLoginCount;

	public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

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
