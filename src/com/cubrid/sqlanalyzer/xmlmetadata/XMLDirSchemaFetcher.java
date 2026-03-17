package com.cubrid.sqlanalyzer.xmlmetadata;

import java.io.File;
import java.io.FilenameFilter;
//import java.io.IOException;
//import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

//import com.cubrid.cubridmigration.core.common.Closer;
import com.cubrid.cubridmigration.core.dbmetadata.IBuildSchemaFilter;
import com.cubrid.cubridmigration.core.dbmetadata.IDBSchemaInfoFetcher;
import com.cubrid.cubridmigration.core.dbmetadata.IDBSource;
import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.dbobject.SchemaCatalog;
//import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.sqlanalyzer.core.dbobject.AnalyzerCatalog;
import com.cubrid.sqlanalyzer.dmlparser.DatabaseManager;
import com.cubrid.sqlanalyzer.dmlparser.SqlMapHandler;

public class XMLDirSchemaFetcher implements IDBSchemaInfoFetcher {
	private Runnable cancelRunnable;
	
    public AnalyzerCatalog fetchSchema(IDBSource ds, IBuildSchemaFilter filter) {
        if (cancelRunnable != null) {
            throw new RuntimeException("One fetching work is running");
        }
        XMLDirSource xmlDir = (XMLDirSource) ds;
//        final Reader reader = xmlDir.createReader();
        
        String XMLDir = xmlDir.getFilePath();
        List<File> fileList = getXmlFilesFromDirectory(XMLDir);

//        cancelRunnable =
//                new Runnable() {
//                    public void run() {
//                        try {
//                            reader.close();
//                        } catch (IOException e) {
//                            // DO nothing
//                        }
//                    }
//                };
        try {
            try {
                SAXParserFactory sf = SAXParserFactory.newInstance();
                DatabaseManager analyzerDBManager = new DatabaseManager();
                
                sf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        		sf.setNamespaceAware(true);
                sf.setValidating(false);
                
                SAXParser sp = sf.newSAXParser();
                //		sp.setProperty(
                //				"http://apache.org/xml/features/continue-after-fatal-error",
                //				true);
//                MySQLXMLSchemaParser structReader = new MySQLXMLSchemaParser();
                SqlMapHandler analyzerHandler = new SqlMapHandler(analyzerDBManager);
                
//                InputSource is = new InputSource(reader);
                
                for (File flie : fileList) {
                	sp.parse(flie, analyzerHandler);
                }
                
                AnalyzerCatalog catalog = new AnalyzerCatalog(); 
                catalog.setQueryDictionary(analyzerHandler.getQueryDictionary());
                
                return catalog;
            } catch (Exception e) {
            	e.printStackTrace();
            }
            finally {
                cancelRunnable = null;
//                Closer.close(reader);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
		return null;
    }
    
    private List<File> getXmlFilesFromDirectory(String directoryPath) {
        List<File> xmlFiles = new ArrayList<File>();
        File directory = new File(directoryPath);
        
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".xml");
                }
            });
            
            if (files != null) {
                xmlFiles.addAll(Arrays.asList(files));
            }
        }
        
        return xmlFiles;
    }
	
    public void cancel() {
        if (cancelRunnable != null) {
            cancelRunnable.run();
            cancelRunnable = null;
        }
    }

	@Override
	public SchemaCatalog fetchSchemaNames(IDBSource ds) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Catalog fetchSchemaObjects(IDBSource ds, SchemaCatalog sc, List<String> schemas) {
		// TODO Auto-generated method stub
		return null;
	}
}
