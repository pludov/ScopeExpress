package fr.pludov.cadrage.ui.focus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import fr.pludov.cadrage.Cadrage;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.Mosaic;
import fr.pludov.cadrage.focus.MosaicImageParameter;
import fr.pludov.cadrage.scope.Scope;
import fr.pludov.cadrage.scope.Scope.ConnectionStateChangedListener;
import fr.pludov.cadrage.scope.ascom.AscomScope;
import fr.pludov.cadrage.ui.utils.Utils;
import fr.pludov.cadrage.utils.SkyAlgorithms;

public class FocusUiScopeManager {
	final FocusUi focusUi;
	Scope scope;
	
	public FocusUiScopeManager(FocusUi focusUi) {
		this.focusUi = focusUi;
		this.scope = null;
	}

	private void actionDeconnecter()
	{
		if (this.scope == null) return;
		this.scope.close();
	}
	

	private void actionConnecter() 
	{
		if (this.scope != null) return;
		scope = new AscomScope();
		scope.addConnectionStateChangedListener(new ConnectionStateChangedListener() {
			
			@Override
			public void onConnectionStateChanged(final Scope scopeThatChanged) {
				if (scope != scopeThatChanged) return;
				
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						refreshScopeMenu();
					}
				});
				
				// Une déconnection, on le met à la poubelle
				if (!scopeThatChanged.isConnected()) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							if (scopeThatChanged == scope) {
								scope = null;
							}
						}
					});
					
				}
			}
		});
		scope.start();
		refreshScopeMenu();
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
				double [] mosaicPos = new double[2];
				
				double imgPos0 = 0.5;
				double imgPos1 = 0.5;
				
				mip.imageToMosaic(0.5 * img.getWidth() * imgPos0, 0.5 * img.getHeight() * imgPos1, mosaicPos);
				mosaic.getSkyProjection().unproject(mosaicPos);
				
				target = mosaicPos;
				
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
