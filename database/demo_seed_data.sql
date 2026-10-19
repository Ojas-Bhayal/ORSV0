-- Optional idempotent sample seed for demo databases created before sample marks were included in demo_ors.sql.
-- Never run demo marks against a production database or a database with real results.

INSERT INTO `st_marksheet` (`ID`,`ROLL_NO`,`STUDENT_ID`,`NAME`,`COURSE_ID`,`COURSE_NAME`,`SUBJECT_ID`,`SUBJECT_NAME`,`MARKS`,`CREATED_BY`,`MODIFIED_BY`,`CREATED_DATETIME`,`MODIFIED_DATETIME`) VALUES
(1,'1',2,'Manish Sharma',1,'Java',1,'String',95,'ram@gmail.com','ram@gmail.com','2026-05-30 11:40:00','2026-05-30 11:40:00'),
(2,'2',1,'Rahul Sahu',1,'Java',1,'String',88,'ram@gmail.com','ram@gmail.com','2026-05-30 11:40:00','2026-05-30 11:40:00'),
(3,'3',3,'Vipin Chandore',1,'Java',1,'String',89,'ram@gmail.com','ram@gmail.com','2026-05-30 11:40:00','2026-05-30 11:40:00'),
(4,'4',4,'Deepak Kumar',1,'Java',1,'String',78,'ram@gmail.com','ram@gmail.com','2026-05-30 11:40:00','2026-05-30 11:40:00'),
(5,'5',5,'Sivam Kumar',1,'Java',1,'String',84,'ram@gmail.com','ram@gmail.com','2026-05-30 11:40:00','2026-05-30 11:40:00')
ON DUPLICATE KEY UPDATE `MARKS`=VALUES(`MARKS`), `MODIFIED_BY`=VALUES(`MODIFIED_BY`), `MODIFIED_DATETIME`=VALUES(`MODIFIED_DATETIME`);
