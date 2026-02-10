package com.cubrid.sqlanalyzer.ui.swt.table;

import org.eclipse.nebula.jface.gridviewer.GridTableViewer;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class GridPaintListener implements PaintListener {

	GridTableViewer tvOverview;
	
	int selectCount, insertCount, updateCount, deleteCount;
	
	@Override
	public void paintControl(PaintEvent e) {
		// TODO Auto-generated method stub
			Grid grid = tvOverview.getGrid();
			
			Font groupHeaderFont = (Font) grid.getData("GROUP_FONT");
			if (groupHeaderFont == null) {
				FontData[] fontData = grid.getFont().getFontData();
				for (FontData fd : fontData) {
					fd.setHeight(fd.getHeight() + 2);
					fd.setStyle(SWT.BOLD);
				}
				
				groupHeaderFont = new Font(Display.getDefault(), fontData);
				grid.setData("GROUP_FONT", groupHeaderFont);
				
				Font finalFont = groupHeaderFont;
				grid.addDisposeListener(event -> {
					if (finalFont != null && !finalFont.isDisposed()) {
						finalFont.dispose();
					}
				});
			}
			
			if (grid.getColumnCount() >= 2) {
				grid.getColumn(0).setAlignment(SWT.CENTER);
				grid.getColumn(1).setAlignment(SWT.CENTER);
				grid.getColumn(0).setVerticalAlignment(SWT.CENTER);
				grid.getColumn(1).setVerticalAlignment(SWT.CENTER);
			}
			
            // span select
			if (selectCount > 0 && 0 < grid.getItemCount()) {
				GridItem selectItem = grid.getItem(0);
				selectItem.setRowSpan(0, (int)(selectCount - 1));
				selectItem.setRowSpan(1, (int)(selectCount - 1));

                selectItem.setFont(0, groupHeaderFont);
                selectItem.setFont(1, groupHeaderFont);
			}
			
			// span insert
			int insertStartIndex = (int)selectCount;
			if (insertCount > 0 && insertStartIndex < grid.getItemCount()) {
				GridItem insertItem = grid.getItem(insertStartIndex);
				insertItem.setRowSpan(0, (int)(insertCount - 1));
				insertItem.setRowSpan(1, (int)(insertCount - 1));

                insertItem.setFont(0, groupHeaderFont);
                insertItem.setFont(1, groupHeaderFont);
			}
			
			// span delete
			int deleteStartIndex = (int)(selectCount + insertCount);
			if (deleteCount > 0 && deleteStartIndex < grid.getItemCount()) {
				GridItem deleteItem = grid.getItem(deleteStartIndex);
				deleteItem.setRowSpan(0, (int)(deleteCount - 1));
				deleteItem.setRowSpan(1, (int)(deleteCount - 1));

                deleteItem.setFont(0, groupHeaderFont);
                deleteItem.setFont(1, groupHeaderFont);
			}
			
			// span update
			int updateStartIndex = (int)(selectCount + insertCount + deleteCount);
			if (updateCount > 0 && updateStartIndex < grid.getItemCount()) {
				GridItem updateItem = grid.getItem(updateStartIndex);
				updateItem.setRowSpan(0, (int)(updateCount - 1));
				updateItem.setRowSpan(1, (int)(updateCount - 1));

                updateItem.setFont(0, groupHeaderFont);
                updateItem.setFont(1, groupHeaderFont);
			}

            // 그룹(SELECT/INSERT/DELETE/UPDATE) 경계선만 별도 스타일로 강조(기본 Grid 라인 굵기/색상은 행별로 제어 불가)
            // - Paint에서 원하는 위치에만 "덧그리기" 방식으로 구현
            // - RowSpan을 쓰고 있으므로, "그룹의 마지막 행 아래"에만 그리는 게 가장 안전함
            Rectangle area = grid.getClientArea();
            Color separatorColor =
                    Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
            int[] boundaryIndices =
                    new int[] {
                        (int) selectCount,
                        (int) (selectCount + insertCount),
                        (int) (selectCount + insertCount + deleteCount),
                        (int) (selectCount + insertCount + deleteCount + updateCount)
                    };

            int oldLineWidth = e.gc.getLineWidth();
            Color oldForeground = e.gc.getForeground();
            e.gc.setForeground(separatorColor);
            e.gc.setLineWidth(2);

            for (int b : boundaryIndices) {
                // b는 "다음 그룹의 시작 index"이므로, 경계선을 그릴 대상은 (b-1) 행의 하단
                if (b <= 0) {
                    continue;
                }
                int itemCount = grid.getItemCount();
                int lastIndex = Math.min(b - 1, itemCount - 1);
                if (lastIndex < 0 || lastIndex >= itemCount) {
                    continue;
                }
                GridItem lastItemOfGroup = grid.getItem(lastIndex);
                // 첫 번째 컬럼 기준 bounds로 y를 잡고, 전체 폭으로 라인을 긋는다
                Rectangle cellBounds = lastItemOfGroup.getBounds(0);
                int y = cellBounds.y + cellBounds.height - 1;
                e.gc.drawLine(area.x, y, area.x + area.width, y);
            }

            e.gc.setLineWidth(oldLineWidth);
            e.gc.setForeground(oldForeground);
		}

}
