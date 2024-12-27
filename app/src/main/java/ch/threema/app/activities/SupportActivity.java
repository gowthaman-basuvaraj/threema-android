/*  _____ _
 * |_   _| |_  _ _ ___ ___ _ __  __ _
 *   | | | ' \| '_/ -_) -_) '  \/ _` |_
 *   |_| |_||_|_| \___\___|_|_|_\__,_(_)
 *
 * Threema for Android
 * Copyright (c) 2013-2024 Threema GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.threema.app.activities;

import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import ch.threema.app.R;
import ch.threema.app.utils.ConfigUtils;
import ch.threema.app.utils.LocaleUtil;
import ch.threema.app.utils.TestUtil;
import ch.threema.app.utils.UrlUtil;
import ch.threema.base.utils.LoggingUtil;

public class SupportActivity extends SimpleWebViewActivity {
	private static final Logger logger = LoggingUtil.getThreemaLogger("SupportActivity");

	@Override
	protected boolean requiresConnection() {
		return true;
	}

	@Override
	protected boolean requiresJavaScript() {
		return true;
	}

	@Override
	protected int getWebViewTitle() {
		return R.string.support;
	}

	@Override
	protected String getWebViewUrl() {
		String baseURL = null;

		if(ConfigUtils.isWorkBuild()) {
			baseURL = preferenceService.getCustomSupportUrl();
		}

		if(TestUtil.isEmptyOrNull(baseURL)) {
			baseURL = getString(R.string.support_url);
		}

		return baseURL + "?lang=" + LocaleUtil.getAppLanguage()
			+ "&version=" + UrlUtil.urlencode(ConfigUtils.getDeviceInfo(true))
			+ "&identity=" + getIdentity();
	}

	private String getIdentity() {
		try {
			return URLEncoder.encode(serviceManager.getUserService().getIdentity(), LocaleUtil.UTF8_ENCODING);
		} catch (UnsupportedEncodingException e) {
			logger.error("Encoding exception", e);
		}
		return "";
	}
}
