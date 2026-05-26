/*
 * Copyright (C) 2008 Search Solution Corporation.
 * Copyright (C) 2016 CUBRID Corporation.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * - Neither the name of the <ORGANIZATION> nor the names of its contributors
 *   may be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 */
package com.cubrid.sqlanalyzer.ui.page.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.osgi.service.prefs.BackingStoreException;

import com.cubrid.cubridmigration.ui.MigrationUIPlugin;
import com.cubrid.cubridmigration.ui.message.Messages;
import com.cubrid.sqlanalyzer.core.AnalyzerConfiguration;

/**
 * New wizard step 1. Select the type of data source and the type of the destination
 *
 * @author Kevin Cao
 */
public class AnalyzerSelectSrcTarTypePageView {

    private static final String TARGET_TYPE_KEY = "target_type";
    private static final String SOURCE_TYPE_KEY = "source_type";

    private Button btnOnlineTar;
    // private Button btnOfflineTar;
    private Button btnParserTar;

//    private Button btnOnlineCUBRIDSrc;
    private Button btnOnlineOracleSrc;
    private Button btnXMLSrc;

    private final List<Button> srcButtons = new ArrayList<Button>(4);

    private final List<Button> tarButtons = new ArrayList<Button>(6);

    public AnalyzerSelectSrcTarTypePageView(Composite parent) {
        Composite sectionClient = new Composite(parent, SWT.NONE);
        sectionClient.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        sectionClient.setLayout(new GridLayout(2, true));

        Group grpSrc = new Group(sectionClient, SWT.NONE);
        grpSrc.setLayout(new GridLayout());
        grpSrc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        grpSrc.setText(Messages.msgSrcType);

        btnOnlineOracleSrc =
                createSrcTarTypeBtn(
                        grpSrc, Messages.btnSrcOnlineOracleDB, Messages.btnSrcOnlineOracleDBDes);
        btnOnlineOracleSrc.setData(AnalyzerConfiguration.SOURCE_TYPE_DB);
        srcButtons.add(btnOnlineOracleSrc);
        
        btnOnlineOracleSrc.setEnabled(true);

        Label comSep = new Label(grpSrc, SWT.SEPARATOR | SWT.HORIZONTAL);
        {
            GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);
            gd.verticalIndent = 8;
            comSep.setLayoutData(gd);
        }

        btnXMLSrc =
                createSrcTarTypeBtn(
                        grpSrc, Messages.btnSrcMySQLDumpDB, Messages.btnSrcMySQLDumpDBDes);
        btnXMLSrc.setData(AnalyzerConfiguration.SOURCE_TYPE_XML);
        srcButtons.add(btnXMLSrc);
        
//        btnDumpSrc.setSelection(true);
        
        Group grpTar = new Group(sectionClient, SWT.NONE);
        grpTar.setLayout(new GridLayout());
        grpTar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        grpTar.setText(Messages.msgDestType);

        btnOnlineTar =
                createSrcTarTypeBtn(
                        grpTar, Messages.btnDestOnlineCUBRIDDB, Messages.btnDestOnlineCUBRIDDBDes);
        btnOnlineTar.setData(AnalyzerConfiguration.TARGET_TYPE_CUBRID);
        tarButtons.add(btnOnlineTar);

        comSep = new Label(grpTar, SWT.SEPARATOR | SWT.HORIZONTAL);
        comSep.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
        {
            GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);
            gd.verticalIndent = 8;
            comSep.setLayoutData(gd);
        }

        btnParserTar =
                createSrcTarTypeBtn(
                        grpTar, "Run local parser", "Run by local CUBRID parse library");
        btnParserTar.setData(AnalyzerConfiguration.TARGET_TYPE_PARSER);
        tarButtons.add(btnParserTar);

//        readDefaultTypes();
        setupDefault();
    }

    /**
     * Create button and description.
     *
     * @param parent Composite
     * @param name String
     * @param des String
     * @return button
     */
    private Button createSrcTarTypeBtn(Composite parent, String name, String des) {
        Button result = new Button(parent, SWT.RADIO);
        result.setText(name);
        result.setToolTipText(des);
        {
            GridData gd = new GridData(SWT.LEFT, SWT.TOP, false, false);
            gd.verticalIndent = 8;
            result.setLayoutData(gd);
        }
        return result;
    }

    /**
     * Retrieves the source type
     *
     * @return integer
     */
    public int getSourceType() {
        for (Button btn : srcButtons) {
            if (btn.getSelection()) {
                return (Integer) btn.getData();
            }
        }
        return AnalyzerConfiguration.SOURCE_TYPE_XML;
    }

    /**
     * Retrieves the target type
     *
     * @return integer
     */
    public int getTargetType() {
        for (Button btn : tarButtons) {
            if (btn.getSelection()) {
                return (Integer) btn.getData();
            }
        }
        return AnalyzerConfiguration.TARGET_TYPE_PARSER;
    }
    
    private void setupDefault() {
    	btnOnlineOracleSrc.setSelection(true);
    	btnParserTar.setSelection(true);
    }

    /** Set default types selection */
//    private void readDefaultTypes() {
//        IEclipsePreferences preferences =
//                new ConfigurationScope().getNode(MigrationUIPlugin.PLUGIN_ID);
//        String srcType =
//                preferences.get(
//                        SOURCE_TYPE_KEY, String.valueOf(MigrationConfiguration.SOURCE_TYPE_CUBRID));
//        String tarType =
//                preferences.get(
//                        TARGET_TYPE_KEY, String.valueOf(MigrationConfiguration.DEST_ONLINE));
//        try {
//            showCfg(Integer.valueOf(srcType), Integer.valueOf(tarType));
//        } catch (Exception ex) {
//            showCfg(MigrationConfiguration.SOURCE_TYPE_CUBRID, MigrationConfiguration.DEST_ONLINE);
//        }
//    }

    /**
     * Save UI to configuration
     *
     * @return error message
     */
    public String save() {
        save2Default();
        return "";
    }

    /** Save configuration to default. */
    private void save2Default() {
        try {
            IEclipsePreferences preferences =
                    new ConfigurationScope().getNode(MigrationUIPlugin.PLUGIN_ID);
            preferences.put(SOURCE_TYPE_KEY, String.valueOf(getSourceType()));
            preferences.put(TARGET_TYPE_KEY, String.valueOf(getTargetType()));
            preferences.flush();
        } catch (BackingStoreException e) {
//            LOG.error("", e);
        }
    }

    /**
     * Show the configuration's source type and target type
     *
     * @param srcType type of source
     * @param tarType type of target
     */
//    public void showCfg(int srcType, int tarType) {
//        boolean flag = false;
//        for (Button btn : srcButtons) {
//            btn.setSelection(false);
//            if (((Integer) btn.getData()).intValue() == srcType) {
//                btn.setSelection(true);
//                flag = true;
//            }
//        }
//        if (!flag) {
//            btnOnlineCUBRIDSrc.setSelection(true);
//        }
//        flag = false;
//        for (Button btn : tarButtons) {
//            btn.setSelection(false);
//            if (((Integer) btn.getData()).intValue() == tarType) {
//                btn.setSelection(true);
//                flag = true;
//            }
//        }
//        if (!flag) {
//            btnOnlineTar.setSelection(true);
//        }
//    }
}
