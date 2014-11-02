package fr.pludov.scopeexpress.ui;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JOptionPane;

import fr.pludov.scopeexpress.focus.ExclusionZone;
import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.focus.Mosaic;
import fr.pludov.scopeexpress.focus.MosaicImageParameter;
import fr.pludov.scopeexpress.focus.MosaicImageParameterListener;
import fr.pludov.scopeexpress.focus.MosaicListener;
import fr.pludov.scopeexpress.focus.PointOfInterest;
import fr.pludov.scopeexpress.focus.SkyProjection;
import fr.pludov.scopeexpress.focus.Star;
import fr.pludov.scopeexpress.focus.StarOccurence;
import fr.pludov.scopeexpress.scope.Scope;
import fr.pludov.scopeexpress.scope.ScopeException;
import fr.pludov.scopeexpress.ui.joystick.ButtonAction;
import fr.pludov.scopeexpress.ui.joystick.JoystickListener;
import fr.pludov.scopeexpress.ui.preferences.BooleanConfigItem;
import fr.pludov.scopeexpress.ui.speech.SpeakUtil;
import fr.pludov.scopeexpress.ui.speech.Speaker;
import fr.pludov.scopeexpress.ui.speech.SpeakerProvider;
import fr.pludov.scopeexpress.ui.utils.Utils;
import fr.pludov.scopeexpress.utils.EndUserException;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;
import fr.pludov.utils.VecUtils;

public class ReCenterDialog extends ReCenterDialogDesign {
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	
	private static final BooleanConfigItem speechEnabledCfg = new BooleanConfigItem(ReCenterDialog.class, "speechEnabled", false);
	
	Mosaic mosaic;
	FocusUi focus;	
	boolean speechEnabled;
	
	// Différences en degrés
	Double diffRa, diffDec, diffAngle;
	
	
	MosaicImageParameter reference;
	MosaicImageParameter current;
	boolean currentSpeaked;
	boolean currentDidGoto;

	private class ReCenterJoystickListener implements JoystickListener
	{
		@Override
		public void triggered() {
			String say = doGoto() ? "goto" : "Erreur: pas prêt";
			// FIXME : ça devrait être géré par le joystick manager 
			try {
				SpeakerProvider.getSpeaker().enqueue(say);
			} catch (EndUserException e) {
				e.report(ReCenterDialog.this);
			}
		}

		@Override
		public boolean isActive() {
			return ReCenterDialog.this.isShowing() && ReCenterDialog.this.getBtnCenter().isEnabled();
		}
	}
	
