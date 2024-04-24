package com.github.tndavidson.pojo;

import java.io.Serializable;

public class CommunicationPermissionBio implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String channel;
	private String item;
	private boolean allowed;
	
	public String getChannel() {
		return channel;
	}
	public void setChannel(String channel) {
		this.channel = channel;
	}
	public String getItem() {
		return item;
	}
	public void setItem(String item) {
		this.item = item;
	}
	public boolean isAllowed() {
		return allowed;
	}
	public void setAllowed(boolean allowed) {
		this.allowed = allowed;
	}
	
}
