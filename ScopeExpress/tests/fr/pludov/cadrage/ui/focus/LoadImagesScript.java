package fr.pludov.cadrage.ui.focus;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class LoadImagesScript extends ScriptTest {

	final String dirPath;
	final String pattern;
	final String [] explicitList;
	final List<File> todoList;
	final ActionMonitor am;
	
	public LoadImagesScript(ActionMonitor am, String dirPath, String pattern) {
		this.dirPath = dirPath;
		this.pattern = pattern;
		this.explicitList = null;
		this.am = am;
		this.todoList = new LinkedList<File>();
	}
	
	public LoadImagesScript(ActionMonitor am, String dirPath, String [] explicitList) {
		this.dirPath = dirPath;
		this.pattern = null;
		this.explicitList = explicitList;
		this.am = am;
		this.todoList = new LinkedList<File>();
	}
	
	@Override
	public void start()
	{
		
		File dir = new File(dirPath);
		
		if (explicitList == null) {
			String [] dirs = dir.list();
			if (dirs == null) {
				done();
			}
			
			
			Arrays.sort(dirs);
			
			for(String s : dirs)
			{
				if (!s.matches(pattern))
				{
					continue;
				}
				File f = new File(dir, s);
				if ((!f.exists()) || (!f.isFile())) {
					continue;
				}
				
				this.todoList.add(f);
			}
		} else {
			for(String explicitFile : this.explicitList)
			{
				File f = new File(dir, explicitFile);

				this.todoList.add(f);
			}
		}
		
		if (this.todoList.isEmpty()) {
			done();
		}
	}

	@Override
	public void step()
	{
		File f = todoList.remove(0);
		am.addImage(f);
		if (todoList.isEmpty()) {
			done();
		}
	}
}
