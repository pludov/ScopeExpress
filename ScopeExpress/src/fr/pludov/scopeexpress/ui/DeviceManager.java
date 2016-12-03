package fr.pludov.scopeexpress.ui;

import java.awt.*;
import java.awt.event.*;
import java.lang.ref.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.Timer;

import fr.pludov.scopeexpress.scope.*;
import fr.pludov.scopeexpress.ui.preferences.*;
import fr.pludov.scopeexpress.ui.widgets.*;
import fr.pludov.scopeexpress.ui.widgets.AbstractIconButton.*;
import fr.pludov.scopeexpress.utils.*;

public abstract class DeviceManager<HARDWARE extends IDeviceBase> {
	static class Labels {

		String CONNECT = "Connecter: ";
		String REFRESH_LIST = "Raffraichir la liste...";
		String DISCONNECT = "Déconnecter";
		String CONTROLER = "Fenêtre de Contrôle";
		String CHOOSE = "Choisir un périphérique...";
		String DISCONNECT_TOOLTIP = "Déconnecter le périphérique...";
		String CONNECT_TOOLTIP = "Connecter au périphérique...";
	}

	public static interface Listener {
		void onDeviceChanged();
	}
	
	final Labels labels;
	final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	public final WeakListenerCollection<Listener> listeners; 
	final FocusUi focusUi;
	final List<DriverProvider<HARDWARE>> deviceProviders;
	HARDWARE device;
	DeviceIdentifier currentDeviceIdentifier;
	// Listes des id de device dispo. Si null, non disponible.
	List<DeviceIdentifier> deviceIdentifiers;
	private DeviceChoosedCallback chooser;
	private DeviceListedCallback lister;
	
	final StringConfigItem lastUsedDevice;
//	
//	public DeviceManager(DriverProvider<HARDWARE> provider, Labels labels, String configKeyName, FocusUi focusUi) {
//		this(labels, configKeyName, focusUi, provider);
//	}

	public DeviceManager(Labels labels, String configKeyName, FocusUi focusUi, DriverProvider<HARDWARE> ... provider) {
		this.focusUi = focusUi;
		this.labels = labels;
		this.deviceProviders = Arrays.asList(provider);
		this.device = null;
		this.currentDeviceIdentifier = null;
		this.listeners = new WeakListenerCollection<Listener>(Listener.class);
		this.lastUsedDevice = new StringConfigItem(DeviceManager.class, "lastUsed_" + configKeyName, "");
		
		startDeviceListing();
	}
	
	private void startDeviceListing()
	{
		if (lister != null) return;
		
		lister = new DeviceListedCallback() {
			boolean first = true;
			int expectedCount = deviceProviders.size();
			
			@Override
			public void onDeviceListed(List<? extends DeviceIdentifier> availables) {
				if (lister != this) return;
				expectedCount--;
				if (expectedCount == 0) {
					lister = null;
				}
				if (first) {
					deviceIdentifiers = new ArrayList<>();
					first = false;
				}
				deviceIdentifiers.addAll(availables);
				
				refreshDeviceMenu();
			}
		};
		
		for(DriverProvider<HARDWARE> dp : deviceProviders) 
		{
			dp.listDevices(lister);
		}
		
	}

	private void actionDeconnecter()
	{
		if (this.device == null) return;
		this.device.close();
		this.device = null;
		this.currentDeviceIdentifier = null;
		refreshDeviceMenu();
		this.listeners.getTarget().onDeviceChanged();
	}
	
	
	private void actionConnecter(DeviceIdentifier si)
	{
		if (this.device != null) return;
		if (this.chooser != null) return;
		
		boolean found = false;
		for(DriverProvider<HARDWARE> dp : this.deviceProviders)
		{
			if (dp.getProviderId().equals(si.getProviderId())) {
				device = dp.buildDevice(si);
				found = true;
			}
		}
		if (!found) {
			return;
		}
		lastUsedDevice.set(si.getStorableId());
		currentDeviceIdentifier = si;
		initDevice();
	}

	JDialog controlerDialog;

	void setControlerDialog(JDialog jdialog) {
		controlerDialog = jdialog;
	}

	private void showDialog()
	{
		JDialog jd = controlerDialog;
		if (jd != null) {
			jd.setVisible(true);
			jd.toFront();
		}
	}

