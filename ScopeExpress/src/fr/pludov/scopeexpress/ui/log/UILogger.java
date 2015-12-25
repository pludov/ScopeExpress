package fr.pludov.scopeexpress.ui.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import fr.pludov.scopeexpress.ui.log.LogMessage.Level;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;
import fr.pludov.scopeexpress.utils.WeakListenerCollection.AsyncKind;

/** 
 * Collecte des logs depuis n'importe quel thread et contient des listener dans le thread swing (asynchrone)
 * Pour notifier les threads
 */
public class UILogger {
	final List<LogMessage> messages = new ArrayList<>();
	
	final WeakListenerCollection<UILoggerListener> listeners = new WeakListenerCollection<>(UILoggerListener.class, AsyncKind.SwingQueueIfRequired);
	
	public UILogger() {
	}

	public int getMessageCount()
	{
		synchronized(messages) {
			return messages.size();
		}
	}
	
	public LogMessage getMessage(int index)
	{
		synchronized(messages)
		{
			return messages.get(index); 
		}
	}
	
	void add(LogMessage msg)
	{
		synchronized(messages)
		{
			messages.add(msg);
		}
		listeners.getTarget().newMessageReceived();
	}
	
	public void error(String message)
	{
		add(new LogMessage(Level.Error, message));
	}

	public void error(String message, Throwable t)
	{
		add(new LogMessage(Level.Error, messageWithException(message, t)));
	}

	public void info(String message)
	{
		add(new LogMessage(Level.Info, message));
	}

	public void info(String message, Throwable t)
	{
		add(new LogMessage(Level.Info, messageWithException(message, t)));
	}
	
	public void warn(String message)
	{
		add(new LogMessage(Level.Warn, message));
	}
	
	public void warn(String message, Throwable t)
	{
		add(new LogMessage(Level.Warn, messageWithException(message, t)));
	}
	
	public void debug(String message)
	{
		add(new LogMessage(Level.Debug, message));
	}

	public void debug(String message, Throwable t)
	{
		add(new LogMessage(Level.Debug, messageWithException(message, t)));
	}
	
	String messageWithException(String message, Throwable t)
	{
		StringWriter errors = new StringWriter();
		errors.write(message);
		errors.write(" : ");
		errors.write(t.getLocalizedMessage());
		errors.write(" at \n");
		t.printStackTrace(new PrintWriter(errors));

		return errors.toString();
	}
}
