package org.esupportail.esupsignature.service.interfaces.listsearch.impl;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPConnection;
import jakarta.xml.soap.SOAPConnectionFactory;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.interfaces.listsearch.UserList;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "global", name = "sympa-url")
public class SympaSoapUserList implements UserList {

    private static final String NAMESPACE = "urn:sympasoap";
    private static final String ENCODING = "http://schemas.xmlsoap.org/soap/encoding/";

    private final GlobalProperties globalProperties;
    private final MessageSource messageSource;

    public SympaSoapUserList(GlobalProperties globalProperties, MessageSource messageSource) {
        this.globalProperties = globalProperties;
        this.messageSource = messageSource;
    }

    @Override
    public String getName() {
        return "sympa-soap";
    }

    @Override
    public List<String> getUsersEmailFromList(String listName) {
        if (!StringUtils.hasText(listName)) {
            return new ArrayList<>();
        }
        String[] listAddress = listName.split("@", 2);
        if (listAddress.length == 2) {
            String domain = listAddress[1].toLowerCase(Locale.ROOT);
            String allowedDomain = globalProperties.getDomain().toLowerCase(Locale.ROOT);
            if (!domain.equals(allowedDomain) && !domain.endsWith("." + allowedDomain)) {
                return new ArrayList<>();
            }
        }
        return run("review", listAddress[0]);
    }

    @Override
    public List<String> getUsersEmailFromAliases(String listName) {
        return new ArrayList<>();
    }

    @Override
    public List<Map.Entry<String, String>> getListOfLists(String search) {
        return new ArrayList<>();
    }

    private List<String> run(String service, String... parameters) {
        try {
            SOAPMessage request = MessageFactory.newInstance().createMessage();
            request.getSOAPPart().getEnvelope().addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            request.getSOAPPart().getEnvelope().addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");
            request.getSOAPPart().getEnvelope().addNamespaceDeclaration("soapenc", ENCODING);
            SOAPElement operation = request.getSOAPBody().addChildElement("authenticateRemoteAppAndRun", "sympa", NAMESPACE);
            operation.setEncodingStyle(ENCODING);
            addString(operation, "appname", globalProperties.getSympaApp());
            addString(operation, "apppassword", globalProperties.getSympaToken());
            addString(operation, "vars", "unused=value");
            addString(operation, "service", service);
            SOAPElement arguments = operation.addChildElement("parameters");
            arguments.addAttribute(new QName("http://www.w3.org/2001/XMLSchema-instance", "type", "xsi"), "soapenc:Array");
            arguments.addAttribute(new QName(ENCODING, "arrayType", "soapenc"), "xsd:string[" + parameters.length + "]");
            for (String parameter : parameters) {
                addString(arguments, "item", parameter);
            }
            request.getMimeHeaders().setHeader("SOAPAction", "\"" + NAMESPACE + "#authenticateRemoteAppAndRun\"");
            request.saveChanges();
            SOAPConnection connection = SOAPConnectionFactory.newInstance().createConnection();
            try {
                SOAPMessage response = connection.call(request, globalProperties.getSympaUrl());
                if (response.getSOAPBody().hasFault()) {
                    if (service.equals("review")
                            && "Unknown list".equals(response.getSOAPBody().getFault().getFaultString())) {
                        return new ArrayList<>();
                    }
                    throw new SOAPException(response.getSOAPBody().getFault().getFaultCode() + ": "
                            + response.getSOAPBody().getFault().getFaultString());
                }
                NodeList returns = response.getSOAPBody().getElementsByTagNameNS("*", "return");
                if (returns.getLength() == 0) {
                    returns = response.getSOAPBody().getElementsByTagNameNS("*", "listInfo");
                }
                if (returns.getLength() != 1) {
                    throw new SOAPException(messageSource.getMessage("error.sympa.soap.response", null,
                            LocaleContextHolder.getLocale()));
                }
                Element result = resolve((Element) returns.item(0), response);
                List<String> values = new ArrayList<>();
                for (Node child = result.getFirstChild(); child != null; child = child.getNextSibling()) {
                    if (child instanceof Element) {
                        values.add(resolve((Element) child, response).getTextContent());
                    }
                }
                return values;
            } finally {
                connection.close();
            }
        } catch (SOAPException exception) {
            throw new EsupSignatureRuntimeException(messageSource.getMessage("error.sympa.soap", null,
                    LocaleContextHolder.getLocale()), exception);
        }
    }

    private void addString(SOAPElement parent, String name, String value) throws SOAPException {
        SOAPElement element = parent.addChildElement(name);
        element.addAttribute(new QName("http://www.w3.org/2001/XMLSchema-instance", "type", "xsi"), "xsd:string");
        element.addTextNode(value);
    }

    private Element resolve(Element element, SOAPMessage response) throws SOAPException {
        if (element.hasAttribute("href")) {
            String reference = element.getAttribute("href");
            NodeList elements = response.getSOAPBody().getElementsByTagName("*");
            for (int index = 0; index < elements.getLength(); index++) {
                Element candidate = (Element) elements.item(index);
                if (reference.equals("#" + candidate.getAttribute("id"))) {
                    return candidate;
                }
            }
            throw new SOAPException();
        }
        return element;
    }
}
