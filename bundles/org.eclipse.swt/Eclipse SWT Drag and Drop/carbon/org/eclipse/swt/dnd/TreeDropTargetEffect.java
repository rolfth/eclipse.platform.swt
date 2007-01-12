/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.dnd;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.Callback;
import org.eclipse.swt.internal.carbon.DataBrowserCallbacks;
import org.eclipse.swt.internal.carbon.OS;
import org.eclipse.swt.widgets.*;

/*public*/ class TreeDropTargetEffect extends DropTargetEffect {
	static final int SCROLL_HYSTERESIS = 150; // milli seconds
	static final int EXPAND_HYSTERESIS = 300; // milli seconds

	int currentEffect = DND.FEEDBACK_NONE;
	TreeItem currentItem;

	TreeItem insertItem = null;
	boolean insertBefore = false;

	TreeItem scrollItem;
	long scrollBeginTime;

	TreeItem expandItem;
	long expandBeginTime;
	
	DataBrowserCallbacks callbacks = null;
	
	int acceptDragProc(int theControl, int itemID, int property, int theRect, int theDrag) {
		return (currentEffect & DND.FEEDBACK_SELECT) != 0 ? 1 : 0;
	}

	static Callback AcceptDragProc;
	static {
		AcceptDragProc = new Callback(TreeDropTargetEffect.class, "AcceptDragProc", 5); //$NON-NLS-1$
		int acceptDragProc = AcceptDragProc.getAddress();
		if (acceptDragProc == 0) SWT.error(SWT.ERROR_NO_MORE_CALLBACKS);
	}

	static int AcceptDragProc(int theControl, int itemID, int property, int theRect, int theDrag) {
		DropTarget target = FindDropTarget(theControl, theDrag);
		if (target == null) return 0;
		return (target.feedback & DND.FEEDBACK_SELECT) != 0 ? 1 : 0;
	}
	
	static DropTarget FindDropTarget(int theControl, int theDrag) {
		if (theControl == 0) return null;
		Display display = Display.findDisplay(Thread.currentThread());
		if (display == null || display.isDisposed()) return null;
		Widget widget = display.findWidget(theControl);
		if (widget == null) return null;
		return (DropTarget)widget.getData(DropTarget.DROPTARGETID); 
	}

	int checkEffect(int effect) {
		// Some effects are mutually exclusive.  Make sure that only one of the mutually exclusive effects has been specified.
		if ((effect & DND.FEEDBACK_SELECT) != 0) effect = effect & ~DND.FEEDBACK_INSERT_AFTER & ~DND.FEEDBACK_INSERT_BEFORE;
		if ((effect & DND.FEEDBACK_INSERT_BEFORE) != 0) effect = effect & ~DND.FEEDBACK_INSERT_AFTER;
		return effect;
	}

	boolean checkWidget(DropTargetEvent event) {
		return ((DropTarget) event.widget).getControl() instanceof Tree;
	}

	/**
	 * This implementation of <code>dragEnter</code> provides a default drag under effect
	 * for the feedback specified in <code>event.feedback</code>.
	 * 
	 * For additional information see <code>DropTargetAdapter.dragEnter</code>.
	 * 
	 * Subclasses that override this method should call <code>super.dragEnter(event)</code>
	 * to get the default drag under effect implementation.
	 *
	 * @param event  the information associated with the drag enter event
	 * 
	 * @see DropTargetAdapter
	 * @see DropTargetEvent
	 */
	public void dragEnter(DropTargetEvent event) {
		if (!checkWidget(event)) return;
		if (callbacks == null) {
			Tree table = (Tree)((DropTarget)event.widget).getControl();
			DataBrowserCallbacks callbacks = new DataBrowserCallbacks ();
			OS.GetDataBrowserCallbacks (table.handle, callbacks);
			callbacks.v1_acceptDragCallback = AcceptDragProc.getAddress();
			OS.SetDataBrowserCallbacks(table.handle, callbacks);
		}
		insertItem = null;
		expandBeginTime = 0;
		expandItem = null;
		scrollBeginTime = 0;
		scrollItem = null;
	}

	/**
	 * This implementation of <code>dragLeave</code> provides a default drag under effect
	 * for the feedback specified in <code>event.feedback</code>.
	 * 
	 * For additional information see <code>DropTargetAdapter.dragLeave</code>.
	 * 
	 * Subclasses that override this method should call <code>super.dragLeave(event)</code>
	 * to get the default drag under effect implementation.
	 *
	 * @param event  the information associated with the drag leave event
	 * 
	 * @see DropTargetAdapter
	 * @see DropTargetEvent
	 */
	public void dragLeave(DropTargetEvent event) {
		if (!checkWidget(event)) return;
		Tree tree = (Tree)((DropTarget)event.widget).getControl();
		if (insertItem != null) {
			setInsertMark(tree, null, false);
			insertItem = null;
		}
		expandBeginTime = 0;
		expandItem = null;
		scrollBeginTime = 0;
		scrollItem = null;
	}

	/**
	 * This implementation of <code>dragOver</code> provides a default drag under effect
	 * for the feedback specified in <code>event.feedback</code>.
	 * 
	 * For additional information see <code>DropTargetAdapter.dragOver</code>.
	 * 
	 * Subclasses that override this method should call <code>super.dragOver(event)</code>
	 * to get the default drag under effect implementation.
	 *
	 * @param event  the information associated with the drag over event
	 * 
	 * @see DropTargetAdapter
	 * @see DropTargetEvent
	 * @see DND#FEEDBACK_SELECT
	 * @see DND#FEEDBACK_INSERT_BEFORE
	 * @see DND#FEEDBACK_INSERT_AFTER
	 * @see DND#FEEDBACK_SCROLL
	 */
	public void dragOver(DropTargetEvent event) {
		if (!checkWidget(event)) return;
		Tree tree = (Tree)((DropTarget)event.widget).getControl();
		TreeItem item = (TreeItem)getItem(tree, event.x, event.y);
		int effect = checkEffect(event.feedback);		
		if ((effect & DND.FEEDBACK_EXPAND) == 0) {
			expandBeginTime = 0;
			expandItem = null;
		} else {
			if (item != null && item.equals(expandItem) && expandBeginTime != 0) {
				if (System.currentTimeMillis() >= expandBeginTime) {
					if (item.getItemCount() > 0 && !item.getExpanded()) {
						Event e = new Event();
						e.x = event.x;
						e.y = event.y;
						e.item = item;
						e.time = (int) System.currentTimeMillis();
						tree.notifyListeners(SWT.Expand, e);
						if (item.isDisposed()) return;
						item.setExpanded(true);
					}
					expandBeginTime = 0;
					expandItem = null;
				}
			} else {
				expandBeginTime = System.currentTimeMillis() + EXPAND_HYSTERESIS;
				expandItem = item;
			}
		}
		
		if ((effect & DND.FEEDBACK_SCROLL) == 0) {
			scrollBeginTime = 0;
			scrollItem = null;
		} else {
			if (item != null && item.equals(scrollItem)  && scrollBeginTime != 0) {
				if (System.currentTimeMillis() >= scrollBeginTime) {
					Rectangle area = tree.getClientArea();
					int headerHeight = tree.getHeaderHeight();
					int itemHeight= tree.getItemHeight();
					Point pt = new Point(event.x, event.y);
					pt = tree.getDisplay().map(null, tree, pt);
					TreeItem nextItem = null;
					if (pt.y < area.y + headerHeight + 2 * itemHeight) {
						nextItem = previousItem(tree, item);
					}
					if (pt.y > area.y + area.height - 2 * itemHeight) {
						nextItem = nextItem(tree, item);
					}
					if (nextItem != null) tree.showItem(nextItem);
					scrollBeginTime = 0;
					scrollItem = null;
				}
			} else {
				scrollBeginTime = System.currentTimeMillis() + SCROLL_HYSTERESIS;
				scrollItem = item;
			}
		}
		
		if ((effect & DND.FEEDBACK_INSERT_AFTER) != 0 ||
			(effect & DND.FEEDBACK_INSERT_BEFORE) != 0) {
			if (currentItem != item || 
				 ((effect & DND.FEEDBACK_INSERT_AFTER) != (currentEffect & DND.FEEDBACK_INSERT_AFTER)) ||
				 ((effect & DND.FEEDBACK_INSERT_BEFORE) != (currentEffect & DND.FEEDBACK_INSERT_BEFORE))) { 
				setInsertMark(tree, item, (effect & DND.FEEDBACK_INSERT_BEFORE) != 0);
				currentEffect = effect;
				currentItem = item;
			}
		} else {
			setInsertMark(tree, null, false);
		}
		// save current effect for selection feedback
		((DropTarget)event.widget).feedback = effect;
	}

	Widget getItem(Tree tree, int x, int y) {
		Point coordinates = new Point(x, y);
		coordinates = tree.toControl(coordinates);
		TreeItem item = tree.getItem(coordinates);
		if (item == null) {
			Rectangle area = tree.getClientArea();
			if (area.contains(coordinates)) {
				// Scan across the width of the tree.
				for (int x1 = area.x; x1 < area.x + area.width; x1++) {
					Point pt = new Point(x1, coordinates.y);
					item = tree.getItem(pt);
					if (item != null) {
						break;
					}
				}
			}
		}
		return item;
	}

	TreeItem nextItem(Tree tree, TreeItem item) {
		if (item == null) return null;
		if (item.getExpanded()) return item.getItem(0);
		TreeItem childItem = item;
		TreeItem parentItem = childItem.getParentItem();
		int index = parentItem == null ? tree.indexOf(childItem) : parentItem.indexOf(childItem);
		int count = parentItem == null ? tree.getItemCount() : parentItem.getItemCount();
		while (true) {
			if (index + 1 < count) return parentItem == null ? tree.getItem(index + 1) : parentItem.getItem(index + 1);
			if (parentItem == null) return null;
			childItem = parentItem;
			parentItem = childItem.getParentItem();
			index = parentItem == null ? tree.indexOf(childItem) : parentItem.indexOf(childItem);
			count = parentItem == null ? tree.getItemCount() : parentItem.getItemCount();
		}
	}

	TreeItem previousItem(Tree tree, TreeItem item) {
		if (item == null) return null;
		TreeItem childItem = item;
		TreeItem parentItem = childItem.getParentItem();
		int index = parentItem == null ? tree.indexOf(childItem) : parentItem.indexOf(childItem);
		if (index == 0) return parentItem;
		TreeItem nextItem = parentItem == null ? tree.getItem(index-1) : parentItem.getItem(index-1);
		int count = nextItem.getItemCount();
		while (count > 0 && nextItem.getExpanded()) {
			nextItem = nextItem.getItem(count - 1);
			count = nextItem.getItemCount();
		}
		return nextItem;
	}

	void setInsertMark(Tree tree, TreeItem item, boolean before) {
		if (item == insertItem && before == insertBefore) return;
		insertItem = item;
		insertBefore = before;
		tree.setInsertMark(item, before);
	}
}
