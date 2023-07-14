Experiment: implementation of an event model using messagedb.

[Event Model](https://miro.com/app/board/uXjVPH9FnX8=/)

```
curl -X PUT -H 'Content-Type: application/json' -d '{"title":"Test 1"}' localhost:8080/cases/{caseId}

curl -X PUT -H 'Content-Type: application/json' -d '{"description":"Look at these details."}' localhost:8080/cases/{caseId}/description

curl -s localhost:8080/cases | jq

curl -X POST localhost:8080/cases/{caseId}/watch

curl -s localhost:8080/cases/{caseId}/watching | jq

PGPASSWORD=postgres psql -h localhost -p 5432 -U postgres -d message_store
SET search_path TO message_store;
\x
select * from messages order by global_position asc;
```
## Slices

- PUT /cases/{caseId} => Start command => Started event
    - or was this 2 slices?
- Started event => Cases view => GET /cases
    - view is a single cases table in postgres
        - store current state of case
        - get case by caseId
        - get all cases by tenantId
    - need to subscribe to events in cases category
        - on each Started event
        - insert row into cases table
- PUT /cases/{caseId}/description => Change description => Description changed
    - 
