package com.bayrex.bnox;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.UserManager;

public class AdminReceiver extends DeviceAdminReceiver {
    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        super.onProfileProvisioningComplete(context, intent);
        DevicePolicyManager policy = (DevicePolicyManager)
                context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(context, AdminReceiver.class);

        if (policy != null && policy.isProfileOwnerApp(context.getPackageName())) {
            policy.setProfileName(admin, "B-Nox");
            policy.addUserRestriction(admin, UserManager.DISALLOW_CROSS_PROFILE_COPY_PASTE);
            policy.addUserRestriction(admin, UserManager.DISALLOW_SHARE_INTO_MANAGED_PROFILE);
        }

        Intent launch = new Intent(context, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(launch);
    }
}
