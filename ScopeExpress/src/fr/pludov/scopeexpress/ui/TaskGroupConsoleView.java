package fr.pludov.scopeexpress.ui;

import java.awt.*;

import javax.swing.*;
import javax.swing.text.*;

import fr.pludov.scopeexpress.script.*;
import fr.pludov.scopeexpress.utils.*;

public class TaskGroupConsoleView extends JScrollPane {
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	final TaskGroup taskGroup;
	final JTextArea logs;

	
	
	public TaskGroupConsoleView(TaskGroup taskGroup) {
		this.taskGroup = taskGroup;
		this.logs = new JTextArea();
		this.logs.setEditable(false);
		getViewport().add(logs);
	
		for(String log : taskGroup.getLogs()) {
			logs.append(log + "\n");
		
		}
		
		taskGroup.getLogListeners().addListener(this.listenerOwner, new TaskGroupLogListener() {
			
			@Override
			public void logAdded(String newLine, boolean replacePrevious) {
				// FIXME: scroll, remove previous content, ...
				logs.append(newLine + "\n");
				
				int pos = logs.getDocument().getLength();
				try {
					Rectangle viewRect;
					viewRect = logs.modelToView(pos);
					// Scroll to make the rectangle visible
					logs.scrollRectToVisible(viewRect);
					// Highlight the text
					logs.setCaretPosition(pos);
					logs.moveCaretPosition(pos);
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}


}
