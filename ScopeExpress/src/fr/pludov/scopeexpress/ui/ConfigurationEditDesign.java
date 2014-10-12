package fr.pludov.scopeexpress.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JTabbedPane;
import net.miginfocom.swing.MigLayout;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import java.awt.Font;
import java.awt.Color;
import javax.swing.JTextField;
import java.awt.Insets;

public class ConfigurationEditDesign extends JDialog {

	private final JPanel contentPanel = new JPanel();
	protected JTabbedPane tabbedPane;
	protected JPanel geoPanel;
	protected JLabel lblLatitude;
	protected JLabel lblLongitude;
	protected JTextField fieldLatitude;
	protected JTextField fieldLongitude;
	protected JLabel fieldLatitudeErr;
	protected JLabel fieldLongitudeErr;
	private JButton okButton;
	private JButton cancelButton;
	protected JPanel cameraPanel;
	protected JLabel lblPixSize;
	protected JTextField fieldPixSize;
	protected JLabel fieldPixSizeErr;
	protected JLabel lblFocal;
	protected JTextField fieldFocal;
	protected JLabel fieldFocalErr;
	protected JPanel filePanel;
	protected JLabel lblRpertoireTycho;
	protected JTextField starCatalogPathTyc2Field;
	protected JButton starCatalogPathTyc2Browse;
	protected JButton starCatalogPathTyc2Download;
	protected JPanel joystickPanel;
	protected JLabel lblRepertoiresAstroNet;
	protected JTextField astroNetPathField;
	protected JButton astroNetBrowse;
	protected JButton astroNetDownload;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			ConfigurationEditDesign dialog = new ConfigurationEditDesign(null);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 * @param parent 
	 */
	public ConfigurationEditDesign(Window parent) {
		super(parent);
		
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		this.contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(this.contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));
		
		this.tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		contentPanel.add(this.tabbedPane);
		
		this.cameraPanel = new JPanel();
		this.cameraPanel.setToolTipText("Param\u00E8tres de l'imageur");
		this.tabbedPane.addTab("Imageur", null, this.cameraPanel, null);
		this.cameraPanel.setLayout(new MigLayout("", "[grow][grow][]", "[][][grow]"));
		
		this.lblPixSize = new JLabel("Taille des pixels");
		this.lblPixSize.setToolTipText("Taille en um des pixels");
		this.cameraPanel.add(this.lblPixSize, "cell 0 0,alignx right");
		
		this.fieldPixSize = new JTextField();
		this.fieldPixSize.setToolTipText("Taille des pixels en um");
		this.cameraPanel.add(this.fieldPixSize, "cell 1 0,growx");
		this.fieldPixSize.setColumns(10);
		
		this.fieldPixSizeErr = new JLabel("(!)");
		this.fieldPixSizeErr.setForeground(Color.RED);
		this.fieldPixSizeErr.setFont(new Font("Tahoma", Font.BOLD, 10));
		this.cameraPanel.add(this.fieldPixSizeErr, "cell 2 0");
		
		this.lblFocal = new JLabel("Focale");
		this.lblFocal.setToolTipText("Focale de l'objectif en mm");
		this.cameraPanel.add(this.lblFocal, "cell 0 1,alignx trailing");
		
		this.fieldFocal = new JTextField();
		this.fieldFocal.setToolTipText("Focale de l'objectif en mm");
		this.cameraPanel.add(this.fieldFocal, "cell 1 1,growx");
		this.fieldFocal.setColumns(10);
		
		this.fieldFocalErr = new JLabel("(!)");
		this.fieldFocalErr.setForeground(Color.RED);
		this.fieldFocalErr.setFont(new Font("Tahoma", Font.BOLD, 10));
		this.cameraPanel.add(this.fieldFocalErr, "cell 2 1");
				
		this.geoPanel = new JPanel();
		this.tabbedPane.addTab("G\u00E9ographie", null, this.geoPanel, null);
		this.geoPanel.setLayout(new MigLayout("", "[][grow][]", "[][]"));
		
