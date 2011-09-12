package org.genomespace.auth.openId;

import org.openid4java.message.MessageException;
import org.openid4java.message.MessageExtension;
import org.openid4java.message.MessageExtensionFactory;
import org.openid4java.message.ParameterList;

public class GenomeSpaceMessageExtensionFactory implements MessageExtensionFactory {

	@Override
	public String getTypeUri() {
		return GenomeSpaceMessageExtension.GS_TOKEN_URI;
	}

	@Override
	public MessageExtension getExtension(ParameterList parameterList,
			boolean isRequest) throws MessageException {
		GenomeSpaceMessageExtension gsme = new GenomeSpaceMessageExtension();
		gsme.setParameters(parameterList);
		return gsme;
	}
}