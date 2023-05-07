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

/**
 * This approach uses the java.net.http.HttpClient classes, which
 * were introduced in Java11.
 */
public class Client {
    private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    private static HttpClient httpClient = HttpClient.newHttpClient();

    public static void main(String... args) throws Exception {
        System.out.println(add() == 0);
        System.out.println(add(1, 2, 3, 4, 5) == 15);
        System.out.println(add(2, 4) == 6);
        System.out.println(subtract(12, 6) == 6);
        System.out.println(multiply(3, 4) == 12);
        System.out.println(multiply(1, 2, 3, 4, 5) == 120);
        System.out.println(divide(10, 5) == 2);
        System.out.println(modulo(10, 5) == 0);
    }

    public static int add(int lhs, int rhs) throws Exception {
        String xml = buildRequestXML("add", lhs, rhs);
        return handleRequest(xml);
    }
    public static int add(Integer... params) throws Exception {
        String xml = buildRequestXML("add", params);
        return handleRequest(xml);
    }
    public static int subtract(int lhs, int rhs) throws Exception {
        String xml = buildRequestXML("subtract", lhs, rhs);
        return handleRequest(xml);
    }
    public static int multiply(int lhs, int rhs) throws Exception {
        String xml = buildRequestXML("multiply", lhs, rhs);
        return handleRequest(xml);
    }
    public static int multiply(Integer... params) throws Exception {
        String xml = buildRequestXML("multiply", params);
        return handleRequest(xml);
    }
    public static int divide(int lhs, int rhs) throws Exception {
        String xml = buildRequestXML("divide", lhs, rhs);
        return handleRequest(xml);
    }
    public static int modulo(int lhs, int rhs) throws Exception {
        String xml = buildRequestXML("modulo", lhs, rhs);
        return handleRequest(xml);
    }

    private static int handleRequest(String xml) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/RPC"))
            .POST(HttpRequest.BodyPublishers.ofString(xml))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();
        String body = response.body();

        return Integer.parseInt(body);
    }
    private static String buildRequestXML(String method, Integer... params) throws Exception {
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Element methodCallElement = doc.createElement("methodCall");
        doc.appendChild(methodCallElement);
        Element methodNameElement = doc.createElement("methodName");
        methodCallElement.appendChild(methodNameElement);
        methodNameElement.appendChild(doc.createTextNode(method));

        Element paramsElement = doc.createElement("params");
        for (Integer param : params) {
            Element paramElement = doc.createElement("param");
            Element value = doc.createElement("value");
            Element intValue = doc.createElement("i4");

            intValue.appendChild(doc.createTextNode(param.toString()));
            value.appendChild(intValue);
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
}