	public ReCenterDialog(Window window)
	{
		super(window);
		this.speechEnabled = speechEnabledCfg.get();
		
		this.getCancelButton().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				closeRequest();
			}
		});

		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowListener() {
			
			@Override
			public void windowOpened(WindowEvent e) {
			}
			
			@Override
			public void windowIconified(WindowEvent e) {
			}
			
			@Override
			public void windowDeiconified(WindowEvent e) {
			}
			
			@Override
			public void windowDeactivated(WindowEvent e) {
			}
			
			@Override
			public void windowClosing(WindowEvent e) {
				closeRequest();
			}
			
			@Override
			public void windowClosed(WindowEvent e) {
			}
			
			@Override
			public void windowActivated(WindowEvent e) {
			}
		});
		
		this.getTglbtnSpeak().addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				speechEnabled = getTglbtnSpeak().isSelected();
				speechEnabledCfg.set(speechEnabled);
				if (speechEnabled) {
					try {
						safeGetSpeaker();
					} catch(EndUserException ex) {
						ex.report(ReCenterDialog.this);
					}
				}
				speachLastImage();
			}
		});
		
		this.getBtnCenter().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doGoto();
			}
		});
		
		
		updateSpeechBtons();
		this.update();
	}

	private void closeRequest()
	{
		if (this.current != null && this.reference != null) {
			int option = JOptionPane.showConfirmDialog(this, 
					"Fermer la session de cadrage ?",
					"Confirmer la fermeture...",
					JOptionPane.OK_CANCEL_OPTION);
			if (option != JOptionPane.OK_OPTION) return;
		}
		
		setVisible(false);
		unregister();
	}
	
	private void updateSpeechBtons() {
		this.getTglbtnSpeak().setSelected(this.speechEnabled);
	}
	
	/**
	 * Retourne un objet speaker si il a pu être initialisé, et sinon, lève une exception.
	 * Au passage, les boutons sont grisés.
	 */
	private Speaker safeGetSpeaker() throws EndUserException
	{
		Speaker speaker;
		try {
			speaker = SpeakerProvider.getSpeaker();
			return speaker;
		} catch (EndUserException e) {
			this.speechEnabled = false;
			updateSpeechBtons();
			throw e;
		}
	}

	private Scope getScope()
	{
		if (focus == null) return null;
		return focus.scopeManager.getScope();
		
	}
	
	private boolean doGoto()
	{
		if (this.diffRa == null) return false;
		if (this.diffDec == null) return false;
		if (currentDidGoto) return false;

		Scope scope = getScope();
		if (scope == null) return false;
		currentDidGoto = true;
		
		double curRa = scope.getRightAscension();
		double curDec = scope.getDeclination();
		try {
			scope.slew(curRa + this.diffRa * 24 / 360, curDec + this.diffDec);
		} catch (ScopeException e) {
			e.printStackTrace();
		}
		
		update();
		
		return true;
		
	}
	
	private void speachLastImage()
	{
		if (!this.speechEnabled) {
			return;
		}
		
		String blahblah = null;
		// On veut indiquer la correction
		if ((!this.currentSpeaked) && this.diffAngle != null)
		{
			this.currentSpeaked = true;
			blahblah = "Tourner de " + SpeakUtil.sign(this.diffAngle) + " " + SpeakUtil.sayPositiveAngle(Math.abs(this.diffAngle));
		}
		
		if (blahblah != null) {
			try {
				safeGetSpeaker().enqueue(blahblah);
			} catch (EndUserException e) {
				e.report(this);
			}
		}
	}
	
	private static class PointProjection
	{
		double [] image2d;
		double [] sky3dj2000s;
		double [] skyRaDecJ2000s;
		
		
		PointProjection(double image2dX, double image2dY)
		{
			this.image2d = new double[]{image2dX, image2dY};
		}
		
		void computeSky(SkyProjection sk)
		{
			sky3dj2000s = new double[3];
			sk.image2dToSky3d(this.image2d, sky3dj2000s);
			skyRaDecJ2000s = new double[2];
			SkyProjection.convert3DToRaDec(sky3dj2000s, skyRaDecJ2000s);	
		}
	}
	
	/**
	 * Recalcule référence - current
	 */
	private void update()
	{
		this.getLblRefImage().setText(reference == null ? "N/A" : reference.getImage().getPath().getName());
		this.getLblCurImage().setText(current == null ? "N/A" : current.getImage().getPath().getName());
		
		// Met à jour des labels
		if (reference == null || !reference.isCorrelated()
				|| current == null || !current.isCorrelated()) {
			diffAngle = null;
			diffRa = null;
			diffDec = null;
		} else {
			PointProjection refImgCenter = new PointProjection(
					reference.getImage().getWidth() / 2.0,
					reference.getImage().getHeight() / 2.0);
			refImgCenter.computeSky(reference.getProjection());
		
			PointProjection refImgTop = new PointProjection(
					reference.getImage().getWidth() / 2.0,
					reference.getImage().getHeight());
			refImgTop.computeSky(reference.getProjection());
			
			PointProjection curImgCenter = new PointProjection(
					current.getImage().getWidth() / 2.0,
					current.getImage().getHeight() / 2.0);
			curImgCenter.computeSky(current.getProjection());
			
			PointProjection curImgTop = new PointProjection(
					current.getImage().getWidth() / 2.0,
					current.getImage().getHeight());
			curImgTop.computeSky(current.getProjection());
			
			// Maintenant on calcule les diff RA/DEC
			diffRa = refImgCenter.skyRaDecJ2000s[0] - curImgCenter.skyRaDecJ2000s[0];
			diffDec = refImgCenter.skyRaDecJ2000s[1] - curImgCenter.skyRaDecJ2000s[1];
			
			// On calcule la position du vecteur "TOP" après correction exact en RA/DEC
			double [] curImgCorrectedTopRaDec = new double[2];
			curImgCorrectedTopRaDec[0] = curImgTop.skyRaDecJ2000s[0] + diffRa;
			curImgCorrectedTopRaDec[1] = curImgTop.skyRaDecJ2000s[1] + diffDec;
			
			double [] curImgCorrectedTopSky3D = new double[3];
			SkyProjection.convertRaDecTo3D(curImgCorrectedTopRaDec, curImgCorrectedTopSky3D);
			
			double [] curImgCorrectionTopRefProjected = new double[2];
			if (reference.getProjection().sky3dToImage2d(curImgCorrectedTopSky3D, curImgCorrectionTopRefProjected))
			{
				// ref = (0, 1)
				double [] ref = VecUtils.sub(refImgTop.image2d, refImgCenter.image2d);
				double [] cur = VecUtils.sub(curImgCorrectionTopRefProjected, refImgCenter.image2d);

				// MAintenant, on calcule un angle (sachant que ref vaut forcement 0,1)...
				diffAngle = 180 * Math.atan2(-cur[0], cur[1]) / Math.PI; 
			} else {
				// Pas projettable... ça ne devrait pas arriver, sauf en cas de champs trés grand (ex: + 90°)
				diffAngle = null;
			}
		}
		
		if (this.diffRa != null) {
			this.getLblDiffRa().setText(Utils.formatDegMinSec(Utils.adjustDegDiff(this.diffRa)));
		} else {
			this.getLblDiffRa().setText("N/A");
		}
		if (this.diffDec != null) {
			this.getLblDiffDec().setText(Utils.formatDegMinSec(Utils.adjustDegDiff(this.diffDec)));
		} else {
			this.getLblDiffDec().setText("N/A");
		}
		if (this.diffAngle != null) {
			this.getLblDiffRotation().setText(Utils.formatDegMinSec(Utils.adjustDegDiff(this.diffAngle)));
		} else {
			this.getLblDiffRotation().setText("N/A");
		}
		boolean centerEnabled = this.diffRa != null && this.diffDec != null && (!currentDidGoto) && getScope() != null;
		this.getBtnCenter().setEnabled(centerEnabled);
	}
	
	private MosaicImageParameter registerImage(Image img)
	{

		MosaicImageParameter mip = mosaic.getMosaicImageParameter(img);
		if (mip != null) {
			mip.listeners.addListener(this.listenerOwner, new MosaicImageParameterListener() {
				
				@Override
				public void correlationStatusUpdated() {
					update();
					speachLastImage();
				}
				
				@Override
				public void onFocalChanged() {
				}
				
				@Override
				public void onPixelSizeChanged() {
				}
			});
		}
		return mip;
	}

	private void unregisterImage(MosaicImageParameter img)
	{
		img.listeners.removeListener(this.listenerOwner);
	}

	private void unregister()
	{
		if (focus != null) {
			focus.getJoystickHandler().getListeners(ButtonAction.Recenter).removeListener(this.listenerOwner);
			focus.scopeManager.listeners.removeListener(this.listenerOwner);
			if (current != null) {
				unregisterImage(current);
			}
			if (reference != null && reference != current) {
				unregisterImage(reference);
			}
			
			if (this.mosaic != null) {
				this.mosaic.listeners.removeListener(listenerOwner);
			}
			this.mosaic = null;
			this.focus = null;
		}
		update();
	}
	
	private void register(FocusUi focusUi, Mosaic m, Image start)
	{
		this.focus = focusUi;
		this.mosaic = m;
		this.reference = registerImage(start);
		
		focusUi.getJoystickHandler().getListeners(ButtonAction.Recenter).addListener(this.listenerOwner, new ReCenterJoystickListener());
		
		focusUi.scopeManager.listeners.addListener(this.listenerOwner, new FocusUiScopeManager.Listener() {
			@Override
			public void onScopeChanged() {
				update();
			}
		});
		
		this.getLblRefImage().setText(start.getPath().getName());
		
		m.listeners.addListener(listenerOwner, new MosaicListener() {
			
			@Override
			public void starRemoved(Star star) {
			}
			
			@Override
			public void starOccurenceRemoved(StarOccurence sco) {
			}
			
			@Override
			public void starOccurenceAdded(StarOccurence sco) {
			}
			
			@Override
			public void starAdded(Star star) {
			}
			
			@Override
			public void pointOfInterestRemoved(PointOfInterest poi) {
			}
			
			@Override
			public void pointOfInterestAdded(PointOfInterest poi) {
			}
			
			@Override
			public void imageRemoved(Image image, MosaicImageParameter mip) {
				boolean isCurrent = current != null && current.getImage() == image;
				boolean isReference = reference != null && reference.getImage() == image;
				if (isCurrent || isReference)
				{
					if (isCurrent) {
						unregisterImage(current);
					} else {
						unregisterImage(reference);
					}
					if (isCurrent) {
						current = null;
					}
					if (isReference) {
						reference = null;
					}

					update();
					speachLastImage();
				}
			}
			
			@Override
			public void imageAdded(Image image, ImageAddedCause cause) {
				boolean isReference = reference != null && reference.getImage() == image;
				if (current != null && current.getImage() != image && !isReference) {
					// On désenregistre current
					unregisterImage(current);
					current = null;
				}
				
				current = registerImage(image);
				currentSpeaked = false;
				currentDidGoto = false;
				
				update();
				speachLastImage();
			}
			
			@Override
			public void exclusionZoneRemoved(ExclusionZone ze) {
			}
			
			@Override
			public void exclusionZoneAdded(ExclusionZone ze) {
			}
		});
		
		update();
		speachLastImage();
	}

	
	public void open(FocusUi focusUi, Mosaic m, Image start)
	{
		unregister();
		register(focusUi, m, start);
	}
	
}
