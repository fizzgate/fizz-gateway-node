package com.fizzgate.xml;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.DOTALL;

/**
 * Converts XML to JSON<br/>
 * <br/>
 * Add default tag prefix(-) by Francis Dong<br/>
 * Change default content name to #text by Francis Dong<br/>
 * Add handerListItem method to handler list item by Francis Dong<br/>
 */

public class XmlToJson {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(XmlToJson.class);

    private static final String TAG = "XmlToJson";
//    private static final String DEFAULT_CONTENT_NAME = "content";
    private static final String DEFAULT_CONTENT_NAME = "#text";
    private static final String DEFAULT_TAG_PREFIX = "-";
    private static final String DEFAULT_ENCODING = "utf-8";
    private static final String DEFAULT_INDENTATION = "   ";
    private String mIndentationPattern = DEFAULT_INDENTATION;

    // default values when a Tag is empty
    private static final String DEFAULT_EMPTY_STRING = "";
    private static final int DEFAULT_EMPTY_INTEGER = 0;
    private static final long DEFAULT_EMPTY_LONG = 0;
    private static final double DEFAULT_EMPTY_DOUBLE = 0;
    private static final boolean DEFAULT_EMPTY_BOOLEAN = false;

    /**
     * Builder class to create a XmlToJson object
     */
    public static class Builder {

        private StringReader mStringSource;
        private InputStream mInputStreamSource;
        private String mInputEncoding = DEFAULT_ENCODING;
        private HashSet<String> mForceListPaths = new HashSet<>();
        private HashSet<Pattern> mForceListPatterns = new HashSet<>();
        private HashMap<String, String> mAttributeNameReplacements = new HashMap<>();
        private HashMap<String, String> mContentNameReplacements = new HashMap<>();
        private HashMap<String, Class> mForceClassForPath = new HashMap<>();    // Integer, Long, Double, Boolean
        private HashSet<String> mSkippedAttributes = new HashSet<>();
        private HashSet<String> mSkippedTags = new HashSet<>();

        /**
         * Constructor
         *
         * @param xmlSource XML source
         */
        public Builder(String xmlSource) {
            mStringSource = new StringReader(xmlSource);
        }

        /**
         * Constructor
         *
         * @param inputStreamSource XML source
         * @param inputEncoding     XML encoding format, can be null (uses UTF-8 if null).
         */
        public Builder(InputStream inputStreamSource, String inputEncoding) {
            mInputStreamSource = inputStreamSource;
            mInputEncoding = (inputEncoding != null) ? inputEncoding : DEFAULT_ENCODING;
        }

        /**
         * Force a XML Tag to be interpreted as a list
         *
         * @param path Path for the tag, with format like "/parentTag/childTag/tagAsAList"
         * @return the Builder
         */
        public Builder forceList(String path) {
            mForceListPaths.add(path);
            return this;
        }

        /**
         * Force a XML Tag to be interpreted as a list, using a RegEx pattern for the path
         *
         * @param pattern Path for the tag using RegEx, like "*childTag/tagAsAList"
         * @return the Builder
         */
        public Builder forceListPattern(String pattern) {
            Pattern pat = Pattern.compile(pattern, DOTALL);
            mForceListPatterns.add(pat);
            return this;
        }

        /**
         * Change the name of an attribute
         *
         * @param attributePath   Path for the attribute, using format like "/parentTag/childTag/childTagAttribute"
         * @param replacementName Name used for replacement (childTagAttribute becomes replacementName)
         * @return the Builder
         */
        public Builder setAttributeName(String attributePath, String replacementName) {
            mAttributeNameReplacements.put(attributePath, replacementName);
            return this;
        }

        /**
         * Change the name of the key for a XML content
         * In XML there is no extra key name for a tag content. So a default name "content" is used.
         * This "content" name can be replaced with a custom name.
         *
         * @param contentPath     Path for the Tag that holds the content, using format like "/parentTag/childTag"
         * @param replacementName Name used in place of the default "content" key
         * @return the Builder
         */
        public Builder setContentName(String contentPath, String replacementName) {
            mContentNameReplacements.put(contentPath, replacementName);
            return this;
        }

