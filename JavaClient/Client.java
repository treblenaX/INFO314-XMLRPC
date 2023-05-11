import java.io.*;
import java.net.*;
import java.net.http.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * This approach uses the java.net.http.HttpClient classes, which
 * were introduced in Java11.
 */
public class Client {
    private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    private static HttpClient httpClient = HttpClient.newHttpClient();
    private static String hostname;
    private static String port;

    public static void main(String... args) throws Exception {
        hostname = args[0];
        port = args[1];

        System.out.println(add() == 0);
        System.out.println(add(1, 2, 3, 4, 5) == 15);
        System.out.println(add(2, 4) == 6);
        System.out.println(subtract(12, 6) == 6);
        System.out.println(multiply(3, 4) == 12);
        System.out.println(multiply(1, 2, 3, 4, 5) == 120);
        System.out.println(divide(10, 5) == 2);
        System.out.println(modulo(10, 5) == 0);
        // errors
        try {   // addition overflow
            System.out.println(add(Integer.MAX_VALUE, Integer.MAX_VALUE));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        try {   // divide by zero
            System.out.println(modulo(10, 0));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        try {   // multiplication overflow
            System.out.println(multiply(Integer.MAX_VALUE, Integer.MAX_VALUE));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        try {   // divide by zero
            System.out.println(divide(0, 0));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        try {
            System.out.println(subtract("1", "2"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static int add(int lhs, int rhs) throws Exception {
        try {
            String xml = buildRequestXML("add", lhs, rhs);
            String xmlResponse = receiveRequest(xml);
            return Integer.parseInt(parseResponseXML(xmlResponse));
        } catch (Exception e) {
            throw e;
        }
    }
    public static int add(Object... params) throws Exception {
        try {
            String xml = buildRequestXML("add", params);
            String xmlResponse = receiveRequest(xml);
            return Integer.parseInt(parseResponseXML(xmlResponse));
        } catch (Exception e) {
            throw e;
        }
    }
    public static int subtract(int lhs, int rhs) throws Exception {
        try {
            String xml = buildRequestXML("subtract", lhs, rhs);
            String xmlResponse = receiveRequest(xml);
            return Integer.parseInt(parseResponseXML(xmlResponse));
        } catch (Exception e) {
            throw e;
        }
    }
    public static int subtract(Object... params) throws Exception {
        try {
            String xml = buildRequestXML("subtract", params);
            String xmlResponse = receiveRequest(xml);
            return Integer.parseInt(parseResponseXML(xmlResponse));
        } catch (Exception e) {
            throw e;
        }
    }
    public static int multiply(int lhs, int rhs) throws Exception {
        try {
            String xml = buildRequestXML("multiply", lhs, rhs);
            String xmlResponse = receiveRequest(xml);
            return Integer.parseInt(parseResponseXML(xmlResponse));
        } catch (Exception e) {
            throw e;
        }
    }
    public static int multiply(Object... params) throws Exception {
        try {
            String xml = buildRequestXML("multiply", params);
            String xmlResponse = receiveRequest(xml);
            return Integer.parseInt(parseResponseXML(xmlResponse));
        } catch (Exception e) {
            throw e;
        }
    }
    public static int divide(int lhs, int rhs) throws Exception {
        try {
            String xml = buildRequestXML("divide", lhs, rhs);
            String xmlResponse = receiveRequest(xml);
            return Integer.parseInt(parseResponseXML(xmlResponse));
        } catch (Exception e) {
            throw e;
        }
    }
    public static int modulo(int lhs, int rhs) throws Exception {
        try {
            String xml = buildRequestXML("modulo", lhs, rhs);
            String xmlResponse = receiveRequest(xml);
            return Integer.parseInt(parseResponseXML(xmlResponse));
        } catch (Exception e) {
            throw e;
        }
    }

    private static String receiveRequest(String xml) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://" + hostname + ":" + port + "/RPC"))
            .header("User-Agent", "Elbert Cheng")
            .POST(HttpRequest.BodyPublishers.ofString(xml))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();
        String body = response.body();

        return body;
    }

    private static String buildRequestXML(String method, Object... params) throws Exception {
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Element methodCallElement = doc.createElement("methodCall");
        doc.appendChild(methodCallElement);
        Element methodNameElement = doc.createElement("methodName");
        methodCallElement.appendChild(methodNameElement);
        methodNameElement.appendChild(doc.createTextNode(method));

        Element paramsElement = doc.createElement("params");
        for (Object param : params) {
            Element paramElement = doc.createElement("param");
            Element value = doc.createElement("value");

            Class<?> objClass = param.getClass();
            String valueTypeString = "";

            switch (objClass.getName()) {
                case "java.lang.Integer":
                    valueTypeString = "i4";
                    break;
                case "java.lang.String":
                    valueTypeString = "string";
                    break;
                default:
                    throw new Exception("Invalid parameter type: " + objClass.getName());
            }

            Element valueType = doc.createElement(valueTypeString);

            valueType.appendChild(doc.createTextNode(param.toString()));
            value.appendChild(valueType);
            paramElement.appendChild(value);
            paramsElement.appendChild(paramElement);
        }
        methodCallElement.appendChild(paramsElement);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);

        return writer.toString();
    }

    private static String parseResponseXML(String xml) throws Exception {
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new InputSource(new StringReader(xml)));

        NodeList faultNode = doc.getElementsByTagName("fault");

        if (faultNode.getLength() != 0) {   // error encountered
            NodeList errorNodes = doc.getElementsByTagName("i4");
            NodeList errorStringNodes = doc.getElementsByTagName("string");
            String code = errorNodes.item(0).getTextContent().trim();
            String str = errorStringNodes.item(0).getTextContent().trim();

            throw new Exception("Error: " + code + " - " + str);
        }

        NodeList list = doc.getElementsByTagName("value");
        return list.item(0).getTextContent().trim();
    }
}
