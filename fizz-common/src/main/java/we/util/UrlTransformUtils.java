package we.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fizz gateway url transform util class
 *
 * @author zhongjie
 */
public class UrlTransformUtils {

	private static final Logger log = LoggerFactory.getLogger(UrlTransformUtils.class);

	private UrlTransformUtils() {}

	public  static final FizzGatewayUrlAntPathMatcher ANT_PATH_MATCHER = new FizzGatewayUrlAntPathMatcher();

	/**
	 * transform the backend path to the real backend request path
	 * @param frontendPath frontend path
	 * @param backendPath backend path
	 * @param reqPath request path
	 * @return the transformed backend path
	 * @throws IllegalStateException when the request path does not match the frontend path pattern
	 * @throws IllegalArgumentException The number of capturing groups in the pattern segment does not match the number of URI template variables it defines
	 */
	public static String transform(String frontendPath, String backendPath, String reqPath) {
		Assert.hasText(frontendPath, "frontend path cannot be null");
		Assert.hasText(backendPath, "backend path cannot be null");
		Assert.hasText(reqPath, "req path cannot be null");
		String bp = backendPath;
		Map<String, String> variables = ANT_PATH_MATCHER.extractUriTemplateVariables(frontendPath, reqPath);
		for (Map.Entry<String, String> entry : variables.entrySet()) {
			backendPath = backendPath.replaceAll("\\{" + Matcher.quoteReplacement(entry.getKey()) + "}", Matcher.quoteReplacement(entry.getValue()));
		}

		if (backendPath.indexOf('{') != -1) {
			backendPath = backendPath.replaceAll("\\{[^/]*}", "");
		}

		if (log.isDebugEnabled()) {
			log.debug("req: " + reqPath + ", frontend: " + frontendPath + ", backend: " + bp + ", target: " + backendPath);
		}

		return backendPath;
	}

	/**
	 * 自定义Ant风格路径匹配器
	 * 设置默认路径分隔符为{@code #}
	 * 使用{@link FizzGatewayAntPathStringMatcher}设置自定义的参数变量值（额外返回变量名为$1...n的键值对）
	 *
	 * @author zhongjie
	 */
	public static class FizzGatewayUrlAntPathMatcher extends AntPathMatcher {
		private static final String DEFAULT_PATH_SEPARATOR = "#";

		private static final int CACHE_TURNOFF_THRESHOLD = 65536;

		private volatile Boolean cachePatterns;

		private final Map<String, String> replaceDoubleStarPatternCache = new ConcurrentHashMap<>(256);

		private final Map<String, String[]> tokenizedPatternCache = new ConcurrentHashMap<>(256);

		final Map<String, AntPathStringMatcher> stringMatcherCache = new ConcurrentHashMap<>(256);

		private boolean caseSensitive = true;

		private static AntPathMatcher DEFAULT_ANT_PATH_MATCHER = new AntPathMatcher();

		public FizzGatewayUrlAntPathMatcher() {
			// 设置默认路径分隔符为#
			super(DEFAULT_PATH_SEPARATOR);
		}

		@Override
		public void setPathSeparator(String pathSeparator) {
			throw new RuntimeException("operation not support");
		}

		@Override
		public void setTrimTokens(boolean trimTokens) {
			throw new RuntimeException("operation not support");
		}

		@Override
		public void setCaseSensitive(boolean caseSensitive) {
			super.setCaseSensitive(caseSensitive);
			this.caseSensitive = caseSensitive;
		}

		@Override
		public void setCachePatterns(boolean cachePatterns) {
			super.setCachePatterns(cachePatterns);
			this.cachePatterns = cachePatterns;
		}

