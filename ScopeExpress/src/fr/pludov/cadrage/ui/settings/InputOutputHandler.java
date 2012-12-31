package fr.pludov.cadrage.ui.settings;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

public class InputOutputHandler<TARGET> {
    TARGET target;

    public static interface Converter<TARGET>
    {
    	void addListener(final InputOutputHandler<TARGET> target);
    	void setParameter(InputOutputHandler<TARGET> ioHandler);
    	void loadParameter(InputOutputHandler<TARGET> ioHandler);
    }
    
	public static abstract class TextConverter<TARGET, CONTENT> implements Converter<TARGET>
	{
		final JTextField component;
		
		TextConverter(JTextField component)
		{
			this.component = component;
		}

		@Override
		public final void addListener(final InputOutputHandler<TARGET> target)
		{
			this.component.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					setParameter(target);
				}
			});
			this.component.addFocusListener(new FocusListener() {
				
				@Override
				public void focusLost(FocusEvent e) {
					setParameter(target);
				}
				
				@Override
				public void focusGained(FocusEvent e) {
				}
			});
		}
		
		@Override
		public final void setParameter(InputOutputHandler<TARGET> ioHandler)
		{
			String currentText = component.getText();

			CONTENT value;
			try {
				
				if (currentText == null || currentText.equals("")) {
					value = null;
				} else {
					value = fromString(currentText);
				}
				
				setParameter(ioHandler.target, value);
			} catch(Exception e) {
				
			}
		}

		@Override
		public final void loadParameter(InputOutputHandler<TARGET> ioHandler)
		{
			CONTENT value = getFromParameter(ioHandler.target);
			
			String text;
			if (value == null) {
				text = "";
			} else {
				text = toString(value);
			}
			component.setText(text);
		}
		
		abstract CONTENT getFromParameter(TARGET parameters);
		abstract void setParameter(TARGET parameters, CONTENT content) throws Exception;
		
		abstract String toString(CONTENT t);
		abstract CONTENT fromString(String s) throws Exception;
	}
	
	public static abstract class IntConverter<TARGET> extends TextConverter<TARGET, Integer>
	{
		IntConverter(JTextField component) {
			super(component);
		}
		
		String toString(Integer t)
		{
			return t.toString();
		}
		
		Integer fromString(String s) throws Exception
		{
			return Integer.parseInt(s);
		}
	}

	public static abstract class PercentConverter<TARGET> extends TextConverter<TARGET, Double>
	{
		PercentConverter(JTextField component) {
			super(component);
		}
		
		String toString(Double d)
		{
			return Double.toString(d * 100.0) + "%";
		}
		
		Double fromString(String s) throws Exception
		{
			s = s.trim();
			if (s.endsWith("%")) s = s.substring(0, s.length() - 1).trim();
			return Double.parseDouble(s) / 100.0;
		}
	}

	public static abstract class DoubleConverter<TARGET> extends TextConverter<TARGET, Double>
	{
		DoubleConverter(JTextField component) {
			super(component);
		}
		
		String toString(Double d)
		{
			return Double.toString(d);
		}
		
		Double fromString(String s) throws Exception
		{
			return Double.parseDouble(s);
		}
	}

	public static abstract class EnumConverter<TARGET, CONTENT extends Enum> implements Converter<TARGET>
	{
		CONTENT [] values;
		JComboBox component;
		
		EnumConverter(JComboBox component, CONTENT [] values) {
			this.component = component;
			this.values = values;
		}
		
		abstract CONTENT getFromParameter(TARGET parameters);
		abstract void setParameter(TARGET parameters, CONTENT content) throws Exception;
		
		@Override
		public void addListener(final InputOutputHandler<TARGET> target) {
			component.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setParameter(target);
				}
			});
			this.component.addFocusListener(new FocusListener() {
				
				@Override
				public void focusLost(FocusEvent e) {
					setParameter(target);
				}
				
				@Override
				public void focusGained(FocusEvent e) {
				}
			});
			
			for(CONTENT item : values)
			{
				component.addItem(item);
			}
		}
		

		@Override
		public final void setParameter(InputOutputHandler<TARGET> ioHandler)
		{
			CONTENT value = (CONTENT)component.getSelectedItem();
			try {
				setParameter(ioHandler.target, value);
			} catch(Exception e) {
				
			}
		}

		@Override
		public final void loadParameter(InputOutputHandler<TARGET> ioHandler)
		{
			CONTENT value = getFromParameter(ioHandler.target);
			
			component.setSelectedItem(value);
		}

	}

	public static abstract class BooleanConverter<TARGET> implements Converter<TARGET>
	{
		JCheckBox component;
		
		BooleanConverter(JCheckBox component) {
			this.component = component;
		}
		
		abstract Boolean getFromParameter(TARGET parameters);
		abstract void setParameter(TARGET parameters, Boolean content) throws Exception;
		
		@Override
		public void addListener(final InputOutputHandler<TARGET> target) {
			component.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setParameter(target);
				}
			});
			this.component.addFocusListener(new FocusListener() {
				
				@Override
				public void focusLost(FocusEvent e) {
					setParameter(target);
				}
				
				@Override
				public void focusGained(FocusEvent e) {
				}
			});
		}
		

		@Override
		public final void setParameter(InputOutputHandler<TARGET> ioHandler)
		{
			Boolean value = component.isSelected();
			try {
				setParameter(ioHandler.target, value);
			} catch(Exception e) {
				
			}
		}

		@Override
		public final void loadParameter(InputOutputHandler<TARGET> ioHandler)
		{
			Boolean value = getFromParameter(ioHandler.target);
			
			component.setSelected(value);
		}

	}

	
	
	private Converter<TARGET> [] converters;
	
	public InputOutputHandler() {
	}
	
	public void init(Converter<TARGET> [] converters)
	{
		this.converters = converters;
		for(Converter<TARGET> converter : converters)
		{
			converter.addListener(this);
		}
	}

	public void loadParameters(TARGET parameters)
	{
		this.target = parameters;
		for(Converter<TARGET> converter : converters)
		{
			converter.loadParameter(this);
		}		
	}
	
}
