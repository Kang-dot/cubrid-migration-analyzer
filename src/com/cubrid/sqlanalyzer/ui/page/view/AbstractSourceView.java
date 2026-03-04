package com.cubrid.sqlanalyzer.ui.page.view;

import com.cubrid.cubridmigration.core.dbobject.Catalog;
import org.eclipse.swt.widgets.Composite;

/**
 * AbstractSourceView
 *
 * @author Kevin Cao
 * @version 1.0 - 2013-6-3 created by Kevin Cao
 */
public interface AbstractSourceView {
	/**
	 * Create controls
	 *
	 * @param parent of the controls
	 */
	void createControls(Composite parent);

	/**
	 * Retrieves the catalog
	 *
	 * @return Catalog
	 */
	Catalog getCatalog();

	/** Hide view */
	void hide();

	/** Initialize the view */
	void init();

	/**
	 * check whether the dialog changed
	 *
	 * @return true if content changed
	 */
	boolean isInputChanged();

	/**
	 * Save to wizard
	 *
	 * @return true if successfully
	 */
	boolean save();

	/** Show view */
	void show();
}
