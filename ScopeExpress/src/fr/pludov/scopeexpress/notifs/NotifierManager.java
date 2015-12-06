package fr.pludov.scopeexpress.notifs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class NotifierManager {
	
	
	private static class MessageQueueDispatcher extends Thread
	{
		INotifier target;
		LinkedBlockingQueue<Object[]> queue;
		volatile boolean dead;
		
		MessageQueueDispatcher(INotifier target)
		{
			this.target = target;
			this.queue = new LinkedBlockingQueue<>();
			this.dead = false;
		}
		
		
		@Override
		public void run() {
			try {
				while(true) {
					if (this.dead) {
						return;
					}
					Object[] dispatch = queue.poll(5, TimeUnit.SECONDS);
					if (this.dead) {
						return;
					}
					if(dispatch != null) {
						target.emit((NotificationChannel)dispatch[0], (String)dispatch[1]);
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static List<MessageQueueDispatcher> notifiers = new ArrayList<>();
	
	/** Uniquement valable depuis le thread swing */
	public static void addNotifier(INotifier n)
	{
		MessageQueueDispatcher mqd = new MessageQueueDispatcher(n);
		mqd.start();
		notifiers.add(mqd);
	}
	
	/** Uniquement valable depuis le thread swing */
	public static void removeNotifier(INotifier n)
	{
		for(int i = 0; i < notifiers.size(); ++i) {
			MessageQueueDispatcher mqd = notifiers.get(i);
			if (mqd.target == n) {
				notifiers.remove(i);
				break;
			}
		}
	}
	
	public static void emit(NotificationChannel nc, String text)
	{
		Object[] message = new Object[]{nc, text};
		for(MessageQueueDispatcher mqd : notifiers)
		{
			mqd.queue.add(message);
		}
	}
	

}
