/*
 *  Copyright (C) 2020 the original author or authors.
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

package we.util;

/**
 * @author hongqiaowei
 */

public final class Consts {

    private Consts() {
    }

    public static final class S {
        public  static  final  String  DEFAULT               =  "default";
        public  static  final  String  TRUE                  =  "true";
        public  static  final  String  FALSE                 =  "false";
        public  static  final  String  TRUE1                 =  "1";
        public  static  final  String  FALSE0                =  "0";

        public  static  final  String  EMPTY                 =  "";
        public  static  final  String  SPACE_STR             =  " ";
        public  static  final  char    SPACE                 =  ' ';
        public  static  final  String  TWO_SPACE_STR         =  "  ";
                                                             
        public  static  final  char    COMMA                 =  ',';
        public  static  final  char    COLON                 =  ':';
        public  static  final  char    FORWARD_SLASH         =  '/';
        public  static  final  String  FORWARD_SLASH_STR     =  "/";
        public  static  final  char    BACK_SLASH            =  '\\';
        public  static  final  char    DOT                   =  '.';
        public  static  final  char    SEMICOLON             =  ';';
        public  static  final  char    QUESTION              =  '?';
        public  static  final  char    DOUBLE_QUOTE          =  '"';
        public  static  final  char    SINGLE_QUOTE          =  '\'';
        public  static  final  char    ASTERISK              =  '*';
        public  static  final  char    DASH                  =  '-';
        public  static  final  char    UNDER_LINE            =  '_';
        public  static  final  char    EQUAL                 =  '=';
        public  static  final  char    AT                    =  '@';
        public  static  final  char    LEFT_SQUARE_BRACKET   =  '[';
        public  static  final  char    RIGHT_SQUARE_BRACKET  =  ']';
        public  static  final  char    LEFT_BRACE            =  '{';
        public  static  final  char    RIGHT_BRACE           =  '}';
        public  static  final  char    SQUARE                =  '^';
        public  static  final  char    HASH                  =  '#';
        public  static  final  char    AND                   =  '&';
        public  static  final  char    OR                    =  '|';
                                                             
        public  static  final  char    LF                    =  '\n';
        public  static  final  char    TAB                   =  '\t';
        public  static  final  char    NUL                   =  '\u0000';
                                                             
        /*                                                   
        private static  final  char    c0                    =  SystemUtils.IS_OS_WINDOWS ? S.BACK_SLASH : S.FORWARD_SLASH;
        public  static  final  char    PATH_SEPARATOR        =  c0;
        */                                                   
        public  static  final  String  LINE_SEPARATOR        =  System.lineSeparator();
                                                             
        public  static  final  String  COMMA_SPACE           =  ", ";
        public  static  final  String  HTTP_PROTOCOL_PREFIX  =  "http://";
    }

    public static final class C {
        public static final String UTF8     = "UTF-8";
        public static final String GBK      = "GBK";
        public static final String ISO88591 = "ISO8859-1";
    }

    public static final class DP {
        public static final String DP10      = "yyyy-MM-dd";
        public static final String DP14      = "yyyyMMddHHmmss";
        public static final String DP19      = "yyyy-MM-dd HH:mm:ss";
        public static final String DP23      = "yyyy-MM-dd HH:mm:ss.SSS";
        public static final byte   MILLS_LEN = 13;
    }

    public static final class P {
        public static final String LOCAL   = "local";
        public static final String DEV     = "dev";
        public static final String TEST    = "test";
        public static final String FAT     = "fat";
        public static final String PREPROD = "preprod";
        public static final String UAT     = "uat";
        public static final String PRO     = "pro";
        public static final String PROD    = "prod";
    }

    public static final String HTTP_SERVER   = "http_server";
    public static final String HTTP_CLIENT   = "http_client";
    public static final String MYSQL         = "mysql";
    public static final String REDIS         = "redis";
    public static final String CODIS         = "codis";
    public static final String MONGO         = "mongo";
    public static final String KAFKA         = "kafka";
    public static final String ELASTICSEARCH = "elasticsearch";
    public static final String SCHED         = "sched";
    public static final String R2DBC         = "r2dbc";

    public static final String TRACE_ID = "id^";
}
