package com.cubrid.sqlanalyzer.ui.editor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.cubrid.cubridmigration.core.common.TimeZoneUtils;
import com.cubrid.cubridmigration.cubrid.CUBRIDTimeUtil;
import com.cubrid.cubridmigration.ui.MigrationUIPlugin;
import com.cubrid.cubridmigration.ui.common.TextAppender;
import com.cubrid.cubridmigration.ui.message.Messages;
import com.cubrid.sqlanalyzer.ui.reporter.AnalyzerOverviewResult;
import com.cubrid.sqlanalyzer.ui.reporter.AnalyzerReport;
import com.cubrid.sqlanalyzer.ui.reporter.AnalyzerReporter;
import com.cubrid.sqlanalyzer.ui.swt.dialog.SimpleTextDialog;
import com.cubrid.sqlanalyzer.ui.swt.table.DetailTableBuilder;
import com.cubrid.sqlanalyzer.ui.swt.table.DetailTableLabelProvider;

public class AnalyzerReportEditorPart extends EditorPart {

    public static final String ID = AnalyzerReportEditorPart.class.getName();
    public static final String EMPTY_CELL_VALUE = "-";

    private TableViewer tvOverview;
    private TableViewer tvSelect;
    private TableViewer tvInsert;
    private TableViewer tvDelete;
    private TableViewer tvUpdate;
    private TableViewer tvObjDetails;
    private TableViewer tvTableRecords;

    private Text txtNonsupport;
    private Text txtLog;
    private Text txtConfigSummary;

    private TabFolder tfReport;
    private TabFolder tfDetail;

    private Label txtOuputDir;

    private AnalyzerReportUIController controller = new AnalyzerReportUIController();

    /** Summary data for each DML type */
    private static class DMLSummary {
        String type;
        long total;
        long error;
        String successRate;

        DMLSummary(String type, long total, long error) {
            this.type = type;
            this.total = total;
            this.error = error;
            if (total == 0) {
                this.successRate = "100%";
            } else {
                double rate = ((double) (total - error) / total) * 100;
                this.successRate = String.format("%.2f%%", rate);
            }
        }
    }

