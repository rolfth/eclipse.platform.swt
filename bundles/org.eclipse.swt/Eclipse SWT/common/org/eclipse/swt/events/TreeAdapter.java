package org.eclipse.swt.events;

/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 1998, 2001  All Rights Reserved
 */

/**
 * This adapter class provides default implementations for the
 * methods described by the <code>TreeListener</code> interface.
 * <p>
 * Classes that wish to deal with <code>TreeEvent</code>s can
 * extend this class and override only the methods which they are
 * interested in.
 * </p>
 *
 * @see TreeListener
 * @see TreeEvent
 */
public abstract class TreeAdapter implements TreeListener {

/**
 * Sent when a tree branch is collapsed.
 * The default behavior is to do nothing.
 *
 * @param e an event containing information about the tree operation
 */
public void treeCollapsed(TreeEvent e) {
}

/**
 * Sent when a tree branch is expanded.
 * The default behavior is to do nothing.
 *
 * @param e an event containing information about the tree operation
 */
public void treeExpanded(TreeEvent e) {
}
}
