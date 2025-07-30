package sqlanalyzer.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.cubrid.sqlanalyzer.ui.AnalyzerWizardDialog;
import com.cubrid.sqlanalyzer.ui.PageHandler;

import org.eclipse.jface.dialogs.MessageDialog;

public class SampleHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
//		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
//		MessageDialog.openInformation(
//				window.getShell(),
//				"SQLAnalyzer",
//				"Hello, Eclipse world");
		
		PageHandler.newMigrationWizard();
		return null;
	}
}
