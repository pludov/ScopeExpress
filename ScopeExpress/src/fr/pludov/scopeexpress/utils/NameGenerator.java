package fr.pludov.scopeexpress.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NameGenerator {

	class Fragment {
		String [] texts;
		boolean mandatory;
		boolean debutFin;
		
		Fragment(String ... texts)
		{
			this.texts = texts;
		}
		
		Fragment mandatory()
		{
			this.mandatory = true;
			return this;
		}
		
		Fragment debutFin()
		{
			this.debutFin = true;
			return this;
		}
		
	}
	
	final List<Fragment> fragments;
	final List<Fragment> stack;
	int [] currentlyUsed;
	final Set<String> forbidden2 = new HashSet<String>(Arrays.asList(
			"cg", "cp", "fp", "fg", "fc", "bf", "bp", "bg", "bc"
	));
			
	
	public NameGenerator() {
		this.stack = new ArrayList<NameGenerator.Fragment>();
		fragments = new ArrayList<NameGenerator.Fragment>();
		addFragment("mob").debutFin();
		addFragment("r", "f", "s", "rapide", "facile", "simple", "super").debutFin();
		//addFragment("astro", "a");
		addFragment("focus", "foc", "f", "map").mandatory();
		addFragment("MES", "sta", "s", "align", "al", "a", "eq", "equat").mandatory();
		addFragment("s","sync", "g", "goto", "p", "cadre", "cadr", "c").mandatory();
				
		
	}
	
	public Fragment addFragment(String ... values)
	{
		Fragment f = new Fragment(values);
		fragments.add(f);
		return f;
	}
	
	public boolean isVoyelle(char c)
	{
		return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u' || c == 'y';
	}
	
	public boolean canConcatenate(String current, String suite)
	{
		if (current.length() == 0) return true;
		char lastChar = current.charAt(current.length() - 1);
		char suiteChar = suite.charAt(0);
		if (isVoyelle(suiteChar))
		{
			// on n'accepte pas deux voyelles de suite
			if (isVoyelle(lastChar)) return false;
			// on accepte une voyelle après une consonne
		} else {

			if (isVoyelle(lastChar)) return true;
			// on ne double pas les consonnes
			if (lastChar == suiteChar) return false;
			// Pas deux consonnes pour commencer
			if (current.length() == 1) return false;
			
			// on n'accepte pas trois consonnes de suite
			if (!isVoyelle(current.charAt(current.length() - 2))) return false;
			// Deux consonnes successives
		}
		
		String newStr = new String(new char[]{lastChar, suiteChar});
		if (forbidden2.contains(newStr)) return false;
		return true;
	}
	
	
	
	
	void recuProceed(String current)
	{
		boolean allMandatory = true;
		
		for(int i = 0; i < currentlyUsed.length; ++i)
		{
			if (fragments.get(i).mandatory && currentlyUsed[i] == -1) {
				allMandatory = false;
				break;
			}
		}
		
		if (allMandatory) {
			System.out.println(current);
		}
		
		// On en continue pas après un "debut fin"
		if (stack.size() > 1 && stack.get(stack.size() - 1).debutFin) {
			return;
		}
		for(int i = 0; i < currentlyUsed.length; ++i)
		{
			if (currentlyUsed[i] != -1) continue;
			// On va boucler avec tous les mots
			Fragment f = fragments.get(i);
			for(int textid = 0; textid < f.texts.length; ++ textid)
			{
				
				String text = f.texts[textid];
				if (!canConcatenate(current, text)) continue;
				currentlyUsed[i] = textid;
				stack.add(f);
				recuProceed(current+text);
				stack.remove(stack.size() - 1);
				
			}
			currentlyUsed[i] = -1;
		}
	}
	
	void proceed()
	{
		currentlyUsed = new int[fragments.size()];
		for(int i = 0; i < currentlyUsed.length; ++i)
		{
			currentlyUsed[i] = -1;
		}
		recuProceed("");
	}
	
	public static void main(String[] args) {
		new NameGenerator().proceed();
	}
}