	protected void initDevice() {
		showDialog();
		device.getStatusListener().addListener(this.listenerOwner,  new IDriverStatusListener() {
			final HARDWARE deviceThatChanged = device;
			
			@Override
			public void onConnectionError(Throwable t) {
				dispatchError(t);
			}
			
			@Override
			public void onConnectionStateChanged() {
				if (device != deviceThatChanged) return;

				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						refreshDeviceMenu();
					}
				});
				
				// Une déconnection, on le met à la poubelle
				if (!device.isConnected()) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							if (deviceThatChanged == device && !device.isConnected()) {
								actionDeconnecter();
							}
						}
					});
					
				}
			}
		});
		device.start();
		refreshDeviceMenu();
		this.listeners.getTarget().onDeviceChanged();
	}
	
	private DriverProvider<HARDWARE> getProviderForClass(String clazz)
	{
		for(DriverProvider<HARDWARE> dp : this.deviceProviders) {
			if (dp.getProviderId().equals(clazz)) {
				return dp;
			}
		}
		return null;
	}
	
	private DeviceIdentifier getIdFor(String storedId)
	{
		if (this.deviceIdentifiers != null) {
			for(DeviceIdentifier si : this.deviceIdentifiers) {
				if (si.matchStorableId(storedId)) {
					return si;
				}
			}
		}
		
		String provider = DeviceIdentifier.Utils.getProviderId(storedId);
		DriverProvider<HARDWARE> dp = getProviderForClass(provider);
		if (dp == null) {
			return null;
		}
		
		return dp.buildIdFor(storedId);
	}


	private void actionChoose(DriverProvider<HARDWARE> dp)
	{
		if (device != null) return;
		if (chooser != null) return;
		if (!dp.canChooseScope()) return;
		
		chooser = new DeviceChoosedCallback() {
			
			@Override
			public void onDeviceChoosed(DeviceIdentifier si) {
				if (chooser != this) {
					return;
				}
				chooser = null;
				if (si == null) return;
				lastUsedDevice.set(si.getStorableId());
				refreshDeviceMenu();
				quickConnect();
			}
		};
		dp.chooseDevice(lastUsedDevice.get(), chooser);
	}
	
	// Connecte au dernier device, ou lance le chooser
	private void quickConnect() {
		if (device != null) return;
		
		String idOfLastUsedDevice = lastUsedDevice.get();
		if (idOfLastUsedDevice == null || "".equals(idOfLastUsedDevice)) {
			// Il faut lancer le chooser
			actionChoose(this.deviceProviders.get(0));
		} else {
			DeviceIdentifier dp = getIdFor(idOfLastUsedDevice);
			if (dp != null) {
				actionConnecter(dp);
			} else {
				actionChoose(this.deviceProviders.get(0));
			}
		}
	}

	abstract JMenuItem getMenuConnecter();

	abstract JMenuItem getMenuDeconnecter();
	
	abstract ToolbarButton getStatusButton();
	
	public void addActionListener()
	{
		if (getMenuDeconnecter() != null) {
			getMenuDeconnecter().addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					actionDeconnecter();
				}
			});
		}
		
		if (getMenuConnecter() != null) {
			focusUi.mntmConnecter.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					quickConnect();
				}
			});
		}
		
		if (getStatusButton() != null) {
			getStatusButton().addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (device == null) {
						quickConnect();
					} else {
						actionDeconnecter();
					}
				}
			});
			register(new StatusControler<ToolbarButton>(getStatusButton()) {
				String errorToDisplay;
				final Timer jtimer = new Timer(4000, new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						cancelErrorDisplay();
					}
					
				}) ;
				
				void cancelErrorDisplay()
				{
					errorToDisplay = null;
					update(getStatusButton());
				}
				
				@Override
				void reportError(Throwable t) {
					errorToDisplay = t.getMessage();
					jtimer.start();
					update(getStatusButton());
				}
				
				@Override
				void update(ToolbarButton o) {
					Status status;
					
					if (device == null) {
						if (deviceIdentifiers == null) {
							if (lister == null) {
								status = Status.ERROR;
							} else {
								status = Status.PENDING;
							}
						} else {
							status = Status.DEFAULT;
						}
					} else {
						if (device.isConnected()) {
							status = Status.OK;
						} else {
							status = Status.PENDING;
						}
					}
					
					if (errorToDisplay != null) {
						if (status == Status.OK) {
							errorToDisplay = null;
						} else if (status != Status.PENDING) {
							status = Status.ERROR;
						}
					}
					
					o.setStatus(status);
					
					o.setEnabled(chooser == null);
					
					String tt = "";
					if (device == null) {
						tt = labels.CONNECT_TOOLTIP;
					} else {
						tt = labels.DISCONNECT_TOOLTIP;
					}
					if (errorToDisplay != null) {
						tt = errorToDisplay;
					}
					o.setToolTipText(tt);
				};
				
				{ jtimer.setRepeats(false); }
			});
