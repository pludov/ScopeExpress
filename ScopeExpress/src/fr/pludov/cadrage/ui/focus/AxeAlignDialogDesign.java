package fr.pludov.cadrage.ui.focus;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.CardLayout;
import java.awt.GridLayout;
import javax.swing.JLabel;
import net.miginfocom.swing.MigLayout;
import javax.swing.JToolBar;
import javax.swing.JToggleButton;
import javax.swing.ImageIcon;

public class AxeAlignDialogDesign extends JDialog {

	private final JPanel contentPanel = new JPanel();
	protected JPanel poleViewPanel;
	protected JPanel panel;
	protected JLabel lblEcartAzimuth;
	protected JLabel lblEcartAltitude;
	protected JLabel lblAzimuth;
	protected JLabel lblAltitude;
	protected JToolBar toolBar;
	protected JToggleButton tglbtnSpeak;
	protected JToggleButton tglbtnAlt;
	protected JToggleButton tglbtnAz;

	/**
	 * Create the dialog.
	 */
	public AxeAlignDialogDesign(Window parent) {
		super(parent);
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		this.contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(this.contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		
		this.poleViewPanel = new JPanel();
		contentPanel.add(this.poleViewPanel, BorderLayout.CENTER);
		this.poleViewPanel.setLayout(new GridLayout(1, 0, 0, 0));
		
		this.panel = new JPanel();
		contentPanel.add(this.panel, BorderLayout.SOUTH);
		this.panel.setLayout(new MigLayout("", "[72px][]", "[14px][]"));
		
		this.lblEcartAzimuth = new JLabel("Ecart azimuth : ");
		this.lblEcartAzimuth.setToolTipText("Si c'est positif, on est trop \u00E0 l'est. Si c'est n\u00E9gatif, on est trop \u00E0 l'ouest");
		this.panel.add(this.lblEcartAzimuth, "cell 0 0,alignx left,aligny top");
		
		this.lblAzimuth = new JLabel("");
		this.panel.add(this.lblAzimuth, "cell 1 0,alignx left");
		
		this.lblEcartAltitude = new JLabel("Ecart altitude :");
		this.lblEcartAltitude.setToolTipText("Si c'est positif, on est trop haut.");
		this.panel.add(this.lblEcartAltitude, "cell 0 1");
		
		this.lblAltitude = new JLabel("");
		this.panel.add(this.lblAltitude, "cell 1 1");
		
		this.toolBar = new JToolBar();
		contentPanel.add(this.toolBar, BorderLayout.NORTH);
		
		this.tglbtnSpeak = new JToggleButton("");
		this.tglbtnSpeak.setToolTipText("Indiquer les corrections \u00E0 apporter en utilisant la synth\u00E8se vocale");
		this.tglbtnSpeak.setIcon(new ImageIcon(AxeAlignDialogDesign.class.getResource("/fr/pludov/cadrage/ui/resources/icons/text-speak.png")));
		this.toolBar.add(this.tglbtnSpeak);
		
		this.tglbtnAlt = new JToggleButton("");
		this.tglbtnAlt.setIcon(new ImageIcon(AxeAlignDialogDesign.class.getResource("/fr/pludov/cadrage/ui/resources/icons/table-border-vertical.png")));
		this.tglbtnAlt.setToolTipText("Donner les indications pour la verticale");
		this.toolBar.add(this.tglbtnAlt);
		
		this.tglbtnAz = new JToggleButton("");
		this.tglbtnAz.setToolTipText("Donner les indications pour l'azimuth (horizon)");
		this.tglbtnAz.setIcon(new ImageIcon(AxeAlignDialogDesign.class.getResource("/fr/pludov/cadrage/ui/resources/icons/table-border-horizontal.png")));
		this.toolBar.add(this.tglbtnAz);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton cancelButton = new JButton("Close");
				cancelButton.setActionCommand("Close");
				buttonPane.add(cancelButton);
			}
		}
	}

}
