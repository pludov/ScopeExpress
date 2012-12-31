package fr.pludov.cadrage.ui.focus;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import net.miginfocom.swing.MigLayout;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;

public class FocusUiDesign {

	private JFrame frmFocus;
	protected JMenuBar menuBar;
	protected JMenu mnfichier;
	protected JMenu mnQuitter;
	protected JTabbedPane tabbedPane;
	protected JPanel imageViewPanel;
	protected JPanel detailsPanel;
	protected JSplitPane detailsSplitPane;
	protected JToolBar toolBar;
	protected JButton followDirBton;
	protected JPanel starDetailPanel;
	protected JButton detectBton;
	protected JButton fwhmGraphBton;
	protected JMenuItem mnOpen;
	protected JMenuItem mnAutoOpen;
	protected JMenu mnEtoiles;
	protected JMenuItem mnChercheEtoiles;
	protected JCheckBoxMenuItem mnChercheEtoilesAuto;

	/**
	 * Create the application.
	 */
	public FocusUiDesign() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		this.frmFocus = new JFrame();
		this.frmFocus.setTitle("Focus");
		this.frmFocus.setBounds(100, 100, 606, 471);
		this.frmFocus.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		this.menuBar = new JMenuBar();
		this.frmFocus.setJMenuBar(this.menuBar);
		
		this.mnfichier = new JMenu("Fichier");
		this.menuBar.add(this.mnfichier);
		
		this.mnOpen = new JMenuItem("Ouvrir");
		this.mnfichier.add(this.mnOpen);
		
		this.mnAutoOpen = new JMenuItem("Surveiller un r\u00E9pertoire");
		this.mnfichier.add(this.mnAutoOpen);
		
		this.mnQuitter = new JMenu("Quitter");
		this.mnfichier.add(this.mnQuitter);
		
		this.mnEtoiles = new JMenu("Etoiles");
		this.menuBar.add(this.mnEtoiles);
		
		this.mnChercheEtoiles = new JMenuItem("Rechercher...");
		this.mnEtoiles.add(this.mnChercheEtoiles);
		
		this.mnChercheEtoilesAuto = new JCheckBoxMenuItem("Rechercher automatiquement");
		this.mnEtoiles.add(this.mnChercheEtoilesAuto);
		
		this.toolBar = new JToolBar();
		this.frmFocus.getContentPane().add(this.toolBar, BorderLayout.NORTH);
		
		this.detectBton = new JButton("Trouver etoiles");
		this.detectBton.setToolTipText("Trouver automatiquement des \u00E9toiles dans l'image courante");
		this.toolBar.add(this.detectBton);
		
		this.fwhmGraphBton = new JButton("Vue fwhm");
		this.fwhmGraphBton.setToolTipText("Tracer un graphe de FWHM");
		this.toolBar.add(this.fwhmGraphBton);
		
		this.followDirBton = new JButton("");
		this.followDirBton.setIcon(new ImageIcon(FocusUiDesign.class.getResource("/com/sun/java/swing/plaf/motif/icons/ScrollRightArrow.gif")));
		this.followDirBton.setToolTipText("Surveiller les nouvelles photos dans un r\u00E9pertoire");
		this.toolBar.add(this.followDirBton);
		
		this.tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		this.frmFocus.getContentPane().add(this.tabbedPane, BorderLayout.CENTER);
		
		this.imageViewPanel = new JPanel();
		this.tabbedPane.addTab("Images", null, this.imageViewPanel, null);
		this.imageViewPanel.setLayout(new BoxLayout(this.imageViewPanel, BoxLayout.X_AXIS));
		
		this.detailsPanel = new JPanel();
		this.tabbedPane.addTab("D\u00E9tails", null, this.detailsPanel, null);
		this.detailsPanel.setLayout(new MigLayout("", "[593px,grow][210px:n:210px]", "[369px,grow]"));
		
		this.detailsSplitPane = new JSplitPane();
		this.detailsSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		this.detailsPanel.add(this.detailsSplitPane, "cell 0 0,grow");
		
		this.starDetailPanel = new JPanel();
		this.detailsPanel.add(this.starDetailPanel, "cell 1 0,grow");
		this.starDetailPanel.setLayout(new BoxLayout(this.starDetailPanel, BoxLayout.X_AXIS));
	}

	protected JFrame getFrmFocus() {
		return this.frmFocus;
	}
}
