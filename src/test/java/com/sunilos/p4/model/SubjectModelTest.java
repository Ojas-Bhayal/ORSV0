package com.sunilos.p4.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sunilos.p4.bean.CourseBean;
import com.sunilos.p4.bean.SubjectBean;
import com.sunilos.p4.exception.DuplicateRecordException;
import com.sunilos.p4.util.JDBCDataSource;

class SubjectModelTest {

	private SubjectModel model;
	private long course1Id;
	private long course2Id;

	@AfterEach
	void clearTestConnectionSupplier() {
		JDBCDataSource.clearTestConnectionSupplier();
	}

	@BeforeEach
	void setUp() throws Exception {
		Class.forName("org.hsqldb.jdbcDriver");
		System.setProperty("ORS_DB_URL", "jdbc:hsqldb:mem:ors_subject_test_db;sql.syntax_mys=true");
		JDBCDataSource.setTestConnectionSupplier(() -> DriverManager.getConnection(
				"jdbc:hsqldb:mem:ors_subject_test_db;sql.syntax_mys=true", "sa", ""));
		System.setProperty("ORS_DB_DRIVER", "org.hsqldb.jdbcDriver");
		System.setProperty("ORS_DB_USER", "sa");
		System.setProperty("ORS_DB_PASSWORD", "");
		JDBCDataSource.closeConnection(null);

		try (Connection conn = JDBCDataSource.getConnection(); Statement st = conn.createStatement()) {
			st.execute("DROP TABLE ST_SUBJECT IF EXISTS");
			st.execute("DROP TABLE ST_COURSE IF EXISTS");
			st.execute("CREATE TABLE ST_COURSE (ID BIGINT PRIMARY KEY, NAME VARCHAR(100), DESCRIPTION VARCHAR(500), DURATION VARCHAR(50), CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP)");
			st.execute("CREATE TABLE ST_SUBJECT (ID BIGINT PRIMARY KEY, NAME VARCHAR(100), DESCRIPTION VARCHAR(500), COURSE_ID BIGINT, COURSE_NAME VARCHAR(100), CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP, CONSTRAINT uq_sc UNIQUE (COURSE_ID, NAME))");
			conn.commit();
		}
		CourseModel cm = new CourseModel();
		CourseBean c1 = new CourseBean(); c1.setName("Java"); c1.setDescription("J"); c1.setDuration("6M");
		course1Id = cm.add(c1);
		CourseBean c2 = new CourseBean(); c2.setName("Python"); c2.setDescription("P"); c2.setDuration("6M");
		course2Id = cm.add(c2);
		model = new SubjectModel();
	}

	@Test
	void testCompositeCourseSubjectUniquenessAndCrud() throws Exception {
		SubjectBean s1 = new SubjectBean();
		s1.setName("Algorithms"); s1.setDescription("Java Algo"); s1.setCourseId(course1Id);
		long pk1 = model.add(s1);
		assertTrue(pk1 > 0);

		// Duplicate in the SAME course must throw DuplicateRecordException
		SubjectBean dupSameCourse = new SubjectBean();
		dupSameCourse.setName("Algorithms"); dupSameCourse.setDescription("Dup"); dupSameCourse.setCourseId(course1Id);
		assertThrows(DuplicateRecordException.class, () -> model.add(dupSameCourse));

		// Same subject name in a DIFFERENT course is allowed by composite (COURSE_ID, NAME) uniqueness
		SubjectBean sameNameDiffCourse = new SubjectBean();
		sameNameDiffCourse.setName("Algorithms"); sameNameDiffCourse.setDescription("Py Algo"); sameNameDiffCourse.setCourseId(course2Id);
		long pk2 = model.add(sameNameDiffCourse);
		assertTrue(pk2 > pk1);

		SubjectBean found = model.findByPK(pk1);
		assertNotNull(found);
		assertEquals(course1Id, found.getCourseId());

		model.delete(found);
		assertNull(model.findByPK(pk1));
	}
}
