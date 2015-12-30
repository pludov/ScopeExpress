package fr.pludov.scopeexpress.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.Timer;

import fr.pludov.scopeexpress.camera.*;
import fr.pludov.scopeexpress.camera.TemperatureAdjusterTask.Status;
import fr.pludov.scopeexpress.ui.preferences.*;
import fr.pludov.scopeexpress.ui.utils.*;
import fr.pludov.scopeexpress.ui.utils.Utils.*;
import fr.pludov.scopeexpress.ui.widgets.*;
import fr.pludov.scopeexpress.ui.widgets.AbstractIconButton.*;
import fr.pludov.scopeexpress.utils.*;

/**
 * Un bouton avec un popup et le fond:
 *   gris : le refroidissemnt n'est pas actif
 *   orange : la caméra refroidi
 *   bleu : la caméra est à température cible
 *   rouge : il y a eu une erreur (disparait lors d'un clic)
 * 
 * Les options sont:
 *   interrompre (met en pause la tache actuelle)
 *   refroidir
 *   changer la température cible
 *   réchauffer
 *
 */
public class CameraControlPanel extends CameraControlPanelDesign {
	private final WeakListenerOwner owner = new WeakListenerOwner(this);
	
	public static DoubleConfigItem lastCoolTemp = new DoubleConfigItem(CameraControlPanel.class, "lastCoolTemp", -5.0);
	public static DoubleConfigItem lastCoolStep = new DoubleConfigItem(CameraControlPanel.class, "lastCoolStep", 3.0);
	public static IntegerConfigItem lastCoolTimeout = new IntegerConfigItem(CameraControlPanel.class, "lastCoolTimeout", 120);
	
	public static DoubleConfigItem lastWarmTemp = new DoubleConfigItem(CameraControlPanel.class, "lastWarmTemp", 10.0);
	public static DoubleConfigItem lastWarmStep = new DoubleConfigItem(CameraControlPanel.class, "lastWarmTemp", 3.0);
	public static IntegerConfigItem lastWarmTimeout = new IntegerConfigItem(CameraControlPanel.class, "lastWarmTimeout", 120);
	
	final FocusUiCameraManager cameraManager;
	Camera camera;
	final Timer exposureTimer;
	final AbstractIconButton btnTemp;
	
	final Color btnTempDefaultBackground;
	
	ShootParameters parameters;
	
