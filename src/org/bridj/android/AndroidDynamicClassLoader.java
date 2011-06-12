package org.bridj.android;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Vector;

import dalvik.system.PathClassLoader;
import com.android.dx.dex.file.DexFile;
import com.android.dx.dex.cf.CfTranslator;
import com.android.dx.dex.cf.CfOptions;

public class AndroidDynamicClassLoader 
{
	final DexFile dexFile = new DexFile();
	final CfOptions cfOptions;
	List<Class> classes;
	final String dexFilePath = "/sdcard/bridj-tmp";
	ClassLoader parentClassLoader;
	
	public AndroidDynamicClassLoader (ClassLoader parentClassLoader) {
		this.parentClassLoader = parentClassLoader;
		cfOptions = new CfOptions();
		cfOptions.strictNameCheck = false;
		classes = new Vector<Class>();
		File dir = new File (dexFilePath);
		if (!dir.exists())
			dir.mkdir();
	}

    public synchronized Class defineClass(String typeName, byte[] javaBytes, int offset, int length) throws ClassNotFoundException
    {
    	try {
	    	dexFile.add(CfTranslator.translate(dexFilePath, javaBytes, cfOptions));
	        try {
	            OutputStream out = null;
	            try {
	                byte [] outArray = dexFile.toDex(null, false);
	                out = openOutput(dexFilePath);
	                out.write(outArray);
	            } finally {
	                closeOutput(out);
	              }
	       } catch (Exception ex) {
	    	   // FIXME: record error.
	    	   return null;
	        }
			PathClassLoader loader = new PathClassLoader(dexFilePath, parentClassLoader);
			return (Class) loader.loadClass(typeName);
    	} finally {
    		cleanupDexFiles();
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
    	File dir = new File(dexFilePath);
    	for (File f : dir.listFiles())
    		f.delete();
    }
}
