package org.eclipse.swt.internal.motif;

/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 1998, 2000  All Rights Reserved
 */
 
public class XmDropProcCallback {
	public int   reason;         // the reason callback was called
	public int   event;          // event structure that triggered callback
	public int   timeStamp;      // timestamp of ;logical event
	public int   dragContext;    // DragContext widget associated with operation
	public short x;              // x-coordinate of pointer
	public short y;              // y-coordinate of pointer
	public byte  dropSiteStatus; // valid or invalid
	public byte  operation;      // current operation
	public byte  operations;     // supported operations
	public byte  dropAction;     // drop or help

	public static final int sizeof = 24;
}