		@Override
		protected AntPathStringMatcher getStringMatcher(String pattern) {
			AntPathStringMatcher matcher = null;
			Boolean cachePatterns = this.cachePatterns;
			if (cachePatterns == null || cachePatterns) {
				matcher = this.stringMatcherCache.get(pattern);
			}
			if (matcher == null) {
				matcher = new FizzGatewayAntPathStringMatcher(pattern, this.caseSensitive);
				if (cachePatterns == null && this.stringMatcherCache.size() >= CACHE_TURNOFF_THRESHOLD) {
					// Try to adapt to the runtime situation that we're encountering:
					// There are obviously too many different patterns coming in here...
					// So let's turn off the cache since the patterns are unlikely to be reoccurring.
					deactivatePatternCache();
					return matcher;
				}
				if (cachePatterns == null || cachePatterns) {
					this.stringMatcherCache.put(pattern, matcher);
				}
			}
			return matcher;
		}

		@Override
		protected String[] tokenizePattern(String pattern) {
			String[] tokenized = null;
			Boolean cachePatterns = this.cachePatterns;
			if (cachePatterns == null || cachePatterns) {
				tokenized = this.tokenizedPatternCache.get(pattern);
			}
			if (tokenized == null) {
				tokenized = tokenizePath(pattern);
				if (cachePatterns == null && this.tokenizedPatternCache.size() >= CACHE_TURNOFF_THRESHOLD) {
					// Try to adapt to the runtime situation that we're encountering:
					// There are obviously too many different patterns coming in here...
					// So let's turn off the cache since the patterns are unlikely to be reoccurring.
					deactivatePatternCache();
					return tokenized;
				}
				if (cachePatterns == null || cachePatterns) {
					this.tokenizedPatternCache.put(pattern, tokenized);
				}
			}
			return tokenized;
		}

		private void deactivatePatternCache() {
			this.cachePatterns = false;
			this.tokenizedPatternCache.clear();
			this.stringMatcherCache.clear();
			this.replaceDoubleStarPatternCache.clear();
		}

		@Override
		public String extractPathWithinPattern(String pattern, String path) {
			return DEFAULT_ANT_PATH_MATCHER.extractPathWithinPattern(pattern, path);
		}

		@Override
		public String combine(String pattern1, String pattern2) {
			return DEFAULT_ANT_PATH_MATCHER.combine(pattern1, pattern2);
		}

		@Override
		protected boolean doMatch(String pattern, String path, boolean fullMatch, Map<String, String> uriTemplateVariables) {
			String replaceDoubleStarPattern = null;
			if (pattern != null) {
				replaceDoubleStarPattern = getReplaceDoubleStarPattern(pattern);
			}
			return super.doMatch(replaceDoubleStarPattern, path, fullMatch, uriTemplateVariables);
		}

		private String getReplaceDoubleStarPattern(String pattern) {
			String replaceDoubleStarPattern = null;
			Boolean cachePatterns = this.cachePatterns;
			if (cachePatterns == null || cachePatterns) {
				replaceDoubleStarPattern = this.replaceDoubleStarPatternCache.get(pattern);
			}
			if (replaceDoubleStarPattern == null) {
				// by-zhongjie 替换**为.*正则模式
				replaceDoubleStarPattern = pattern.replaceAll("/\\*\\*$", "/{\\$:.*}")
					.replaceAll("/\\*\\*/", "/{\\$:.*}/")
					.replaceAll("^\\*\\*/", "{\\$:.*}/");
				if (cachePatterns == null && this.replaceDoubleStarPatternCache.size() >= CACHE_TURNOFF_THRESHOLD) {
					// Try to adapt to the runtime situation that we're encountering:
					// There are obviously too many different patterns coming in here...
					// So let's turn off the cache since the patterns are unlikely to be reoccurring.
					deactivatePatternCache();
					return replaceDoubleStarPattern;
				}
				if (cachePatterns == null || cachePatterns) {
					this.replaceDoubleStarPatternCache.put(pattern, replaceDoubleStarPattern);
				}
			}
			return replaceDoubleStarPattern;
		}

