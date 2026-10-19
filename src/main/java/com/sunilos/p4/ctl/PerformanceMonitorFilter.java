package com.sunilos.p4.ctl;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.PropertyReader;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Performance Monitor Filter measures request duration against the quantified
 * SLA acceptance thresholds in SE Experiment 2 (Stories 1 & 2, Constraints 1 & 2):
 * - Search & Marksheet result queries: <= 3000 ms (sla.search.ms)
 * - Faculty grade / form save submissions: <= 2000 ms (sla.save.ms)
 * - Standard page navigation & dashboards: <= 1500 ms (sla.nav.ms)
 */
@WebFilter("/*")
public class PerformanceMonitorFilter implements Filter {

	private static Logger log = Logger.getLogger(PerformanceMonitorFilter.class);

	@Override
	public void init(FilterConfig conf) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;
		String url = request.getRequestURI();
		String method = request.getMethod();

		long thresholdMs = resolveThresholdMs(url, method);

		long start = System.currentTimeMillis();
		chain.doFilter(req, resp);
		long end = System.currentTimeMillis();

		long diffMs = end - start;
		boolean compliant = diffMs <= thresholdMs;

		if (!response.isCommitted()) {
			response.setHeader("X-ORS-Response-Time-Ms", String.valueOf(diffMs));
			response.setHeader("X-ORS-SLA-Threshold-Ms", String.valueOf(thresholdMs));
			response.setHeader("X-ORS-SLA-Compliant", String.valueOf(compliant));
		}

		if (!compliant) {
			log.warn("SLA WARNING: " + url + " took " + diffMs + " ms (threshold: " + thresholdMs + " ms)");
		} else {
			log.debug(url + " executed in " + diffMs + " ms (SLA <= " + thresholdMs + " ms)");
		}
	}

	public static long resolveThresholdMs(String url, String method) {
		long searchSla = DataUtility.getLong(PropertyReader.getValue("sla.search.ms"));
		long saveSla = DataUtility.getLong(PropertyReader.getValue("sla.save.ms"));
		long navSla = DataUtility.getLong(PropertyReader.getValue("sla.nav.ms"));

		if (searchSla <= 0) searchSla = 3000L;
		if (saveSla <= 0) saveSla = 2000L;
		if (navSla <= 0) navSla = 1500L;

		if (url != null && (url.contains("ListCtl") || url.contains("GetMarksheetCtl") || url.contains("AuditCtl"))) {
			return searchSla;
		}
		if ("POST".equalsIgnoreCase(method)) {
			return saveSla;
		}
		return navSla;
	}

	@Override
	public void destroy() {
	}

}