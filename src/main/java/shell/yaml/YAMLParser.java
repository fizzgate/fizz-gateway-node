/**
 * Copyright (c) 2008, http://www.snakeyaml.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package shell.yaml;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.dataformat.yaml.JacksonYAMLParseException;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.events.*;
import org.yaml.snakeyaml.reader.StreamReader;

import java.io.IOException;
import java.io.Reader;

public class YAMLParser extends com.fasterxml.jackson.dataformat.yaml.YAMLParser {
    protected final ParserImpl _yamlParser;
    public YAMLParser(IOContext ctxt, BufferRecycler br, int parserFeatures, int formatFeatures, ObjectCodec codec, Reader reader) {
        super(ctxt, br, parserFeatures, formatFeatures, codec, reader);
        this._yamlParser = new ParserImpl(new StreamReader(reader));
    }

    public JsonToken nextToken() throws IOException {
        this._currentIsAlias = false;
        this._binaryValue = null;
        if (this._closed) {
            return null;
        } else {
            while(true) {
                Event evt;
                try {
                    evt = this._yamlParser.getEvent();
                } catch (YAMLException var5) {
                    if (var5 instanceof MarkedYAMLException) {
                        throw com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.MarkedYAMLException.from(this, (MarkedYAMLException)var5);
                    }

                    throw new JacksonYAMLParseException(this, var5.getMessage(), var5);
                }

                if (evt == null) {
                    this._currentAnchor = null;
                    return this._currToken = null;
                }

                this._lastEvent = evt;
                if (this._parsingContext.inObject()) {
                    if (this._currToken != JsonToken.FIELD_NAME) {
                        if (!evt.is(Event.ID.Scalar)) {
                            this._currentAnchor = null;
                            if (evt.is(Event.ID.MappingEnd)) {
                                if (!this._parsingContext.inObject()) {
                                    this._reportMismatchedEndMarker(125, ']');
                                }

                                this._parsingContext = this._parsingContext.getParent();
                                return this._currToken = JsonToken.END_OBJECT;
                            }

                            this._reportError("Expected a field name (Scalar value in YAML), got this instead: " + evt);
                        }

                        ScalarEvent scalar = (ScalarEvent)evt;
                        String newAnchor = scalar.getAnchor();
                        if (newAnchor != null || this._currToken != JsonToken.START_OBJECT) {
                            this._currentAnchor = scalar.getAnchor();
                        }

                        String name = scalar.getValue();
                        this._currentFieldName = name;
                        this._parsingContext.setCurrentName(name);
                        return this._currToken = JsonToken.FIELD_NAME;
                    }
                } else if (this._parsingContext.inArray()) {
                    this._parsingContext.expectComma();
                }

                this._currentAnchor = null;
                if (evt.is(Event.ID.Scalar)) {
                    JsonToken t = this._decodeScalar((ScalarEvent)evt);
                    this._currToken = t;
                    return t;
                }

                Mark m;
                if (evt.is(Event.ID.MappingStart)) {
                    m = evt.getStartMark();
                    MappingStartEvent map = (MappingStartEvent)evt;
                    this._currentAnchor = map.getAnchor();
                    this._parsingContext = this._parsingContext.createChildObjectContext(m.getLine(), m.getColumn());
                    return this._currToken = JsonToken.START_OBJECT;
                }

                if (evt.is(Event.ID.MappingEnd)) {
                    this._reportError("Not expecting END_OBJECT but a value");
                }

                if (evt.is(Event.ID.SequenceStart)) {
                    m = evt.getStartMark();
                    this._currentAnchor = ((NodeEvent)evt).getAnchor();
                    this._parsingContext = this._parsingContext.createChildArrayContext(m.getLine(), m.getColumn());
                    return this._currToken = JsonToken.START_ARRAY;
                }

                if (evt.is(Event.ID.SequenceEnd)) {
                    if (!this._parsingContext.inArray()) {
                        this._reportMismatchedEndMarker(93, '}');
                    }

                    this._parsingContext = this._parsingContext.getParent();
                    return this._currToken = JsonToken.END_ARRAY;
                }

                if (!evt.is(Event.ID.DocumentEnd) && !evt.is(Event.ID.DocumentStart)) {
                    if (evt.is(Event.ID.Alias)) {
                        AliasEvent alias = (AliasEvent)evt;
                        this._currentIsAlias = true;
                        this._textValue = alias.getAnchor();
                        this._cleanedTextValue = null;
                        return this._currToken = JsonToken.VALUE_STRING;
                    }

                    if (evt.is(Event.ID.StreamEnd)) {
                        this.close();
                        return this._currToken = null;
                    }

                    if (evt.is(Event.ID.StreamStart)) {
                    }
                }
            }
        }
    }
}