		this.lblLatitude = new JLabel("Latitude : ");
		this.geoPanel.add(this.lblLatitude, "cell 0 0,alignx trailing");
		
		this.fieldLatitude = new JTextField();
		this.geoPanel.add(this.fieldLatitude, "cell 1 0,growx");
		this.fieldLatitude.setColumns(10);
		
		this.fieldLatitudeErr = new JLabel("(!)");
		this.fieldLatitudeErr.setForeground(Color.RED);
		this.fieldLatitudeErr.setFont(new Font("Tahoma", Font.BOLD, 10));
		this.geoPanel.add(this.fieldLatitudeErr, "cell 2 0");
		
		this.lblLongitude = new JLabel("Longitude : ");
		this.geoPanel.add(this.lblLongitude, "cell 0 1,alignx trailing");
		
		this.fieldLongitude = new JTextField();
		this.geoPanel.add(this.fieldLongitude, "cell 1 1,growx");
		this.fieldLongitude.setColumns(10);
		
		this.fieldLongitudeErr = new JLabel("(!)");
		this.fieldLongitudeErr.setForeground(Color.RED);
		this.fieldLongitudeErr.setFont(new Font("Tahoma", Font.BOLD, 10));
		this.geoPanel.add(this.fieldLongitudeErr, "cell 2 1");
		
		this.filePanel = new JPanel();
		this.tabbedPane.addTab("Fichiers", null, this.filePanel, null);
		this.filePanel.setLayout(new MigLayout("", "[][grow][][]", "[][]"));
		
		this.lblRpertoireTycho = new JLabel("R\u00E9pertoire Tycho-2");
		this.filePanel.add(this.lblRpertoireTycho, "cell 0 0,alignx trailing");
		
		this.starCatalogPathTyc2Field = new JTextField();
		this.filePanel.add(this.starCatalogPathTyc2Field, "cell 1 0,growx");
		this.starCatalogPathTyc2Field.setColumns(10);
		
		this.starCatalogPathTyc2Browse = new JButton("...");
		this.starCatalogPathTyc2Browse.setMargin(new Insets(2, 2, 2, 2));
		this.starCatalogPathTyc2Browse.setToolTipText("Permet de rechercher l'emplacement des fichiers pour tycho2");
		this.filePanel.add(this.starCatalogPathTyc2Browse, "cell 2 0,aligny bottom");
		
		this.starCatalogPathTyc2Download = new JButton("T\u00E9l\u00E9charger");
		this.starCatalogPathTyc2Download.setMargin(new Insets(2, 2, 2, 2));
		this.filePanel.add(this.starCatalogPathTyc2Download, "cell 3 0");
		
		this.lblRepertoiresAstroNet = new JLabel("R\u00E9pertoires des indexes Astrometry.net");
		this.filePanel.add(this.lblRepertoiresAstroNet, "cell 0 1,alignx trailing");
		
		this.astroNetPathField = new JTextField();
		this.filePanel.add(this.astroNetPathField, "cell 1 1,growx");
		this.astroNetPathField.setColumns(10);
		
		this.astroNetBrowse = new JButton("...");
		this.astroNetBrowse.setMargin(new Insets(2, 2, 2, 2));
		this.astroNetBrowse.setToolTipText("Permet de rechercher l'emplacement des fichiers pour astrometry.net");
		this.filePanel.add(this.astroNetBrowse, "cell 2 1");
		
		this.astroNetDownload = new JButton("T\u00E9l\u00E9charger");
		this.astroNetDownload.setMargin(new Insets(2, 2, 2, 2));
		this.filePanel.add(this.astroNetDownload, "cell 3 1");
		
		this.joystickPanel = new JPanel();
		this.tabbedPane.addTab("Joystick", null, this.joystickPanel, null);
		this.joystickPanel.setLayout(new BorderLayout(0, 0));
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				cancelButton = new JButton("Cancel");
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

	public JButton getOkButton() {
		return this.okButton;
	}
	public JButton getCancelButton() {
		return this.cancelButton;
	}
}