    /** Label provider for DML summary table */
    private static class DMLSummaryLabelProvider extends LabelProvider
            implements ITableLabelProvider {

        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            if (!(element instanceof DMLSummary)) {
                return "";
            }
            DMLSummary summary = (DMLSummary) element;
            switch (columnIndex) {
                case 0:
                    return summary.type;
                case 1:
                    return String.valueOf(summary.total);
                case 2:
                    return String.valueOf(summary.error);
                case 3:
                    return summary.successRate;
                default:
                    return "";
            }
        }
    }

    private TextAppender noSupportedAppender =
            new TextAppender() {

                public void append(String text) {
                    txtNonsupport.append(text);
                }
            };

    private TextAppender logAppender =
            new TextAppender() {

                public void append(String text) {
                    txtLog.append(text);
                }
            };

    /**
     * Create overview page
     *
     * @param tfReport parent
     */
    private void createOverviewPage(TabFolder tfReport) {
        TabItem tiOverview = new TabItem(tfReport, SWT.NONE);
        tiOverview.setText(Messages.lblOverview);

        Composite comOverview = new Composite(tfReport, SWT.NONE);
        tiOverview.setControl(comOverview);
        comOverview.setLayout(new GridLayout());
        comOverview.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        ScrolledComposite scComposite = new ScrolledComposite(comOverview, SWT.H_SCROLL);
        scComposite.setLayout(new GridLayout(1, false));
        scComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Composite comTime = new Composite(scComposite, SWT.NONE);
        comTime.setLayout(new GridLayout(8, false));
        comTime.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

        Label lblStartTime = new Label(comTime, SWT.NONE);
        lblStartTime.setLayoutData(new GridData(SWT.CENTER));
        lblStartTime.setText(Messages.lblStartTime);
        Label txtStartTime = new Label(comTime, SWT.NONE);
        txtStartTime.setLayoutData(new GridData(SWT.CENTER));
        AnalyzerReport report = getReporter().getReport();
        txtStartTime.setText(
                CUBRIDTimeUtil.defaultFormatMilin(new Date(report.getTotalStartTime())));
        final Color clrBlue = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
        txtStartTime.setForeground(clrBlue);

        Label lblEndTime = new Label(comTime, SWT.NONE);
        lblEndTime.setLayoutData(new GridData(SWT.CENTER));
        lblEndTime.setText(Messages.lblEndTime);
        Label txtEndTime = new Label(comTime, SWT.NONE);
        txtEndTime.setLayoutData(new GridData(SWT.CENTER));
        txtEndTime.setText(CUBRIDTimeUtil.defaultFormatMilin(new Date(report.getTotalEndTime())));
        txtEndTime.setForeground(clrBlue);

        Label lblTotalTime = new Label(comTime, SWT.NONE);
        lblTotalTime.setLayoutData(new GridData(SWT.CENTER));
        lblTotalTime.setText(Messages.lblTotalTimeSpend);
        Label txtTotalTime = new Label(comTime, SWT.NONE);
        txtTotalTime.setLayoutData(new GridData(SWT.CENTER));
        txtTotalTime.setText(
                TimeZoneUtils.format(report.getTotalEndTime() - report.getTotalStartTime()));
        txtTotalTime.setForeground(clrBlue);

//        if (controller.isFileOutputMigration(report)) {
//            Label lblOuputDir = new Label(comTime, SWT.NONE);
//            lblOuputDir.setLayoutData(new GridData(SWT.CENTER));
//            lblOuputDir.setText(Messages.lblOutputDir);
//            txtOuputDir = new Label(comTime, SWT.NONE);
//            txtOuputDir.setLayoutData(new GridData(SWT.CENTER));
//            final MigrationBriefReport brief = this.getReporter().getReport().getBrief();
//            txtOuputDir.setText(brief.getOutputDir());
//            txtOuputDir.setForeground(clrBlue);
//
//            txtOuputDir.setCursor(Resources.getInstance().getCursor(SWT.CURSOR_HAND));
//            txtOuputDir.addMouseListener(
//                    new MouseAdapter() {
//
//                        public void mouseDown(MouseEvent event) {
//                            Program.launch(txtOuputDir.getText());
//                        }
//                    });
//        }

        scComposite.setContent(comTime);
        scComposite.setExpandHorizontal(true);
        scComposite.setExpandVertical(true);
        scComposite.setMinSize(comTime.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        scComposite.layout(true);

        tvOverview =
                new TableViewer(
                        comOverview, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
        Table table = tvOverview.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        String[] titles = {
            Messages.colType, Messages.colTotal, Messages.colError, Messages.colProgress
        };
        int[] widths = {150, 150, 150, 150};

        for (int i = 0; i < titles.length; i++) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(titles[i]);
            column.setWidth(widths[i]);
            if (i > 0) {
                column.setAlignment(SWT.RIGHT);
            }
        }

        tvOverview.setContentProvider(new ArrayContentProvider());
        tvOverview.setLabelProvider(new DMLSummaryLabelProvider());

        tvOverview.addDoubleClickListener(
                new IDoubleClickListener() {
                    public void doubleClick(DoubleClickEvent event) {
                        if (event.getSelection().isEmpty()) {
                            return;
                        }
                        int index = tvOverview.getTable().getSelectionIndex();
                        
                        if (index < 0) {
                            return;
                        }
                        // Index mapping: 0: SELECT, 1: INSERT, 2: DELETE, 3: UPDATE
                        tfReport.setSelection(index + 1);
                    }
                });
    }

    public void createPartControl(Composite parent) {
        Composite backGroundCom = new Composite(parent, SWT.NONE);
        backGroundCom.setLayout(new GridLayout());
        backGroundCom.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        tfReport = new TabFolder(backGroundCom, SWT.NONE);
        tfReport.setLayout(new GridLayout());
        tfReport.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        createOverviewPage(tfReport);
        createSelectPage(tfReport);
        createInsertPage(tfReport);
        createDeletePage(tfReport);
        createUpdatePage(tfReport);

        setContent2Tables();
        tfReport.setSelection(0);
        tfReport.layout();
    }

    /**
     * Create select page
     *
     * @param tfReport parent
     */
    private void createSelectPage(TabFolder tfReport) {
        TabItem tiSelect = new TabItem(tfReport, SWT.NONE);
        tiSelect.setText("SELECT");

        Composite comSelect = new Composite(tfReport, SWT.NONE);
        tiSelect.setControl(comSelect);
        comSelect.setLayout(new GridLayout());
        comSelect.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        DetailTableBuilder tvBuilder = new DetailTableBuilder();
        tvBuilder.setContentProvider(new ArrayContentProvider());
        tvBuilder.setLabelProvider(new DetailTableLabelProvider());
        tvSelect =
                tvBuilder.buildTable(
                        comSelect, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
        addDMLTableDoubleClickListener(tvSelect);
    }

    /**
     * Add double click listener to DML detail table
     *
     * @param tv TableViewer
     */
    private void addDMLTableDoubleClickListener(TableViewer tv) {
        tv.addDoubleClickListener(
                new IDoubleClickListener() {
                    public void doubleClick(DoubleClickEvent event) {
                        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                        Object firstElement = selection.getFirstElement();
                        if (firstElement instanceof AnalyzerOverviewResult) {
                            AnalyzerOverviewResult result = (AnalyzerOverviewResult) firstElement;
                            StringBuilder sb = new StringBuilder();
                            sb.append(result.getQuery());
                            if (!result.isSuccess() && result.getErrorMessage() != null) {
                                sb.append("\n\n\nError:\n");
                                sb.append(result.getErrorMessage());
                            }
                            SimpleTextDialog dialog =
                                    new SimpleTextDialog(getSite().getShell(), sb.toString());
                            dialog.open();
                        }
                    }
                });
    }

    /**
     * Create insert page
     *
     * @param tfReport parent
     */
    private void createInsertPage(TabFolder tfReport) {
        TabItem tiInsert = new TabItem(tfReport, SWT.NONE);
        tiInsert.setText("INSERT");

        Composite comInsert = new Composite(tfReport, SWT.NONE);
        tiInsert.setControl(comInsert);
        comInsert.setLayout(new GridLayout());
        comInsert.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        DetailTableBuilder tvBuilder = new DetailTableBuilder();
        tvBuilder.setContentProvider(new ArrayContentProvider());
        tvBuilder.setLabelProvider(new DetailTableLabelProvider());
        tvInsert =
                tvBuilder.buildTable(
                        comInsert, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
        addDMLTableDoubleClickListener(tvInsert);
    }

    /**
     * Create delete page
     *
     * @param tfReport parent
     */
    private void createDeletePage(TabFolder tfReport) {
        TabItem tiDelete = new TabItem(tfReport, SWT.NONE);
        tiDelete.setText("DELETE");

        Composite comDelete = new Composite(tfReport, SWT.NONE);
        tiDelete.setControl(comDelete);
        comDelete.setLayout(new GridLayout());
        comDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        DetailTableBuilder tvBuilder = new DetailTableBuilder();
        tvBuilder.setContentProvider(new ArrayContentProvider());
        tvBuilder.setLabelProvider(new DetailTableLabelProvider());
        tvDelete =
                tvBuilder.buildTable(
                        comDelete, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
        addDMLTableDoubleClickListener(tvDelete);
    }

    /**
     * Create update page
     *
     * @param tfReport parent
     */
    private void createUpdatePage(TabFolder tfReport) {
        TabItem tiUpdate = new TabItem(tfReport, SWT.NONE);
        tiUpdate.setText("UPDATE");

        Composite comUpdate = new Composite(tfReport, SWT.NONE);
        tiUpdate.setControl(comUpdate);
        comUpdate.setLayout(new GridLayout());
        comUpdate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        DetailTableBuilder tvBuilder = new DetailTableBuilder();
        tvBuilder.setContentProvider(new ArrayContentProvider());
        tvBuilder.setLabelProvider(new DetailTableLabelProvider());
        tvUpdate =
                tvBuilder.buildTable(
                        comUpdate, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
        addDMLTableDoubleClickListener(tvUpdate);
    }


    /**
     * Do no thing
     *
     * @param monitor IProgressMonitor
     */
    public void doSave(IProgressMonitor monitor) {
        // Do no thing

    }

    /** Do nothing */
    public void doSaveAs() {
        // Do no thing

    }

    /** @return MigrationReporter */
    private AnalyzerReporter getReporter() {
        return (AnalyzerReporter) getEditorInput();
    }

    /**
     * Initializes this editor with the given editor site and input.
     *
     * @param site the editor site
     * @param input the editor input
     * @exception PartInitException if this editor was not initialized successfully
     */
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        setTitleToolTip(input.getToolTipText());
        setTitleImage(MigrationUIPlugin.getImage("icon/exportReport.gif"));
    }

    /**
     * No changes
     *
     * @return false
     */
    public boolean isDirty() {
        return false;
    }

    /**
     * Don't save
     *
     * @return false
     */
    public boolean isSaveAsAllowed() {
        return false;
    }

    /** Save all report information to a directory. */
//    private void saveAllTableContent() {
//        DirectoryDialog fd = new DirectoryDialog(tfReport.getShell(), SWT.NONE);
//        String file = fd.open();
//        if (file == null) {
//            return;
//        }
//        String savedFiles = controller.saveReportToDirectory(getReporter(), file);
//        MessageDialog.openInformation(
//                getSite().getShell(),
//                Messages.msgInformation,
//                Messages.bind(Messages.msgReportSaved, savedFiles));
//    }

    /** Fill the data to tables. */
    private void setContent2Tables() {
        AnalyzerReporter reporter = getReporter();
        AnalyzerReport report = reporter.getReport();

        List<DMLSummary> summaryList = new ArrayList<>();
        summaryList.add(
                new DMLSummary(
                        "SELECT", report.getSelectTotalCount(), report.getSelectErrorCount()));
        summaryList.add(
                new DMLSummary(
                        "INSERT", report.getInsertTotalCount(), report.getInsertErrorCount()));
        summaryList.add(
                new DMLSummary(
                        "DELETE", report.getDeleteTotalCount(), report.getDeleteErrorCount()));
        summaryList.add(
                new DMLSummary(
                        "UPDATE", report.getUpdateTotalCount(), report.getUpdateErrorCount()));

        tvOverview.setInput(summaryList);

        setupDmlTableInput(tvSelect, report.getSelectResults(), "SELECT");
        setupDmlTableInput(tvInsert, report.getInsertResults(), "INSERT");
        setupDmlTableInput(tvDelete, report.getDeleteResults(), "DELETE");
        setupDmlTableInput(tvUpdate, report.getUpdateResults(), "UPDATE");
    }

    /**
     * Set the input for a DML table and mark the first item in the group.
     *
     * @param viewer the TableViewer to update
     * @param results the list of results
     * @param type the DML type name
     */
    private void setupDmlTableInput(
            TableViewer viewer, List<AnalyzerOverviewResult> results, String type) {
        if (results != null && !results.isEmpty()) {
            AnalyzerOverviewResult first = results.get(0);
            first.setFirstInGroup(true);
            first.setQueryType(type);
        }
        viewer.setInput(results);
    }

    /** Set focus event */
    public void setFocus() {
        // Do nothing here
    }
}
