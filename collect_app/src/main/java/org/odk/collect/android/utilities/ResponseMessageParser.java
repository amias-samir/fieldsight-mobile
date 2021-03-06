package org.odk.collect.android.utilities;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import timber.log.Timber;

/**
 * Created by Jon Nordling on 2/21/17.
 * The purpose of this class is to handle the XML parsing
 * of the server responses
 */

public class ResponseMessageParser {
    private static final String MESSAGE_XML_TAG = "message";
    private static final String FIELDSIGHT_INSTANCE_ID_XML_TAG = "finstanceID";
    private final String httpResponse;
    private final int responseCode;
    private final String reasonPhrase;

    public boolean isValid;
    public String messageResponse;
    public String fieldSightInstanceId;

    public ResponseMessageParser(String httpResponse, int responseCode, String reasonPhrase) {
        this.httpResponse = httpResponse;
        this.responseCode = responseCode;
        this.reasonPhrase = reasonPhrase;
        this.messageResponse = parseXMLMessage();
        this.fieldSightInstanceId = parseForFieldSightInstanceId();
        if (messageResponse != null && fieldSightInstanceId != null) {
            this.isValid = true;
        }
    }

    public boolean isValid() {
        return this.isValid;
    }

    public String getFieldSightInstanceId() {
        return this.fieldSightInstanceId;
    }

    public String getMessageResponse() {
        return this.messageResponse;
    }

    public String parseXMLMessage() {
        String message = null;
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = dbFactory.newDocumentBuilder();
            Document doc = null;

            if (httpResponse.contains("OpenRosaResponse")) {
                doc = builder.parse(new ByteArrayInputStream(httpResponse.getBytes()));
                doc.getDocumentElement().normalize();

                if (doc.getElementsByTagName(MESSAGE_XML_TAG).item(0) != null) {
                    message = doc.getElementsByTagName(MESSAGE_XML_TAG).item(0).getTextContent();
                } else {
                    isValid = false;
                }
            }

            return message;

        } catch (SAXException | IOException | ParserConfigurationException e) {
            Timber.e(e, "Error parsing XML message due to %s ", e.getMessage());
            isValid = false;
        }

        return message;
    }

    private String parseForFieldSightInstanceId() {
        String fieldSightInstanceId = null;
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;

        try {
            builder = dbFactory.newDocumentBuilder();
            Document doc = null;

            if (httpResponse.contains("OpenRosaResponse")) {
                doc = builder.parse(new ByteArrayInputStream(httpResponse.getBytes()));
                doc.getDocumentElement().normalize();

                NodeList nList = doc.getElementsByTagName("submissionMetadata");
                try {
                    fieldSightInstanceId = ((Element) nList.item(0)).getAttribute(FIELDSIGHT_INSTANCE_ID_XML_TAG);
                } catch (Exception e) {
                    isValid = false;
                    Timber.e(e, "Failed to retrive fieldsight submission id from submission sucess xml");
                }

            }

            return fieldSightInstanceId;

        } catch (SAXException | IOException | ParserConfigurationException e) {
            Timber.e(e, "Error parsing XML message due to %s ", e.getMessage());
            isValid = false;
        }

        return fieldSightInstanceId;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }
}
