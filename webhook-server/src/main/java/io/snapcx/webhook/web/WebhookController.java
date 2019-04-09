package io.snapcx.webhook.web;

import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import io.snapcx.webhook.dto.Notifications;
import io.snapcx.webhook.integrations.MailChimp;

@RestController
public class WebhookController {

	protected final Log logger = LogFactory.getLog(getClass());

	@Autowired
	@Qualifier("mailChimp")
	private MailChimp mailChimp;

	@RequestMapping(value = "/notifications", method = RequestMethod.GET, produces = { MediaType.TEXT_PLAIN_VALUE,
			MediaType.APPLICATION_JSON_VALUE }, consumes = { MediaType.APPLICATION_XML_VALUE,
					MediaType.APPLICATION_JSON_VALUE })
	public List<String> getNotifications() {
		List<String> notifications = this.mailChimp.getAllNotification();
		Notifications result = new Notifications(notifications);
		return notifications;
	}
	
	@RequestMapping(value = "/notification", method = RequestMethod.GET, produces = { MediaType.TEXT_PLAIN_VALUE,
			MediaType.APPLICATION_JSON_VALUE }, consumes = { MediaType.APPLICATION_XML_VALUE,
					MediaType.APPLICATION_JSON_VALUE })
	public String getNotification(){
		return this.mailChimp.getQueueNotification();
	}

	@RequestMapping(value = "/notifications", method = RequestMethod.POST, produces = { MediaType.TEXT_PLAIN_VALUE,
			MediaType.APPLICATION_JSON_VALUE }, consumes = { MediaType.APPLICATION_XML_VALUE,
					MediaType.APPLICATION_JSON_VALUE })
	public String process3ScaleToMailChimp(@RequestBody(required = true) String threeScaleRequest) {
		long startTime = System.currentTimeMillis();
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(threeScaleRequest));
			Document xmlDom = docBuilder.parse(is);
			String orgName = null;
			String email = null;
			String name = null;
			if (null != xmlDom) {
				Element eventNode = xmlDom.getDocumentElement();
				Element objectNode = DomUtils.getChildElementByTagName(eventNode, "object");
				if (null != objectNode) {
					Element accountNode = DomUtils.getChildElementByTagName(objectNode, "account");
					if (null != accountNode) {
						Element orgNameNode = DomUtils.getChildElementByTagName(accountNode, "org_name");
						orgName = DomUtils.getTextValue(orgNameNode);
					}
					Element usersNode = DomUtils.getChildElementByTagName(accountNode, "users");
					if (null != usersNode) {
						Element userNode = DomUtils.getChildElementByTagName(usersNode, "user");
						name = DomUtils.getChildElementValueByTagName(userNode, "name");
						email = DomUtils.getChildElementValueByTagName(userNode, "email");
					}
				}
			}
			//orgName = "vaibhav";
			//email = "vaibhav@gmail.com";
			if (null != orgName && null != email) {
				String response = this.mailChimp.addNewMemberToList(email, orgName, name, "");
				// return response;
			} else {
				throw new Exception("Something wrong in parsing XML request and getting values");
			}
		} catch (Exception ex) {
			this.logger.error("3ScaleToMailChimp : Caught exception at rest controller.", ex);
			return "FAIL";
		}

		long endTime = System.currentTimeMillis();
		String responseTime = (endTime - startTime) + " msecs";
		this.logger.info("Time took for server to process this request is " + responseTime);
		return "SUCCESS";
	}
	
	@RequestMapping(value = "/ping", method = RequestMethod.GET)
	public String ping() {
		return "webhook_pong";
	}

	@RequestMapping(value = "/lists/{id}/members", method = RequestMethod.POST, produces = { MediaType.TEXT_PLAIN_VALUE,
			MediaType.APPLICATION_JSON_VALUE }, consumes = { MediaType.TEXT_PLAIN_VALUE,
					MediaType.APPLICATION_JSON_VALUE })
	public String result(@RequestBody(required = true) String threeScaleRequest) {
		return "webhook_pong result=" + threeScaleRequest;
	}

}
