package com.hello.ble;

import com.google.common.base.Optional;

import java.util.List;

/**
 * Created by pangwu on 7/1/14.
 */
public interface PillDiscoveryCallback {
    public void onScanCompleted(final List<Pill> discoveredPills);
}