        /**
         * Force an attribute or content value to be a INTEGER. A default value is used if the content is missing.
         * @param path Path for the Tag content or Attribute, using format like "/parentTag/childTag"
         * @return the Builder
         */
        public Builder forceIntegerForPath(String path) {
            mForceClassForPath.put(path, Integer.class);
            return this;
        }

        /**
         * Force an attribute or content value to be a LONG. A default value is used if the content is missing.
         * @param path Path for the Tag content or Attribute, using format like "/parentTag/childTag"
         * @return the Builder
         */
        public Builder forceLongForPath(String path) {
            mForceClassForPath.put(path, Long.class);
            return this;
        }

        /**
         * Force an attribute or content value to be a DOUBLE. A default value is used if the content is missing.
         * @param path Path for the Tag content or Attribute, using format like "/parentTag/childTag"
         * @return the Builder
         */
        public Builder forceDoubleForPath(String path) {
            mForceClassForPath.put(path, Double.class);
            return this;
        }

        /**
         * Force an attribute or content value to be a BOOLEAN. A default value is used if the content is missing.
         * @param path Path for the Tag content or Attribute, using format like "/parentTag/childTag"
         * @return the Builder
         */
        public Builder forceBooleanForPath(String path) {
            mForceClassForPath.put(path, Boolean.class);
            return this;
        }

        /**
         * Skips a Tag (will not be present in the JSON)
         *
         * @param path Path for the Tag, using format like "/parentTag/childTag"
         * @return the Builder
         */
        public Builder skipTag(String path) {
            mSkippedTags.add(path);
            return this;
        }

        /**
         * Skips an attribute (will not be present in the JSON)
         *
         * @param path Path for the Attribute, using format like "/parentTag/childTag/ChildTagAttribute"
         * @return the Builder
         */
        public Builder skipAttribute(String path) {
            mSkippedAttributes.add(path);
            return this;
        }

        /**
         * Creates the XmlToJson object
         *
         * @return a XmlToJson instance
         */
        public XmlToJson build() {
            return new XmlToJson(this);
        }
    }

    private StringReader mStringSource;
    private InputStream mInputStreamSource;
    private String mInputEncoding;
    private HashSet<String> mForceListPaths;
    private HashSet<Pattern> mForceListPatterns = new HashSet<>();
    private HashMap<String, String> mAttributeNameReplacements;
    private HashMap<String, String> mContentNameReplacements;
    private HashMap<String, Class> mForceClassForPath;
    private HashSet<String> mSkippedAttributes = new HashSet<>();
    private HashSet<String> mSkippedTags = new HashSet<>();
    private JSONObject mJsonObject; // Used for caching the result

    private XmlToJson(Builder builder) {
        mStringSource = builder.mStringSource;
        mInputStreamSource = builder.mInputStreamSource;
        mInputEncoding = builder.mInputEncoding;
        mForceListPaths = builder.mForceListPaths;
        mForceListPatterns = builder.mForceListPatterns;
        mAttributeNameReplacements = builder.mAttributeNameReplacements;
        mContentNameReplacements = builder.mContentNameReplacements;
        mForceClassForPath = builder.mForceClassForPath;
        mSkippedAttributes = builder.mSkippedAttributes;
        mSkippedTags = builder.mSkippedTags;

        mJsonObject = convertToJSONObject(); // Build now so that the InputStream can be closed just after
    }

    /**
     * @return the JSONObject built from the XML
     */
    public JSONObject toJson() {
        return mJsonObject;
    }

    private JSONObject convertToJSONObject() {
        try {
            Tag parentTag = new Tag("", "xml");

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);   // tags with namespace are taken as-is ("namespace:tagname")
            XmlPullParser xpp = factory.newPullParser();

            setInput(xpp);

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.START_DOCUMENT) {
                eventType = xpp.next();
            }
            readTags(parentTag, xpp);

