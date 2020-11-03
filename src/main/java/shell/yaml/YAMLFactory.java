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

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.io.IOContext;

import java.io.*;


public class YAMLFactory extends  com.fasterxml.jackson.dataformat.yaml.YAMLFactory{
    public YAMLParser createParser(InputStream in) throws IOException {
        IOContext ctxt = this._createContext(in, false);
        return this._createParser(this._decorate(in, ctxt), ctxt);
    }

    public YAMLParser createParser(File f) throws IOException {
        IOContext ctxt = this._createContext(f, true);
        return this._createParser(this._decorate(new FileInputStream(f), ctxt), ctxt);
    }
    protected YAMLParser _createParser(InputStream in, IOContext ctxt) throws IOException {
        return new YAMLParser(ctxt, this._getBufferRecycler(), this._parserFeatures, this._yamlParserFeatures, this._objectCodec, this._createReader(in, (JsonEncoding)null, ctxt));
    }

}
