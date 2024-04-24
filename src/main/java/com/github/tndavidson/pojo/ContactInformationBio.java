package com.github.tndavidson.pojo;

import java.io.Serializable;

public class ContactInformationBio implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String addressLine;
	private String city;
	private String state;
	private String phone;
	private String email;
	
	public String getAddressLine() {
		return addressLine;
	}
	public void setAddressLine(String addressLine) {
		this.addressLine = addressLine;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}

	
}
