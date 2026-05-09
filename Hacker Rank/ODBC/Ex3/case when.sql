SELECT 
    CASE
        WHEN grades.grade < 8 THEN NULL
        ELSE students.name
    END as Name,
    grades.grade, 
            students.marks
FROM students
JOIN grades
ON students.marks 
BETWEEN grades.min_mark AND grades.max_mark
ORDER BY 
    grades.grade DESC,
    CASE WHEN grades.grade >= 8 THEN students.name END ASC,
    CASE WHEN grades.grade < 8 THEN students.marks END ASC;
