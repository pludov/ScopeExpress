package fr.pludov.scopeexpress.tasks.autofocus;

import java.awt.Color;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Objects;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import fr.pludov.scopeexpress.focuser.Focuser;
import fr.pludov.scopeexpress.scope.DeviceIdentifier;
import fr.pludov.scopeexpress.tasks.BaseStatus;
import fr.pludov.scopeexpress.tasks.BaseTask;
import fr.pludov.scopeexpress.tasks.TaskDetailView;
import fr.pludov.scopeexpress.tasks.TaskStatusListener;
import fr.pludov.scopeexpress.ui.AutoFocusTask.Listener;
import fr.pludov.scopeexpress.ui.AutoFocusTask.Status;
import fr.pludov.scopeexpress.ui.AutoFocusTask;
import fr.pludov.scopeexpress.ui.DeviceManager;
import fr.pludov.scopeexpress.ui.FocusUi;
import fr.pludov.scopeexpress.ui.preferences.BooleanConfigItem;
import fr.pludov.scopeexpress.ui.preferences.ConfigItem;
import fr.pludov.scopeexpress.ui.preferences.StringConfigItem;
import fr.pludov.scopeexpress.ui.settings.AstrometryParameterPanel;
import fr.pludov.scopeexpress.ui.settings.InputOutputHandler;
import fr.pludov.scopeexpress.ui.settings.AstrometryParameterPanel.AstrometryParameter;
import fr.pludov.scopeexpress.ui.settings.InputOutputHandler.Converter;
import fr.pludov.scopeexpress.ui.settings.InputOutputHandler.DegConverter;
import fr.pludov.scopeexpress.ui.settings.InputOutputHandler.IntConverter;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;

public class TaskAutoFocusDetails extends TaskAutoFocusDetailsDesign implements TaskDetailView {
	

	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	final FocusUi focusUi;

	TaskAutoFocus currentTask;
	
	TaskAutoFocusGraph graph;
	
	// Sert à changer automatiquement de page
	Integer previousPassId;

	Focuser currentFocuser;
	
	public TaskAutoFocusDetails(FocusUi focusUi) {
		super();
		this.focusUi = focusUi;
		
		graph = new TaskAutoFocusGraph();
		pass1GraphPanelParent.add(graph);
		refreshStatusDisplay();
	}
	
	@Override
	public JPanel getMainPanel() {
		return this;
	}
	
	@Override
	public void setTask(BaseTask bt) {
		setTask((TaskAutoFocus)bt);
	}
	
	public void refreshStatusDisplay()
	{
		if (currentTask == null) {
			this.currentImageTextField.setText("");
			this.currentStepTextField.setText("");
			this.currentPassTextField.setText("");
			this.previousPassId = null;
		} else {
			Integer currentPosition = null;
			if (currentTask.getStatus().isTerminal()) {
				currentPosition = currentTask.finalPosition;
			} else {
				Focuser foc = focusUi.getFocuserManager().getDevice();
				if (foc != null && foc.isConnected()) {
					currentPosition = foc.position();
				}
			}
			if (currentPosition == null) {
				this.currentPositionTextField.setText("N/A");
			} else {
				this.currentPositionTextField.setText(currentPosition.toString());
			}
			
			
			this.currentPassTextField.setText(Integer.toString(currentTask.passId + 1));
			this.currentStepTextField.setText(Integer.toString(currentTask.stepId + 1));
			this.currentImageTextField.setText(Integer.toString(currentTask.imageId + 1));
		}
		
		if (currentTask != null && currentTask.getPassCenter() != null) {
			graph.loadTaskStatus(currentTask);
		} else {
			graph.loadTaskStatus(null);
		}
		
	}
	
	public void setTask(TaskAutoFocus aft)
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
}
