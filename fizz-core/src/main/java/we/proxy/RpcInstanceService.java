/*
 *  Copyright (C) 2021 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package we.proxy;

/**
 * RPC instance service interface
 *
 * @author zhongjie
 */
public interface RpcInstanceService {
     enum RpcTypeEnum {
        /**
         * gRPC
         */
        gRPC((byte)2),
        /**
         * HTTP
         */
        HTTP((byte)3);

        RpcTypeEnum(Byte type) {
            this.type = type;
        }
        private Byte type;

        public Byte getType() {
            return type;
        }
    }

    /**
     * get an instance
     *
     * @param rpcTypeEnum RPC type
     * @param service service name
     * @return instance, {@code null} if instance not-exist
     */
    String getInstance(RpcTypeEnum rpcTypeEnum, String service);
}