            unsetInput();

            return convertTagToJson(parentTag, false);
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setInput(XmlPullParser xpp) {
        if (mStringSource != null) {
            try {
                xpp.setInput(mStringSource);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
        } else {
            try {
                xpp.setInput(mInputStreamSource, mInputEncoding);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
        }
    }

    private void unsetInput() {
        if (mStringSource != null) {
            mStringSource.close();
        }
        // else the InputStream has been given by the user, it is not our role to close it
    }

    private void readTags(Tag parent, XmlPullParser xpp) {
        try {
            int eventType;
            do {
                eventType = xpp.next();
                if (eventType == XmlPullParser.START_TAG) {
                    String tagName = xpp.getName();
                    String path = parent.getPath() + "/" + tagName;

                    boolean skipTag = mSkippedTags.contains(path);

                    Tag child = new Tag(path, tagName);
                    if (!skipTag) {
                        parent.addChild(child);
                    }

                    // Attributes are taken into account as key/values in the child
                    int attrCount = xpp.getAttributeCount();
                    for (int i = 0; i < attrCount; ++i) {
                        String attrName = xpp.getAttributeName(i);
                        String attrValue = xpp.getAttributeValue(i);
                        String attrPath = parent.getPath() + "/" + child.getName() + "/" + attrName;

                        // Skip Attributes
                        if (mSkippedAttributes.contains(attrPath)) {
                            continue;
                        }

                        attrName = getAttributeNameReplacement(attrPath, attrName);
                        Tag attribute = new Tag(attrPath, attrName);
                        attribute.setContent(attrValue);
                        child.addChild(attribute);
                    }

                    readTags(child, xpp);
                } else if (eventType == XmlPullParser.TEXT) {
                    String text = xpp.getText();
                    parent.setContent(text);
                } else if (eventType == XmlPullParser.END_TAG) {
                    return;
                } else if (eventType == XmlPullParser.END_DOCUMENT) {
                    return;
                } else {
                	LOGGER.info("{} unknown xml eventType {}", TAG, eventType);
                }
            } while (eventType != XmlPullParser.END_DOCUMENT);
        } catch (XmlPullParserException | IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    private JSONObject convertTagToJson(Tag tag, boolean isListElement) {
        JSONObject json = new JSONObject();

        // Content is injected as a key/value
        if (tag.getContent() != null) {
            String path = tag.getPath();
            String name = getContentNameReplacement(path, DEFAULT_CONTENT_NAME);
            putContent(path, json, name, tag.getContent());
        }

        try {

            HashMap<String, ArrayList<Tag>> groups = tag.getGroupedElements(); // groups by tag names so that we can detect lists or single elements
            for (ArrayList<Tag> group : groups.values()) {

                if (group.size() == 1) {    // element, or list of 1
                    Tag child = group.get(0);
                    if (isForcedList(child)) {  // list of 1
                        JSONArray list = new JSONArray();
                        list.put(handerListItem(child.getPath(), convertTagToJson(child, true)));
                        String childrenNames = child.getName();
                        json.put(childrenNames, list);
                    } else {    // stand alone element
                        if (child.hasChildren()) {
                            JSONObject jsonChild = convertTagToJson(child, false);
                            json.put(child.getName(), jsonChild);
                        } else {
                            String path = child.getPath();
                            putContent(path, json, child.getName(), child.getContent());
                        }
                    }
                } else {    // list
                    JSONArray list = new JSONArray();
                    for (Tag child : group) {
                        list.put(handerListItem(child.getPath(), convertTagToJson(child, true)));
                    }
                    String childrenNames = group.get(0).getName();
                    json.put(childrenNames, list);
                }
            }
            return json;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Convert to single value if JSONObject only contain content key <br/>
     * Returns "abc" if JSONObject is {"#text": "abc"} <br/>
     */
    @SuppressWarnings({ "rawtypes", "unused" })
	private Object handerListItem(String path, JSONObject json) {
    	if(json.length() == 1 && json.has(DEFAULT_CONTENT_NAME)) {
    		Object val = json.get(DEFAULT_CONTENT_NAME);
    		if(val == null) {
    			return val;
    		}
    		String content =  String.valueOf(val);
    		try {
                // checks if the user wants to force a class (Int, Double... for a given path)
                Class forcedClass = mForceClassForPath.get(path);
                if (forcedClass == null) {  // default behaviour, put it as a String
                    return content;
                } else {
                    if (forcedClass == Integer.class) {
                        try {
                            return Integer.parseInt(content);
                        } catch (NumberFormatException exception) {
                        	return DEFAULT_EMPTY_INTEGER;
                        }
                    } else if (forcedClass == Long.class) {
                        try {
                        	return Long.parseLong(content);
                        } catch (NumberFormatException exception) {
                        	return DEFAULT_EMPTY_LONG;
                        }
                    } else if (forcedClass == Double.class) {
                        try {
                        	return Double.parseDouble(content);
                        } catch (NumberFormatException exception) {
                        	return DEFAULT_EMPTY_DOUBLE;
                        }
                    } else if (forcedClass == Boolean.class) {
                        if (content == null) {
                        	return DEFAULT_EMPTY_BOOLEAN;
                        } else if (content.equalsIgnoreCase("true")) {
                        	return true;
                        } else if (content.equalsIgnoreCase("false")) {
                        	return false;
                        } else {
                        	return DEFAULT_EMPTY_BOOLEAN;
                        }
                    }
                }
            } catch (JSONException exception) {
                // keep continue in case of error
            }
    	}
    	return json;
    }

    private void putContent(String path, JSONObject json, String tag, String content) {
        try {
            // checks if the user wants to force a class (Int, Double... for a given path)
            Class forcedClass = mForceClassForPath.get(path);
            if (forcedClass == null) {  // default behaviour, put it as a String
                if (content == null) {
                    content = DEFAULT_EMPTY_STRING;
                }
                json.put(tag, content);
            } else {
                if (forcedClass == Integer.class) {
                    try {
                        Integer number = Integer.parseInt(content);
                        json.put(tag, number);
                    } catch (NumberFormatException exception) {
                        json.put(tag, DEFAULT_EMPTY_INTEGER);
                    }
                } else if (forcedClass == Long.class) {
                    try {
                        Long number = Long.parseLong(content);
                        json.put(tag, number);
                    } catch (NumberFormatException exception) {
                        json.put(tag, DEFAULT_EMPTY_LONG);
                    }
                } else if (forcedClass == Double.class) {
                    try {
                        Double number = Double.parseDouble(content);
                        json.put(tag, number);
                    } catch (NumberFormatException exception) {
                        json.put(tag, DEFAULT_EMPTY_DOUBLE);
                    }
                } else if (forcedClass == Boolean.class) {
                    if (content == null) {
                        json.put(tag, DEFAULT_EMPTY_BOOLEAN);
                    } else if (content.equalsIgnoreCase("true")) {
                        json.put(tag, true);
                    } else if (content.equalsIgnoreCase("false")) {
                        json.put(tag, false);
                    } else {
                        json.put(tag, DEFAULT_EMPTY_BOOLEAN);
                    }
                }
            }

        } catch (JSONException exception) {
            // keep continue in case of error
        }
    }

    private boolean isForcedList(Tag tag) {
        String path = tag.getPath();
        if (mForceListPaths.contains(path)) {
            return true;
        }
        for(Pattern pattern : mForceListPatterns) {
            Matcher matcher = pattern.matcher(path);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }

    private String getAttributeNameReplacement(String path, String defaultValue) {
        String result = mAttributeNameReplacements.get(path);
        if (result != null) {
            return result;
        }
        return DEFAULT_TAG_PREFIX + defaultValue;
    }

    private String getContentNameReplacement(String path, String defaultValue) {
        String result = mContentNameReplacements.get(path);
        if (result != null) {
            return result;
        }
        return defaultValue;
    }

    @Override
    public String toString() {
        if (mJsonObject != null) {
            return mJsonObject.toString();
        }
        return null;
    }

    /**
     * Format the Json with indentation and line breaks
     *
     * @param indentationPattern indentation to use, for example " " or "\t".
     *                           if null, use the default 3 spaces indentation
     * @return the formatted Json
     */
    public String toFormattedString(String indentationPattern) {
        if (indentationPattern == null) {
            mIndentationPattern = DEFAULT_INDENTATION;
        } else {
            mIndentationPattern = indentationPattern;
        }
        return toFormattedString();
    }

    /**
     * Format the Json with indentation and line breaks.
     * Uses the last intendation pattern used, or the default one (3 spaces)
     *
     * @return the Builder
     */
    public String toFormattedString() {
        if (mJsonObject != null) {
            String indent = "";
            StringBuilder builder = new StringBuilder();
            builder.append("{\n");
            format(mJsonObject, builder, indent);
            builder.append("}\n");
            return builder.toString();
        }
        return null;
    }

    private void format(JSONObject jsonObject, StringBuilder builder, String indent) {
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            builder.append(indent);
            builder.append(mIndentationPattern);
            builder.append("\"");
            builder.append(key);
            builder.append("\": ");
            Object value = jsonObject.opt(key);
            if (value instanceof JSONObject) {
                JSONObject child = (JSONObject) value;
                builder.append(indent);
                builder.append("{\n");
                format(child, builder, indent + mIndentationPattern);
                builder.append(indent);
                builder.append(mIndentationPattern);
                builder.append("}");
            } else if (value instanceof JSONArray) {
                JSONArray array = (JSONArray) value;
                formatArray(array, builder, indent + mIndentationPattern);
            } else {
                formatValue(value, builder);
            }
            if (keys.hasNext()) {
                builder.append(",\n");
            } else {
                builder.append("\n");
            }
        }
    }

    private void formatArray(JSONArray array, StringBuilder builder, String indent) {
        builder.append("[\n");

        for (int i = 0; i < array.length(); ++i) {
            Object element = array.opt(i);
            if (element instanceof JSONObject) {
                JSONObject child = (JSONObject) element;
                builder.append(indent);
                builder.append(mIndentationPattern);
                builder.append("{\n");
                format(child, builder, indent + mIndentationPattern);
                builder.append(indent);
                builder.append(mIndentationPattern);
                builder.append("}");
            } else if (element instanceof JSONArray) {
                JSONArray child = (JSONArray) element;
                formatArray(child, builder, indent + mIndentationPattern);
            } else {
            	builder.append(indent);
                builder.append(mIndentationPattern);
                formatValue(element, builder);
            }
            if (i < array.length() - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append(indent);
        builder.append("]");
    }

    private void formatValue(Object value, StringBuilder builder) {
        if (value instanceof String) {
            String string = (String) value;

            // Escape special characters
            string = string.replaceAll("\\\\", "\\\\\\\\");                     // escape backslash
            string = string.replaceAll("\"", Matcher.quoteReplacement("\\\"")); // escape double quotes
            string = string.replaceAll("/", "\\\\/");                           // escape slash
            string = string.replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t");  // escape \n and \t
            string = string.replaceAll("\r", "\\\\r");  // escape \r

            builder.append("\"");
            builder.append(string);
            builder.append("\"");
        } else if (value instanceof Long) {
            Long longValue = (Long) value;
            builder.append(longValue);
        } else if (value instanceof Integer) {
            Integer intValue = (Integer) value;
            builder.append(intValue);
        } else if (value instanceof Boolean) {
            Boolean bool = (Boolean) value;
            builder.append(bool);
        } else if (value instanceof Double) {
            Double db = (Double) value;
            builder.append(db);
        } else {
            builder.append(value.toString());
        }
    }

}
