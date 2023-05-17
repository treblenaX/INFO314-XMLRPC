package edu.uw.info314.xmlrpc.server;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.*;

import static spark.Spark.*;

class Call {
    public String name;
    public List<Integer> args = new ArrayList<>();

    public Call(String name, List<Integer> args) {
        this.name = name;
        this.args = args;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[" + this.name + "] ");

        if (this.args.size() > 0) sb.append(this.args.get(0));

        for (int i = 1; i < this.args.size(); i++) {
            sb.append(", " + this.args.get(i));
        }

        return sb.toString();
    }
}

public class App {
    private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    public static final Logger LOG = Logger.getLogger(App.class.getCanonicalName());
    private static Set<String> acceptedPaths = new HashSet<>(Arrays.asList("/RPC"));
    private static Set<String> acceptedMethods = new HashSet<>(Arrays.asList("post"));
    private static Set<String> callMethods = new HashSet<>(Arrays.asList("add", "subtract", "multiply", "divide", "modulo"));

    enum ErrorResponse {
        DIVIDE_BY_ZERO(1, "divide by zero"),
        INTEGER_OVERFLOW(2, "integer overflow"),
        ILLEGAL_ARGUMENT_TYPE(3, "illegal argument type");

        int code;
        String str;

        ErrorResponse(int code, String str) {
            this.code = code;
            this.str = str;
        }
    }

    public static void main(String[] args) throws Exception {
        LOG.info("Starting up on port 8080");
        port(8080);

        path("/RPC", () -> {
            before("/*", (request, response) -> {
                String path = request.pathInfo();
                String method = request.requestMethod().toLowerCase();

                if (!acceptedPaths.contains(path)) halt(404, "Not Found.");
                if (!acceptedMethods.contains(method)) halt(405, "Not Supported.");
            });
            post("", (request, response) -> {
                Object result = -1;
                String hostname = request.host();

                try {
                    Call call = extractXMLRPCCall(request.body());
                    result = calculate(call);
                } catch (IllegalArgumentException e) {
                    response.status(500);
                    return constructXMLRPCCall(null, ErrorResponse.ILLEGAL_ARGUMENT_TYPE);
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();

                    if (cause instanceof ArithmeticException) {
                        ArithmeticException ex = (ArithmeticException) cause;

                        switch (ex.getMessage()) {
                            case "/ by zero":
                                response.status(500);
                                return constructXMLRPCCall(null, ErrorResponse.DIVIDE_BY_ZERO);
                            case "integer overflow":
                                response.status(500);
                                return constructXMLRPCCall(null, ErrorResponse.INTEGER_OVERFLOW);
                            default:
                                ex.printStackTrace();
                                break;
                        }
                    }
                }

                response.header("Host", hostname);
                response.status(200);
                return constructXMLRPCCall(result, null);
            });
        });
    }

    private static Object calculate(Call call) throws Exception {
        Class<?> Calc = Calc.class;
        Method method = Calc.getMethod(call.name, int[].class);
        Calc calc = new Calc();
        return method.invoke(calc, (Object) call.args.stream().mapToInt(Integer::intValue).toArray());
    }

    public static Call extractXMLRPCCall(String xml) throws Exception {
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new InputSource(new StringReader(xml.replaceAll("\n", ""))));
        doc.getDocumentElement().normalize();

        String methodName = doc.getElementsByTagName("methodName").item(0).getTextContent();
        NodeList params = doc.getElementsByTagName("param");
        NodeList intParams = doc.getElementsByTagName("i4");

        if (params.getLength() != intParams.getLength()) throw new IllegalArgumentException("Invalid params.");

        List<Integer> values = new ArrayList<>();

        for (int i = 0; i < params.getLength(); i++) {
            values.add(Integer.parseInt(intParams.item(i).getTextContent()));
        }

        return new Call(methodName, values);
    }

    private static String constructXMLRPCCall(Object response, ErrorResponse error) throws Exception {
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Element methodResponseElement = doc.createElement("methodResponse");
        doc.appendChild(methodResponseElement);

        if (error != null) {
            Element faultElement = doc.createElement("fault");
            Element valueElement = doc.createElement("value");
            Element structElement = doc.createElement("struct");

            // fault code
            Element codeMemberElement = doc.createElement("member");
            Element codeNameElement = doc.createElement("name");
            codeNameElement.appendChild(doc.createTextNode("faultCode"));
            Element codeValueElement = doc.createElement("value");
            Element codeIntElement = doc.createElement("i4");
            codeIntElement.appendChild(doc.createTextNode(error.code + ""));
            codeValueElement.appendChild(codeIntElement);

            codeMemberElement.appendChild(codeNameElement);
            codeMemberElement.appendChild(codeValueElement);

            // fault string
            Element strMemberElement = doc.createElement("member");
            Element strNameElement = doc.createElement("name");
            strNameElement.appendChild(doc.createTextNode("faultString"));
            Element strValueElement = doc.createElement("value");
            Element strStringElement = doc.createElement("string");
            strStringElement.appendChild(doc.createTextNode(error.str));
            strValueElement.appendChild(strStringElement);

            strMemberElement.appendChild(strNameElement);
            strMemberElement.appendChild(strValueElement);

            structElement.appendChild(codeMemberElement);
            structElement.appendChild(strMemberElement);
            valueElement.appendChild(structElement);
            faultElement.appendChild(valueElement);
            methodResponseElement.appendChild(faultElement);
        } else {
            Element paramsElement = doc.createElement("params");
            Element paramElement = doc.createElement("param");
            Element valueElement = doc.createElement("value");
            Element valIntElement = doc.createElement("i4");
            valIntElement.appendChild(doc.createTextNode("" + response));
            valueElement.appendChild(valIntElement);
            paramElement.appendChild(valueElement);
            paramsElement.appendChild(paramElement);
            methodResponseElement.appendChild(paramsElement);
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);

        return writer.toString();
    }
}
