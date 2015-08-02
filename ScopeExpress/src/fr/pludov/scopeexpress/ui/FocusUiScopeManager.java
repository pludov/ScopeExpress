package fr.pludov.scopeexpress.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.focus.Mosaic;
import fr.pludov.scopeexpress.focus.MosaicImageParameter;
import fr.pludov.scopeexpress.focus.SkyProjection;
import fr.pludov.scopeexpress.scope.DeviceIdentifier;
import fr.pludov.scopeexpress.scope.Scope;
import fr.pludov.scopeexpress.scope.ScopeException;
import fr.pludov.scopeexpress.scope.ascom.AscomScopeProvider;
import fr.pludov.scopeexpress.scope.dummy.DummyScope;
import fr.pludov.scopeexpress.ui.widgets.ToolbarButton;
import fr.pludov.scopeexpress.utils.SkyAlgorithms;


public class FocusUiScopeManager extends DeviceManager<Scope>{

	static class Labels extends DeviceManager.Labels {
		Labels() {
			CHOOSE = "Choisir un téléscope...";
			DISCONNECT_TOOLTIP = "Déconnecter de la monture...";
			CONNECT_TOOLTIP = "Connecter à la monture...";
		}
	}
	
	public FocusUiScopeManager(FocusUi focusUi) {
		super(new AscomScopeProvider(), new Labels(), "scope", focusUi);
	}

	@Override
	JMenuItem getMenuConnecter()
	{
		return focusUi.mntmConnecter;
	}

	@Override
	JMenuItem getMenuDeconnecter()
	{
		return focusUi.mntmDconnecter;
	}
	
	@Override
	ToolbarButton getStatusButton()
	{
		return focusUi.scopeButton;
	}
	
	public void createFakeScope(double ra, double dec)
	{
		if (this.device != null) return;
		device = new DummyScope();
		currentDeviceIdentifier = new DeviceIdentifier() {

			@Override
			public String getTitle() {
				return "<dummy>";
			}

			@Override
			public String getStorableId() {
				return "<dummy>";
			}

			@Override
			public boolean matchStorableId(String storedId) {
				return getStorableId().equals(storedId);
			}
		};
		initDevice();
		try {
			device.sync(ra, dec);
		} catch (ScopeException e) {
			throw new RuntimeException("sync failed on dummy scope", e);
		}
	}

