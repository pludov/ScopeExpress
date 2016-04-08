package fr.pludov.scopeexpress.tasks.interaction;

import java.awt.event.*;

import javax.swing.*;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.utils.*;

public class TaskUserRequestDetails extends TaskUserRequestDetailsDesign implements TaskDetailView {

	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	TaskUserRequest currentTask;
	
	public TaskUserRequestDetails() {
		super();
		refreshStatusDisplay();
		btnOk.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				pushStatus(true);
			}
		});
		
		btnAnnuler.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				pushStatus(false);
			}
		});
	}
	
	void pushStatus(boolean b)
	{
		if (currentTask != null && !currentTask.getStatus().isTerminal())
		{
			currentTask.pushStatus(b);
		}
		
	}
	
	@Override
	public JPanel getMainPanel() {
		return this;
	}
	
	@Override
	public void setTask(BaseTask bt) {
		setTask((TaskUserRequest)bt);
	}
	

	public void setTask(TaskUserRequest aft)
	{
		if (currentTask != null) {
			currentTask.statusListeners.removeListener(this.listenerOwner);
			currentTask = null;
		}
		
		currentTask = aft;
		if (currentTask != null) {
			currentTask.statusListeners.addListener(this.listenerOwner, new TaskStatusListener() {
				
				@Override
				public void statusChanged() {
					refreshStatusDisplay();
				}
			});
		}
		refreshStatusDisplay();
		
	}
	
	void refreshStatusDisplay()
	{
		if (currentTask == null) {
			this.lblLabel.setText("");
		} else {
			this.lblLabel.setText(currentTask.get(currentTask.getDefinition().userMessage));
		}
		if (currentTask == null || currentTask.getStatus().isTerminal()) {
			this.btnOk.setEnabled(false);
			this.btnAnnuler.setEnabled(false);
		} else {
			this.btnOk.setEnabled(true);
			this.btnAnnuler.setEnabled(true);
		}
		
	}

}
