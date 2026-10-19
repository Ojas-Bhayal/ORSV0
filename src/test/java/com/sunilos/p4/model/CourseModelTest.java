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

import com.sunilos.p4.bean.CourseBean;
import com.sunilos.p4.exception.DuplicateRecordException;
import com.sunilos.p4.util.JDBCDataSource;

class CourseModelTest {

	private CourseModel model;

	@AfterEach
	void clearTestConnectionSupplier() {
		JDBCDataSource.clearTestConnectionSupplier();
	}

	@BeforeEach
	void setUp() throws Exception {
		Class.forName("org.hsqldb.jdbcDriver");
		System.setProperty("ORS_DB_URL", "jdbc:hsqldb:mem:ors_course_test_db;sql.syntax_mys=true");
		JDBCDataSource.setTestConnectionSupplier(() -> DriverManager.getConnection(
				"jdbc:hsqldb:mem:ors_course_test_db;sql.syntax_mys=true", "sa", ""));
		System.setProperty("ORS_DB_DRIVER", "org.hsqldb.jdbcDriver");
		System.setProperty("ORS_DB_USER", "sa");
		System.setProperty("ORS_DB_PASSWORD", "");
		JDBCDataSource.closeConnection(null);

		try (Connection conn = JDBCDataSource.getConnection(); Statement st = conn.createStatement()) {
			st.execute("DROP TABLE ST_COURSE IF EXISTS");
			st.execute("CREATE TABLE ST_COURSE (ID BIGINT PRIMARY KEY, NAME VARCHAR(100), DESCRIPTION VARCHAR(500), DURATION VARCHAR(50), CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP)");
			conn.commit();
		}
		model = new CourseModel();
	}

	private CourseBean buildBean(String name) {
		CourseBean bean = new CourseBean();
		bean.setName(name);
		bean.setDescription("Core curriculum");
		bean.setDuration("6M");
		return bean;
	}

	@Test
	void testAddFindUpdateDeleteAndSearch() throws Exception {
		long pk = model.add(buildBean("Java"));
		assertTrue(pk > 0);
		assertThrows(DuplicateRecordException.class, () -> model.add(buildBean("Java")));

		CourseBean found = model.findByPK(pk);
		assertNotNull(found);
		assertEquals("Java", found.getName());

		found.setName("Spring");
		model.update(found);
		assertEquals("Spring", model.findByName("Spring").getName());

		CourseBean criteria = new CourseBean();
		criteria.setName("Spring");
		List<CourseBean> list = model.search(criteria, 0, 0);
		assertEquals(1, list.size());

		model.delete(found);
		assertNull(model.findByPK(pk));
	}
}
