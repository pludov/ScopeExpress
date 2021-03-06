package fr.pludov.scopeexpress.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import fr.pludov.astrometry.*;
import fr.pludov.scopeexpress.catalogs.*;
import fr.pludov.scopeexpress.focus.*;
import fr.pludov.scopeexpress.ui.joystick.*;
import fr.pludov.scopeexpress.ui.preferences.*;
import fr.pludov.scopeexpress.ui.utils.*;
import fr.pludov.scopeexpress.utils.*;

public class ConfigurationEdit extends ConfigurationEditDesign {
	static abstract class ConfigItem<DATA> {
		
		// Fourni la valeur en conf (forcement pr�sente, utilise le d�faut si besoin)
		abstract DATA getSaved();
		// Sauve en conf
		abstract void setSaved(DATA o);
		abstract DATA get(Configuration config);
		abstract void set(Configuration config, DATA o);
		
		abstract void setupEditor(ConfigurationEdit ce);
		abstract DATA get(ConfigurationEdit ce) throws EndUserException;
		abstract void set(ConfigurationEdit ce, DATA d);
	}
	
	static abstract class ConfigItemString extends ConfigItem<String> {
		final StringConfigItem stringConfigItem;
		final String defaultValue;
		
		ConfigItemString(String name, String defaultValue)
		{
			this.defaultValue = defaultValue;
			this.stringConfigItem = new StringConfigItem(Configuration.class, name, defaultValue);
		}
		
		@Override
		String getSaved() {
			String result = stringConfigItem.get();
			if (result == null) {
				return defaultValue;
			}
			return result;
		}
		
		@Override
		void setSaved(String o) {
			stringConfigItem.set(o);
		}
	
		abstract JTextField getInputField(ConfigurationEdit e);
		abstract JLabel getErrorField(ConfigurationEdit e);

		@Override
		final String get(ConfigurationEdit ce) throws EndUserException
		{
			EndUserException errorMessage = null;
			try {
				JTextField textField = getInputField(ce);
				String currentValue = textField.getText();
				String d = currentValue;
			
				if (d == null) {
					throw new EndUserException("Valeur invalide");
				}
				return d;
			} catch(EndUserException e) {
				errorMessage = e;
				throw e;
			} finally {
				JLabel error = getErrorField(ce);
				if (error != null) {
					if (errorMessage != null) {
						error.setVisible(true);
						error.setToolTipText(errorMessage.getMessage());
					} else {
						error.setVisible(false);
						error.setToolTipText("");
					}
				}
			}
		}
		
