package fr.pludov.scopeexpress.tasks;

import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.util.*;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import fr.pludov.scopeexpress.ui.utils.DialogLayout;
import net.miginfocom.swing.MigLayout;

public class ComposedConfigurationDialog implements IConfigurationDialog {
	Map<TaskParameterId<?>, IFieldDialog<?>> parameters = new HashMap<>();
	final JPanel panel;
	final MigLayout layout;
	int childCount = 0;
	
	public ComposedConfigurationDialog() {
		this.panel = new JPanel();
		layout =  null;
		this.panel.setLayout(new DialogLayout());
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
	public void setWidgetValues(ITaskParameterView view) {
		for(Map.Entry<TaskParameterId<?>, IFieldDialog<?>> parameter : parameters.entrySet())
		{
			Object value = view.get(parameter.getKey());
			((IFieldDialog)parameter.getValue()).set(value);
		}
	}
	
	@Override
	public void loadWidgetValues(ITaskParameterView view) {
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
}
