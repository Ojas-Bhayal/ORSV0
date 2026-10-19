package com.sunilos.p4.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.sunilos.p4.bean.DropdownListBean;

/**
 * HTML Utility class to produce HTML contents like Dropdown List.
 * 
 * @author Rays Technologies
 * @version 1.0
 * @Copyright (c) Rays Technologies
 * 
 */

public class HTMLUtility {
    

	/** Escapes untrusted text for safe insertion into HTML text or quoted attributes. */
	public static String escapeHtml(Object value) {
		if (value == null) {
			return "";
		}
		String text = String.valueOf(value);
		return text.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	/**
	 * Create HTML SELECT list from MAP paramters values
	 * 
	 * @param name
	 * @param selectedVal
	 * @param map
	 * @return
	 */
	
	public static String getList(String name, String selectedVal,
			HashMap<String, String> map) {

		StringBuffer sb = new StringBuffer("<select class='form-control' name='" + escapeHtml(name) + "'>");

		Set<String> keys = map.keySet();
		String val = null;

		for (String key : keys) {
			val = map.get(key);
			if (key.trim().equals(selectedVal)) {
				sb.append("<option selected value='" + escapeHtml(key) + "'>" + escapeHtml(val)
						+ "</option>");
			} else {
				sb.append("<option value='" + escapeHtml(key) + "'>" + escapeHtml(val) + "</option>");
			}
		}
		sb.append("</select>");
		return sb.toString();
	}

	/**
	 * Create HTML SELECT List from List parameter
	 * 
	 * @param name
	 * @param selectedVal
	 * @param list
	 * @return
	 */
	public static String getList(String name, String selectedVal, List list) {

		Collections.sort(list);

		List<DropdownListBean> dd = (List<DropdownListBean>) list;

		StringBuffer sb = new StringBuffer("<select class='form-control' name='" + escapeHtml(name) + "'>");

		String key = null;
		String val = null;

		for (DropdownListBean obj : dd) {
			key = obj.getKey();
			val = obj.getValue();

			if (key.trim().equals(selectedVal)) {
				sb.append("<option selected value='" + escapeHtml(key) + "'>" + escapeHtml(val)
						+ "</option>");
			} else {
				sb.append("<option value='" + escapeHtml(key) + "'>" + escapeHtml(val) + "</option>");
			}
		}
		sb.append("</select>");
		return sb.toString();
	}

}
