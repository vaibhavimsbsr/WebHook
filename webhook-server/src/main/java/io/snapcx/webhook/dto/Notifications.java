package io.snapcx.webhook.dto;

import java.util.List;

public class Notifications {
	
	private List<String> notification;
	
	public Notifications(List<String> notification) {
		this.notification = notification;
	}

	public List<String> getNotification() {
		return notification;
	}

	public void setNotification(List<String> notification) {
		this.notification = notification;
	}
	
	

}
