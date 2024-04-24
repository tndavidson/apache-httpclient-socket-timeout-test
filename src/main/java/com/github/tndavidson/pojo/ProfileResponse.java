package com.github.tndavidson.pojo;

public class ProfileResponse {
	
	private ContactInformationBio contactInfo;
	private CommunicationPermissionBio commPerm;
	private String zombieResponse;
	
	public ProfileResponse(ContactInformationBio ci, CommunicationPermissionBio cp, String zombie) {
		this.contactInfo = ci;
		this.commPerm = cp;
		this.zombieResponse = zombie;
	}
	
	public ProfileResponse() {}

	public ContactInformationBio getContactInfo() {
		return contactInfo;
	}

	public void setContactInfo(ContactInformationBio contactInfo) {
		this.contactInfo = contactInfo;
	}

	public CommunicationPermissionBio getCommPerm() {
		return commPerm;
	}

	public void setCommPerm(CommunicationPermissionBio commPerm) {
		this.commPerm = commPerm;
	}

	public String getZombieResponse() {
		return zombieResponse;
	}

	public void setZombieResponse(String zombieResponse) {
		this.zombieResponse = zombieResponse;
	}

}
