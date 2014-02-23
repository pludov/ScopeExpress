package fr.pludov.cadrage.ui.settings;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import fr.pludov.cadrage.ui.preferences.BooleanConfigItem;
import fr.pludov.cadrage.ui.preferences.EnumConfigItem;
import fr.pludov.cadrage.ui.preferences.StringConfigItem;
import fr.pludov.cadrage.ui.utils.Utils;

public class InputOutputHandler<TARGET> {
    TARGET target;

    public static interface Converter<TARGET>
    {
    	void addListener(final InputOutputHandler<TARGET> target);
    	void setParameter(InputOutputHandler<TARGET> ioHandler);
    	void loadParameter(InputOutputHandler<TARGET> ioHandler);

    	// Lit la valeur depuis la configuration. Faux indique que ce n'est pas supporté
    	boolean setConfigValue(InputOutputHandler<TARGET> ioHandler);

    	// Enregistre la valeur courante en configuration
    	void saveConfigValue(InputOutputHandler<TARGET> ioHandler);
    }
    
	public static abstract class TextConverter<TARGET, CONTENT> implements Converter<TARGET>
	{
		final JTextField component;
		final JLabel errorLabel;
		final StringConfigItem configItem;

		TextConverter(JTextField component, StringConfigItem configItem)
		{
			this(component, null, configItem);
		}
		
		TextConverter(JTextField component, JLabel errorLabel, StringConfigItem configItem)
		{
			this.component = component;
			this.errorLabel = errorLabel;
			this.configItem = configItem;
		}

		TextConverter(JTextField component)
		{
			this(component, null);
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
			if (!component.isEnabled()) return;
			
			String currentText = component.getText();

			Exception error = null;
			CONTENT value;
			try {
				
				if (currentText == null || currentText.equals("")) {
					value = null;
				} else {
					value = fromString(currentText);
				}
				
				setParameter(ioHandler.target, value);
				
				saveConfigValue(ioHandler);
			} catch(Exception e) {
				error = e;
			}
			
			if (this.errorLabel != null) {
				this.errorLabel.setToolTipText(error != null ? error.getMessage() : null);
				this.errorLabel.setVisible(error != null);
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
			if (this.errorLabel != null) {
				this.errorLabel.setToolTipText(null);
				this.errorLabel.setVisible(false);
			}
		}
		
		@Override
    	public boolean setConfigValue(InputOutputHandler<TARGET> ioHandler)
    	{
    		if (this.configItem == null) return false;
    		if (!this.configItem.exists()) return false;
    		String value = this.configItem.get();
    		try {
    			CONTENT v = fromString(value);
    			setParameter(ioHandler.target, v);
    			return true;
    		} catch(Exception e) {
    			return false;
    		}
    	}

    	@Override
    	public void saveConfigValue(InputOutputHandler<TARGET> ioHandler)
    	{
    		if (this.configItem == null) return;
    		CONTENT c = getFromParameter(ioHandler.target);
    		this.configItem.set(toString(c));
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

	// Gestion d'une valeur en degrés représentée au format HHMMSS (RA)
	public static abstract class HourMinSecConverter<TARGET> extends TextConverter<TARGET, Double>
	{
		HourMinSecConverter(JTextField component, JLabel errorLabel, StringConfigItem configItem) {
			super(component, errorLabel, configItem);
		}
		
		String toString(Double d)
		{
			if (d == null) return "";
			return Utils.formatHourMinSec(d);
		}
		
		Double fromString(String s) throws Exception
		{
			Double d = Utils.getDegFromHourMinSec(s);
			if (d == null) {
				throw new Exception("Attendu au format: hh mm ss.s");
			}
			return d;
		}
	}
	
	// Gestion d'une valeur en degrés
	public static abstract class DegConverter<TARGET> extends TextConverter<TARGET, Double>
	{
		DegConverter(JTextField component, JLabel errorLabel, StringConfigItem configItem) {
			super(component, errorLabel, configItem);
		}
		
		@Override
		String toString(Double t) {
			if (t == null) return "";
			return Utils.formatDegMinSec(t);
		}
		
		@Override
		Double fromString(String s) throws Exception {
			Double d = Utils.getDegFromDegMinSec(s);
			if (d == null) {
				throw new Exception("Attendu au format: dd° mm' ss.s''");
			}
			return d;
		}
		
	}
	
	public static abstract class EnumConverter<TARGET, CONTENT extends Enum<CONTENT>> implements Converter<TARGET>
	{
		final CONTENT [] values;
		final JComboBox component;
		final EnumConfigItem<CONTENT> configItem;
		
		EnumConverter(JComboBox component, CONTENT [] values) {
			this(component, values, null);
		}
		
		EnumConverter(JComboBox component, CONTENT [] values, EnumConfigItem<CONTENT> defaultConfig) {
			this.component = component;
			this.values = values;
			this.configItem = defaultConfig;
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
				saveConfigValue(ioHandler);
			} catch(Exception e) {
				
			}
		}

		@Override
		public final void loadParameter(InputOutputHandler<TARGET> ioHandler)
		{
			CONTENT value = getFromParameter(ioHandler.target);
			
			component.setSelectedItem(value);
		}

		@Override
    	public boolean setConfigValue(InputOutputHandler<TARGET> ioHandler)
    	{
    		if (this.configItem == null) return false;
    		if (!this.configItem.exists()) return false;
    		CONTENT value = this.configItem.get();
    		try {
    			setParameter(ioHandler.target, value);
    			return true;
    		} catch(Exception e) {
    			return false;
    		}
    	}

    	@Override
    	public void saveConfigValue(InputOutputHandler<TARGET> ioHandler)
    	{
    		if (this.configItem == null) return;
    		CONTENT value = getFromParameter(ioHandler.target);
    		this.configItem.set(value);
    	}
	}

	public static abstract class BooleanConverter<TARGET> implements Converter<TARGET>
	{
		JCheckBox component;
		BooleanConfigItem configItem;

		BooleanConverter(JCheckBox component) {
			this.component = component;
		}

		BooleanConverter(JCheckBox component, BooleanConfigItem configItem) {
			this.component = component;
			this.configItem = configItem;
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
				
				saveConfigValue(ioHandler);
			} catch(Exception e) {
				
			}
		}

		@Override
		public final void loadParameter(InputOutputHandler<TARGET> ioHandler)
		{
			Boolean value = getFromParameter(ioHandler.target);
			
			component.setSelected(value);
		}


		@Override
    	public boolean setConfigValue(InputOutputHandler<TARGET> ioHandler)
    	{
    		if (this.configItem == null) return false;
    		if (!this.configItem.exists()) return false;
    		Boolean value = this.configItem.get();
    		try {
    			setParameter(ioHandler.target, value);
    			return true;
    		} catch(Exception e) {
    			return false;
    		}
    	}

    	@Override
    	public void saveConfigValue(InputOutputHandler<TARGET> ioHandler)
    	{
    		if (this.configItem == null) return;
    		Boolean value = getFromParameter(ioHandler.target);
    		this.configItem.set(value);
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

	public void loadWithConfigParameters(TARGET parameters)
	{
		this.target = parameters;
		for(Converter<TARGET> converter : converters)
		{
			converter.setConfigValue(this);
			converter.loadParameter(this);
		}
	}
}
