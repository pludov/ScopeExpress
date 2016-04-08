package fr.pludov.scopeexpress.tasks;

import java.util.*;

/** 
 * Controle l'affichage d'un champ (d'un fils direct d'une tache
 * Mis sous la forme :
 *   chemin d'une sous tache
 *     => groupe parent (optionnel)
 *     => juste avant XXX
 *     => juste après XXX
 */
public class DisplayOrderConstraint {

	// Chemin concerné
	private final ArrayList<String> sourcePath;
	// Chemin réécrit (null = n'est pas une réécriture)
	final ArrayList<String> targetPath;
	
	DisplayOrderConstraint(ArrayList<String> sourcePath, ArrayList<String> targetPath,
			List<String> fieldsAtBegining, List<String> fieldsAtEnd) {
		super();
		this.sourcePath = sourcePath;
		this.targetPath = targetPath;
	}


	public List<String> rewritePath(List<String> subList) {
		if (targetPath == null) return null;
		for(int i = 0; i < sourcePath.size(); ++i) {
			if (i >= subList.size()) {
				return null;
			}
			if (!subList.get(i).equals(sourcePath.get(i))) {
				return null;
			}
		}
		// Ok, ça matche... On construit un nouveau tableau
		ArrayList<String> result = new ArrayList<>();
		result.addAll(targetPath);
		result.addAll(subList.subList(sourcePath.size(), subList.size()));
		return result;
	}
	
	
	static final ArrayList<String> fromPath(String path)
	{
		ArrayList<String> result = new ArrayList<>();
		for(String s : path.split("/"))
		{
			if (s == null || s.isEmpty()) {
				continue;
			}
			result.add(s);
		}
		
		return result;
	}
	
	public static DisplayOrderConstraint moveFieldToGroup(String path, String targetGroupPath)
	{
		ArrayList<String> pathList = fromPath(path);
		ArrayList<String> targetPathList = fromPath(targetGroupPath);
		targetPathList.add(pathList.get(pathList.size() - 1));
		
		return new DisplayOrderConstraint(pathList, targetPathList, null, null);
	}
	
	public static DisplayOrderConstraint moveGroupToGroup(String groupPath, String targetGroupPath)
	{
		ArrayList<String> pathList = fromPath(groupPath);
		ArrayList<String> targetPathList = fromPath(targetGroupPath);
		
		return new DisplayOrderConstraint(pathList, targetPathList, null, null);
		
	}
	
	/** */
	public static DisplayOrderConstraint moveFieldsAtBegining(String groupPath, String ... fields)
	{
		ArrayList<String> pathList = fromPath(groupPath);
		
		return new DisplayOrderConstraint(pathList, null, Arrays.asList(fields), null);
	}
	

	/** */
	public static DisplayOrderConstraint moveFieldsAtEnd(String groupPath, String ... fields)
	{
		ArrayList<String> pathList = fromPath(groupPath);
		
		return new DisplayOrderConstraint(pathList, null, null, Arrays.asList(fields));
	}


	public static class GroupDetails extends DisplayOrderConstraint
	{
		String title;
		boolean titlePresent = false;
		
		// Ordre partiel (début xor fin).
		List<String> fieldsAtBegining;
		List<String> fieldsAtEnd;

		
		GroupDetails(ArrayList<String> sourcePath) {
			super(sourcePath, null, null, null);
		}
		
		public GroupDetails setTitle(String t)
		{
			this.titlePresent = true;
			this.title = t;
			return this;
		}

		public GroupDetails setFirstFields(String ... fields)
		{
			this.fieldsAtBegining = Arrays.asList(fields);
			return this;
		}
		
		public GroupDetails setLastFields(String ... fields)
		{
			this.fieldsAtEnd = Arrays.asList(fields);
			return this;
		}
		
		public boolean hasTitle() {
			return titlePresent;
		}
		
		public String getTitle() {
			return title;
		}

		public List<String> getFirstFields() {
			return fieldsAtBegining;
		}

		public List<String> getLastFields() {
			return fieldsAtEnd;
		}
	}
	
	public static GroupDetails groupDetails(String groupPath) {
		ArrayList<String> pathList = fromPath(groupPath);
		
		return new GroupDetails(pathList);
	}


	public ArrayList<String> getSourcePath() {
		return sourcePath;
	}
}
