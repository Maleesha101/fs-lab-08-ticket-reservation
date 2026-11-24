#!/bin/bash
# Smoke test for the Secure Ticket Reservation System.
# Usage: ./scripts/smoke-test.sh
set -euo pipefail

BASE=http://localhost:8080
set -x

# 0. Cleanup: delete any pre-existing EC/2022/042 user so the run is repeatable
mongosh --quiet ticket_reservation_db --eval 'db.users.deleteMany({username:"EC/2022/042"})' >/dev/null 2>&1 || true

# 1. Register EC/2022/042
REGISTER_RESP=$(curl -s -o /tmp/reg.json -w "%{http_code}" -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"EC/2022/042","password":"EC/2022/042","fullName":"EC/2022/042","role":"CUSTOMER"}')
test "$REGISTER_RESP" = "201" || { echo "FAIL register"; exit 1; }
jq -e '.username == "EC/2022/042"' /tmp/reg.json > /dev/null || { echo "FAIL register body"; exit 1; }

# 2. Login
LOGIN_RESP=$(curl -s -o /tmp/login.json -w "%{http_code}" -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"EC/2022/042","password":"EC/2022/042"}')
test "$LOGIN_RESP" = "200" || { echo "FAIL login"; exit 1; }
TOKEN=$(jq -r '.token' /tmp/login.json)

# 3. Admin login (organizer creates event)
ADMIN_LOGIN=$(curl -s -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')
ADMIN_TOKEN=$(echo "$ADMIN_LOGIN" | jq -r '.token')

# 4. Organizer login (actually admin creating as organizer - organizer=admin here)
ORG_LOGIN=$(curl -s -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')
ORG_TOKEN=$(echo "$ORG_LOGIN" | jq -r '.token')

# 5. Customer login
CUST_LOGIN=$(curl -s -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" \
  -d '{"username":"customer","password":"customer123"}')
CUST_TOKEN=$(echo "$CUST_LOGIN" | jq -r '.token')

# 6. Create event (20 seats, 1000) - as organizer (admin)
CREATE_RESP=$(curl -s -o /tmp/event.json -w "%{http_code}" -X POST "$BASE/api/events" \
  -H "Authorization: Bearer $ORG_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Smoke Test Event","description":"For smoke testing","venue":"Test Hall","eventDate":"2026-12-31T10:00:00","totalSeats":20,"ticketPrice":1000.0}')
test "$CREATE_RESP" = "201" || { echo "FAIL create event"; exit 1; }
EVENT_ID=$(jq -r '.id' /tmp/event.json)

# 7. Reserve 2 seats as customer
BOOK_RESP=$(curl -s -o /tmp/res.json -w "%{http_code}" -X POST "$BASE/api/reservations" \
  -H "Authorization: Bearer $CUST_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":\"$EVENT_ID\",\"seats\":2}")
test "$BOOK_RESP" = "201" || { echo "FAIL book"; exit 1; }
RESERVATION_ID=$(jq -r '.id' /tmp/res.json)
RES_CODE=$(jq -r '.reservationCode' /tmp/res.json)
test "$RES_CODE" != "null" || { echo "FAIL reservation code"; exit 1; }

# 8. Event should show 18 available seats
AVAILABLE=$(curl -s "$BASE/api/events/$EVENT_ID" -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.availableSeats')
test "$AVAILABLE" = "18" || { echo "FAIL: expected 18 available, got $AVAILABLE"; exit 1; }

# 9. /reservations/my should show CONFIRMED + TKT-XXXXXXXX
MY_RES=$(curl -s "$BASE/api/reservations/my" -H "Authorization: Bearer $CUST_TOKEN" | jq -r '.[0].status')
test "$MY_RES" = "CONFIRMED" || { echo "FAIL: expected CONFIRMED, got $MY_RES"; exit 1; }
MY_CODE=$(curl -s "$BASE/api/reservations/my" -H "Authorization: Bearer $CUST_TOKEN" | jq -r '.[0].reservationCode')
case "$MY_CODE" in TKT-*) ;; *) echo "FAIL: reservation code format wrong: $MY_CODE"; exit 1;; esac

# 10. Cancel
CANCEL_RESP=$(curl -s -o /tmp/cancel.json -w "%{http_code}" -X DELETE "$BASE/api/reservations/$RESERVATION_ID" \
  -H "Authorization: Bearer $CUST_TOKEN")
test "$CANCEL_RESP" = "200" || { echo "FAIL cancel"; exit 1; }
CANCEL_STATUS=$(jq -r '.status' /tmp/cancel.json)
test "$CANCEL_STATUS" = "CANCELLED" || { echo "FAIL: expected CANCELLED, got $CANCEL_STATUS"; exit 1; }

# 11. Event should show 20 available seats again
AVAILABLE2=$(curl -s "$BASE/api/events/$EVENT_ID" -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.availableSeats')
test "$AVAILABLE2" = "20" || { echo "FAIL: expected 20 available after cancel, got $AVAILABLE2"; exit 1; }

# 12. Security: 401 without token
NO_TOKEN=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/events")
test "$NO_TOKEN" = "401" || { echo "FAIL: expected 401 without token, got $NO_TOKEN"; exit 1; }

# 13. Security: 403 customer creating event
CUST_CREATE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/events" \
  -H "Authorization: Bearer $CUST_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"X","venue":"Y","eventDate":"2026-12-31T10:00:00","totalSeats":10,"ticketPrice":1.0}')
test "$CUST_CREATE" = "403" || { echo "FAIL: expected 403 for customer creating event, got $CUST_CREATE"; exit 1; }

# 14. 409 overbooking
OVERBOOK=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/reservations" \
  -H "Authorization: Bearer $CUST_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":\"$EVENT_ID\",\"seats\":21}")
test "$OVERBOOK" = "409" || { echo "FAIL: expected 409 for overbooking, got $OVERBOOK"; exit 1; }

echo "ALL SMOKE TESTS PASSED"