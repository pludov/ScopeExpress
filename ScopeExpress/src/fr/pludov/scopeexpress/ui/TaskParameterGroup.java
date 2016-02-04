package fr.pludov.scopeexpress.ui;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;

import fr.pludov.scopeexpress.tasks.*;
import fr.pludov.scopeexpress.ui.utils.*;

/*
 * Un groupe de paramètre, avec un cadre optionnel
 */
public class TaskParameterGroup {
	
	class FieldView<T> {
		IFieldDialog<T> dialog;
		TaskFieldStatus<T> previousStatus;
	};
	
	JPanel panel;
	final String title;
	
	final JPanel logicErrorPanel;
	final boolean forceVisible;
	List<String> logicErrors;
	List<FieldView<?>> dialogs;
	List<TaskParameterGroup> containers;
	TaskParameterGroup parent;

	final SubTaskPath path;
	
	TaskParameterGroup(SubTaskPath path, String title, boolean forceVisible)
	{
		this.path = path;
		panel = new JPanel();
		
		panel.setLayout(new DialogLayout());
		if (title != null) {
			panel.setBorder(new TitledBorder(title));
		}
		this.title = title;
		this.forceVisible = forceVisible;
		panel.setVisible(forceVisible);
		
		logicErrorPanel = new JPanel();
		logicErrorPanel.setLayout(new BoxLayout(logicErrorPanel, BoxLayout.Y_AXIS));
		panel.add(logicErrorPanel);
		
		dialogs = new ArrayList<>();
		containers = new ArrayList<>();
	}
	
	void add(TaskParameterGroup child)
	{
		containers.add(child);
		child.parent = this;
		panel.add(child.panel);
		if (child.isVisible()) {
			updateLayout();
		}
	}
	
	boolean isVisible()
	{
		return panel.isVisible();
	}
	
	<T> void add(IFieldDialog<T> dialog, TaskFieldStatus<T> status)
	{
		final FieldView<T> fieldView = new FieldView<T>();
		fieldView.dialog = dialog;
		fieldView.previousStatus = status;
		dialogs.add(fieldView);
		panel.add(dialog.getPanel());
		if (fieldView.previousStatus.isVisible()) {
			updateLayout();
		}
		dialog.setContainer(this);
		dialog.setContainerData(fieldView);
	}
	
	<T> void statusUpdated(final IFieldDialog<T> dialog, TaskFieldStatus<T> status)
	{
		FieldView<T> fieldView = (FieldView<T>) dialog.getContainerData();
		// Vérifie le changement de visiblité
		TaskFieldStatus<T> previousStatus = fieldView.previousStatus;
		fieldView.previousStatus = status;
		if (fieldView.previousStatus.isVisible() != previousStatus.isVisible()) {
			updateLayout();
		}
	}
	
	private boolean wantVisible()
	{
		if (forceVisible) return true;
		// Rendre visible si un parent devient visible
		for(FieldView<?> child : this.dialogs) {
			if (child.previousStatus.isVisible()) {
				return true;
			}
		}
		for(TaskParameterGroup tdc : this.containers) {
			if (tdc.isVisible()) {
				return true;
			}
		}
		return false;
	}
	
	void updateLayout()
	{
		System.out.println("Relayout of " + toString());
		this.panel.setVisible(wantVisible());
		this.panel.revalidate();
		if (parent != null) {
			parent.updateLayout();
		}
	}
	
	@Override
	public String toString() {
		if (parent == null) {
			if (title == null) {
				return "ROOT";
			} else {
				return title;
			}
		} else {
			return parent.toString() + "/" + title;
		}
	}
	
	
	void setLogicError(List<String> logicError)
	{
		boolean wasVisible = logicErrorPanel.isVisible();
		logicErrorPanel.setVisible(!logicError.isEmpty());
		logicErrorPanel.removeAll();
		for(String err : logicError) {
			JLabel error = new JLabel(err);
			error.setFont(error.getFont().deriveFont(Font.BOLD));
			error.setForeground(Color.RED);
			error.setToolTipText(err);
			logicErrorPanel.add(error, Component.LEFT_ALIGNMENT);
		}
		if (wasVisible != logicErrorPanel.isVisible()) {
			updateLayout();
		}
	}

	public void loadErrors(fr.pludov.scopeexpress.ui.TaskParameterPanel.TaskParameterTestView rootTaskView) {
		if (!isVisible()) {
			return;
		}
		for(TaskParameterGroup child: this.containers)
		{
			child.loadErrors(rootTaskView);
		}
		List<String> errors = new ArrayList<>();
		if (parent == null) {
			errors.addAll(rootTaskView.getTopLevelErrors());
		}
		errors.addAll(rootTaskView.getAllErrors(path));
		setLogicError(errors);
	};
}
