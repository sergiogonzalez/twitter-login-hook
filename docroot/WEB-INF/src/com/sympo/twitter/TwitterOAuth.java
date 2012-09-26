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

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.struts.BaseStrutsAction;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.expando.model.ExpandoColumn;
import com.liferay.portlet.expando.model.ExpandoTable;
import com.liferay.portlet.expando.model.ExpandoTableConstants;
import com.liferay.portlet.expando.model.ExpandoValue;
import com.liferay.portlet.expando.service.ExpandoColumnLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoTableLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoValueLocalServiceUtil;

import com.sympo.twitter.constants.TwitterConstants;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

/**
 * @author Sergio González
 */
public class TwitterOAuth extends BaseStrutsAction {

	public String execute(
			HttpServletRequest request, HttpServletResponse response)
		throws Exception {

		HttpSession session = request.getSession();

		ThemeDisplay themeDisplay = (ThemeDisplay)request.getAttribute(
				WebKeys.THEME_DISPLAY);

		String twitterApiKey = PropsUtil.get("twitter.api.key");
		String twitterApiSecret = PropsUtil.get("twitter.api.secret");

		OAuthService service = new ServiceBuilder().provider(TwitterApi.class)
		.apiKey(twitterApiKey).apiSecret(twitterApiSecret).build();

		String oauthVerifier = ParamUtil.getString(request, "oauth_verifier");
		String oauthToken = ParamUtil.getString(request, "oauth_token");

		if (Validator.isNull(oauthVerifier) || Validator.isNull(oauthToken)) {
			return null;
		}

		Verifier v = new Verifier(oauthVerifier);

		Token requestToken = new Token(oauthToken, twitterApiSecret);

		Token accessToken = service.getAccessToken(requestToken, v);

		String verifyCredentialsURL = PropsUtil.get(
			"twitter.api.verify.credentials.url");

		OAuthRequest authrequest = new OAuthRequest(
			Verb.GET, verifyCredentialsURL);

		service.signRequest(accessToken, authrequest);

		String bodyResponse = authrequest.send().getBody();

		Document document = SAXReaderUtil.read(bodyResponse);

		Element rootElement = document.getRootElement();

		Element idElement = rootElement.element("id");

		String twitterId = idElement.getStringValue();

		ExpandoTable expandoTable = ExpandoTableLocalServiceUtil.getTable(
			User.class.getName(), ExpandoTableConstants.DEFAULT_TABLE_NAME);

		ExpandoColumn expandoColumn = ExpandoColumnLocalServiceUtil.getColumn(
			expandoTable.getTableId(), "twitterId");

		List<ExpandoValue> expandoValues =
			ExpandoValueLocalServiceUtil.getColumnValues(
				expandoColumn.getCompanyId(), User.class.getName(),
				ExpandoTableConstants.DEFAULT_TABLE_NAME, "twitterId",
				twitterId, QueryUtil.ALL_POS, QueryUtil.ALL_POS);

		int usersCount = expandoValues.size();

		if (usersCount == 1) {
			ExpandoValue expandoValue = expandoValues.get(0);

			long userId = expandoValue.getClassPK();

			User user = UserLocalServiceUtil.getUser(userId);

			session.setAttribute(
				TwitterConstants.TWITTER_ID_LOGIN, user.getUserId());

			String redirect =
				PortalUtil.getPortalURL(request) + themeDisplay.getURLSignIn();

			response.sendRedirect(redirect);

			return null;
		}
		else if (usersCount > 1) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"There is more than 1 user with the same Twitter Id");
			}
		}

		Element nameElement = rootElement.element("name");
		Element screenNameElement = rootElement.element("screen_name");

		String userName = nameElement.getStringValue();
		String screenName = screenNameElement.getStringValue();

		session.setAttribute(
			TwitterConstants.TWITTER_LOGIN_PENDING, Boolean.TRUE);

		String createAccountURL = PortalUtil.getCreateAccountURL(
			request, themeDisplay);

		createAccountURL = HttpUtil.setParameter(
			createAccountURL, "firstName", userName);
		createAccountURL = HttpUtil.setParameter(
				createAccountURL, "screenName", screenName);
		createAccountURL = HttpUtil.setParameter(
				createAccountURL, "twitterId", twitterId);

		response.sendRedirect(createAccountURL);

		return null;
	}

	private static Log _log = LogFactoryUtil.getLog(TwitterOAuth.class);

}