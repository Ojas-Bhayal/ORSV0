package com.sunilos.p4.util;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.EnrollmentBean;
import com.sunilos.p4.bean.FacultyAllocationBean;
import com.sunilos.p4.bean.FacultyBean;
import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.bean.StudentBean;
import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.model.EnrollmentModel;
import com.sunilos.p4.model.FacultyAllocationModel;
import com.sunilos.p4.model.FacultyModel;
import com.sunilos.p4.model.StudentModel;

/**
 * ScopeAuthorizationUtil resolves authenticated row-level data scope for
 * Faculty, Program Chair, and Student roles (Phase 3 Tasks 3, 4, 5, 6, 10).
 *
 * All lookups fail closed: if a required database record is missing or an
 * exception occurs, the returned scope is empty/invalid and access is denied.
 */
public final class ScopeAuthorizationUtil {

	private static final Logger log = Logger.getLogger(ScopeAuthorizationUtil.class);

	private ScopeAuthorizationUtil() {
	}

	public static final class DataScope {
		private final boolean valid;
		private final long roleId;
		private final long studentId;
		private final long collegeId;
		private final Set<Long> courseIds;
		private final Set<Long> subjectIds;

		public DataScope(boolean valid, long roleId, long studentId, long collegeId, Set<Long> courseIds,
				Set<Long> subjectIds) {
			this.valid = valid;
			this.roleId = roleId;
			this.studentId = studentId;
			this.collegeId = collegeId;
			this.courseIds = courseIds != null ? Collections.unmodifiableSet(new LinkedHashSet<>(courseIds))
					: Collections.emptySet();
			this.subjectIds = subjectIds != null ? Collections.unmodifiableSet(new LinkedHashSet<>(subjectIds))
					: Collections.emptySet();
		}

		public boolean isValid() {
			return valid;
		}

		public long getRoleId() {
			return roleId;
		}

		public long getStudentId() {
			return studentId;
		}

		public long getCollegeId() {
			return collegeId;
		}

		public Set<Long> getCourseIds() {
			return courseIds;
		}

		public Set<Long> getSubjectIds() {
			return subjectIds;
		}

		public boolean isCourseAllowed(long requestedCourseId) {
			if (!valid) {
				return false;
			}
			if (roleId == RoleBean.ADMIN) {
				return true;
			}
			if (requestedCourseId <= 0) {
				return !courseIds.isEmpty();
			}
			return courseIds.contains(requestedCourseId);
		}

		public boolean isSubjectAllowed(long requestedCourseId, long requestedSubjectId) {
			if (!isCourseAllowed(requestedCourseId)) {
				return false;
			}
			if (roleId == RoleBean.ADMIN) {
				return true;
			}
			if (requestedSubjectId <= 0) {
				return !subjectIds.isEmpty();
			}
			return subjectIds.contains(requestedSubjectId);
		}

		public boolean isCollegeAllowed(long requestedCollegeId) {
			if (!valid) {
				return false;
			}
			if (roleId == RoleBean.ADMIN) {
				return true;
			}
			if (requestedCollegeId <= 0) {
				return true;
			}
			return collegeId > 0 && collegeId == requestedCollegeId;
		}
	}

	/**
	 * Resolves the authoritative row-level DataScope for the authenticated user.
	 * Never trusts client-submitted IDs; queries ST_STUDENT, ST_ENROLLMENT,
	 * ST_FACULTY, and ST_FACULTY_ALLOCATION directly.
	 */
	public static DataScope resolveScope(UserBean user) {
		if (user == null || DataValidator.isNull(user.getLogin())) {
			return new DataScope(false, 0L, -1L, -1L, null, null);
		}
		long roleId = user.getRoleId();
		if (roleId == RoleBean.ADMIN) {
			return new DataScope(true, RoleBean.ADMIN, -1L, -1L, null, null);
		}

		try {
			if (roleId == RoleBean.STUDENT) {
				StudentModel studentModel = new StudentModel();
				StudentBean student = studentModel.findByEmailId(user.getLogin());
				if (student == null || student.getId() <= 0) {
					return new DataScope(false, roleId, -1L, -1L, null, null);
				}
				EnrollmentModel enrollmentModel = new EnrollmentModel();
				List<EnrollmentBean> enrollments = enrollmentModel.findByStudentId(student.getId());
				Set<Long> enrolledCourses = new LinkedHashSet<>();
				if (enrollments != null) {
					for (EnrollmentBean e : enrollments) {
						if ("ACTIVE".equalsIgnoreCase(e.getStatus()) && e.getCourseId() > 0) {
							enrolledCourses.add(e.getCourseId());
						}
					}
				}
				long colId = student.getCollegeId() != null ? student.getCollegeId() : -1L;
				return new DataScope(!enrolledCourses.isEmpty(), roleId, student.getId(), colId, enrolledCourses, null);
			}

			if (roleId == RoleBean.FACULTY || roleId == RoleBean.PROGRAM_CHAIR) {
				FacultyModel facultyModel = new FacultyModel();
				FacultyBean profile = facultyModel.findByEmail(user.getLogin());
				long collegeId = (profile != null && profile.getCollegeId() > 0) ? profile.getCollegeId() : -1L;

				FacultyAllocationModel allocModel = new FacultyAllocationModel();
				List<FacultyAllocationBean> allocations = allocModel.findByFacultyEmail(user.getLogin());
				Set<Long> courses = new LinkedHashSet<>();
				Set<Long> subjects = new LinkedHashSet<>();
				if (allocations != null) {
					for (FacultyAllocationBean a : allocations) {
						if (a.getCourseId() > 0) {
							courses.add(a.getCourseId());
						}
						if (a.getSubjectId() > 0) {
							subjects.add(a.getSubjectId());
						}
					}
				}
				if (roleId == RoleBean.FACULTY) {
					// Spec 05: ST_FACULTY_ALLOCATION is authoritative for Faculty scope.
					// Stale courseId/subjectId fields on ST_FACULTY must NOT keep access active after allocation removal.
					boolean valid = !courses.isEmpty() && !subjects.isEmpty();
					return new DataScope(valid, roleId, -1L, collegeId, courses, subjects);
				}

				// For Program Chair, include department/college courses from profile if present
				if (profile != null && profile.getCourseId() > 0) {
					courses.add(profile.getCourseId());
				}
				if (profile != null && profile.getSubjectId() > 0) {
					subjects.add(profile.getSubjectId());
				}

				// Program Chair must have a valid department/college or course scope
				boolean validChair = (collegeId > 0) || !courses.isEmpty();
				return new DataScope(validChair, roleId, -1L, collegeId, courses, subjects);
			}
		} catch (Exception e) {
			log.error("Failed closed while resolving row-level data scope for " + user.getLogin(), e);
		}
		return new DataScope(false, roleId, -1L, -1L, null, null);
	}
}
