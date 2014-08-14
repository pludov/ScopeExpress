package fr.pludov.cadrage.ui.focus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.Mosaic;
import fr.pludov.cadrage.focus.MosaicImageParameter;
import fr.pludov.cadrage.focus.SkyProjection;
import fr.pludov.cadrage.scope.Scope;
import fr.pludov.cadrage.scope.ScopeException;
import fr.pludov.cadrage.scope.ascom.AscomScope;
import fr.pludov.cadrage.scope.dummy.DummyScope;
import fr.pludov.cadrage.utils.SkyAlgorithms;
import fr.pludov.cadrage.utils.WeakListenerCollection;
import fr.pludov.cadrage.utils.WeakListenerOwner;

public class FocusUiScopeManager {
	
	public static interface Listener {
		void onScopeChanged();
	}
	
	final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	public final WeakListenerCollection<Listener> listeners; 
	final FocusUi focusUi;
	Scope scope;
	
	public FocusUiScopeManager(FocusUi focusUi) {
		this.focusUi = focusUi;
		this.scope = null;
		this.listeners = new WeakListenerCollection<FocusUiScopeManager.Listener>(Listener.class);
	}

	private void actionDeconnecter()
	{
		if (this.scope == null) return;
		this.scope.close();
		this.scope = null;
		this.listeners.getTarget().onScopeChanged();
	}
	
	public void createFakeScope(double ra, double dec)
	{
		if (this.scope != null) return;
		scope = new DummyScope();
		initScope();
		try {
			scope.sync(ra, dec);
		} catch (ScopeException e) {
			throw new RuntimeException("sync failed on dummy scope", e);
		}
	}
	
	
	private void actionConnecter() 
	{
		if (this.scope != null) return;
		scope = new AscomScope();
		initScope();
	}

	private void initScope() {
		scope.getListeners().addListener(this.listenerOwner,  new Scope.Listener() {
			final Scope scopeThatChanged = scope;
			
			@Override
			public void onCoordinateChanged() {
				
			}
			
			@Override
			public void onConnectionStateChanged() {
				if (scope != scopeThatChanged) return;

				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						refreshScopeMenu();
					}
				});
				
				// Une déconnection, on le met à la poubelle
				if (!scope.isConnected()) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							if (scopeThatChanged == scope && !scope.isConnected()) {
								actionDeconnecter();
							}
						}
					});
					
				}
			}
		});
		scope.start();
		refreshScopeMenu();
		this.listeners.getTarget().onScopeChanged();
	}
	
	public void addActionListener()
	{
		focusUi.mntmDconnecter.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				actionDeconnecter();
			}
		});
		
		focusUi.mntmConnecter.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				actionConnecter();
			}
		});
	
		refreshScopeMenu();
	}
	
	public void refreshScopeMenu()
	{
		focusUi.mntmConnecter.setEnabled(scope == null);
		focusUi.mntmDconnecter.setEnabled(scope != null && scope.isConnected());
		focusUi.mntmLoadStarAroundScope.setEnabled(scope != null && scope.isConnected());
	}

	public Scope getScope() {
		return scope;
	}

	public Scope getConnectedScope() {
		if (scope != null && !scope.isConnected()) return null;
		return scope;
	}

	public void addImageContextMenu(Mosaic mosaic, JPopupMenu contextMenu, List<MosaicImageListEntry> entries) {
		final double [] target;
		Scope scope = getConnectedScope();
		if (scope == null || entries.size() != 1) {
			target = null;
		} else {
			Image img = entries.get(0).getTarget();
			MosaicImageParameter mip = mosaic.getMosaicImageParameter(img);
			if (mip == null || !mip.isCorrelated()) {
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
				final Scope scopeTarget = getConnectedScope();
				if (scopeTarget == null) return;
				
				new Thread ("scope goto") {
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
				final Scope scopeTarget = getConnectedScope();
				if (scopeTarget == null) return;
				
				new Thread ("scope sync") {
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