		@Override
		final void set(ConfigurationEdit ce, String value)
		{
			JTextField textField = getInputField(ce);
			textField.setText(value);
			JLabel error = getErrorField(ce);
			if (error != null) {
				error.setVisible(false);
				error.setToolTipText("");
			}
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
	
	static abstract class ConfigItemDouble extends ConfigItem<Double> {
		final StringConfigItem stringConfigItem;
		final double defaultValue;
		
		ConfigItemDouble(String name, double defaultValue)
		{
			this.defaultValue = defaultValue;
			this.stringConfigItem = new StringConfigItem(Configuration.class, name, formatValue(defaultValue));
		}
		
		@Override
		Double getSaved()
		{
			Double result;
			try {
				result = parseValue(stringConfigItem.get());
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
			stringConfigItem.set(formatValue(o));
		}

		String formatValue(double d) {
			return Double.toString(d);
		}
		
		Double parseValue(String str)
		{
			return Double.parseDouble(str);
		}
		
		@Override
		abstract Double get(Configuration config);
		@Override
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
				Double d = parseValue(currentValue);
			
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
			textField.setText(formatValue(value));
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
	
	static abstract class ConfigItemDeg extends ConfigItemDouble {
		
		
		ConfigItemDeg(String name, double defaultValue)
		{
			super(name, defaultValue);
		}
		
		@Override
		Double parseValue(String str) {
			return Utils.getDegFromInput(str);
		}
		
		@Override
		String formatValue(double d) {
			return Utils.formatDegMinSec(d);
		}
		
	}

	static abstract class ConfigItemBoolean extends ConfigItem<Boolean> {
		final StringConfigItem stringConfigItem;
		final boolean defaultValue;
		
		ConfigItemBoolean(String name, boolean defaultValue)
		{
			this.defaultValue = defaultValue;
			this.stringConfigItem = new StringConfigItem(Configuration.class, name, Boolean.toString(defaultValue));
		}
		
		@Override
		Boolean getSaved()
		{
			String str = stringConfigItem.get();
			if (str == null || "".equals(str)) {
				return defaultValue;
			} else {
				return Boolean.valueOf(str);
			}
		}
		
		@Override
		void setSaved(Boolean b)
		{
			stringConfigItem.set(b.toString());
		}

		
		@Override
		abstract Boolean get(Configuration config);
		@Override
		abstract void set(Configuration config, Boolean o);
		abstract JCheckBox getInputField(ConfigurationEdit e);
		
		@Override
		final Boolean get(ConfigurationEdit ce) throws EndUserException
		{
			return getInputField(ce).isSelected();
		}
		
		@Override
		final void set(ConfigurationEdit ce, Boolean value)
		{
			getInputField(ce).setSelected(value);
		}
		
		@Override
		void setupEditor(final ConfigurationEdit ce) {
			JCheckBox field = getInputField(ce);
			Utils.addCheckboxChangeListener(field, new Runnable() {
				@Override
				public void run() {
					try {
						get(ce);
					} catch(EndUserException e) {}
				}
			});
		}

	}
	static ConfigItem<?> tycho2 = new ConfigItemString("tycho2Path", "") {

		@Override
		String get(Configuration config) {
			return config.getStarCatalogPathTyc2();
		}

		@Override
		void set(Configuration config, String o) {
			config.setStarCatalogPathTyc2(o);
		}

		@Override
		JTextField getInputField(ConfigurationEdit e) {
			return e.starCatalogPathTyc2Field;
		}

		@Override
		JLabel getErrorField(ConfigurationEdit e) {
			return null;
		}
	};
	
	static ConfigItem<?> astrometryNetPath = new ConfigItemString("astrometryNetPath", "") {

		@Override
		String get(Configuration config) {
			return config.getAstrometryNetPath();
		}

		@Override
		void set(Configuration config, String o) {
			config.setAstrometryNetPath(o);
		}

		@Override
		JTextField getInputField(ConfigurationEdit e) {
			return e.astroNetPathField;
		}

		@Override
		JLabel getErrorField(ConfigurationEdit e) {
			return null;
		}
	};

	
	static ConfigItem<?> [] configItems = {
		new ConfigItemDeg("latitude", Utils.getDegFromInput("+48� 0' 0\"")) {

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
		new ConfigItemDeg("longitude", Utils.getDegFromInput("+2� 0' 0\"")){

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
		},
		new ConfigItemDouble("echantillonage", 5.0) {

			@Override
			Double get(Configuration config) {
				return config.getPixelSize();
			}

			@Override
			void set(Configuration config, Double o) {
				config.setPixelSize(o);
			}

			@Override
			JTextField getInputField(ConfigurationEdit e) {
				return e.fieldPixSize;
			}

			@Override
			JLabel getErrorField(ConfigurationEdit e) {
				return e.fieldPixSizeErr;
			}
		},
		new ConfigItemString("fitBase", Utils.getUserDocumentFolder() + File.separator + "photos") {
			
			
			@Override
			void set(Configuration config, String o) {
				config.setFitBase(o);
			}
			
			@Override
			String get(Configuration config) {
				return config.getFitBase();
			}
			
			@Override
			JTextField getInputField(ConfigurationEdit e) {
				return e.fieldFitBase;
			}
			
			@Override
			JLabel getErrorField(ConfigurationEdit e) {
				return null;
			}
		},
		// Voir FileNAmeGEnerator pour les patterns
		new ConfigItemString("fitPattern", "$SESSIONyyyy-MM-dd$/img$-<TARGET$$-<PHASE$$-<TYPE$$-bin<BIN$$-<FILTER$$-<EXP$_$NOW$") {
			
			
			@Override
			void set(Configuration config, String o) {
				config.setFitPattern(o);
			}
			
			@Override
			String get(Configuration config) {
				return config.getFitPattern();
			}
			
			@Override
			JTextField getInputField(ConfigurationEdit e) {
				return e.fieldFitPattern;
			}
			
			@Override
			JLabel getErrorField(ConfigurationEdit e) {
				return null;
			}
		},	
		new ConfigItemDouble("focal", 600) {

			@Override
			Double get(Configuration config) {
				return config.getFocal();
			}

			@Override
			void set(Configuration config, Double o) {
				config.setFocal(o);
			}

			@Override
			JTextField getInputField(ConfigurationEdit e) {
				return e.fieldFocal;
			}

			@Override
			JLabel getErrorField(ConfigurationEdit e) {
				return e.fieldFocalErr;
			}
		},
		new ConfigItemBoolean("ircEnabled", false) {
			@Override
			Boolean get(Configuration config) {
				return config.isIrcEnabled();
			}

			@Override
			void set(Configuration config, Boolean o) {
				config.setIrcEnabled(o);
			}

			@Override
			JCheckBox getInputField(ConfigurationEdit e) {
				return e.chckbxIrc;
			}
			
		},
		new ConfigItemBoolean("httpEnabled", false) {
			@Override
			Boolean get(Configuration config) {
				return config.isHttpEnabled();
			}

			@Override
			void set(Configuration config, Boolean o) {
				config.setHttpEnabled(o);
			}

			@Override
			JCheckBox getInputField(ConfigurationEdit e) {
				return e.chckbxHttp;
			}			
		},
		
		tycho2,
		astrometryNetPath
	};
	
	JoystickConfPanel joystickConfPanel;
	final Application application;
	
	public ConfigurationEdit(Window parent, FocusUi focusUi) {
		super(parent);
		application = focusUi.getApplication();
		
		for(ConfigItem i : configItems)
		{
			i.setupEditor(this);
		}
	
		this.joystickConfPanel = new JoystickConfPanel(focusUi);
		this.joystickPanel.add(this.joystickConfPanel);
		
		setupBrowseButton(starCatalogPathTyc2Field, starCatalogPathTyc2Browse,
				"Trouver l'emplacement du fichier tyc2.dat",
				"Fichier de donn�es Tycho-2 (tyc2.dat)",
				false,
				"tyc2\\.dat");
		
		createTaskButton(this.starCatalogPathTyc2Download, "T�l�chargement de tyc2.dat", new SyncTaskFactory() {
			@Override
			public SyncTask createWorker() {
				return new Tycho2Fetch() {
					@Override
					protected void done() {
						ConfigurationEdit.this.starCatalogPathTyc2Field.setText(getResult().toString());
					}
				};
			}
			
		});
		
		setupBrowseButton(astroNetPathField, astroNetBrowse, 
				"Emplacement des fichiers d'indexes astrometry.net",
				"Emplacement des fichiers d'indexes astrometry.net",
				true, 
				null);
		
		
		this.astroNetDownload.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String path = astroNetPathField.getText();
				final File targetDir = (path == null || path.trim().isEmpty()) ? null : new File(path.trim());
				final AstroNetIndexSelector selector = new AstroNetIndexSelector(ConfigurationEdit.this) {
					@Override
					public void onDone() {
						final SyncTask fetcher = new IndexesFetch(targetDir, getSelectedLevel()) {
							@Override
							protected void done() {
								ConfigurationEdit.this.astroNetPathField.setText(getResult().toString());
							}
						};
						fetcher.execute(ConfigurationEdit.this);
					}
				};
				selector.setModal(true);;
				selector.setVisible(true);
			}
		});
		
		
		getOkButton().addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					setValuesTo(Configuration.getCurrentConfiguration());
					application.configurationUpdated();
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
	
	private void setupBrowseButton(final JTextField starCatalogPathTyc2Field, JButton starCatalogPathTyc2Browse, 
									final String title,
									final String fileDescription,
									final boolean wantDir,
									final String regex) {
		starCatalogPathTyc2Browse.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setMultiSelectionEnabled(false);
				chooser.setFileSelectionMode(wantDir ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
				chooser.setDialogTitle(title);
		
				String lastOpenLocationValue = starCatalogPathTyc2Field.getText();
				File currentDir = "".equals(lastOpenLocationValue) ? null : new File(lastOpenLocationValue);
				chooser.setSelectedFile(currentDir);
				chooser.setFileFilter(new FileFilter() {
					
					@Override
					public String getDescription() {
						return fileDescription;
					}
					
					@Override
					public boolean accept(File f) {
						if (f.isDirectory()) return true;
						if (wantDir) return false;
						return f.getName().toLowerCase().matches(regex);
					}
				});
				if (chooser.showOpenDialog(starCatalogPathTyc2Field.getParent()) != JFileChooser.APPROVE_OPTION) return;
				
				currentDir = chooser.getSelectedFile();
				starCatalogPathTyc2Field.setText(currentDir.toString());
			}
		});
		
	}

	interface SyncTaskFactory
	{
		public SyncTask createWorker();
	}

	private void createTaskButton(final JButton button, final String title, final SyncTaskFactory factory)
	{
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final SyncTask fetcher = factory.createWorker();
				fetcher.execute(ConfigurationEdit.this);
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
			
			Object currentValue = item.get(config);
			if (Utils.equalsWithNullity(currentValue, values[i])) {
				continue;
			}
			config.setModified(true);
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
		config.setModified(false);
	}

	public JoystickConfPanel getJoystickConfPanel() {
		return joystickConfPanel;
	}
}
