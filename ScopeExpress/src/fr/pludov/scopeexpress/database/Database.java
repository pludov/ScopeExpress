package fr.pludov.scopeexpress.database;

import java.io.*;

import javax.swing.*;

import fr.pludov.scopeexpress.utils.*;

/** 
 * 
 * Représente la base de conf de l'application
 */
public class Database<ROOT extends BaseDatabaseItem<ROOT>> {
	final Class<ROOT> rootClass;
	final File storage;
	ROOT root;
	
	public Database(ROOT root, File storage) {
		this.rootClass = (Class<ROOT>)root.getClass();
		this.root = root;
		this.storage = storage;
	}

	private Database(Class<ROOT> rootClass, File storage) {
		this.rootClass = rootClass;
		this.storage = storage;
		try {
			this.root = (ROOT)rootClass.getConstructor(Database.class).newInstance(this);
		} catch(Exception e) {
			throw new RuntimeException("Default initilization failed for : " + rootClass, e);
		}
		
	}

	static <ROOT extends BaseDatabaseItem<ROOT>> Database<ROOT> load(Class<ROOT> rootClass, File storage, InputStream from) throws IOException, ClassNotFoundException
	{
		Database<ROOT> result = new Database<>(rootClass, storage);
		
		try(DatabaseObjectInputStream dois = new DatabaseObjectInputStream(result, from))
		{
			result.root = (ROOT) dois.readObject();
			if (result.root == null) {
				throw new IOException("Null read");
			}
			if (!rootClass.isAssignableFrom(result.root.getClass())) {
				throw new ClassCastException("Cannot convert " + result.root.getClass() + " into " + rootClass);
			}
		}
		
		return result;
	}

	public static <ROOT extends BaseDatabaseItem<ROOT>> Database<ROOT> loadWithDefault(Class<ROOT> rootClass, File file)
	{
		try {
			if (!file.exists()) {
				return new Database<>(rootClass, file);
			}
			try(FileInputStream fis = new FileInputStream(file)) {
				return load(rootClass, file, fis);
			}
		} catch(Throwable t) {
			new EndUserException("Erreur de lecture de " + file, t).report(null);
			return new Database<>(rootClass, file);
		}
	}

	
	public void save(File into) throws FileNotFoundException, IOException
	{
		File tmp = File.createTempFile(".temp.save", ".dat", into.getParentFile());
		
		try(FileOutputStream fos = new FileOutputStream(tmp);
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				ObjectOutputStream oos = new ObjectOutputStream(bos))
		{
			oos.writeObject(root);
		}
		
		tmp.renameTo(into);
	}
	
	
	public void save() throws FileNotFoundException, IOException
	{
		save(storage);
	}
	
	boolean savePending = false;
	public void asyncSave()
	{
		if (!savePending) {
			savePending = true;
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					savePending = false;
					try {
						save();
					} catch(Exception e) {
						new EndUserException("Erreur de sauvegarde de configuration", e).report(null);
					}
				}
				
			});
		}
	}

	public ROOT getRoot() {
		return root;
	}
}
