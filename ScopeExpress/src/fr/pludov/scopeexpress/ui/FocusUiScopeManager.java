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
