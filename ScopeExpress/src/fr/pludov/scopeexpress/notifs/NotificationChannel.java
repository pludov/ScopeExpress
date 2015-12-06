package fr.pludov.scopeexpress.notifs;

public enum NotificationChannel {
	Photo("photos", "Données sur les photos (FWHM, ...)"),
	Debug("debug", "Message de debug de l'application"),
	Alert("alertes" , "Les alertes");
	
	
	final String title;
	final String details;
	
	NotificationChannel(String title, String details) {
		this.title = title;
		this.details = details;
	}

	public String getTitle() {
		return title;
	}

	public String getDetails() {
		return details;
	}
	
	public void emit(String text) {
		NotifierManager.emit(this, text);
	};
}
