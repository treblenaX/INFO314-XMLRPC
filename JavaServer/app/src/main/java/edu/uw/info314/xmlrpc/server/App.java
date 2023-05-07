package edu.uw.info314.xmlrpc.server;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
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

    public static void main(String[] args) throws Exception {
        LOG.info("Starting up on port 8080");
        port(8080);

        // This is the mapping for POST requests to "/RPC";

        before("/*", (request, response) -> {
            String path = request.pathInfo();
            String method = request.requestMethod().toLowerCase();

            if (!acceptedPaths.contains(path)) halt(404, "Not Found.");
            if (!acceptedMethods.contains(method)) halt(405, "Not Supported.");
        });

        // this is where you will want to handle incoming XML-RPC requests
        post("/RPC", (request, response) -> {
            Object result = -1;
            String hostname = request.host();
            Call call = extractXMLRPCCall(request.body());

            try {
                Class<?> Calc = Calc.class;
//                System.out.println(call.name);
                Method method = Calc.getMethod(call.name, int[].class);
                Calc calc = new Calc();
//                System.out.println((Object) call.args);
                result = method.invoke(calc, (Object) call.args.stream().mapToInt(Integer::intValue).toArray());
//                System.out.println(result);
            } catch (Exception e) {
                e.printStackTrace();
            }

            response.header("Host", hostname);
            response.status(200);
            return result;
        });
    }

    public static Call extractXMLRPCCall(String xml) throws Exception {
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new InputSource(new StringReader(xml)));
        doc.getDocumentElement().normalize();

        Node rootNode = doc.getFirstChild();
        if (!rootNode.getNodeName().equals("methodCall")) throw new Exception("Invalid method call node.");

        Node nameNode = rootNode.getFirstChild();
        if (!nameNode.getNodeName().equals("methodName")) throw new Exception("Invalid method name node.");

        String name = nameNode.getTextContent();

        Node paramsNode = rootNode.getLastChild();
        if (!paramsNode.getNodeName().equals("params")) throw new Exception("Invalid params node.");

        NodeList paramsList = paramsNode.getChildNodes();

        if (paramsList.getLength() == 0) return new Call(name, new ArrayList<>(Arrays.asList(0)));

        List<Integer> valueList = new ArrayList<>();

        for (int i = 0; i < paramsList.getLength(); i++) {
            Node paramNode = paramsList.item(i);
            Node valueNode = paramNode.getFirstChild();
            if (valueNode == null || !valueNode.getNodeName().equals("value")) throw new Exception("Invalid value node.");
            Node intNode = valueNode.getFirstChild();
            if (intNode == null || !intNode.getNodeName().equals("i4")) throw new Exception("Invalid int node.");
            valueList.add(Integer.parseInt(intNode.getTextContent()));
        }

        return new Call(name, valueList);
    }
}
