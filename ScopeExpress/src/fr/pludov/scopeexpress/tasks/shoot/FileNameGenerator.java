package fr.pludov.scopeexpress.tasks.shoot;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import fr.pludov.scopeexpress.filterwheel.*;
import fr.pludov.scopeexpress.ui.*;

public class FileNameGenerator {

	final FocusUi focusUi;
	
	abstract class Substituer {
		final String prefix;
		Substituer(String prefix)
		{
			this.prefix = prefix;
		}
		
		abstract String proceed(String arg, TaskShoot task, TaskShootDefinition taskDef) throws IOException;
	}
	

	private String formatDate(long when, String format) throws IOException
	{
		if (format.length() == 0) {
			format = "yyyy-MM-dd_HHmmss";
		}
		return new SimpleDateFormat(format).format(when);
	}
	

	private List<Substituer> getSubstituer()
	{
		ArrayList<Substituer> result = new ArrayList<>();
		
		result.add(new Substituer("TARGET") {
			@Override
			String proceed(String arg, TaskShoot task, TaskShootDefinition taskDef)  {
				return task.get(taskDef.target);
			}
		});
		result.add(new Substituer("KIND") {
			@Override
			String proceed(String arg, TaskShoot task, TaskShootDefinition taskDef)  {
				return task.get(taskDef.kind).filePart;
			}
		});
		result.add(new Substituer("SESSION") {
			@Override
			String proceed(String arg, TaskShoot task, TaskShootDefinition taskDef) throws IOException {
				return formatDate(focusUi.getSessionStartTime(), arg);
			}
		});
		result.add(new Substituer("EXP") {
			@Override
			String proceed(String arg, TaskShoot task, TaskShootDefinition taskDef) throws IOException {
				Double d = task.get(taskDef.exposure);
				if (d == null) {
					return "NA";
				}
				if (Math.floor(d) == d) {
					return Long.toString(d.longValue()) + "s";
				}
				d = d * 1000;
				return Long.toString(d.longValue()) + "ms";
			}
		});
		result.add(new Substituer("BIN") {
			@Override
			String proceed(String arg, TaskShoot task, TaskShootDefinition taskDef) throws IOException {
				return Objects.toString(task.get(taskDef.bin));
			}
		});
		result.add(new Substituer("NOW") {
			@Override
			String proceed(String arg, TaskShoot task, TaskShootDefinition taskDef) throws IOException {
				return formatDate(System.currentTimeMillis(), arg);
			}
		});
		result.add(new Substituer("FILTER") {
			@Override
			String proceed(String arg, TaskShoot task, TaskShootDefinition taskDef) throws IOException {
				FilterWheel fw = focusUi.getFilterWheelManager().getConnectedDevice();
				if (fw == null ) {
					return "NA";
				}
				int pos;
				try {
					pos = fw.getCurrentPosition();
				} catch (FilterWheelException e1) {
					task.logger.warn("Erreur de lecture du filtre actif", e1);
					return "NA";
				}
				try {
					String [] filters = fw.getFilters();
					if (pos < 0 || pos >= filters.length) {
						task.logger.warn("Pas de nom pour le filtre "+ pos);
						return "Filter" + pos;
					}
					return filters[pos];
				} catch (FilterWheelException e) {
					task.logger.warn("Erreur de lecture des filtre", e);
					return "Filter" + pos;
				}
				
			}
		});
		return result;
		
	}
	
	
	public FileNameGenerator(FocusUi focusUi) {
		this.focusUi = focusUi;
	}


	private String doSubstitution(String parameter, TaskShoot task, TaskShootDefinition taskDef) throws IOException
	{
		for(Substituer s : getSubstituer())
		{
			if (parameter.startsWith(s.prefix)) {
				return s.proceed(parameter.substring(s.prefix.length()), task, taskDef);
			}
		}
		throw new IOException("Substitution invalide: " + parameter);
	}


	public String performFileNameExpansion(String base, String fileName, TaskShoot task, TaskShootDefinition taskDef) throws IOException
	{
		Pattern p = Pattern.compile("\\$([^$]*)\\$");
		Matcher m = p.matcher(fileName);
		StringBuffer sb = new StringBuffer();
		while(m.find()) {
			String parameter = m.group(1);
			m.appendReplacement(sb, doSubstitution(parameter, task, taskDef));
		}
		m.appendTail(sb);
		
		fileName = sb.toString();
		
		File target = new File(base + "/" + fileName);
		File targetDir = target.getParentFile();
		targetDir.mkdirs();
		if ((!targetDir.exists()) || (!targetDir.isDirectory())) throw new IOException("Répertoire invalide : " + targetDir);
		
		return fileName;
	}

}
