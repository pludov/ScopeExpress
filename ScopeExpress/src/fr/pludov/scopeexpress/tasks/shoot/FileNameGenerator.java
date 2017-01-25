package fr.pludov.scopeexpress.tasks.shoot;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import fr.pludov.scopeexpress.camera.*;
import fr.pludov.scopeexpress.database.content.*;
import fr.pludov.scopeexpress.filterwheel.*;
import fr.pludov.scopeexpress.focuser.*;
import fr.pludov.scopeexpress.ui.*;

public class FileNameGenerator {

	final FocusUi focusUi;
	
	abstract class Substituer {
		final String prefix;
		Substituer(String prefix)
		{
			this.prefix = prefix;
		}
		
		abstract String proceed(String arg, RunningShootInfo shoot) throws IOException;
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
			String proceed(String arg, RunningShootInfo shoot)  {
				Target currentTarget = focusUi.getDatabase().getRoot().getCurrentTarget();
				if (currentTarget != null) {
					return currentTarget.getName();
				}
				return null;
			}
		});
		result.add(new Substituer("TYPE") {
			@Override
			String proceed(String arg, RunningShootInfo shoot)  {
				return Objects.toString(shoot.getType(), null);
			}
		});
		result.add(new Substituer("PHASE") {
			@Override
			String proceed(String arg, RunningShootInfo shoot)  {
				return shoot.getPhase();
			}
		});
		
		result.add(new Substituer("SESSION") {
			@Override
			String proceed(String arg, RunningShootInfo shoot) throws IOException {
				return formatDate(focusUi.getSessionStartTime(), arg);
			}
		});
		result.add(new Substituer("EXP") {
			@Override
			String proceed(String arg, RunningShootInfo shoot) throws IOException {
				Double d = shoot.getExp();
				if (Math.floor(d) == d) {
					return Long.toString(d.longValue()) + "s";
				}
				d = d * 1000;
				return Long.toString(d.longValue()) + "ms";
			}
		});
		result.add(new Substituer("BIN") {
			@Override
			String proceed(String arg, RunningShootInfo shoot) throws IOException {
				Integer v;
				if (arg.isEmpty() || arg.toLowerCase().equals("x")) {
					v = shoot.getBinx();
				} else {
					v = shoot.getBiny();
				}
				return v != null ? Integer.toString(v) : null;
			}
		});
		result.add(new Substituer("NOW") {
			@Override
			String proceed(String arg, RunningShootInfo shoot) throws IOException {
				return formatDate(System.currentTimeMillis(), arg);
			}
		});
		result.add(new Substituer("FILTER") {
			@Override
			String proceed(String arg, RunningShootInfo shoot) throws IOException {
				FilterWheel fw = focusUi.getFilterWheelManager().getConnectedDevice();
				if (fw == null ) {
					return null;
				}
				int pos;
				try {
					pos = fw.getCurrentPosition();
				} catch (FilterWheelException e1) {
					return null;
				}
				try {
					String [] filters = fw.getFilters();
					if (pos < 0 || pos >= filters.length) {
						return "#" + pos;
					}
					return filters[pos];
				} catch (FilterWheelException e) {
					return "#" + pos;
				}
				
			}
		});
		result.add(new Substituer("FOCUSER") {
			@Override
			String proceed(String arg, RunningShootInfo shoot) throws IOException {
				Focuser fw = focusUi.getFocuserManager().getConnectedDevice();
				if (fw == null ) {
					return null;
				}
				return "" + fw.position();
				
			}
		});

		return result;
		
	}
	
	
	public FileNameGenerator(FocusUi focusUi) {
		this.focusUi = focusUi;
	}


	private String doSubstitution(String parameter, RunningShootInfo task) throws IOException
	{
		String prefix, suffix;
		Pattern p = Pattern.compile("^(?:(.*)<|)([^\\<\\>]*?)(?:>(.*)|)$");
		
		Matcher m = p.matcher(parameter);
		if (m.find()) {
			prefix = Objects.toString(m.group(1), "");
			parameter = m.group(2);
			suffix = Objects.toString(m.group(3), "");
		} else {
			prefix = "";
			suffix = "";
		}
		
		for(Substituer s : getSubstituer())
		{
			if (parameter.startsWith(s.prefix)) {
				String repl = s.proceed(parameter.substring(s.prefix.length()), task);
				if (repl != null) {
					return prefix + repl + suffix;
				} else {
					return "";
				}
			}
		}
		throw new IOException("Substitution invalide: " + parameter);
	}


	public String performFileNameExpansion(String base, String fileName, RunningShootInfo task) throws IOException
	{
		Pattern p = Pattern.compile("\\$([^$]*)\\$");
		Matcher m = p.matcher(fileName);
		StringBuffer sb = new StringBuffer();
		while(m.find()) {
			String parameter = m.group(1);
			m.appendReplacement(sb, doSubstitution(parameter, task));
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
