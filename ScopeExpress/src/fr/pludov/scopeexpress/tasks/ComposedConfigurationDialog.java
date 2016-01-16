package fr.pludov.scopeexpress.tasks;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import fr.pludov.scopeexpress.ui.utils.*;
import net.miginfocom.swing.*;

public class ComposedConfigurationDialog implements IConfigurationDialog {
	Map<TaskParameterId<?>, IFieldDialog<?>> parameters = new HashMap<>();
	final JPanel panel;
	final JLabel logicErrorLabel;
	final MigLayout layout;
	int childCount = 0;
	
	public ComposedConfigurationDialog() {
		this.panel = new JPanel();
		layout =  null;
		this.panel.setLayout(new DialogLayout());
		this.logicErrorLabel = new JLabel();
		this.logicErrorLabel.setForeground(Color.RED);
		this.logicErrorLabel.setFont(this.logicErrorLabel.getFont().deriveFont(Font.BOLD));
		this.panel.add(logicErrorLabel);
	}

	public void add(SimpleFieldDialog<?> dialog)
	{
		//this.layout.getColumnConstraints();
		this.panel.add(dialog.panel);
		parameters.put(dialog.id, dialog);
		childCount++;
	}
	
	@Override
	public JPanel getPanel() {
		return this.panel;
	}

	@Override
	public <T> void set(TaskParameterId<T> parameter, T value) {
		IFieldDialog<T> ifd = (IFieldDialog<T>) parameters.get(parameter);
		if (ifd == null) {
			throw new RuntimeException("missing field : " + parameter);
		}
		ifd.set(value);
	}

	@Override
	public <T> T get(TaskParameterId<T> parameter) {
		IFieldDialog<T> ifd = (IFieldDialog<T>) parameters.get(parameter);
		if (ifd == null) {
			throw new RuntimeException("missing field : " + parameter);
		}
		return ifd.get();
	}

	@Override
	public void setWidgetValues(ISafeTaskParameterView view) {
		for(Map.Entry<TaskParameterId<?>, IFieldDialog<?>> parameter : parameters.entrySet())
		{
			Object value = view.get(parameter.getKey());
			((IFieldDialog)parameter.getValue()).set(value);
		}
	}
	
	@Override
	public void loadWidgetValues(ITaskParameterBaseView view) {
		for(Map.Entry<TaskParameterId<?>, IFieldDialog<?>> parameter : parameters.entrySet())
		{
			Object value = ((IFieldDialog)parameter.getValue()).get();
			view.set((TaskParameterId)parameter.getKey(), value);
		}
	}
	
	@Override
	public boolean hasError() {
		for(IFieldDialog<?> ifd : parameters.values()) {
			if (ifd.hasError()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void setLogicErrors(ITaskParameterTestView testView) {
		for(Map.Entry<TaskParameterId<?>, IFieldDialog<?>> parameter : parameters.entrySet())
		{
			String error = testView.getFieldError(parameter.getKey());
			parameter.getValue().setLogicError(error);
		}
		List<String> errors = testView.getAllErrors();
		if (!errors.isEmpty()) {
			this.logicErrorLabel.setText(errors.get(0));
		} else {
			this.logicErrorLabel.setText(null);
		}
		
	}
}
