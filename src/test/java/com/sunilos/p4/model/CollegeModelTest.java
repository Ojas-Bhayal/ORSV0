package com.sunilos.p4.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sunilos.p4.bean.CollegeBean;
import com.sunilos.p4.exception.DuplicateRecordException;
import com.sunilos.p4.util.JDBCDataSource;

class CollegeModelTest {

	private CollegeModel model;

	@AfterEach
	void clearTestConnectionSupplier() {
		JDBCDataSource.clearTestConnectionSupplier();
	}

	@BeforeEach
	void setUp() throws Exception {
		Class.forName("org.hsqldb.jdbcDriver");
		System.setProperty("ORS_DB_URL", "jdbc:hsqldb:mem:ors_college_test_db;sql.syntax_mys=true");
		JDBCDataSource.setTestConnectionSupplier(() -> DriverManager.getConnection(
				"jdbc:hsqldb:mem:ors_college_test_db;sql.syntax_mys=true", "sa", ""));
		System.setProperty("ORS_DB_DRIVER", "org.hsqldb.jdbcDriver");
		System.setProperty("ORS_DB_USER", "sa");
		System.setProperty("ORS_DB_PASSWORD", "");
		JDBCDataSource.closeConnection(null);

		try (Connection conn = JDBCDataSource.getConnection(); Statement st = conn.createStatement()) {
			st.execute("DROP TABLE ST_COLLEGE IF EXISTS");
			st.execute("CREATE TABLE ST_COLLEGE (ID BIGINT PRIMARY KEY, NAME VARCHAR(100), ADDRESS VARCHAR(255), STATE VARCHAR(50), CITY VARCHAR(50), PHONE_NO VARCHAR(20), CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP)");
			conn.commit();
		}
		model = new CollegeModel();
	}

	private CollegeBean buildBean(String name) {
		CollegeBean bean = new CollegeBean();
		bean.setName(name);
		bean.setAddress("Bhopal Road");
		bean.setState("MP");
		bean.setCity("Bhopal");
		bean.setPhoneNo("9826012345");
		return bean;
	}

	@Test
	void testAddFindUpdateDeleteAndSearch() throws Exception {
		long pk1 = model.add(buildBean("MIT"));
		assertTrue(pk1 > 0);

		assertThrows(DuplicateRecordException.class, () -> model.add(buildBean("MIT")));

		CollegeBean found = model.findByPK(pk1);
		assertNotNull(found);
		assertEquals("MIT", found.getName());

		CollegeBean byName = model.findByName("MIT");
		assertNotNull(byName);
		assertEquals(pk1, byName.getId());

		found.setName("LNCT");
		model.update(found);
		assertEquals("LNCT", model.findByPK(pk1).getName());

		CollegeBean criteria = new CollegeBean();
		criteria.setName("LNCT");
		List<CollegeBean> results = model.search(criteria, 0, 0);
		assertEquals(1, results.size());

		model.delete(found);
		assertNull(model.findByPK(pk1));
	}
}
