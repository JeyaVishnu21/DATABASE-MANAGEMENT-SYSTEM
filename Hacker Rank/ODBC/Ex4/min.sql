--occupations

Select
    min(case when occupation = 'Doctor'     then name end),
    min(case when occupation = 'Professor'  then name end),
    min(case when occupation = 'Singer'     then name end),
    min(case when occupation = 'Actor'      then name end)
From (
    Select
        name,
        occupation,
        row_number() over (Partition by occupation Order by name) as rnum
    From Occupations) as result
Group by rnum;
