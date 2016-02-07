package ru.cache.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Filter intercepting requests and responses. Check on request if current
 * request url matches one of supposed for caching. If true, then look in cache
 * and return if already cached. Proceed and cache on response otherwise.
 *
 * @author Natal Kaplya
 */
@WebFilter(urlPatterns = { "/*" })
public class CacheFilter implements Filter {

	private boolean doBeforeProcessing(ServletRequest request, ServletResponse response) throws IOException {
		if (request instanceof HttpServletRequest) {
			StringBuilder sb = new StringBuilder();
			sb.append(((HttpServletRequest) request).getRequestURL().toString());
			String q = ((HttpServletRequest) request).getQueryString();
			if (q != null) {
				sb.append("?");
				sb.append(q);
			}
			String cachedVal = RestCacheConfigurator.getCache().getFromCache(sb.toString(), request);
			if (cachedVal != null) {
				response.setContentType("text/html;charset=UTF-8");
				response.setCharacterEncoding("UTF-8");
				// LOG.log(Level.INFO, String.format("Cache for url: %s
				// \nreturned:%s", sb.toString(), cachedVal));
				response.getWriter().append(cachedVal).close();
				return true;
			} else {
				return false;
			}
		}
		return false;
	}

	@Override
	public void destroy() {
	}

	static class ServletOutputStreamCopier extends ServletOutputStream {

		private OutputStream outputStream;
		private ByteArrayOutputStream copy;

		public ServletOutputStreamCopier(OutputStream outputStream) {
			this.outputStream = outputStream;
			this.copy = new ByteArrayOutputStream(1024);
		}

		@Override
		public void write(int b) throws IOException {
			outputStream.write(b);
			copy.write(b);
		}

		public byte[] getCopy() {
			return copy.toByteArray();
		}

		@Override
		public boolean isReady() {
			throw new UnsupportedOperationException("Not supported yet."); // To
																			// change
																			// body
																			// of
																			// generated
																			// methods,
																			// choose
																			// Tools
																			// |
																			// Templates.
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
			throw new UnsupportedOperationException("Not supported yet."); // To
																			// change
																			// body
																			// of
																			// generated
																			// methods,
																			// choose
																			// Tools
																			// |
																			// Templates.
		}
	}

	static class HttpServletResponseCopier extends HttpServletResponseWrapper {

		private ServletOutputStream outputStream;
		private PrintWriter writer;
		private ServletOutputStreamCopier copier;

		public HttpServletResponseCopier(HttpServletResponse response) throws IOException {
			super(response);
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			if (writer != null) {
				throw new IllegalStateException("getWriter() has already been called on this response.");
			}

			if (outputStream == null) {
				outputStream = getResponse().getOutputStream();
				copier = new ServletOutputStreamCopier(outputStream);
			}

			return copier;
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			if (outputStream != null) {
				throw new IllegalStateException("getOutputStream() has already been called on this response.");
			}

			if (writer == null) {
				copier = new ServletOutputStreamCopier(getResponse().getOutputStream());
				writer = new PrintWriter(new OutputStreamWriter(copier, getResponse().getCharacterEncoding()), true);
			}

			return writer;
		}

		@Override
		public void flushBuffer() throws IOException {
			if (writer != null) {
				writer.flush();
			} else if (outputStream != null) {
				copier.flush();
			}
		}

		public byte[] getCopy() {
			if (copier != null) {
				return copier.getCopy();
			} else {
				return new byte[0];
			}
		}
	}

	private void doAfterProcessing(ServletRequest request, HttpServletResponseCopier response)
			throws IOException, ServletException {
		if (request instanceof HttpServletRequest) {
			StringBuilder sb = new StringBuilder();
			sb.append(((HttpServletRequest) request).getRequestURL().toString());
			String q = ((HttpServletRequest) request).getQueryString();
			if (q != null) {
				sb.append("?");
				sb.append(q);
			}
			if (RestCacheConfigurator.getCache().putInCache(sb.toString(), new String(response.getCopy(), "UTF-8"),
					request)) {
				// LOG.log(Level.INFO, String.format("Element to cache added.
				// Url: %s, \n Cache: %s", sb.toString().toString(),
				// ((HttpServletRequest) request).getRequestURL().toString()));
			}
		}
	}

	/**
	 *
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 * @param chain
	 *            The filter chain we are processing
	 *
	 * @exception IOException
	 *                if an input/output error occurs
	 * @exception ServletException
	 *                if a servlet error occurs
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (!RestCacheConfigurator.isActive()) {
			chain.doFilter(request, response);
			return;
		}

		if (doBeforeProcessing(request, response)) {
			// LOG.log(Level.INFO, "Cache returned for url:" +
			// ((HttpServletRequest)
			// request).getRequestURL().toString()+"?"+((HttpServletRequest)
			// request).getQueryString());
			return;
		}

		HttpServletResponseCopier responseCopier = new HttpServletResponseCopier((HttpServletResponse) response);

		chain.doFilter(request, responseCopier);
		responseCopier.flushBuffer();

		// byte[] copy = responseCopier.getCopy();
		// System.out.println(new String(copy,
		// response.getCharacterEncoding())); // Do your logging job here. This
		// is just a basic example.
		Object isRequestCacheable = request.getAttribute(KeyRestrictedCache.isCacheableAttributeName);
		if (isRequestCacheable != null && isRequestCacheable.equals(true)) {
			if (response instanceof HttpServletResponse) {
				// cache only if successful request
				if (((HttpServletResponse) response).getStatus() == 200) {
					doAfterProcessing(request, responseCopier);
				}
			}
		}
	}

	/**
	 * Init method for this filter
	 */
	@Override
	public void init(FilterConfig filterConfig) {
	}
}
