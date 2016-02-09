package fr.pludov.scopeexpress.tasks;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.ui.resources.*;
import fr.pludov.scopeexpress.ui.resources.IconProvider.*;
import fr.pludov.scopeexpress.ui.utils.*;
import fr.pludov.scopeexpress.utils.*;

public class TaskControl extends TaskControlDesign {
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	
	BaseTask currentTask;

	private JButton configButton;

	private JButton pauseButton;
	
	private JButton stopButton;

	private JButton removeButton;

	
	
	public TaskControl() {
		configButton = new JButton("Paramètres");
		configButton.setIcon(IconProvider.getIcon("config", IconSize.IconSizeButton));
		configButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentTask != null) {
					TaskParameterPanel tpd = new TaskParameterPanel(currentTask.focusUi, currentTask.getDefinition(), false);
					tpd.showEditDialog(SwingUtilities.getWindowAncestor(TaskControl.this), currentTask);
					// tpd.editCurrentParameter(SwingUtilities.getWindowAncestor(TaskControl.this), currentTask);
				}
			}
		});
		super.buttonPanel.add(configButton);

		pauseButton = new JButton("Pause");
		pauseButton.setIcon(IconProvider.getIcon("pause", IconSize.IconSizeButton));
		pauseButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (currentTask != null) {
					if (currentTask.getStatus() == BaseStatus.Paused) {
						currentTask.resume();
					} else {
						currentTask.requestPause();
					}
				}
			}
		});
		super.buttonPanel.add(pauseButton);

		stopButton = new JButton("Stop");
		stopButton.setIcon(IconProvider.getIcon("stop", IconSize.IconSizeButton));
		stopButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (currentTask != null) {
					currentTask.requestCancelation();
				}
			}
		});
		super.buttonPanel.add(stopButton);
		
		removeButton = new JButton("Oublier");
		removeButton.setIcon(IconProvider.getIcon("close", IconSize.IconSizeButton));
		removeButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (currentTask != null) {
					currentTask.forget();
				}
			}
		});
		super.buttonPanel.add(removeButton);
	
		this.currentTask = null;
		refreshTaskDisplay();
		
	}
	
	public void setCurrentTask(BaseTask bt)
	{
		if (this.currentTask == bt) {
			return;
		}
		if (this.currentTask != null) {
			this.currentTask.statusListeners.removeListener(this.listenerOwner);
		}
		this.currentTask = bt;
		if (this.currentTask != null) {
			this.currentTask.statusListeners.addListener(this.listenerOwner, new TaskStatusListener() {
				
				@Override
				public void statusChanged() {
					refreshTaskDisplay();
				}
			});
		}
		refreshTaskDisplay();
	}
	
	void morphPauseButton(boolean showResume)
	{
		if (showResume) {
			pauseButton.setText("Reprendre");
			pauseButton.setIcon(IconProvider.getIcon("play", IconSize.IconSizeButton));
		} else {
			pauseButton.setText("Pause");
			pauseButton.setIcon(IconProvider.getIcon("pause", IconSize.IconSizeButton));
		}
	}
	
	public void refreshTaskDisplay()
	{
		BaseTask bt = currentTask;
		if (bt == null) {
			this.lblDbutfin.setEnabled(false);
			this.lblStatus.setEnabled(false);
			this.lblShowStatus.setText(" ");
			this.lblShowTiming.setText(" ");
			this.lblShowTitle.setText(" ");
			configButton.setEnabled(false);
			stopButton.setEnabled(false);
			removeButton.setEnabled(false);
			morphPauseButton(false);
			pauseButton.setEnabled(false);
		} else {
			this.lblDbutfin.setEnabled(true);
			this.lblStatus.setEnabled(true);
			this.lblShowStatus.setText(bt.getStatus().getTitle() + (bt.getStatusDetails() != null ? " : " + bt.getStatusDetails() : ""));
			this.lblShowStatus.setForeground(bt.getStatus().getColor());
			
			
			if (bt.getStartTime() != null) {
				if (bt.getEndTime() != null) {
					this.lblShowTiming.setText(
							new Date(bt.getStartTime()).toLocaleString()
							 + " - " + Utils.formatDuration(bt.getEndTime() - bt.getStartTime()));
				} else {
					this.lblShowTiming.setText(new Date(bt.getStartTime()).toLocaleString());
				}
			} else {
				this.lblShowTiming.setText("");
			}
			this.lblShowTitle.setText(bt.getTitle());
			
			configButton.setEnabled(true/*bt.getParentLauncher() == null*/);
			stopButton.setEnabled((!bt.getStatus().isTerminal()) && (!bt.hasPendingCancelation()));
			removeButton.setEnabled(bt.getStatus().isTerminal());
			
			if (bt.getStatus() == BaseStatus.Paused) {
				morphPauseButton(true);
				pauseButton.setEnabled(true);
			} else {
				morphPauseButton(false);
				pauseButton.setEnabled(bt.pausable());
			}
		}
		
	}
	
	
	
}
