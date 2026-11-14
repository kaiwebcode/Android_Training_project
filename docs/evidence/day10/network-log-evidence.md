# Day 10 - Network and Logcat Evidence
Date: 14 November 2026
Device: OnePlus EB2101

## Successful task retrieval

GET https://gorest.co.in/public/v3/tasks?page%5Bsize%5D=10

HTTP 200 OK
Content-Type: application/json

Verified using Android Studio Network Inspector and Logcat.

## Open issue: Task completion update

PUT /public/v3/tasks/{id}

HTTP 405 Method Not Allowed
Allowed methods: GET, HEAD, PATCH, DELETE

Observed in actual Logcat output.
The repository retains completion locally even
when this remote update fails.

This issue remains open.