	CameraControlPanel(FocusUiCameraManager cameraManager)
	{
		this.cameraManager = cameraManager;
		this.parameters = new ShootParameters();
		loadParameters(this.parameters);
		

		this.btnTemp= new ToolbarButton("temperature-control", true);
		add(this.btnTemp, "cell 2 5 1 2");
		btnTempDefaultBackground = this.btnTemp.getBackground();
		this.btnTemp.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				GlobalContext gc = new GlobalContext();
				if (gc.canStartCooler()) {
					coolTo(lastCoolTemp.get());
				} else if (gc.canWarmup()) {
					warmTo(lastWarmTemp.get());
				}
			}
		});
		// Positionne ce label maintenant pour réserver l'espace requis
		lblTempValue.setText("CCD:-99.99°C EXt:-99.99°C Cooler:off Set:-99.99°C");
		lblCameraName.setText("Very long long text XXXXXXXXXX");
		this.btnTemp.setPopupProvider(new PopupProvider() {
			
			@Override
			public JPopupMenu popup() {
				GlobalContext gc = new GlobalContext();
				
				JPopupMenu jpopup = new JPopupMenu();
				
				if (gc.canStartCooler()) {
					JMenuItem cool = new JMenuItem("Refroidir jusqu'à " + lastCoolTemp.get() + "°C");
					cool.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent arg0) {
							coolTo(lastCoolTemp.get());
						};
					});
					jpopup.add(cool);
				}
				if (gc.canWarmup()) {
					JMenuItem warm = new JMenuItem("Réchauffer à " + lastWarmTemp.get() + "°C");
					warm.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent arg0) {
							warmTo(lastWarmTemp.get());
						};
					});
					jpopup.add(warm);
					
					JMenuItem warmAdvanced = new JMenuItem("Réchauffer...");
					warmAdvanced.addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent arg0) {
							showCameraTemperatureAdjustmentDialog(1);
						}
					});
					jpopup.add(warmAdvanced);
				}
				if (gc.canCoolMore()) {
					JMenuItem coolAdvanced = new JMenuItem("Refroidir...");
					coolAdvanced.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent arg0) {
							showCameraTemperatureAdjustmentDialog(-1);
						}
					});
					jpopup.add(coolAdvanced);
				}
				
				if (gc.canInterrupt()) {
					JMenuItem interrupt = new JMenuItem("Interrompre le " + (gc.currentTemperatureAdjustment.getSens() == -1 ? "refroidissement" : "réchauffement"));
					interrupt.addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent arg0) {
							GlobalContext gc = new GlobalContext();
							if (gc.currentTemperatureAdjustment != null) {
								Double setTemp = gc.temps.getCCDTemperature();
								gc.currentTemperatureAdjustment.cancel();
								try {
									camera.setCcdTemperature(true, setTemp);
								} catch (CameraException e) {
									new EndUserException("Impossible de positionner la température", e);
								}
							}
						}
					});
					jpopup.add(interrupt);
				}
				
				return jpopup;
			}
		});
		
		double [] exposures = {0.001, 0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1.0, 1.5, 2.0, 3.0, 4.0, 5.0, 10.0, 15.0, 20.0, 30.0, 60, 90, 120, 180, 240, 300, 600, 1200};
		for(double d : exposures) {
			this.comboExp.addItem(expFormat.format(d));
		}
		
		
		this.cameraManager.listeners.addListener(this.owner, new DeviceManager.Listener() {
			@Override
			public void onDeviceChanged() {
				setCamera(CameraControlPanel.this.cameraManager.getDevice());
			}
		});
		setCamera(this.cameraManager.getDevice());
		
		this.exposureTimer = new Timer(1000, new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				refresh();

			}
		});
		this.exposureTimer.setCoalesce(true);
		this.exposureTimer.setRepeats(true);
		
		Utils.addComboChangeListener(this.comboExp, new Runnable() {
			@Override
			public void run() {
				try {
					if (comboExp.isEnabled()) {
						parameters.setExp(expFormat.parse((String)comboExp.getSelectedItem()).doubleValue());
						comboExp.setSelectedItem(expFormat.format(parameters.getExp()));
					}
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		refresh();
		
		this.btnShoot.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					camera.startShoot(parameters);
				}catch(CameraException e) {
					new EndUserException(e).report(CameraControlPanel.this);
				}
			}
		});
		
		this.btnInterrupt.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent ev) {
				try {
					camera.cancelCurrentShoot();
				} catch(CameraException e) {
					new EndUserException(e).report(CameraControlPanel.this);
				}
			}
		});
	}
	
	private String getTemperatureAdjusterTaskTitle(TemperatureAdjusterTask tat)
	{
		if (tat.isShutdownOnTarget()) {
			return "Réchauffement à " + tempFormat.format( tat.getTargetTemperature()) + "°C";
		} else {
			return "Refroidissement à " + tempFormat.format( tat.getTargetTemperature()) + "°C";
		}
	}
	
	
	
	class GlobalContext {
		boolean hasCamera = (camera != null && camera.isConnected());
		CameraProperties camProps = hasCamera ? camera.getProperties() : null;
		TemperatureParameters temps = hasCamera ? camera.getTemperature() : null;
		RunningShootInfo currentShoot = hasCamera ? camera.getCurrentShoot() : null;
		TemperatureAdjusterTask currentTemperatureAdjustment = hasCamera ? cameraManager.getCurrentAdjuster() : null;
		
		boolean canStartCooler() {
			return currentTemperatureAdjustment == null && hasCamera && temps != null && !temps.isCoolerOn();
		}

		boolean canCoolMore() {
			return currentTemperatureAdjustment == null && hasCamera && temps != null;
		}
		
		public boolean canInterrupt() {
			return currentTemperatureAdjustment != null && hasCamera;
		}

		boolean canChangeCooler() {
			return currentTemperatureAdjustment == null && hasCamera && temps != null && temps.isCoolerOn();
		}
		
		boolean canWarmup() {
			return currentTemperatureAdjustment == null && hasCamera && temps != null && temps.isCoolerOn();
		}
		
		void updateDisplay()
		{
			progressBar.setStringPainted(true);
			btnConnecter.setVisible(false);
			btnShoot.setVisible(!hasCamera || (hasCamera && currentShoot == null));
			btnShoot.setEnabled(hasCamera);
			btnInterrupt.setVisible(hasCamera && currentShoot != null);
			
			// lblExp.setVisible(hasCamera);
			lblExp.setEnabled(hasCamera);
			comboExp.setEditable(hasCamera);
			comboExp.setEnabled(hasCamera && currentShoot == null);
			
			lblGain.setEnabled(hasCamera);
			comboGain.setEditable(false && hasCamera);
			comboGain.setEnabled(false && hasCamera && currentShoot == null);
			
			lblMode.setEnabled(hasCamera);
			comboMode.setEditable(false && hasCamera);
			comboMode.setEnabled(false && hasCamera && currentShoot == null);
			
			lblTemp.setEnabled(hasCamera);

			// camProps.isCanSetCCDTemperature());
			lblTempValue.setVisible(hasCamera && (camProps.isCanSetCCDTemperature() || camProps.isCanGetCoolerPower()));
			// btnTemp.setVisible(hasCamera &&
			// camProps.isCanSetCCDTemperature());

			lblCameraName.setText(hasCamera ? "Model:" + camProps.getSensorName() : "");
			
			if (!hasCamera) {
				exposureTimer.stop();
				
				progressBar.setValue(0);
				progressBar.setString("Déconnecté");
				
				btnTemp.setStatus(AbstractIconButton.Status.DEFAULT);
				btnTemp.setToolTipText(null);

			} else {
				if (temps == null) {
					lblTempValue.setText("N/A");
					btnTemp.setStatus(AbstractIconButton.Status.DEFAULT);
					btnTemp.setToolTipText(null);
				} else {
					String temp = "";
					List<String> tempMessages = new ArrayList<>();
					if (temps.getCCDTemperature() != null) {
						tempMessages.add("CCD:" + tempFormat.format(temps.getCCDTemperature()));
					}
					if (temps.getHeatSinkTemperature() != null) {
						tempMessages.add("Ext:" + tempFormat.format(temps.getHeatSinkTemperature()));
					}
					tempMessages.add("Cooler:" + (temps.isCoolerOn() ? "on" : "off"));
					if (temps.isCoolerOn() && camProps.isCanSetCCDTemperature() && temps.getSetCCDTemperature() != null) {
						tempMessages.add("Set:" + tempFormat.format(temps.getSetCCDTemperature()));
					}
					for(String msg : tempMessages) {
						if (temp.equals("")) {
							temp = msg;
						} else {
							temp += " " + msg;
						}
					}
					lblTempValue.setText(temp);
					
					btnTemp.setStatus(currentTemperatureAdjustment != null ?
							AbstractIconButton.Status.PENDING : 
							temps.isCoolerOn() ? AbstractIconButton.Status.OK : AbstractIconButton.Status.DEFAULT);
					
					btnTemp.setToolTipText(currentTemperatureAdjustment != null ? 
							getTemperatureAdjusterTaskTitle(currentTemperatureAdjustment) : null);
				}
				
				
				if (currentShoot == null) {
					exposureTimer.stop();
					
					progressBar.setString("Inactif");
					progressBar.setValue(0);
					progressBar.setMaximum(0);

				} else {
					long currentShootStart = currentShoot.getStartTime();
					progressBar.setStringPainted(true);
					if (currentShoot.getExp() >= 2.0) {
						int durationsec = (int)Math.floor(currentShoot.getExp());
						int value = (int)Math.floor((System.currentTimeMillis() - currentShootStart) / 1000);
						if (value > durationsec) {
							value = durationsec;
						}
						
						progressBar.setMaximum(durationsec);
						progressBar.setValue(value);
						progressBar.setString("Capture: " + value + "s /"+ durationsec);
					} else {
						int durationmsec = (int)Math.floor(currentShoot.getExp() * 1000);
						long value = System.currentTimeMillis() - currentShootStart;
						if (value > durationmsec) {
							value = durationmsec;
						}
						
						progressBar.setMaximum(durationmsec);
						progressBar.setValue((int)value);
						progressBar.setString("Capture: " + value + "ms /"+ durationmsec);
					}
					
				}
			}
		}
	}
	
	// FIXME: Move to utils
	public static final NumberFormat tempFormat = new DecimalFormat("0.0°");
	public static final NumberFormat tempParseFormat = new DecimalFormat(".");
	public static final NumberFormat expFormat = new DecimalFormat("0.###");
	
	void loadParameters(ShootParameters p)
	{
		comboExp.setSelectedItem(expFormat.format(p.getExp()));
	}
	
	void showCameraTemperatureAdjustmentDialog(final int sens)
	{
		Utils.openDialog(this, new WindowBuilder<JDialog>() {
			@Override
			public JDialog build(Window w) {
				final JDialog jd = new JDialog(w);
				final CameraTemperatureAdjustment cta = new CameraTemperatureAdjustment();
				
				if (sens == -1) {
					cta.setValue(lastCoolTemp.get(), lastCoolStep.get(), lastCoolTimeout.get());
				} else {
					cta.setValue(lastWarmTemp.get(), lastWarmStep.get(), lastWarmTimeout.get());
				}
				
				Utils.addDialogButton(jd, new Runnable() {
					@Override
					public void run() {
						if (sens == -1) {
							lastCoolTemp.set(cta.getTargetTemperature());
							lastCoolStep.set(cta.getTemperatureStep());
							lastCoolTimeout.set(cta.getTimeout());
							jd.dispose();
							coolTo(lastCoolTemp.get());
						} else {
							lastWarmTemp.set(cta.getTargetTemperature());
							lastWarmStep.set(cta.getTemperatureStep());
							lastWarmTimeout.set(cta.getTimeout());
							jd.dispose();
							warmTo(lastWarmTemp.get());
						}
					}
				});
				Utils.autoDisposeDialog(jd);

				if (sens == -1) {
					jd.setTitle("Refroidissement");
				} else {
					jd.setTitle("Réchauffement");
				}
				jd.add(cta);
				jd.pack();
				jd.setModal(true);
				if (w != null) {
					jd.setLocationRelativeTo(w);
				}
				jd.setVisible(true);
				return jd;
			}
			
			@Override
			public boolean isInstance(Window w) {
				return false;
			}
		});
			
	}
	
	void coolTo(double temp)
	{
		final TemperatureAdjusterTask tat = new TemperatureAdjusterTask();
		tat.setTimeout(lastCoolTimeout.get());
		tat.setTemperatureStep(lastCoolStep.get());
		tat.setTargetTemperature(temp);
		tat.setSens(-1);
		tat.setShutdownOnTarget(false);
		pushTemperatureAdjusterTask(tat);
	}
	
	void warmTo(double temp)
	{
		final TemperatureAdjusterTask tat = new TemperatureAdjusterTask();
		tat.setTimeout(lastWarmTimeout.get());
		tat.setTemperatureStep(lastWarmStep.get());
		tat.setTargetTemperature(temp);
		tat.setSens(1);
		tat.setShutdownOnTarget(true);
		pushTemperatureAdjusterTask(tat);
	}
	
	void pushTemperatureAdjusterTask(final TemperatureAdjusterTask tat)
	{
		tat.listeners.addListener(this.owner, new TemperatureAdjusterTask.Listener() {
			
			@Override
			public void onStatusChanged() {
				if (tat.getStatus() == Status.Error) {
					String title;
					if (tat.getSens() == -1) {
						title = "Impossible de terminer le refroidissement";
					} else {
						title = "Impossible de terminer le réchauffement";
					}
					new EndUserException(title, new Exception(tat.getStatusErrorDetails())).report(CameraControlPanel.this);
				}
			}
		});
		cameraManager.setCurrentAdjuster(tat);
	}
	
	void refresh()
	{
//		boolean hasCamera = (camera != null && camera.isConnected());
//		CameraProperties camProps = hasCamera ? camera.getProperties() : null;
//		TemperatureParameters temps = hasCamera ? camera.getTemperature() : null;
//		RunningShootInfo currentShoot = hasCamera ? camera.getCurrentShoot() : null;
		
		new GlobalContext().updateDisplay();
	}
	
	
	void setCamera(Camera camera)
	{
		if (camera == this.camera) {
			return;
		}
		if (this.camera != null) {
			this.camera.getListeners().removeListener(this.owner);
			
		}
		this.camera = camera;
		if (camera != null) {
			this.camera.getListeners().addListener(this.owner, new Camera.Listener() {
				
				@Override
				public void onShootStarted(RunningShootInfo rsi) {
					refresh();
					exposureTimer.setDelay(rsi.getExp() >= 2.0 ? 1000 : 100);
					exposureTimer.setInitialDelay(exposureTimer.getDelay());
					exposureTimer.start();
					loadParameters(rsi);
				}
				@Override
				public void onShootDone(RunningShootInfo shootInfo, File generatedFits) {
					
					loadParameters(parameters);
					refresh();
				}
				
				@Override
				public void onShootInterrupted() {
				}
				
				@Override
				public void onConnectionStateChanged() {
					refresh();
				}
				
				@Override
				public void onTempeatureUpdated() {
					refresh();
				}
			});
		}
		
		refresh();
	}
	
}