		protected static class FizzGatewayAntPathStringMatcher extends AntPathStringMatcher {
			// by-zhongjie 将 \?|\*|\{((?:\{[^/]+?\}|[^/{}]|\\[{}])+?)\} 改为 \?|\*|\{((?:\{[^/]+?\}|[^{}]|\\[{}])+?)\}，排除/的限制
			private static final Pattern GLOB_PATTERN = Pattern.compile("\\?|\\*|\\{((?:\\{[^/]+?\\}|[^{}]|\\\\[{}])+?)\\}");

			// by-zhongjie 将 (.*) 改为 ([^/]*)，限制变量只能匹配在非/的字符内
			private static final String DEFAULT_VARIABLE_PATTERN = "([^/]*)";

			private final Pattern pattern;

			private final List<String> variableNames = new LinkedList<>();

			// by-zhongjie 匿名占位符
			private final String ANONYMOUS_PLACEHOLDER = "$";

			public FizzGatewayAntPathStringMatcher(String pattern) {
				this(pattern, true);
			}

			public FizzGatewayAntPathStringMatcher(String pattern, boolean caseSensitive) {
				super(pattern, caseSensitive);
				StringBuilder patternBuilder = new StringBuilder();
				Matcher matcher = GLOB_PATTERN.matcher(pattern);
				int end = 0;
				while (matcher.find()) {
					patternBuilder.append(quote(pattern, end, matcher.start()));
					String match = matcher.group();
					if ("?".equals(match)) {
						// by-zhongjie 对 ? 也使用模式匹配
						patternBuilder.append('(');
						patternBuilder.append('.');
						patternBuilder.append(')');
						this.variableNames.add(ANONYMOUS_PLACEHOLDER);
					}
					else if ("*".equals(match)) {
						// by-zhongjie 对 * 也使用模式匹配
						patternBuilder.append(DEFAULT_VARIABLE_PATTERN);
						this.variableNames.add(ANONYMOUS_PLACEHOLDER);
					}
					else if (match.startsWith("{") && match.endsWith("}")) {
						int colonIdx = match.indexOf(':');
						if (colonIdx == -1) {
							patternBuilder.append(DEFAULT_VARIABLE_PATTERN);
							this.variableNames.add(matcher.group(1));
						}
						else {
							String variablePattern = match.substring(colonIdx + 1, match.length() - 1);
							patternBuilder.append('(');
							patternBuilder.append(variablePattern);
							patternBuilder.append(')');
							String variableName = match.substring(1, colonIdx);
							this.variableNames.add(variableName);
						}
					}
					end = matcher.end();
				}
				patternBuilder.append(quote(pattern, end, pattern.length()));
				this.pattern = (caseSensitive ? Pattern.compile(patternBuilder.toString()) :
					Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE));
			}

			private String quote(String s, int start, int end) {
				if (start == end) {
					return "";
				}
				return Pattern.quote(s.substring(start, end));
			}


			@Override
			public boolean matchStrings(String str, @Nullable Map<String, String> uriTemplateVariables) {
				Matcher matcher = this.pattern.matcher(str);
				if (matcher.matches()) {
					if (uriTemplateVariables != null) {
						// SPR-8455
						if (this.variableNames.size() != matcher.groupCount()) {
							throw new IllegalArgumentException("The number of capturing groups in the pattern segment " +
								this.pattern + " does not match the number of URI template variables it defines, " +
								"which can occur if capturing groups are used in a URI template regex. " +
								"Use non-capturing groups instead.");
						}
						for (int i = 1; i <= matcher.groupCount(); i++) {
							String name = this.variableNames.get(i - 1);
							String value = matcher.group(i);

							if (!ANONYMOUS_PLACEHOLDER.equals(name)) {
								uriTemplateVariables.put(name, value);
							}
							// by-zhongjie 对提取到的变量按序号输出
							uriTemplateVariables.put(ANONYMOUS_PLACEHOLDER + i, value);
						}
					}
					return true;
				}
				else {
					return false;
				}
			}
		}
	}
}
