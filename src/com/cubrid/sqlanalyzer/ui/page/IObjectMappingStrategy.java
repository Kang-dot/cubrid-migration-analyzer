package com.cubrid.sqlanalyzer.ui.page;

import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.swt.widgets.Composite;

/**
 * Object Mapping Page의 View와 로직을 분리하기 위한 전략 인터페이스
 */
public interface IObjectMappingStrategy {

    void createControl(Composite parent);

    Composite getContainer();

    void afterShowCurrentPage();

    boolean handlePageLeaving(PageChangingEvent event);
}
