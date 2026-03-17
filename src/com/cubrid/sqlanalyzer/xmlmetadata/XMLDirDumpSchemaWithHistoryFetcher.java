package com.cubrid.sqlanalyzer.xmlmetadata;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

import com.cubrid.cubridmigration.core.common.DBUtils;
import com.cubrid.cubridmigration.core.common.PathUtils;
import com.cubrid.cubridmigration.core.dbmetadata.DBSchemaInfoFetcherFactory;
import com.cubrid.cubridmigration.core.dbmetadata.IBuildSchemaFilter;
import com.cubrid.cubridmigration.core.dbmetadata.IDBSchemaInfoFetcher;
import com.cubrid.cubridmigration.core.dbmetadata.IDBSource;
import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.dbobject.Column;
import com.cubrid.cubridmigration.core.dbobject.Schema;
import com.cubrid.cubridmigration.core.dbobject.SchemaCatalog;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.dbtype.DatabaseType;
import com.cubrid.cubridmigration.mysql.MysqlXmlDumpSource;
import com.cubrid.cubridmigration.ui.database.MysqlXmlDumpParsingHistory;
import com.cubrid.cubridmigration.ui.database.MysqlXmlDumpParsingHistoryManager;

public class XMLDirDumpSchemaWithHistoryFetcher implements IDBSchemaInfoFetcher {
    private IDBSchemaInfoFetcher builder;

    /** cancel generating catalog */
    public void cancel() {
        if (builder != null) {
            builder.cancel();
        }
    }
    
    protected Catalog readHistory(String xmlFile, String xmlFileCharset) {
        Catalog catalog = null;
        try {
            MysqlXmlDumpParsingHistory xmlCH =
                    MysqlXmlDumpParsingHistoryManager.getXMLCatalogHistory(xmlFile, xmlFileCharset);
            if (xmlCH != null && new File(xmlFile).length() == xmlCH.getXmlFileLength()) {
                XMLDecoder decoder =
                        new XMLDecoder(new FileInputStream(xmlCH.getFullJsonFileName()));
                catalog = (Catalog) decoder.readObject();
                decoder.close();
            }
        } catch (Exception ex) {
//            LOG.error("", ex);
        }
        return catalog;
    }
    
    /**
     * Save history of parsing XML dump file to local JSON file.
     *
     * @param xmlFile XML file full name
     * @param xmlFileCharset XML file char-set
     * @param catalog the catalog to be saved.
     */
    protected void saveParsingHistory(String xmlFile, String xmlFileCharset, Catalog catalog) {
        if (catalog == null || catalog.getSchemas().isEmpty()) {
            throw new IllegalArgumentException("");
        }
        final Schema schema = catalog.getSchemas().get(0);
        if (schema == null) {
            throw new IllegalArgumentException("");
        }
        try {

            MysqlXmlDumpParsingHistory history =
                    MysqlXmlDumpParsingHistoryManager.getXMLCatalogHistory(xmlFile, xmlFileCharset);
            if (history == null) {
                String jsonFileName = DBUtils.getIdentity();
                history =
                        new MysqlXmlDumpParsingHistory(
                                xmlFile, xmlFileCharset, new File(xmlFile).length(), jsonFileName);
                MysqlXmlDumpParsingHistoryManager.appendMigrationHistory(history);
            }
            //			else {
            //				jsonFileName = history.getJsonFileName();
            //			}
            File catalogFile = new File(history.getFullJsonFileName());
            if (catalogFile.exists()) {
                PathUtils.deleteFile(catalogFile);
            }
            PathUtils.createFile(catalogFile);
            XMLEncoder encoder = new XMLEncoder(new FileOutputStream(catalogFile));
            // For JDK 7: XMLEncoder of 7 has error when writing objects.
            // Set column's table property to NULL
            for (Table tbl : schema.getTables()) {
                for (Column col : tbl.getColumns()) {
                    col.setTableOrView(null);
                }
            }
            encoder.writeObject(catalog);
            encoder.close();
        } catch (Exception e) {
//            LOG.error("failed to save xml catalog information", e);
        }
        // For JDK 7: XMLEncoder of 7 has error when writing objects.
        // Restore column's table property
        for (Table tbl : schema.getTables()) {
            for (Column col : tbl.getColumns()) {
                col.setTableOrView(tbl);
            }
        }
    }

    /**
     * Get the MYSQL dump file's schema, if the schema information was saved at local, and the file
     * is not changed, load the saved schema directly.
     *
     * @param ds should be a MYSQLXMLDumpSource
     * @param filter IBuildSchemaFilter
     * @return catalog may be null.
     */
    public Catalog fetchSchema(IDBSource ds, IBuildSchemaFilter filter) {
        try {
            MysqlXmlDumpSource mysqlds = (MysqlXmlDumpSource) ds;
            String xmlFile = mysqlds.getFileName();
            String xmlFileCharset = mysqlds.getCharset();
            Catalog catalog = readHistory(xmlFile, xmlFileCharset);
            if (catalog == null) {
                builder = DBSchemaInfoFetcherFactory.createFetcher(ds);
                catalog = builder.fetchSchema(ds, null);
                saveParsingHistory(xmlFile, xmlFileCharset, catalog);
            }
            if (catalog != null) {
                catalog.setDatabaseType(DatabaseType.MYSQL);
            }
            return catalog;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
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
