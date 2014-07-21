package com.hello.ble;

import com.hello.ble.devices.Pill;

/**
 * Created by pangwu on 7/2/14.
 */
public interface PillOperationCallback<T> {
    public void onCompleted(final Pill connectedPill, final T data);
}
