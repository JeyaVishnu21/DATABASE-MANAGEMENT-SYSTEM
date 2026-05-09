--contest leaderboard

SELECT
    m.hacker_id,
    h.name,
    sum(m.max) as total
From
    (
    SELECT
    s.hacker_id,
    h.name,
    s.challenge_id,
    max(score) as max
    from submissions s
    Join hackers h ON h.hacker_id = s.hacker_id
    GROUP BY
    s.hacker_id, h.name, s.challenge_id
    ) as m
Join
    hackers h ON m.hacker_id = h.hacker_id
GROUP BY
    m.hacker_id, h.name
having
    sum(m.max) > 0
Order BY
    total DESC, m.hacker_id ASC
