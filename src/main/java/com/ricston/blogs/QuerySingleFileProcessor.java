package com.ricston.blogs;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.Callable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.sftp.SftpConnector;
import org.mule.transport.sftp.SftpFileArchiveInputStream;
import org.mule.transport.sftp.SftpInputStream;
import org.mule.transport.sftp.SftpReceiverRequesterUtil;
import org.mule.transport.sftp.notification.SftpNotifier;

public class QuerySingleFileProcessor implements Callable, Startable {
	
	private SftpConnector sftpConnector;
	private ImmutableEndpoint immutableEndpoint;
	
	private SftpReceiverRequesterUtil util;

	@Inject
	private MuleContext muleContext;

	@Override
	public void start() throws MuleException {
		// Lookup the SFTPConnector from the Mule Registry
		sftpConnector = (SftpConnector) this.muleContext.getRegistry().lookupConnector("SftpConnector");
		final EndpointBuilder endpointBuilder = this.muleContext.getRegistry().lookupObject("QuerySingleFileEndpoint");
		
		// Build an inbound endpoint based on the properties supplied in the global endpoint.
		this.immutableEndpoint = endpointBuilder.buildInboundEndpoint();
		this.util = new SftpReceiverRequesterUtil(immutableEndpoint);
	}

	@Override
	public Object onCall(MuleEventContext eventContext) throws Exception {
	    // Create a notifier which will notify notification subscribers of this SFTP event.
		final SftpNotifier notifier = new SftpNotifier(sftpConnector, eventContext.getMessage(), immutableEndpoint, eventContext.getFlowConstruct().getName());
		
		// Obtain the name of the file we want to download from a flow variable.
		final String fileName = eventContext.getMessage().getProperty("fileName", PropertyScope.INVOCATION);
		if (StringUtils.isBlank(fileName)) {
			throw new Exception("The flow variable 'fileName' cannot be null since it indicates which file to retrieve from the SFTP server.");
		}
		
		try {
		    // Download the file
			final InputStream inputStream = util.retrieveFile(fileName, notifier);
			
			// We only do this because org.mule.transport.sftp.SftpStream is package-private,
			// but the subclasses are public.
			if (inputStream instanceof SftpFileArchiveInputStream) {
				final SftpFileArchiveInputStream sftpArchiveInputStream = (SftpFileArchiveInputStream) inputStream;
				sftpArchiveInputStream.performPostProcessingOnClose(this.shouldDeleteFile());
			}
			
			if (inputStream instanceof SftpInputStream) {
				final SftpInputStream sftpInputStream = (SftpInputStream) inputStream;
				sftpInputStream.performPostProcessingOnClose(this.shouldDeleteFile());
			}
			
			return inputStream;
		}
		catch (IOException e) {
			throw new DefaultMuleException(MessageFactory.createStaticMessage("An IOException was thrown whilst attempting to download '%s'", fileName), e);
		}
	}
	
	/**
	 * A method that simply checks whether a file should be deleted or not based on the 'autoDelete' property
	 * defined on the connector or global endpoint. If you want to dynamically resolve this based on the Mule message,
	 * you can add it as a parameter.
	 */
	protected boolean shouldDeleteFile() {
		final String endpointAutoDelete = (String) immutableEndpoint.getProperty("autoDelete");
		final boolean connectorAutoDelete = sftpConnector.isAutoDelete(); 
		return connectorAutoDelete || Boolean.valueOf(endpointAutoDelete);
	}
	
}
