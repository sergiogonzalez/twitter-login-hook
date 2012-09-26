/**
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.sympo.twitter;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.security.auth.AutoLogin;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;

import com.sympo.twitter.constants.TwitterConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author Sergio González
 */
public class TwitterAutoLogin implements AutoLogin {

	public String[] login(
		HttpServletRequest request, HttpServletResponse response) {

		String[] credentials = null;

		try {
			long companyId = PortalUtil.getCompanyId(request);

			HttpSession session = request.getSession();

			Long userId = (Long)session.getAttribute(
				TwitterConstants.TWITTER_ID_LOGIN);

			if (userId == null) {
				return credentials;
			}

			session.removeAttribute(TwitterConstants.TWITTER_ID_LOGIN);

			User user = UserLocalServiceUtil.getUserById(userId);

			credentials = new String[3];

			credentials[0] = String.valueOf(user.getUserId());
			credentials[1] = user.getPassword();
			credentials[2] = Boolean.TRUE.toString();
		}
		catch (Exception e) {
			_log.error(e, e);
		}

		return credentials;
	}

	private static Log _log = LogFactoryUtil.getLog(TwitterAutoLogin.class);

}