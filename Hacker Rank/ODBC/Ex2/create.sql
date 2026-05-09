CREATE TABLE  student(
     student_id INTEGER PRIMARY KEY,
     student_Name TEXT UNIQUE,
     department TEXT NOT NULL CHECK (department IN ('CSE', 'ECE', 'EEE')));
