package org.bridj.android;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import dalvik.system.DexClassLoader;
import com.android.dx.dex.file.DexFile;
import com.android.dx.dex.cf.CfTranslator;
import com.android.dx.dex.cf.CfOptions;

public class AndroidDynamicClassLoader 
{
	final CfOptions cfOptions;
	List<Class> classes;
	final String dexDirPath = "/sdcard/bridj-tmp";
	ClassLoader parentClassLoader;
	
	public AndroidDynamicClassLoader (ClassLoader parentClassLoader) {
		this.parentClassLoader = parentClassLoader;
		cfOptions = new CfOptions();
		cfOptions.strictNameCheck = false;
		classes = new Vector<Class>();
		File dir = new File (dexDirPath);
		if (!dir.exists())
			dir.mkdir();
	}

    public synchronized Class defineClass(String typeName, byte[] javaBytes, int offset, int length) throws ClassNotFoundException
    {
    	String dexFilePath = dexDirPath + "/" + typeName + ".zip";
    	
    	String[] subdirs = typeName.split("/");
    	String current = dexDirPath;
    	for(int i = 0; i < subdirs.length - 1; i++) {
    		current += File.separator + subdirs[i];
    		new File(current).mkdir();
    	}
    	
    	try {
        	DexFile dexFile = new DexFile();
	    	dexFile.add(CfTranslator.translate(typeName, javaBytes, cfOptions));
	        try {
	            OutputStream out = null;
	            try {
	                byte [] outArray = dexFile.toDex(null, false);
	                out = openOutput(dexFilePath);
	                ZipOutputStream zip = new ZipOutputStream(out);
	                zip.putNextEntry(new ZipEntry ("classes.dex"));
	                zip.write(outArray, 0, outArray.length);
	                zip.closeEntry();
	                zip.close();
	            } finally {
	                closeOutput(out);
	              }
	       } catch (Exception ex) {
				throw new RuntimeException("Failed to create implementation class for callback type " + typeName + " : " + ex, ex);
	        }
			DexClassLoader loader = new DexClassLoader(dexFilePath, dexDirPath, null, parentClassLoader);
			return (Class) loader.loadClass(typeName);
    	} finally {
    		//cleanupDexFiles();
    	}
    }
    
    
    private OutputStream openOutput(String name) throws IOException {
        if (name.equals("-") ||
                name.startsWith("-.")) {
            return System.out;
        }

        return new FileOutputStream(name);
    }
    private void closeOutput(OutputStream stream) throws IOException {
        if (stream == null) {
            return;
         }

        stream.flush();

        if (stream != System.out) {
            stream.close();
         }
    }
    private void cleanupDexFiles()
    {
    	File dir = new File(dexDirPath);
    	for (File f : dir.listFiles())
    		f.delete();
    }
}
