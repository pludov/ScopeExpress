package fr.pludov.scopeexpress.ui.utils;

import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.SwingUtilities;

import fr.pludov.scopeexpress.ui.AstroNetIndexSelector;
import fr.pludov.scopeexpress.ui.preferences.BooleanConfigItem;
import fr.pludov.scopeexpress.ui.preferences.StringConfigItem;

/**
 * Cette fenêtre permet de demander quelque chose à l'utilisateur en lui donnant la possibilité de ne 
 * pas répondre.
 * 
 * Son choix est enregistré en conf selon les paramètres fournis au constructeur.
 * 
 * isYes() et isNo() peuvent retourner tous les deux false si l'utilisateur n'a encore rien choisi.
 * 
 * Potentiellement, tous les choix ne peuvent pas être conservé (ex: une mise à jour de conf ne pourra pas enregistrer "yes")
 */
public abstract class AskNowOrLater extends AskNowOrLaterDesign {
	// Si l'utilisateur vient de choisir quelque chose
	Boolean userChoice;
	
	final String versionKey;
	final StringConfigItem lastChoice;
	final Set<Boolean> recordableOptions;
	
	/**
	 * @param parent la fenêtre parente
	 * @param title le titre du dialog
	 * @param htmlText le contenu du dialogue (html)
	 * @param lastChoice la clef de conf qui contient le choix - doit valoir vide par défaut
	 * @param versionKey arbitraire - permet de rendre obsolète des choix de valeurs liés aux précédentes versions
	 */
	public AskNowOrLater(Window parent, String title, String htmlText, StringConfigItem lastChoice, String versionKey, boolean ... recordableOptions)
	{
		super(parent);
		this.versionKey = versionKey;
		this.lastChoice = lastChoice;
		
		this.setTitle(title);
		this.getTxtpn().setText(htmlText);
		this.recordableOptions = new HashSet<Boolean>();
		for(boolean b : recordableOptions) {
			this.recordableOptions.add(b);
		}
		
		this.getYesButton().addActionListener(getActionListenerFor(true));
		this.getNoButton().addActionListener(getActionListenerFor(false));
		
	}
	
	ActionListener getActionListenerFor(final boolean v)
	{
		return new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				userChoice = v;
				if (getChckbxDontAsk().isSelected() && recordableOptions.contains(v)) {
					recordChoice(v);
				} else {
					clearChoice();
				}
				
				onDone();
			}
		};
	}
	
	void recordChoice(boolean b)
	{
		lastChoice.set((b ? "t" : "f") + this.versionKey);
	}
	
	void clearChoice()
	{
		lastChoice.set("");
	}
	
	public boolean is(boolean value)
	{
		if (userChoice != null) {
			return userChoice == value;
		}

		if (!recordableOptions.contains(value)) {
			return false;
		}
		
		if (isShowing()) {
			return false;
		}
		
		String currentConf = lastChoice.get();
		if (currentConf == null || "".equals(currentConf)) {
			return false;
		}
		if (!currentConf.substring(1).equals(this.versionKey)) {
			return false;
		}
		
		char c = currentConf.charAt(0);
		return c == (value ? 't' : 'f');
	}
	
	public boolean isYes()
	{
		return is(true);
	}
	
	public boolean isNo()
	{
		return is(false);
	}
	
	/**
	 * Doit utiliser isYes ou isNo pour effectivement faire quelque chose
	 */
	public abstract void onDone();
	
	/**
	 * Montre le dialogue ou appelle directement onDone
	 */
	public void perform()
	{
		if (isYes()) {
			userChoice = true;
		} else if (isNo()) {
			userChoice = false;
		} else {
			setModal(true);
			setVisible(true);
			return;
		}
		
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				onDone();
			}
		});
	}
}
