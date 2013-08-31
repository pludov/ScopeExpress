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
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.JProgressBar;
import java.awt.Insets;
import org.eclipse.wb.swing.FocusTraversalOnArray;
import java.awt.Component;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

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
	protected JButton detectBton;
	protected JButton fwhmGraphBton;
	protected JMenuItem mnOpen;
	protected JMenuItem mnAutoOpen;
	protected JMenu mnEtoiles;
	protected JMenuItem mnChercheEtoiles;
	protected JCheckBoxMenuItem mnChercheEtoilesAuto;
	protected JPanel statusPanel;
	protected JLabel taskQueueStatus;
	protected JProgressBar taskQueueProgress;
	protected JButton taskQueueStop;
	protected JButton shootButton;
	protected JMenuItem mnPolaire;
	protected JMenuItem mntmAutre;
	protected JTabbedPane starOccurenceControlPane;
	protected JPanel starDetailPanel;
	protected JPanel graphParamPanel;
	protected JMenu mnTests;
	protected JButton scriptAdvanceBton;
	protected JMenuItem mnProjectSave;
	protected JTabbedPane graphsPane;
	protected JPanel fwhmEvolutionPanel;
	protected JPanel fwhmRepartitionPanel;
	protected JPanel fwhmEvolutionGraphPanel;
	protected JPanel fwhmEvolutionHistoPanel;
	protected JMenu mnTlscope;
	protected JMenuItem mntmConnecter;
	protected JMenuItem mntmDconnecter;
	protected JMenuItem mntmLoadStarAroundScope;
	protected JButton btnReset;
	protected JMenu mnConfiguration;
	protected JMenuItem mntmConfiguration;
	protected JMenuItem mntmSauverConfig;

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
		
		this.mnProjectSave = new JMenuItem("Sauver le projet");
		this.mnfichier.add(this.mnProjectSave);
		
		this.mnQuitter = new JMenu("Quitter");
		this.mnfichier.add(this.mnQuitter);
		
		this.mnEtoiles = new JMenu("Etoiles");
		this.menuBar.add(this.mnEtoiles);
		
		this.mnChercheEtoiles = new JMenuItem("Rechercher...");
		this.mnEtoiles.add(this.mnChercheEtoiles);
		
		this.mnChercheEtoilesAuto = new JCheckBoxMenuItem("Rechercher automatiquement");
		this.mnEtoiles.add(this.mnChercheEtoilesAuto);
		
		this.mnPolaire = new JMenuItem("Charger autours du pole Nord");
		this.mnEtoiles.add(this.mnPolaire);
		
		this.mntmLoadStarAroundScope = new JMenuItem("Charger autours du t\u00E9l\u00E9scope");
		this.mnEtoiles.add(this.mntmLoadStarAroundScope);
		
		this.mntmAutre = new JMenuItem("Charger ...");
		this.mnEtoiles.add(this.mntmAutre);
		
		this.mnTlscope = new JMenu("T\u00E9l\u00E9scope");
		this.menuBar.add(this.mnTlscope);
		
		this.mntmConnecter = new JMenuItem("Connecter");
		this.mnTlscope.add(this.mntmConnecter);
		
		this.mntmDconnecter = new JMenuItem("D\u00E9connecter");
		this.mnTlscope.add(this.mntmDconnecter);
		
		this.mnConfiguration = new JMenu("Configuration");
		this.menuBar.add(this.mnConfiguration);
		
		this.mntmConfiguration = new JMenuItem("Configuration");
		this.mnConfiguration.add(this.mntmConfiguration);
		
		this.mntmSauverConfig = new JMenuItem("Sauver");
		this.mnConfiguration.add(this.mntmSauverConfig);
		
		this.mnTests = new JMenu("Tests");
		this.menuBar.add(this.mnTests);
		
		this.toolBar = new JToolBar();
		this.frmFocus.getContentPane().add(this.toolBar, BorderLayout.NORTH);
		
		this.detectBton = new JButton("Trouver etoiles");
		this.detectBton.setIcon(new ImageIcon(FocusUiDesign.class.getResource("/fr/pludov/cadrage/ui/resources/icons/star.png")));
		this.detectBton.setToolTipText("Trouver automatiquement des \u00E9toiles dans l'image courante");
		this.toolBar.add(this.detectBton);
		
		this.fwhmGraphBton = new JButton("Vue fwhm");
		this.fwhmGraphBton.setToolTipText("Tracer un graphe de FWHM");
		this.toolBar.add(this.fwhmGraphBton);
		
		this.followDirBton = new JButton("");
		this.followDirBton.setIcon(new ImageIcon(FocusUiDesign.class.getResource("/fr/pludov/cadrage/ui/resources/icons/media-playback-start-7.png")));
		this.followDirBton.setToolTipText("Surveiller les nouvelles photos dans un r\u00E9pertoire");
		this.toolBar.add(this.followDirBton);
		
		this.shootButton = new JButton("");
		this.shootButton.setIcon(new ImageIcon(FocusUiDesign.class.getResource("/fr/pludov/cadrage/ui/resources/icons/media-record-5.png")));
		this.shootButton.setToolTipText("Prendre une photo (n\u00E9cessite la surveillance d'un r\u00E9pertoire et APT)");
		this.toolBar.add(this.shootButton);
		
		this.scriptAdvanceBton = new JButton("Avancer le script");
		this.toolBar.add(this.scriptAdvanceBton);
		
		this.btnReset = new JButton("Reset");
		this.btnReset.setToolTipText("Efface les donn\u00E9es et repart sur un \u00E9tat vierge");
		this.btnReset.setIcon(new ImageIcon(FocusUiDesign.class.getResource("/fr/pludov/cadrage/ui/resources/icons/view-remove.png")));
		this.toolBar.add(this.btnReset);
		
		this.tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		this.frmFocus.getContentPane().add(this.tabbedPane, BorderLayout.CENTER);
		
		this.imageViewPanel = new JPanel();
		this.imageViewPanel.setFocusable(false);
		this.tabbedPane.addTab("Images", null, this.imageViewPanel, null);
		this.imageViewPanel.setLayout(new BoxLayout(this.imageViewPanel, BoxLayout.X_AXIS));
		
		this.detailsPanel = new JPanel();
		this.detailsPanel.setFocusable(false);
		this.tabbedPane.addTab("D\u00E9tails", null, this.detailsPanel, null);
		this.detailsPanel.setLayout(new MigLayout("", "[593px,grow][235px:n:235px,grow]", "[369px,grow]"));
		
		this.detailsSplitPane = new JSplitPane();
		this.detailsSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		this.detailsPanel.add(this.detailsSplitPane, "cell 0 0,grow");
		
		this.graphsPane = new JTabbedPane(JTabbedPane.TOP);
		this.detailsSplitPane.setRightComponent(this.graphsPane);
		
		this.fwhmEvolutionPanel = new JPanel();
		this.graphsPane.addTab("Evolution", null, this.fwhmEvolutionPanel, null);
		this.fwhmEvolutionPanel.setLayout(new MigLayout("", "[grow,fill][250px,fill]", "[grow,fill]"));
		
		this.fwhmEvolutionGraphPanel = new JPanel();
		this.fwhmEvolutionPanel.add(this.fwhmEvolutionGraphPanel, "cell 0 0,alignx center,aligny center");
		this.fwhmEvolutionGraphPanel.setLayout(new BoxLayout(this.fwhmEvolutionGraphPanel, BoxLayout.X_AXIS));
		
		this.fwhmEvolutionHistoPanel = new JPanel();
		this.fwhmEvolutionPanel.add(this.fwhmEvolutionHistoPanel, "cell 1 0,grow");
		this.fwhmEvolutionHistoPanel.setLayout(new BoxLayout(this.fwhmEvolutionHistoPanel, BoxLayout.X_AXIS));
		
		this.fwhmRepartitionPanel = new JPanel();
		this.graphsPane.addTab("R\u00E9partition", null, this.fwhmRepartitionPanel, null);
		this.fwhmRepartitionPanel.setLayout(new BoxLayout(this.fwhmRepartitionPanel, BoxLayout.X_AXIS));
		
		this.starOccurenceControlPane = new JTabbedPane(JTabbedPane.TOP);
		this.detailsPanel.add(this.starOccurenceControlPane, "cell 1 0,grow");
		
		this.starDetailPanel = new JPanel();
		this.starOccurenceControlPane.addTab("Etoile", null, this.starDetailPanel, null);
		
		this.graphParamPanel = new JPanel();
		this.starOccurenceControlPane.addTab("Graphique", null, this.graphParamPanel, null);
		
		this.statusPanel = new JPanel();
		this.frmFocus.getContentPane().add(this.statusPanel, BorderLayout.SOUTH);
		this.statusPanel.setLayout(new MigLayout("insets 0", "[grow][][]", "[]"));
		
		this.taskQueueStatus = new JLabel("Status");
		this.taskQueueStatus.setFont(new Font("Tahoma", Font.PLAIN, 10));
		this.statusPanel.add(this.taskQueueStatus, "flowx,cell 0 0");
		
		this.taskQueueProgress = new JProgressBar();
		this.statusPanel.add(this.taskQueueProgress, "cell 1 0");
		
		this.taskQueueStop = new JButton("stop");
		this.taskQueueStop.setMargin(new Insets(0, 5, 0, 5));
		this.taskQueueStop.setIconTextGap(2);
		this.taskQueueStop.setIcon(null);
		this.statusPanel.add(this.taskQueueStop, "cell 2 0");
		this.frmFocus.getContentPane().setFocusTraversalPolicy(new FocusTraversalOnArray(new Component[]{this.imageViewPanel, this.detailsPanel, this.toolBar, this.tabbedPane}));
	}

	protected JFrame getFrmFocus() {
		return this.frmFocus;
	}
}
