/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/
package no.nordicsemi.android.log;

import android.app.Activity;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * The log session. This object can be created with the use of {@link no.nordicsemi.android.log.Logger#newSession(android.content.Context, String, String)} and is used to append new log entries to the Master Control Panel log.
 */
public class LogSession {
	/* package */final Context context;
	/* package */final Uri sessionUri;

	/* package */LogSession(final Context context, final Uri sessionUri) {
		this.context = context;
		this.sessionUri = sessionUri;
	}

	/**
	 * Returns the session {@link android.net.Uri}. The Uri may be saved in {@link android.app.Activity#onSaveInstanceState(android.os.Bundle)} to recreate the session using {@link no.nordicsemi.android.log.Logger#openSession(android.content.Context, android.net.Uri)} when
	 * orientation change. Use this Uri also to open the log session in the nRF Logger.
	 * 
	 * <pre>
	 * Intent intent = new Intent(Intent.ACTION_VIEW, mLogSession.getSessionUri());
	 * startActivity(intent);
	 * </pre>
	 * 
	 * @return the session Uri
	 */
	public Uri getSessionUri() {
		return sessionUri;
	}

	/**
	 * Returns the session entries {@link android.net.Uri}. New log entries may be inserted using this Uri.
	 * 
	 * @return the session entries Uri
	 */
	public Uri getSessionEntriesUri() {
		return sessionUri.buildUpon().appendEncodedPath(LogContract.Log.CONTENT_DIRECTORY).build();
	}

	/**
	 * Returns the {@link android.net.Uri} that may by used to obtain all sessions created by the same application (and the same profile) as this session. It may be used to open the list of log sessions in the
	 * nRF Logger application or to obtain list of sessions using {@link android.content.ContentProvider}. Keep in mind that sessions with {@link no.nordicsemi.android.log.LogContract.Session#NUMBER} equal to 0 are "date sessions". Date
	 * sessions are created each time a new session is being added by the application (and profile) in a new day. See {@link no.nordicsemi.android.log.Logger} for more information.
	 * 
	 * <pre>
	 * Intent intent = new Intent(Intent.ACTION_VIEW, mLogSession.getSessionsUri());
	 * startActivity(intent);
	 * </pre>
	 * 
	 * @return the Uri for all sessions created by the app used to create this session or <code>null</code> if the session Uri is invalid or the owner app data does not exist in the database
	 */
	public Uri getSessionsUri() {
		try {
			final Cursor cursor = context.getContentResolver().query(sessionUri, new String[] { LogContract.Session.APPLICATION_ID }, null, null, null);
			try {
				if (cursor.moveToNext()) {
					final long appId = cursor.getLong(0);
					return LogContract.Session.createSessionsUri(appId);
				}
				return null;
			} finally {
				cursor.close();
			}
		} catch (final Exception e) {
			return null;
		}
	}

	/**
	 * Returns the session read-only content Uri. It can be used to obtain all log entries in a single row (as a String field) with fixed syntax:<br/>
	 * f.e.:
	 * 
	 * <pre>
	 * [Application name], [Creation date]
	 * [Session name] ([Session key])
	 * D	10.34.01.124	This is the oldest log message (debug)
	 * V	10.34.02.238	This is the log message (verbose)
	 * I	10.34.03.527	This is the log message (info)
	 * W	10.34.04.812	This is the log message (warning)
	 * E	10.34.05.452	This is the log message (error)
	 * </pre>
	 * 
	 * @return the {@link android.net.Uri} that can be read using {@link android.content.ContentResolver#query(android.net.Uri, String[], String, String[], String)} method. The value will be in the first row, column number 0 (with id:
	 *         {@link no.nordicsemi.android.log.LogContract.Session.Content#CONTENT}).
	 */
	public Uri getSessionContentUri() {
		return sessionUri.buildUpon().appendEncodedPath(LogContract.Log.CONTENT_DIRECTORY).appendEncodedPath(LogContract.Session.Content.CONTENT).build();
	}

	@Override
	public String toString() {
		return sessionUri.toString();
	}
}
