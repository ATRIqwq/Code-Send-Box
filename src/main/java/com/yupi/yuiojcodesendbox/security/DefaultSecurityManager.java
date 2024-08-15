package com.yupi.yuiojcodesendbox.security;

import java.security.Permission;

public class DefaultSecurityManager extends SecurityManager{
    @Override
    public void checkPermission(Permission perm) {
        System.out.println("默认开放所有权限");
        super.checkPermission(perm);
    }
}
