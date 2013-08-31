package fr.pludov.cadrage.ui.focus;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JTextField;

import fr.pludov.cadrage.ui.dialogs.MosaicStarter;
import fr.pludov.cadrage.ui.preferences.BooleanConfigItem;
import fr.pludov.cadrage.ui.preferences.StringConfigItem;
import fr.pludov.cadrage.ui.utils.Utils;
import fr.pludov.cadrage.utils.EndUserException;

public class ConfigurationEdit extends ConfigurationEditDesign {
	static abstract class ConfigItem<DATA> {
		
		// Fourni la valeur en conf (forcement présente, utilise le défaut si besoin)
		abstract DATA getSaved();
		// Sauve en conf
		abstract void setSaved(DATA o);
		abstract DATA get(Configuration config);
		abstract void set(Configuration config, DATA o);
		
		abstract void setupEditor(ConfigurationEdit ce);
		abstract DATA get(ConfigurationEdit ce) throws EndUserException;
		abstract void set(ConfigurationEdit ce, DATA d);
	}
	
	static abstract class ConfigItemDeg extends ConfigItem<Double> {
		
		final StringConfigItem stringConfigItem;
		final double defaultValue;
		
		ConfigItemDeg(String name, double defaultValue)
		{
			this.defaultValue = defaultValue;
			this.stringConfigItem = new StringConfigItem(Configuration.class, name, Utils.formatDegMinSec(defaultValue));
		}
		
		@Override
		Double getSaved()
		{
			Double result;
			try {
				result = Utils.getDegFromInput(stringConfigItem.get());
			} catch(NumberFormatException e) {
				result = null;
			}
			if (result == null) {
				result = defaultValue;
			}
			return result;
		}
		
		@Override
		void setSaved(Double o)
		{
			stringConfigItem.set(Utils.formatDegMinSec(o));
		}
		
		
		abstract Double get(Configuration config);
		abstract void set(Configuration config, Double o);
		abstract JTextField getInputField(ConfigurationEdit e);
		abstract JLabel getErrorField(ConfigurationEdit e);
		
		@Override
		final Double get(ConfigurationEdit ce) throws EndUserException
		{
			EndUserException errorMessage = null;
			try {
				JTextField textField = getInputField(ce);
				String currentValue = textField.getText();
				Double d = Utils.getDegFromInput(currentValue);
			
				if (d == null) {
					throw new EndUserException("Valeur invalide");
				}
				return d;
			} catch(EndUserException e) {
				errorMessage = e;
				throw e;
			} finally {
				JLabel error = getErrorField(ce);
				if (errorMessage != null) {
					error.setVisible(true);
					error.setToolTipText(errorMessage.getMessage());
				} else {
					error.setVisible(false);
					error.setToolTipText("");
				}
			}
		}
		
		@Override
		final void set(ConfigurationEdit ce, Double value)
		{
			JTextField textField = getInputField(ce);
			textField.setText(Utils.formatDegMinSec(value));
			JLabel error = getErrorField(ce);
			error.setVisible(false);
			error.setToolTipText("");
		}
		
		@Override
		void setupEditor(final ConfigurationEdit ce) {
			JTextField field = getInputField(ce);
			Utils.addTextFieldChangeListener(field, new Runnable() {
				@Override
				public void run() {
					try {
						get(ce);
					} catch(EndUserException e) {}
				}
			});
		}
	}
	
	static ConfigItem [] configItems = {
		new ConfigItemDeg("latitude", Utils.getDegFromInput("+48° 0' 0\"")) {

			@Override
			Double get(Configuration config) {
				return config.getLatitude();
			}

			@Override
			void set(Configuration config, Double o) {
				config.setLatitude(o);
			}

			@Override
			JTextField getInputField(ConfigurationEdit e) {
				return e.fieldLatitude;
			}

			@Override
			JLabel getErrorField(ConfigurationEdit e) {
				return e.fieldLatitudeErr;
			}
		},
		new ConfigItemDeg("longitude", Utils.getDegFromInput("+2° 0' 0\"")){

			@Override
			Double get(Configuration config) {
				return config.getLongitude();
			}

			@Override
			void set(Configuration config, Double o) {
				config.setLongitude(o);
			}

			@Override
			JTextField getInputField(ConfigurationEdit e) {
				return e.fieldLongitude;
			}

			@Override
			JLabel getErrorField(ConfigurationEdit e) {
				return e.fieldLongitudeErr;
			}
		}
		
			
	};
	
	public ConfigurationEdit(Window parent) {
		super(parent);
		for(ConfigItem i : configItems)
		{
			i.setupEditor(this);
		}
		
		getOkButton().addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					setValuesTo(Configuration.getCurrentConfiguration());
					setVisible(false);
				} catch(EndUserException ex) {
					ex.report(ConfigurationEdit.this);
				}
			}
		});
		
		getCancelButton().addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				ConfigurationEdit.this.setVisible(false);
			}
		});
	}
	
	public void loadValuesFrom(Configuration config)
	{
		for(ConfigItem i : configItems)
		{
			Object value = i.get(config);
			i.set(this, value);
		}
	}
	
	public void setValuesTo(Configuration config) throws EndUserException
	{
		Object [] values = new Object[configItems.length];
		
		for(int i = 0; i < configItems.length; ++i)
		{
			ConfigItem item = configItems[i];
			values[i] = item.get(this);
		}
		
		for(int i = 0; i < configItems.length; ++i)
		{
			ConfigItem item = configItems[i];
			item.set(config, values[i]);
		}
	}

	static void loadDefaults(Configuration config)
	{
		for(ConfigItem i : configItems)
		{
			Object value = i.getSaved();
			i.set(config, value);
		}
	}
	
	static void save(Configuration config)
	{
		for(ConfigItem i : configItems)
		{
			Object value = i.get(config);
			i.setSaved(value);
		}
	}
}
