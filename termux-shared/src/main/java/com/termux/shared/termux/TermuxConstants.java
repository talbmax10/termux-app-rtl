/*
 * Copyright (c) 2020-2026 The Termux:Shared authors.
 *
 * This file is part of the Termux project and is licensed under the GPLv3-only
 * license. See LICENSE.md for details.
 */

package com.termux.shared.termux;

/** Shared constants for the Termux app and its plugins. */
public final class TermuxConstants {
    private TermuxConstants() {}

    public static final String TERMUX_GITHUB_ORGANIZATION_NAME = "termux";
    public static final String TERMUX_GITHUB_ORGANIZATION_URL = "https://github.com/" + TERMUX_GITHUB_ORGANIZATION_NAME;
    public static final String FDROID_PACKAGES_BASE_URL = "https://f-droid.org/en/packages";

    public static final String TERMUX_APP_NAME = "Termux RTL";
    public static final String TERMUX_PACKAGE_NAME = "com.termux.rtl";
    public static final String TERMUX_GITHUB_REPO_NAME = "termux-app-rtl";
    public static final String TERMUX_GITHUB_REPO_URL = "https://github.com/talbmax10/termux-app-rtl";
    public static final String TERMUX_GITHUB_ISSUES_REPO_URL = TERMUX_GITHUB_REPO_URL + "/issues";
    public static final String TERMUX_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_PACKAGE_NAME;

    public static final String TERMUX_API_APP_NAME = "Termux:API";
    public static final String TERMUX_API_PACKAGE_NAME = TERMUX_PACKAGE_NAME + ".api";
    public static final String TERMUX_BOOT_APP_NAME = "Termux:Boot";
    public static final String TERMUX_BOOT_PACKAGE_NAME = TERMUX_PACKAGE_NAME + ".boot";
    public static final String TERMUX_FLOAT_APP_NAME = "Termux:Float";
    public static final String TERMUX_FLOAT_PACKAGE_NAME = TERMUX_PACKAGE_NAME + ".window";
    public static final String TERMUX_STYLING_APP_NAME = "Termux:Styling";
    public static final String TERMUX_STYLING_PACKAGE_NAME = TERMUX_PACKAGE_NAME + ".styling";
    public static final String TERMUX_TASKER_APP_NAME = "Termux:Tasker";
    public static final String TERMUX_TASKER_PACKAGE_NAME = TERMUX_PACKAGE_NAME + ".tasker";
    public static final String TERMUX_WIDGET_APP_NAME = "Termux:Widget";
    public static final String TERMUX_WIDGET_PACKAGE_NAME = TERMUX_PACKAGE_NAME + ".widget";

    public static final String TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH = "/data/data/" + TERMUX_PACKAGE_NAME;
    public static final String TERMUX_FILES_DIR_PATH = TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH + "/files";
    public static final String TERMUX_PREFIX_DIR_PATH = TERMUX_FILES_DIR_PATH + "/usr";
    public static final String TERMUX_BIN_PREFIX_DIR_PATH = TERMUX_PREFIX_DIR_PATH + "/bin";
    public static final String TERMUX_ETC_PREFIX_DIR_PATH = TERMUX_PREFIX_DIR_PATH + "/etc";
    public static final String TERMUX_HOME_DIR_PATH = TERMUX_FILES_DIR_PATH + "/home";
    public static final String TERMUX_APPS_DIR_PATH = TERMUX_FILES_DIR_PATH + "/apps";
}
