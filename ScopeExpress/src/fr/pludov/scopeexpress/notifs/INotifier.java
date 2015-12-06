package fr.pludov.scopeexpress.notifs;

public interface INotifier {
	void emit(NotificationChannel nc, String content);
}
