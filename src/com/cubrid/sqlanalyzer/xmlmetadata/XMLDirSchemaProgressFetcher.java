package com.cubrid.sqlanalyzer.xmlmetadata;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;

import com.cubrid.cubridmigration.core.dbmetadata.IDBSchemaInfoFetcher;
import com.cubrid.cubridmigration.core.dbmetadata.IDBSource;
import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.io.IReaderEvent;
//import com.cubrid.cubridmigration.ui.database.MysqlXmlDumpSchemaWithHistoryFetcher;
import com.cubrid.cubridmigration.ui.database.SchemaFetcherWithProgress;
import com.cubrid.cubridmigration.ui.message.Messages;

public class XMLDirSchemaProgressFetcher extends SchemaFetcherWithProgress {
    private final boolean hisFirst;

    public XMLDirSchemaProgressFetcher(boolean hisFirst) {
        this.hisFirst = hisFirst;
    }
    
    /**
     * Create schema information fetcher, if property <hisFirst> is true, the fetcher will be
     * <MysqlXmlDumpSchemaWithHistoryFetcher>
     *
     * @return IDBSchemaInfoFetcher
     */
    protected IDBSchemaInfoFetcher createFetcher() {
        if (hisFirst) {
        	
        	// TODO: ANALYZER When the core function is completed, it will be activated
            //return new MysqlXmlDumpSchemaWithHistoryFetcher();
        }
//        return super.createFetcher();
        return new XMLDirSchemaFetcher();
    }

    /**
     * Initialize the <IProgressMonitor> and create thread to run <IDBSchemaInfoFetcher>
     *
     * @param fetcher to be executed.
     * @param pm IProgressMonitor
     * @return The fetching thread
     */
    protected Thread startFetchingThread(
            final IDBSchemaInfoFetcher fetcher, final IProgressMonitor pm) {
        final XMLDirSource ds = (XMLDirSource) dbSource;
        String xmlFilePath = ds.getFilePath();
        File filePath = new File(xmlFilePath);
        boolean isPath = filePath.isDirectory();
        long length = 0;
        
        if (isPath)
        	length = (long) filePath.listFiles().length;
        
        final long factor = getFactor(length);
        final int pmLength = (int) (length / factor);
        final String name = Messages.progressMetadata;
        pm.beginTask(name, pmLength);

        final IReaderEvent readerEvent =
                new IReaderEvent() {
                    private long workCounter = 0;
                    private long progress = 0;
                    private long lastShowTime = 0;
                    private long startTime = 0;

                    public void readChars(final int count) {
                        final Runnable runnable =
                                new Runnable() {
                                    public void run() {
                                        workCounter = workCounter + count;
                                        long worked = workCounter / factor;
                                        if (worked == 0) {
                                            return;
                                        }
                                        workCounter = workCounter % factor;
                                        pm.worked((int) worked);

                                        progress = progress + worked;
                                        // Refresh time remaining per 2 seconds.
                                        if (startTime == 0) {
                                            startTime = System.currentTimeMillis();
                                            lastShowTime = System.currentTimeMillis();
                                            return;
                                        }
                                        if ((System.currentTimeMillis() - lastShowTime) < 2000) {
                                            return;
                                        }
                                        lastShowTime = System.currentTimeMillis();
                                        final long timeUsed = lastShowTime - startTime;
                                        long msRemain =
                                                ((pmLength - progress) * timeUsed / progress)
                                                        / 1000;
                                        pm.setTaskName(
                                                name
                                                        + MessageFormat.format(
                                                                Messages.msgTimeRemaining,
                                                                msRemain));
                                    }
                                };
                        Display.getDefault().syncExec(runnable);
                    }
                };
        Thread thread =
                new Thread(Messages.msgFetchingXMLSchema) {
                    public void run() {
                        try {
                        	XMLDirSource tempDs = ((XMLDirSource) dbSource).clone();
                            tempDs.setEvent(readerEvent);
                            catalog = fetcher.fetchSchema(tempDs, null);
                        } catch (Exception ex) {
                            exception = ex;
                        } finally {
                            isFinished = true;
                        }
                    }
                };
        thread.start();
        return thread;
    }

    /**
     * Retrieves the factor used by file length shown.
     *
     * @param len file length
     * @return 1:Byte 1024:KB 1024*1024=MB ...
     */
    protected long getFactor(long len) {
        long factor = 1;
        while ((len / factor) > Integer.MAX_VALUE) {
            factor = factor * 1024;
        }
        return factor;
    }

    /**
     * return Catalog object, the new catalog object will be added into <CubridNodeManager>
     * automatically
     *
     * @param ds ConnParameters
     * @param hisFirst boolean
     * @return Catalog
     */
    public static Catalog fetch(IDBSource ds, boolean hisFirst) {
    	XMLDirSchemaProgressFetcher runnable =
                new XMLDirSchemaProgressFetcher(hisFirst);
        runnable.setDBSource(ds);
        Catalog catalog = runnable.fetch();
        return catalog;
    }
}
