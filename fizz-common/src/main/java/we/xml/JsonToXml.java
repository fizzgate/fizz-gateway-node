/*
    Copyright 2016 Arnaud Guyon

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */

package we.xml;

//import android.util.Xml;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Converts JSON to XML <br/>
 * <br/>
 * Add default tag prefix(-) by Francis Dong<br/>
 * Change default content name to #text by Francis Dong<br/>
 */

public class JsonToXml {

	private static final String DEFAULT_TAG_PREFIX = "-";
    private static final int DEFAULT_INDENTATION = 3;
    // TODO: Set up Locale in the builder
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    public static class Builder {

        private JSONObject mJson;
        private HashSet<String> mForcedAttributes = new HashSet<>();
        private HashSet<String> mForcedContent = new HashSet<>();

        /**
         * Constructor
         * @param jsonObject a JSON object
         */
        public Builder(JSONObject jsonObject) {
            mJson = jsonObject;
        }

        /**
         * Constructor
         * @param inputStream InputStream containing the JSON
         */
        public Builder(InputStream inputStream) {
            this(FileReader.readFileFromInputStream(inputStream));
        }

        /**
         * Constructor
         * @param jsonString String containing the JSON
         */
        public Builder(String jsonString) {
            try {
                mJson = new JSONObject(jsonString);
            } catch (JSONException exception) {
                exception.printStackTrace();
            }
        }

        /**
         * Force a TAG to be an attribute of the parent TAG
         * @param path Path for the attribute, using format like "/parentTag/childTag/childTagAttribute"
         * @return the Builder
         */
        public Builder forceAttribute(String path) {
            mForcedAttributes.add(path);
            return this;
        }

        /**
         * Force a TAG to be the content of its parent TAG
         * @param path Path for the content, using format like "/parentTag/contentTag"
         * @return the Builder
         */
        public Builder forceContent(String path) {
            mForcedContent.add(path);
            return this;
        }

        /**
         * Creates the JsonToXml object
         * @return a JsonToXml instance
         */
        public JsonToXml build() {
            return new JsonToXml(mJson, mForcedAttributes, mForcedContent);
        }
    }

    private JSONObject mJson;
    private HashSet<String> mForcedAttributes;
    private HashSet<String> mForcedContent;

    private JsonToXml(JSONObject jsonObject, HashSet<String> forcedAttributes, HashSet<String> forcedContent) {
        mJson = jsonObject;
        mForcedAttributes = forcedAttributes;
        mForcedContent = forcedContent;
    }

    /**
     *
     * @return the XML
     */
    @Override
    public String toString() {
        Node rootNode = new Node(null, "");
        prepareObject(rootNode, mJson);
        return nodeToXML(rootNode);
    }

    /**
     *
     * @return the formatted XML with a default indent (3 spaces)
     */
    public String toFormattedString() {
        return toFormattedString(DEFAULT_INDENTATION);
    }

    /**
     *
     * @param indent size of the indent (number of spaces)
     * @return the formatted XML
     */
    public String toFormattedString(int indent) {
        String input = toString();
        try {
            Source xmlInput = new StreamSource(new StringReader(input));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "" + indent);
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (Exception e) {
            throw new RuntimeException(e); // TODO: do my own
        }
    }

    private String nodeToXML(Node node) {
//        XmlSerializer serializer = Xml.newSerializer();
        try {
        	XmlSerializer serializer = new KXmlSerializer();
        			
        	StringWriter writer = new StringWriter();
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);

            nodeToXml(serializer, node);

            serializer.endDocument();
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);  // TODO: do my own
        }
    }

    private void nodeToXml(XmlSerializer serializer, Node node) throws IOException {
        String nodeName = node.getName();
        if (nodeName != null) {
            serializer.startTag("", nodeName);

            for (Node.Attribute attribute : node.getAttributes()) {
                serializer.attribute("", attribute.mKey, attribute.mValue);
            }
            String nodeContent = node.getContent();
            if (nodeContent != null) {
                serializer.text(nodeContent);
            }
        }

        for (Node subNode : node.getChildren()) {
            nodeToXml(serializer, subNode);
        }

        if (nodeName != null) {
            serializer.endTag("", nodeName);
        }
    }

    private void prepareObject(Node node, JSONObject json) {
        Iterator<String> keyterator = json.keys();
        while (keyterator.hasNext()) {
            String key = keyterator.next();
            Object object = json.opt(key);
            if (object != null) {
                if (object instanceof JSONObject) {
                    JSONObject subObject = (JSONObject) object;
                    String path = node.getPath() + "/" + key;
                    Node subNode = new Node(key, path);
                    node.addChild(subNode);
                    prepareObject(subNode, subObject);
                } else if (object instanceof JSONArray) {
                    JSONArray array = (JSONArray) object;
                    prepareArray(node, key, array);
                } else {
                    String path = node.getPath() + "/" + key;
                    // JSON numbers are represented either Integer or Double (IEEE 754)
                    // Long may be represented in scientific notation because they are stored as Double
                    // This workaround attempts to represent Long and Double objects accordingly
                    String value;
                    if (object instanceof Double) {
                        double d = (double) object;
                        // If it is a Long
                        if (d % 1 == 0) {
                            value = Long.toString((long) d);
                        } else {
                            // TODO: Set up number of decimal digits per attribute in the builder
                            // Set only once. Represent all double numbers up to 20 decimal digits
                            if (DECIMAL_FORMAT.getMaximumFractionDigits() == 0) {
                                DECIMAL_FORMAT.setMaximumFractionDigits(20);
                            }
                            value = DECIMAL_FORMAT.format(d);
                        }
                    } else {
                        // Integer, Boolean and String are handled here
                        value = object.toString();
                    }
                    if (isAttribute(path)) {
                    	if(key.startsWith(DEFAULT_TAG_PREFIX)) {
                    		key = key.substring(1, key.length());
                    	}
                        node.addAttribute(key, value);
                    } else if (isContent(path) ) {
                        node.setContent(value);
                    } else {
                        Node subNode = new Node(key, node.getPath());
                        subNode.setContent(value);
                        node.addChild(subNode);
                    }
                }
            }
        }
    }

    private void prepareArray(Node node, String key, JSONArray array) {
        int count = array.length();
        String path = node.getPath() + "/" + key;
        for (int i = 0; i < count; ++i) {
            Node subNode = new Node(key, path);
            Object object = array.opt(i);
            if (object != null) {
                if (object instanceof JSONObject) {
                    JSONObject jsonObject = (JSONObject) object;
                    prepareObject(subNode, jsonObject);
                } else if (object instanceof JSONArray) {
                    JSONArray subArray = (JSONArray) object;
                    prepareArray(subNode, key, subArray);
                } else {
                    String value = object.toString();
                    subNode.setName(key);
                    subNode.setContent(value);
                }
            }
            node.addChild(subNode);
        }
    }

	private boolean isAttribute(String path) {
		if (mForcedAttributes.contains(path)) {
			return true;
		}
		String[] paths = path.split("/");
		if (paths[paths.length - 1].startsWith(DEFAULT_TAG_PREFIX)) {
			return true;
		}
		return false;
	}

    private boolean isContent(String path) {
        if (mForcedContent.contains(path)) {
			return true;
		}
		String[] paths = path.split("/");
		if ("#text".equals(paths[paths.length - 1])) {
			return true;
		}
		return false;
    }
}
