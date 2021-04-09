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

import java.util.ArrayList;

/**
 * Used to store data when converting from JSON to XML
 */

/* package */ class Node {

    /* package */ class Attribute {
        String mKey;
        String mValue;
        Attribute(String key, String value) {
            mKey = key;
            mValue = value;
        }
    }

    private String mName;
    private String mPath;
    private String mContent;
    private ArrayList<Attribute> mAttributes = new ArrayList<>();
    private ArrayList<Node> mChildren = new ArrayList<>();

    /* package */ Node(String name, String path) {
        mName = name;
        mPath = path;
    }

    /* package */ void addAttribute(String key, String value) {
        mAttributes.add(new Attribute(key, value));
    }

    /* package */ void setContent(String content) {
        mContent = content;
    }

    /* package */ void setName(String name) {
        mName = name;
    }

    /* package */ void addChild(Node child) {
        mChildren.add(child);
    }

    /* package */ ArrayList<Attribute> getAttributes() {
        return mAttributes;
    }

    /* package */ String getContent() {
        return mContent;
    }

    /* package */ ArrayList<Node> getChildren() {
        return mChildren;
    }

    /* package */ String getPath() {
        return mPath;
    }

    /* package */ String getName() {
        return mName;
    }
}
