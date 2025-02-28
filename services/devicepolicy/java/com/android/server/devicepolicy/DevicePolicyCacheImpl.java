/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.server.devicepolicy;

import android.annotation.UserIdInt;
import android.app.admin.DevicePolicyCache;
import android.app.admin.DevicePolicyManager;
import android.os.UserHandle;
import android.util.IndentingPrintWriter;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;

import com.android.internal.annotations.GuardedBy;

import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of {@link DevicePolicyCache}, to which {@link DevicePolicyManagerService} pushes
 * policies.
 *
 * TODO Move other copies of policies into this class too.
 */
public class DevicePolicyCacheImpl extends DevicePolicyCache {
    /**
     * Lock object. For simplicity we just always use this as the lock. We could use each object
     * as a lock object to make it more fine-grained, but that'd make copy-paste error-prone.
     */
    private final Object mLock = new Object();

    /**
     * Indicates which user is screen capture disallowed on. Can be {@link UserHandle#USER_NULL},
     * {@link UserHandle#USER_ALL} or a concrete user ID.
     */
    @GuardedBy("mLock")
    private int mScreenCaptureDisallowedUser = UserHandle.USER_NULL;

    /**
     * Indicates if screen capture is disallowed on a specific user or all users if
     * it contains {@link UserHandle#USER_ALL}.
     */
    @GuardedBy("mLock")
    private Set<Integer> mScreenCaptureDisallowedUsers = new HashSet<>();

    @GuardedBy("mLock")
    private final SparseIntArray mPasswordQuality = new SparseIntArray();

    @GuardedBy("mLock")
    private final SparseIntArray mPermissionPolicy = new SparseIntArray();

    /** Maps to {@code ActiveAdmin.mAdminCanGrantSensorsPermissions}.
     *
     * <p>For users affiliated with the device, they inherit the policy from {@code DO} so
     * it will map to the {@code DO}'s policy. Otherwise it will map to the admin of the requesting
     * user.
     */
    @GuardedBy("mLock")
    private final SparseBooleanArray mCanGrantSensorsPermissions = new SparseBooleanArray();

    public void onUserRemoved(int userHandle) {
        synchronized (mLock) {
            mPasswordQuality.delete(userHandle);
            mPermissionPolicy.delete(userHandle);
            mCanGrantSensorsPermissions.delete(userHandle);
        }
    }

    @Override
    public boolean isScreenCaptureAllowed(int userHandle) {
        synchronized (mLock) {
            return !mScreenCaptureDisallowedUsers.contains(userHandle)
                    && !mScreenCaptureDisallowedUsers.contains(UserHandle.USER_ALL);
        }
    }

    public int getScreenCaptureDisallowedUser() {
        synchronized (mLock) {
            return mScreenCaptureDisallowedUser;
        }
    }

    public void setScreenCaptureDisallowedUser(int userHandle) {
        synchronized (mLock) {
            mScreenCaptureDisallowedUser = userHandle;
        }
    }

    public void setScreenCaptureDisallowedUser(int userHandle, boolean disallowed) {
        synchronized (mLock) {
            if (disallowed) {
                mScreenCaptureDisallowedUsers.add(userHandle);
            } else {
                mScreenCaptureDisallowedUsers.remove(userHandle);
            }
        }
    }

    @Override
    public int getPasswordQuality(@UserIdInt int userHandle) {
        synchronized (mLock) {
            return mPasswordQuality.get(userHandle,
                    DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
        }
    }

    /** Updat the password quality cache for the given user */
    public void setPasswordQuality(int userHandle, int quality) {
        synchronized (mLock) {
            mPasswordQuality.put(userHandle, quality);
        }
    }

    @Override
    public int getPermissionPolicy(@UserIdInt int userHandle) {
        synchronized (mLock) {
            return mPermissionPolicy.get(userHandle,
                    DevicePolicyManager.PERMISSION_POLICY_PROMPT);
        }
    }

    /** Update the permission policy for the given user. */
    public void setPermissionPolicy(@UserIdInt int userHandle, int policy) {
        synchronized (mLock) {
            mPermissionPolicy.put(userHandle, policy);
        }
    }

    @Override
    public boolean canAdminGrantSensorsPermissionsForUser(@UserIdInt int userId) {
        synchronized (mLock) {
            return mCanGrantSensorsPermissions.get(userId, false);
        }
    }

    /** Sets ahmin control over permission grants for user. */
    public void setAdminCanGrantSensorsPermissions(@UserIdInt int userId, boolean canGrant) {
        synchronized (mLock) {
            mCanGrantSensorsPermissions.put(userId, canGrant);
        }
    }

    /** Dump content */
    public void dump(IndentingPrintWriter pw) {
        pw.println("Device policy cache:");
        pw.increaseIndent();
        pw.println("Screen capture disallowed users: " + mScreenCaptureDisallowedUsers);
        pw.println("Password quality: " + mPasswordQuality.toString());
        pw.println("Permission policy: " + mPermissionPolicy.toString());
        pw.println("Admin can grant sensors permission: "
                + mCanGrantSensorsPermissions.toString());
        pw.decreaseIndent();
    }
}
