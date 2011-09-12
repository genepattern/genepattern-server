package org.genomespace.auth.openId;

import org.openid4java.message.MessageExtension;
import org.openid4java.message.ParameterList;

public class GenomeSpaceMessageExtension implements MessageExtension {
	public static final String GS_TOKEN_URI = "http://identity.genomespace.org/token";
	public static final String GS_TOKEN_ALIAS = "genomespace-token";
	public static final String GS_USERNAME_ALIAS = "genomespace-username";

	private ParameterList paramList = new ParameterList();

	@Override
	public String getTypeUri() {
		return GS_TOKEN_URI;
	}

	@Override
	public ParameterList getParameters() {
		return paramList;
	}

	@Override
	public void setParameters(ParameterList params) {
		paramList = new ParameterList(params);
	}

	@Override
	public boolean providesIdentifier() {
		return false;
	}

	@Override
	public boolean signRequired() {
		return false;
	}

}
