package fr.pludov.scopeexpress.ui;

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
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import fr.pludov.scopeexpress.focuser.Focuser;
import fr.pludov.scopeexpress.scope.DeviceIdentifier;
import fr.pludov.scopeexpress.ui.AutoFocusTask.Listener;
import fr.pludov.scopeexpress.ui.AutoFocusTask.Status;
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

public class AutoFocusDialog extends AutoFocusDialogDesign {
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	final InputOutputHandler<AutoFocusParameters> ioHandler;
	final AutoFocusParameters parameter;
	final FocusUi focusUi;

	AutoFocusTask currentTask;
	
	AutoFocusGraph [] graphs;
	
	IntConverter<AutoFocusParameters> startPosition;
	IntConverter<AutoFocusParameters> photoDuration;
	IntConverter<AutoFocusParameters> photoCount;
	IntConverter<AutoFocusParameters> backlash;
	InputOutputHandler.BooleanConverter<AutoFocusParameters> [] passEnabled;
	IntConverter<AutoFocusParameters> [] passWidth;
	IntConverter<AutoFocusParameters> [] passStepCount;
	// Sert à changer automatiquement de page
	Integer previousPassId;

	Focuser currentFocuser;
	
	public AutoFocusDialog(Window window, FocusUi focusUi) {
		super(window);
		this.focusUi = focusUi;

		focusUi.focuserManager.listeners.addListener(this.listenerOwner, new DeviceManager.Listener() {
			@Override
			public void onDeviceChanged() {
				setCurrentFocuser(AutoFocusDialog.this.focusUi.focuserManager.getDevice());
				loadDefaultParametersForCurrentFocuser();
				updateFocuserState();
			}
		});
		setCurrentFocuser(focusUi.focuserManager.getDevice());
		startPosition = new IntConverter<AutoFocusParameters>(this.startPositionTextField, this.startPositionErrorLabel, 
				new StringConfigItem(AutoFocusParameters.class, "startPosition#", "5000")) {

			@Override
			public Integer getFromParameter(AutoFocusParameters parameters) {
				return parameters.startPosition;
			}

			@Override
			public void setParameter(AutoFocusParameters parameters,
					Integer content) throws Exception {
				if (content == null || content < 0) {
					throw new Exception("Doit être positif");
				}
				parameters.startPosition = content;
			}
		};
		
		photoDuration = new IntConverter<AutoFocusParameters>(this.photoDurationTextField, this.photoDurationErrorLabel, 
				new StringConfigItem(AutoFocusParameters.class, "photoDuration#", "1")) {
			@Override
			public Integer getFromParameter(AutoFocusParameters parameters) {
				return parameters.photoDuration;
			}
			
			@Override
			public void setParameter(AutoFocusParameters parameters, Integer content) throws Exception {
				if (content == null || content < 1 || content > 60) {
					throw new Exception("Doit être entre 1 et 60s");
				}
				parameters.photoDuration = content;
			}
		};
		
		photoCount = new IntConverter<AutoFocusParameters>(this.photoCountTextField, this.photoCountErrorLabel, 
				new StringConfigItem(AutoFocusParameters.class, "photoCount#", "3")) {
			@Override
			public Integer getFromParameter(AutoFocusParameters parameters) {
				return parameters.photoCount;
			}
			
			@Override
			public void setParameter(AutoFocusParameters parameters, Integer content) throws Exception {
				if (content == null) {
					throw new Exception("obligatoire");
				}
				if (content < 1) {
					throw new Exception("Minimum une photo par pas !");
				}
				if (content > 99) {
					throw new Exception("Trop de pauses par pas : 99 max");
				}
				parameters.photoCount = content;
			}
		};

		backlash = new IntConverter<AutoFocusParameters>(this.backlashTextField, this.backlashErrorLabel, 
				new StringConfigItem(AutoFocusParameters.class, "backlash#", "0")) {
			@Override
			public Integer getFromParameter(AutoFocusParameters parameters) {
				return parameters.backlash;
			}
			
			@Override
			public void setParameter(AutoFocusParameters parameters, Integer content) throws Exception {
				if (content == null) {
					throw new Exception("obligatoire");
				}
				if (content < 0) {
					throw new Exception("Valeur négative interdite !");
				}
				parameters.backlash = content;
			}
		};
		
		Object [] passControls = 
			{
				this.pass1Checkbox,
				this.pass1WidthTextField,
				this.pass1WidthErrorLabel,
				this.pass1StepCountTextField,
				this.pass1StepCountErrorLabel,
				this.pass2Checkbox,
				this.pass2WidthTextField,
				this.pass2WidthErrorLabel,
				this.pass2StepCountTextField,
				this.pass2StepCountErrorLabel,
				this.pass3Checkbox,
				this.pass3WidthTextField,
				this.pass3WidthErrorLabel,
				this.pass3StepCountTextField,
				this.pass3StepCountErrorLabel,
			};
		
		passEnabled = new InputOutputHandler.BooleanConverter[AutoFocusParameters.maxPassCount];
		passWidth = new InputOutputHandler.IntConverter[AutoFocusParameters.maxPassCount];
		passStepCount = new InputOutputHandler.IntConverter[AutoFocusParameters.maxPassCount];
		for(int passIdTmp = 0; passIdTmp < 3; ++passIdTmp) 
		{
			final int passId = passIdTmp;
			int base = passId * 5;
			passEnabled[passId] = new InputOutputHandler.BooleanConverter<AutoFocusParameters>((JCheckBox)passControls[base], 
					new BooleanConfigItem(AutoFocusParameters.class, "pass" + passId + "Enabled#", true)) {
				@Override
				public Boolean getFromParameter(AutoFocusParameters parameters) {
					return parameters.isPassEnabled(passId);
				}
				
				@Override
				public void setParameter(AutoFocusParameters parameters, Boolean content) throws Exception {
					if (content == null) throw new Exception("Obligatoire");
					parameters.setPassEnabled(passId, content);
					
				}
			};
			passWidth[passId] = new IntConverter<AutoFocusParameters>((JTextField)passControls[base + 1], (JLabel)passControls[base + 2], 
					new StringConfigItem(AutoFocusParameters.class, "pass" + passId + "Width#", Integer.toString(10000 >> (2 * passIdTmp)))) {
				@Override
				public Integer getFromParameter(AutoFocusParameters parameters) {
					return parameters.getPassWidth(passId);
				}
				
				@Override
				public void setParameter(AutoFocusParameters parameters, Integer content) throws Exception {
					if (content == null) {
						throw new Exception("obligatoire");
					}
					if (content < 1) {
						throw new Exception("Doit être au moins 1");
					}
					parameters.setPassWidth(passId, content);
				}
			};
			passStepCount[passId] = new IntConverter<AutoFocusParameters>((JTextField)passControls[base + 3], (JLabel)passControls[base + 4], 
					new StringConfigItem(AutoFocusParameters.class, "pass" + passId + "StepCount#", "10")) {
				@Override
				public Integer getFromParameter(AutoFocusParameters parameters) {
					return parameters.getPassStepCount(passId);
				}
				
				@Override
				public void setParameter(AutoFocusParameters parameters, Integer content) throws Exception {
					if (content == null) {
						throw new Exception("obligatoire");
					}
					if (content < 3) {
						throw new Exception("Doit être au moins 3");
					}
					if (content > 99) {
						throw new Exception("Le nombre de pas maxi est de 99");
					}
					
					parameters.setPassStepCount(passId, content);
				}
			};
			
		}
		
		
		ioHandler = new InputOutputHandler<AutoFocusParameters>();
		
		ioHandler.init(new InputOutputHandler.Converter[] {
				startPosition,
				photoDuration,
				photoCount,
				backlash,
				passEnabled[0],
				passWidth[0],
				passStepCount[0],
				passEnabled[1],
				passWidth[1],
				passStepCount[1],
				passEnabled[2],
				passWidth[2],
				passStepCount[2],
		});
		

		this.parameter = new AutoFocusParameters();
		loadDefaultParametersForCurrentFocuser();
		ioHandler.loadParameters(this.parameter);
		
		
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent arg0) {
				doClose();
			}
		});
		

		this.btnInterrompre.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				interrupt();
			}
		});
		
		this.btnStart.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				start();
			}
		});
		
		
		graphs = new AutoFocusGraph[AutoFocusParameters.maxPassCount];
		graphs[0] = new AutoFocusGraph();
		this.pass1GraphPanelParent.add(graphs[0]);
		graphs[1] = new AutoFocusGraph();
		this.pass2GraphPanelParent.add(graphs[1]);
		graphs[2] = new AutoFocusGraph();
		this.pass3GraphPanelParent.add(graphs[2]);
		
		
		refreshStatusDisplay();
		updateFocuserState();
	}

	void setCurrentFocuser(Focuser focuser)
	{
		if (currentFocuser == focuser) {
			return;
		}
		
		if (currentFocuser != null) {
			currentFocuser.getListeners().removeListener(this.listenerOwner);
		}
		currentFocuser = focuser;
		if (currentFocuser != null) {
			currentFocuser.getListeners().addListener(this.listenerOwner, new Focuser.Listener() {
				
				@Override
				public void onMoving() {
					updateFocuserState();
				}
				
				@Override
				public void onMoveEnded() {
					updateFocuserState();
				}
				
				@Override
				public void onConnectionStateChanged() {
					updateFocuserState();
				}
			});
		}
	}
	
	void loadDefaultParametersForCurrentFocuser()
	{
		DeviceIdentifier deviceId = focusUi.focuserManager.getCurrentDeviceIdentifier();
		if (deviceId == null) {
			return;
		}
		for(Converter<AutoFocusParameters> converter : ioHandler.getConverters())
		{
			ConfigItem ci = converter.getConfigItem();
			if (ci != null) {
				int startAt = ci.getKey().indexOf('#');
				if (startAt == -1) {
					continue;
				}
				ci.setKey(ci.getKey().substring(0, startAt) + deviceId.getStorableId());
			}
		}
		
		ioHandler.loadWithConfigParameters(this.parameter);
	}

	void updateBtonState()
	{
		Focuser focuser = currentFocuser;
		this.btnStart.setEnabled(focuser != null && 
				(currentTask == null || canDiscardStatus(currentTask.status)));
		
		this.btnInterrompre.setEnabled(currentTask != null && canInterruptStatus(currentTask.status));
		
		this.btnClose.setEnabled(currentTask == null || canDiscardStatus(currentTask.status));
	}
	
	public void updateFocuserState()
	{
		updateBtonState();
		if (currentFocuser == null) {
			this.currentPositionTextField.setBackground(Color.red);
			this.currentPositionTextField.setText("Pas de focuser");
		} else if (!currentFocuser.isConnected()) {
			this.currentPositionTextField.setBackground(Color.red);
			this.currentPositionTextField.setText("Non connecté");
		} else {
			this.currentPositionTextField.setBackground(Color.green);
			this.currentPositionTextField.setText(Integer.toString(currentFocuser.position()));
		}
	}
	
	public void refreshStatusDisplay()
	{
		if (currentTask == null) {
			this.currentImageTextField.setText("");
			this.currentStepTextField.setText("");
			this.currentPassTextField.setText("");
			this.statusLabel.setText("En attente");
			this.statusLabel.setForeground(Color.black);
			this.statusLabel.setToolTipText("");
			this.previousPassId = null;
		} else {
			this.currentPassTextField.setText(Integer.toString(currentTask.passId + 1));
			this.currentStepTextField.setText(Integer.toString(currentTask.stepId + 1));
			this.currentImageTextField.setText(Integer.toString(currentTask.imageId + 1));
			this.statusLabel.setText(currentTask.status.title + 
					(currentTask.error != null ? ": " + currentTask.error : "") +  
					(currentTask.isInterrupting() ? " (arret)" : ""));
			this.statusLabel.setForeground(currentTask.status.color);
			if (currentTask.status == Status.Error) {
				System.out.println(currentTask.error);
				this.statusLabel.setToolTipText(currentTask.error);
			} else {
				this.statusLabel.setToolTipText("");
			}
			if (!Objects.equals(this.previousPassId, currentTask.passId)) {
				graphTab.setSelectedIndex(currentTask.passId);
				this.previousPassId = currentTask.passId;
			}
		}
		
		for(int i = 0; i < graphs.length; ++i) {
			graphs[i].loadTaskStatus(currentTask, i);
		}
		
		updateBtonState();
	}

	private static boolean canDiscardStatus(Status status)
	{
		return (status == Status.Canceled || status == Status.Error || status == Status.Finished);
	}

	private static boolean canInterruptStatus(Status status)
	{
		return !canDiscardStatus(status);
	}
	
	public void start()
	{
		if (currentTask != null) {
			if (currentTask.status != Status.Canceled && currentTask.status != Status.Error && currentTask.status != Status.Finished) {
				return;
			}
			currentTask.listeners.removeListener(this.listenerOwner);
			currentTask = null;
		}
		
		currentTask = new AutoFocusTask(focusUi.focusMosaic, focusUi, parameter);
		currentTask.listeners.addListener(this.listenerOwner, new Listener() {
			@Override
			public void onStatusChanged() {
				refreshStatusDisplay();
			}
		});
		try {
			currentTask.start();
		} catch(Exception e) {
		}
		refreshStatusDisplay();
	}

	public void interrupt()
	{
		if (currentTask != null) {
			currentTask.interrupt();
		}
	}

	public void doClose()
	{
		if (currentTask == null || canDiscardStatus(currentTask.status)) {
			currentTask = null;
			setVisible(false);
		}
	}
}
