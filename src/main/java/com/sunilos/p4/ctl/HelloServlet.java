package com.sunilos.p4.ctl;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Build Identifier & Health Check Endpoint (/HelloServlet and /version).
 * Phase 1 Task 5: Reports the packaged release build identifier so runtime
 * deployments can be verified against the packaged release.
 */
@WebServlet(urlPatterns = { "/HelloServlet", "/version" })
public class HelloServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	public static final String DEFAULT_BUILD_ID = "ORS-2.1.1-RELEASE-20261015";
	public static final String DEFAULT_VERSION = "2.1.1";
	private static final ObjectMapper MAPPER = new ObjectMapper();

	public static Properties loadBuildProperties() {
		Properties props = new Properties();
		props.setProperty("build.id", DEFAULT_BUILD_ID);
		props.setProperty("build.version", DEFAULT_VERSION);
		props.setProperty("build.timestamp", "2026-10-15");
		try (InputStream in = HelloServlet.class.getClassLoader().getResourceAsStream("build.properties")) {
			if (in != null) {
				props.load(in);
			}
		} catch (Exception ignored) {
		}
		return props;
	}

	public static String getBuildId() {
		return loadBuildProperties().getProperty("build.id", DEFAULT_BUILD_ID);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Properties props = loadBuildProperties();
		String buildId = props.getProperty("build.id", DEFAULT_BUILD_ID);
		response.setContentType("application/json;charset=UTF-8");
		response.setHeader("X-ORS-Build-Id", buildId);

		Map<String, String> payload = new LinkedHashMap<>();
		payload.put("status", "UP");
		payload.put("application", "ORSV0");
		payload.put("buildId", buildId);
		payload.put("version", props.getProperty("build.version", DEFAULT_VERSION));
		payload.put("timestamp", props.getProperty("build.timestamp", ""));

		PrintWriter out = response.getWriter();
		out.print(MAPPER.writeValueAsString(payload));
		out.flush();
	}
}