//			scopeButton.setPopupProvider(new ToolbarButton.PopupProvider() {
//				@Override
//				public JPopupMenu popup() {
//					JPopupMenu result = new JPopupMenu();
//					
//					JMenuItem changeStatus = new JMenuItem("switch");
//					result.add(changeStatus);
//					changeStatus.addActionListener(new ActionListener() {
//						
//						@Override
//						public void actionPerformed(ActionEvent e) {
//							if (scopeButton.getStatus() == AbstractIconButton.Status.ACTIVATED)
//								scopeButton.setStatus(AbstractIconButton.Status.DEFAULT);
//							else 
//								scopeButton.setStatus(AbstractIconButton.Status.ACTIVATED);
//						}
//					});
//					
//					return result;
//				}
//			});
			getStatusButton().setPopupProvider(new ToolbarButton.PopupProvider() {
				
				@Override
				public JPopupMenu popup() {
					JPopupMenu result = new JPopupMenu();
					
	
					String lastUsed = lastUsedDevice.get();
					DeviceIdentifier lastUsedId;
					if (deviceIdentifiers != null) {
						for(final DeviceIdentifier si : deviceIdentifiers) {
							JMenuItem activate = buildActivateDeviceIdMenuItem(lastUsed, si);
							result.add(activate);
						}
					} else if (lastUsed != null && !lastUsed.equals("") && (lastUsedId = getIdFor(lastUsed)) != null) {
						// Reconnection au dernier
						JMenuItem activate = buildActivateDeviceIdMenuItem(lastUsed, lastUsedId);
						result.add(activate);
					}
	
					// Accès au chooser
					for(DriverProvider<HARDWARE> dp : deviceProviders) {
						if (dp.canChooseScope()) {
							JMenuItem chooserMnu = new JMenuItem(labels.CHOOSE);
							result.add(chooserMnu);
							register(new StatusControler<JMenuItem>(chooserMnu) {
								@Override
								void update(JMenuItem o) {
									o.setEnabled(device == null && chooser == null);
								}
								
								@Override
								void reportError(Throwable t) {
								}
							});
							chooserMnu.addActionListener(new ActionListener() {
								@Override
								public void actionPerformed(ActionEvent e) {
									actionChoose(dp);
								};
							});
						}
					}
					
					if (controlerDialog != null) {
						JMenuItem controler = new JMenuItem(labels.CONTROLER);
						result.add(controler);
						controler.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								showDialog();
							}
						});
					}
					
					// Menu déconnecter
					JMenuItem disconnect = new JMenuItem(labels.DISCONNECT);
					result.add(disconnect);
					register(new StatusControler<JMenuItem>(disconnect) {
						@Override
						void update(JMenuItem o) {
							o.setEnabled(device != null && chooser == null);
						}
						
						@Override
						void reportError(Throwable t) {
						}
					});
					disconnect.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							actionDeconnecter();
						};
					});
					
					
					JMenuItem reloadPopup = new JMenuItem(labels.REFRESH_LIST);
					register(new StatusControler<JMenuItem>(reloadPopup) {
						@Override
						void update(JMenuItem jmi)
						{
							jmi.setEnabled(lister == null);
						}
						
						@Override
						void reportError(Throwable t) {
						}
					});
					
					return result;
				}
	
				private JMenuItem buildActivateDeviceIdMenuItem(String lastUsed,
						final DeviceIdentifier si) {
					JMenuItem activate = new JMenuItem(labels.CONNECT + si.getTitle());
					
					if (si.matchStorableId(lastUsed)) {
						activate.setFont(activate.getFont().deriveFont(Font.BOLD));
					}
					activate.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							actionConnecter(si);
						}
					});
					register(new StatusControler<JMenuItem>(activate) {
						@Override
						void update(JMenuItem o) {
							o.setEnabled(device == null && chooser == null);
						};
						
						@Override
						void reportError(Throwable t) {
						}
					});
					return activate;
				}
			});
		}
		
		refreshDeviceMenu();
	}
	
	public void refreshDeviceMenu()
	{
		if (getMenuConnecter() != null) {
			getMenuConnecter().setEnabled(device == null);
		}
		if (getMenuDeconnecter() != null) {
			getMenuDeconnecter().setEnabled(device != null && device.isConnected());
		}
		
		for(Iterator<StatusControler<?>> it = this.statusControlers.iterator(); it.hasNext(); )
		{
			updateOrDropStatusControler(it, it.next());
		}
	}
	
	void dispatchError(Throwable t) {
		for(Iterator<StatusControler<?>> it = this.statusControlers.iterator(); it.hasNext(); )
		{
			dispatchErrorOrDropStatusControler(it, it.next(), t);
		}
		
	}

	private <X> void dispatchErrorOrDropStatusControler(Iterator<StatusControler<?>> it, StatusControler<X> sc, Throwable thr)
	{
		X t = sc.reference.get();
		if (t == null) {
			it.remove();
		} else {
			sc.reportError(thr);
		}
	}
	
	private <X> void updateOrDropStatusControler(Iterator<StatusControler<?>> it, StatusControler<X> sc)
	{
		X t = sc.reference.get();
		if (t == null) {
			it.remove();
		} else {
			sc.update(t);
		}
	}

	public HARDWARE getDevice() {
		return device;
	}

	public HARDWARE getConnectedDevice() {
		if (device != null && !device.isConnected()) return null;
		return device;
	}


	private static abstract class StatusControler<WIDGET> {
		final WeakReference<WIDGET> reference;
		
		StatusControler(WIDGET w)
		{
			this.reference = new WeakReference<WIDGET>(w);
		}
		
		abstract void reportError(Throwable t);
		abstract void update(WIDGET o);
	}
	
	private final LinkedList<StatusControler<?>> statusControlers = new LinkedList<StatusControler<?>>();
	
	private <TYPE> void register(StatusControler<TYPE> sc) {
		TYPE t = sc.reference.get();
		if (t == null) return;
		statusControlers.add(sc);
		sc.update(t);
	}

	public DeviceIdentifier getCurrentDeviceIdentifier() {
		return currentDeviceIdentifier;
	}
	

}