	public void addImageContextMenu(Mosaic mosaic, JPopupMenu contextMenu, List<MosaicImageListEntry> entries) {
		final double [] target;
		Scope scope = getConnectedDevice();
		if (scope == null || entries.size() != 1) {
			target = null;
		} else {
			MosaicImageParameter mip = entries.get(0).getTarget();
			Image img = mip.getImage();
			if (!mip.isCorrelated()) {
				target = null;
			} else {
				double [] mosaic3dPos = new double[3];
				
				double imgPos0 = 0.5;
				double imgPos1 = 0.5;
				
				mip.getProjection().image2dToSky3d(new double[]{0.5 * img.getWidth() * imgPos0, 0.5 * img.getHeight() * imgPos1}, mosaic3dPos);
				target = new double[2];
				SkyProjection.convert3DToRaDec(mosaic3dPos, target);
				
				// On veut des coordonnées Vraies, en H pour l'AD
				target[0] *= 24.0 / 360;
				double [] coordVraies = SkyAlgorithms.raDecNowFromJ2000(target[0], target[1], 0);
				target[0] = coordVraies[0];
				target[1] = coordVraies[1];
			}
		}
		
		
		
		JMenuItem scopeGotoMenu = new JMenuItem();
		scopeGotoMenu.setText("Téléscope: Goto");
		scopeGotoMenu.setEnabled(target != null);
		scopeGotoMenu.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				final Scope scopeTarget = getConnectedDevice();
				if (scopeTarget == null) return;
				
				new Thread ("scope goto") {
					@Override
					public void run() {
						try {
							if (target == null) return;
							scopeTarget.slew(target[0], target[1]);
						} catch(Throwable t) {
							t.printStackTrace();
						}
					};	
				}.start();
			}
		});
		
		contextMenu.add(scopeGotoMenu);
		
		JMenuItem scopeSyncMenu = new JMenuItem();
		scopeSyncMenu.setText("Téléscope: Sync");
		scopeSyncMenu.setEnabled(target != null);
		scopeSyncMenu.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				final Scope scopeTarget = getConnectedDevice();
				if (scopeTarget == null) return;
				
				new Thread ("scope sync") {
					@Override
					public void run() {
						try {
							if (target == null) return;
							scopeTarget.sync(target[0], target[1]);
						} catch(Throwable t) {
							t.printStackTrace();
						}
					};	
				}.start();
			}
		});
		
		contextMenu.add(scopeSyncMenu);
	}
	

}
//public class FocusUiScopeManager {
//	public static interface Listener {
//		void onScopeChanged();
//	}
//	
//	final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
//	public final WeakListenerCollection<Listener> listeners; 
//	final FocusUi focusUi;
//	final ScopeProvider scopeProvider;
//	Scope scope;
//	// Listes des id de téléscope dispo. Si null, non disponible.
//	List<? extends ScopeIdentifier> scopeIdentifiers;
//	private ScopeChoosedCallback chooser;
//	private ScopeListedCallback lister;
//	static StringConfigItem lastUsedScope = new StringConfigItem(FocusUiScopeManager.class, "lastUsedScope", "");
//	
//	public FocusUiScopeManager(FocusUi focusUi) {
//		this.focusUi = focusUi;
//		this.scopeProvider = new AscomScopeProvider();
//		this.scope = null;
//		this.listeners = new WeakListenerCollection<FocusUiScopeManager.Listener>(Listener.class);
//		
//		startScopeListing();
//	}
//	
//	private void startScopeListing()
//	{
//		if (lister != null) return;
//		
//		lister = new ScopeListedCallback() {
//			
//			@Override
//			public void onScopeListed(List<? extends ScopeIdentifier> availables) {
//				if (lister != this) return;
//				lister = null;
//				scopeIdentifiers = availables;
//				refreshScopeMenu();
//			}
//		};
//		
//		this.scopeProvider.listScopes(lister);
//	}
//
//	private void actionDeconnecter()
//	{
//		if (this.scope == null) return;
//		this.scope.close();
//		this.scope = null;
//		refreshScopeMenu();
//		this.listeners.getTarget().onScopeChanged();
//	}
//	
//	public void createFakeScope(double ra, double dec)
//	{
//		if (this.scope != null) return;
//		scope = new DummyScope();
//		initScope();
//		try {
//			scope.sync(ra, dec);
//		} catch (ScopeException e) {
//			throw new RuntimeException("sync failed on dummy scope", e);
//		}
//	}
//	
//	
//	private void actionConnecter(ScopeIdentifier si)
//	{
//		if (this.scope != null) return;
//		if (this.chooser != null) return;
//		lastUsedScope.set(si.getStorableId());
//		scope = scopeProvider.buildScope(si);
//		initScope();
//	}
//
//	private void initScope() {
//		scope.getListeners().addListener(this.listenerOwner,  new Scope.Listener() {
//			final Scope scopeThatChanged = scope;
//			
//			@Override
//			public void onCoordinateChanged() {
//				
//			}
//			
//			@Override
//			public void onConnectionStateChanged() {
//				if (scope != scopeThatChanged) return;
//
//				SwingUtilities.invokeLater(new Runnable() {
//					@Override
//					public void run() {
//						refreshScopeMenu();
//					}
//				});
//				
//				// Une déconnection, on le met à la poubelle
//				if (!scope.isConnected()) {
//					SwingUtilities.invokeLater(new Runnable() {
//						@Override
//						public void run() {
//							if (scopeThatChanged == scope && !scope.isConnected()) {
//								actionDeconnecter();
//							}
//						}
//					});
//					
//				}
//			}
//		});
//		scope.start();
//		refreshScopeMenu();
//		this.listeners.getTarget().onScopeChanged();
//	}
//	
//	private ScopeIdentifier getIdFor(String storedId)
//	{
//		if (this.scopeIdentifiers != null) {
//			for(ScopeIdentifier si : this.scopeIdentifiers) {
//				if (si.matchStorableId(storedId)) {
//					return si;
//				}
//			}
//		}
//		return scopeProvider.buildIdFor(storedId);
//	}
//
//
//	private void actionChoose()
//	{
//		if (scope != null) return;
//		if (chooser != null) return;
//		chooser = new ScopeChoosedCallback() {
//			
//			@Override
//			public void onScopeChoosed(ScopeIdentifier si) {
//				if (chooser != this) {
//					return;
//				}
//				chooser = null;
//				if (si == null) return;
//				lastUsedScope.set(si.getStorableId());
//				refreshScopeMenu();
//				quickConnect();
//			}
//		};
//		scopeProvider.chooseScope(lastUsedScope.get(), chooser);
//	}
//	
//	// Connecte au dernier scope, ou lance le chooser
//	private void quickConnect() {
//		if (scope != null) return;
//		
//		String idOfLastUsedScope = lastUsedScope.get();
//		if (idOfLastUsedScope == null || "".equals(idOfLastUsedScope)) {
//			// Il faut lancer le chooser
//			actionChoose();
//		} else {
//			actionConnecter(getIdFor(idOfLastUsedScope));
//		}
//	}
//
//	public void addActionListener()
//	{
//		focusUi.mntmDconnecter.addActionListener(new ActionListener() {
//			
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				actionDeconnecter();
//			}
//		});
//		
//		focusUi.mntmConnecter.addActionListener(new ActionListener() {
//			
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				quickConnect();
//			}
//		});
//	
//		focusUi.scopeButton.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				if (scope == null) {
//					quickConnect();
//				} else {
//					actionDeconnecter();
//				}
//			}
//		});
//		register(new StatusControler<ToolbarButton>(focusUi.scopeButton) {
//			void update(ToolbarButton o) {
//				Status status;
//				
//				if (scope == null) {
//					if (scopeIdentifiers == null) {
//						if (lister == null) {
//							status = Status.ERROR;
//						} else {
//							status = Status.PENDING;
//						}
//					} else {
//						status = Status.DEFAULT;
//					}
//				} else {
//					if (scope.isConnected()) {
//						status = Status.OK;
//					} else {
//						status = Status.PENDING;
//					}
//				}
//				
//				o.setStatus(status);
//				
//				o.setEnabled(chooser == null);
//				
//				String tt = "";
//				if (scope == null) {
//					tt = "Connecter à la monture...";
//				} else {
//					tt = "Déconnecter de la monture...";
//				}
//				o.setToolTipText(tt);
//			};
//		});
//		
//		focusUi.scopeButton.setPopupProvider(new ToolbarButton.PopupProvider() {
//			
//			@Override
//			public JPopupMenu popup() {
//				JPopupMenu result = new JPopupMenu();
//				
//
//				String lastUsed = lastUsedScope.get();
//				if (scopeIdentifiers != null) {
//					for(final ScopeIdentifier si : scopeIdentifiers) {
//						JMenuItem activate = buildActivateScopeIdMenuItem(lastUsed, si);
//						result.add(activate);
//					}
//				} else if (lastUsed != null && !lastUsed.equals("")) {
//					// Reconnection au dernier
//					JMenuItem activate = buildActivateScopeIdMenuItem(lastUsed, scopeProvider.buildIdFor(lastUsed));
//					result.add(activate);
//				}
//
//				// Accès au chooser
//				JMenuItem chooserMnu = new JMenuItem("Choisir un téléscope...");
//				result.add(chooserMnu);
//				register(new StatusControler<JMenuItem>(chooserMnu) {
//					@Override
//					void update(JMenuItem o) {
//						o.setEnabled(scope == null && chooser == null);
//					}
//				});
//				chooserMnu.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent e) {
//						actionChoose();
//					};
//				});
//				
//				// Menu déconnecter
//				JMenuItem disconnect = new JMenuItem("Déconnecter");
//				result.add(disconnect);
//				register(new StatusControler<JMenuItem>(disconnect) {
//					@Override
//					void update(JMenuItem o) {
//						o.setEnabled(scope != null && chooser == null);
//					}
//				});
//				disconnect.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent e) {
//						actionDeconnecter();
//					};
//				});
//				
//				
//				JMenuItem reloadPopup = new JMenuItem("Raffraichir la liste...");
//				register(new StatusControler<JMenuItem>(reloadPopup) {
//					void update(JMenuItem jmi)
//					{
//						jmi.setEnabled(lister == null);
//					}
//				});
//				
//				return result;
//			}
//
//			private JMenuItem buildActivateScopeIdMenuItem(String lastUsed,
//					final ScopeIdentifier si) {
//				JMenuItem activate = new JMenuItem("Connecter: " + si.getTitle());
//				
//				if (si.matchStorableId(lastUsed)) {
//					activate.setFont(activate.getFont().deriveFont(Font.BOLD));
//				}
//				activate.addActionListener(new ActionListener() {
//					@Override
//					public void actionPerformed(ActionEvent e) {
//						actionConnecter(si);
//					}
//				});
//				register(new StatusControler<JMenuItem>(activate) {
//					void update(JMenuItem o) {
//						o.setEnabled(scope == null && chooser == null);
//					};
//				});
//				return activate;
//			}
//		});
//		
//		refreshScopeMenu();
//	}
//	
//	public void refreshScopeMenu()
//	{
//		focusUi.mntmConnecter.setEnabled(scope == null);
//		focusUi.mntmDconnecter.setEnabled(scope != null && scope.isConnected());
////		focusUi.scopeButton.setEnabled(scope == null || scope.isConnected());
//		
////		Status status;
////		
////		if (scope == null) {
////			if (scopeIdentifiers == null) {
////				if (lister == null) {
////					status = Status.ERROR;
////				} else {
////					status = Status.PENDING;
////				}
////			} else {
////				status = Status.DEFAULT;
////			}
////		} else {
////			if (scope.isConnected()) {
////				status = Status.OK;
////			} else {
////				status = Status.PENDING;
////			}
////		}
////		
////		focusUi.scopeButton.setStatus(status);
//		
//		for(Iterator<StatusControler<?>> it = this.statusControlers.iterator(); it.hasNext(); )
//		{
//			updateOrDropStatusControler(it, it.next());
//		}
//	}
//
//	private <X> void updateOrDropStatusControler(Iterator<StatusControler<?>> it, StatusControler<X> sc)
//	{
//		X t = sc.reference.get();
//		if (t == null) {
//			it.remove();
//		} else {
//			sc.update(t);
//		}
//	}
//
//	public Scope getScope() {
//		return scope;
//	}
//
//	public Scope getConnectedScope() {
//		if (scope != null && !scope.isConnected()) return null;
//		return scope;
//	}
//
//	public void addImageContextMenu(Mosaic mosaic, JPopupMenu contextMenu, List<MosaicImageListEntry> entries) {
//		final double [] target;
//		Scope scope = getConnectedScope();
//		if (scope == null || entries.size() != 1) {
//			target = null;
//		} else {
//			MosaicImageParameter mip = entries.get(0).getTarget();
//			Image img = mip.getImage();
//			if (!mip.isCorrelated()) {
//				target = null;
//			} else {
//				double [] mosaic3dPos = new double[3];
//				
//				double imgPos0 = 0.5;
//				double imgPos1 = 0.5;
//				
//				mip.getProjection().image2dToSky3d(new double[]{0.5 * img.getWidth() * imgPos0, 0.5 * img.getHeight() * imgPos1}, mosaic3dPos);
//				target = new double[2];
//				SkyProjection.convert3DToRaDec(mosaic3dPos, target);
//				
//				// On veut des coordonnées Vraies, en H pour l'AD
//				target[0] *= 24.0 / 360;
//				double [] coordVraies = SkyAlgorithms.raDecNowFromJ2000(target[0], target[1], 0);
//				target[0] = coordVraies[0];
//				target[1] = coordVraies[1];
//			}
//		}
//		
//		
//		
//		JMenuItem scopeGotoMenu = new JMenuItem();
//		scopeGotoMenu.setText("Téléscope: Goto");
//		scopeGotoMenu.setEnabled(target != null);
//		scopeGotoMenu.addActionListener(new ActionListener() {
//			
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				final Scope scopeTarget = getConnectedScope();
//				if (scopeTarget == null) return;
//				
//				new Thread ("scope goto") {
//					public void run() {
//						try {
//							if (target == null) return;
//							scopeTarget.slew(target[0], target[1]);
//						} catch(Throwable t) {
//							t.printStackTrace();
//						}
//					};	
//				}.start();
//			}
//		});
//		
//		contextMenu.add(scopeGotoMenu);
//		
//		JMenuItem scopeSyncMenu = new JMenuItem();
//		scopeSyncMenu.setText("Téléscope: Sync");
//		scopeSyncMenu.setEnabled(target != null);
//		scopeSyncMenu.addActionListener(new ActionListener() {
//			
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				final Scope scopeTarget = getConnectedScope();
//				if (scopeTarget == null) return;
//				
//				new Thread ("scope sync") {
//					public void run() {
//						try {
//							if (target == null) return;
//							scopeTarget.sync(target[0], target[1]);
//						} catch(Throwable t) {
//							t.printStackTrace();
//						}
//					};	
//				}.start();
//			}
//		});
//		
//		contextMenu.add(scopeSyncMenu);
//	}
//
//
//	private abstract class StatusControler<WIDGET> {
//		final WeakReference<WIDGET> reference;
//		
//		StatusControler(WIDGET w)
//		{
//			this.reference = new WeakReference<WIDGET>(w);
//		}
//		
//		
//		abstract void update(WIDGET o);
//	}
//	
//	private final LinkedList<StatusControler<?>> statusControlers = new LinkedList<StatusControler<?>>();
//	
//	private <TYPE> void register(StatusControler<TYPE> sc) {
//		TYPE t = sc.reference.get();
//		if (t == null) return;
//		statusControlers.add(sc);
//		sc.update(t);
//	}
//	
//}
