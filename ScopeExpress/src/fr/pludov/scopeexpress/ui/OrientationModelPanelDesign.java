package fr.pludov.scopeexpress.ui;

import java.awt.*;

import javax.swing.*;

import fr.pludov.scopeexpress.OrientationModel.*;
import net.miginfocom.swing.*;

public class OrientationModelPanelDesign extends JPanel {
	protected JLabel lblAngleNord;
	protected JLabel lblNewLabel;
	protected JLabel lblEchantillonageHorsBin;
	protected JLabel lblTailleDesPixels;
	protected JLabel lblRaTitle;
	protected JLabel lblDecTitle;
	protected JLabel lblAngle;
	protected JLabel lblFocale;
	protected JComboBox comboFocale;
	protected JLabel lblEch;
	protected JComboBox comboEch;
	protected JLabel lblPixsize;
	protected JComboBox comboPixSize;
	protected JLabel lblShiftRaTitle;
	protected JLabel lblShiftDecTitle;
	protected JComboBox comboAngle;
	protected JComboBox comboDelta;
	protected JLabel lblRa;
	protected JLabel lblDec;
	protected JLabel lblShiftRa;
	protected JLabel lblShiftDec;

	/**
	 * Create the panel.
	 */
	public OrientationModelPanelDesign() {
		setLayout(new MigLayout("", "[right][grow][grow,fill]", "[][][][][][][][]"));
		
		this.lblRaTitle = new JLabel("RA:");
		add(this.lblRaTitle, "flowx,cell 0 0");
		
		this.lblRa = new JLabel("Ra");
		this.lblRa.setFont(new Font("Monospaced", Font.PLAIN, 11));
		add(this.lblRa, "cell 1 0 2 1");
		
		this.lblDecTitle = new JLabel("DEC:");
		add(this.lblDecTitle, "cell 0 1");
		
		this.lblDec = new JLabel("Dec");
		this.lblDec.setFont(new Font("Monospaced", Font.PLAIN, 11));
		add(this.lblDec, "cell 1 1 2 1");
		
		this.lblAngleNord = new JLabel("Angle nord:");
		this.lblAngleNord.setToolTipText("Direction en haut de l'image. 0 indique le nord, 90 l'ouest, ...");
		add(this.lblAngleNord, "cell 0 2");
		
		this.lblAngle = new JLabel("lblAngle");
		this.lblAngle.setFont(new Font("Monospaced", Font.PLAIN, 11));
		add(this.lblAngle, "cell 1 2");
		
		this.comboAngle = new JComboBox();
		this.comboAngle.setModel(new DefaultComboBoxModel(Field.values()));
		add(this.comboAngle, "cell 2 2");
		
		this.lblNewLabel = new JLabel("Focale:");
		add(this.lblNewLabel, "cell 0 3");
		
		this.lblFocale = new JLabel("lblFocale");
		this.lblFocale.setFont(new Font("Monospaced", Font.PLAIN, 11));
		add(this.lblFocale, "flowx,cell 1 3");
		
		this.comboFocale = new JComboBox();
		this.comboFocale.setModel(new DefaultComboBoxModel(Origin.values()));
		add(this.comboFocale, "cell 2 3");
		
		this.lblEchantillonageHorsBin = new JLabel("Echantillonage hors bin");
		this.lblEchantillonageHorsBin.setToolTipText("Echantillonage d'un pixel (hors bin \u00E9ventuel)");
		add(this.lblEchantillonageHorsBin, "cell 0 4");
		
		this.lblEch = new JLabel("lblEch");
		this.lblEch.setFont(new Font("Monospaced", Font.PLAIN, 11));
		add(this.lblEch, "cell 1 4");
		
		this.comboEch = new JComboBox();
		this.comboEch.setModel(new DefaultComboBoxModel(Origin.values()));
		add(this.comboEch, "cell 2 4");
		
		this.lblTailleDesPixels = new JLabel("Taille des pixels");
		this.lblTailleDesPixels.setToolTipText("Taille des pixels en \u03BCm (issu du driver camera si possible)");
		add(this.lblTailleDesPixels, "cell 0 5");
		
		this.lblPixsize = new JLabel("lblPixSize");
		this.lblPixsize.setFont(new Font("Monospaced", Font.PLAIN, 11));
		add(this.lblPixsize, "cell 1 5");
		
		this.comboPixSize = new JComboBox();
		this.comboPixSize.setModel(new DefaultComboBoxModel(Origin.values()));
		add(this.comboPixSize, "cell 2 5");
		
		this.lblShiftRaTitle = new JLabel("\u0394 RA:");
		add(this.lblShiftRaTitle, "cell 0 6");
		
		this.lblShiftRa = new JLabel("DeltaRa");
		this.lblShiftRa.setFont(new Font("Monospaced", Font.PLAIN, 11));
		add(this.lblShiftRa, "cell 1 6");
		
		this.comboDelta = new JComboBox();
		add(this.comboDelta, "cell 2 6");
		
		this.lblShiftDecTitle = new JLabel("\u0394 DEC:");
		add(this.lblShiftDecTitle, "cell 0 7");
		
		this.lblShiftDec = new JLabel("DeltaDec");
		this.lblShiftDec.setFont(new Font("Monospaced", Font.PLAIN, 11));
		add(this.lblShiftDec, "cell 1 7");

	}

}
