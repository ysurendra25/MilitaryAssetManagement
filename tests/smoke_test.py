#!/usr/bin/env python3
"""End-to-end smoke tests for MAMS. Run against a freshly seeded server."""
import requests

import os
BASE = os.environ.get("MAMS_BASE", "http://localhost:8080/api")
PASS, FAIL = 0, 0

def check(name, cond, extra=""):
    global PASS, FAIL
    if cond:
        PASS += 1
        print(f"  ok  {name}")
    else:
        FAIL += 1
        print(f" FAIL {name} {extra}")

def login(username, password):
    s = requests.Session()
    r = s.post(f"{BASE}/auth/login", json={"username": username, "password": password})
    assert r.status_code == 200, f"login failed for {username}: {r.status_code} {r.text}"
    return s, r.json()

# ---- 1. auth ----
print("== auth ==")
r = requests.get(f"{BASE}/meta/bases")
check("unauthenticated request is rejected (401)", r.status_code == 401)

r = requests.post(f"{BASE}/auth/login", json={"username": "admin", "password": "wrong"})
check("wrong password rejected (401)", r.status_code == 401)

admin, me = login("admin", "admin123")
check("admin login returns role", me["role"] == "ADMIN")
check("failed logins are audited",
      any(l["status"] == 401 and "Failed login" in (l["details"] or "")
          for l in admin.get(f"{BASE}/audit?limit=500").json()))

# ---- 2. meta ----
print("== reference data ==")
bases = {b["name"]: b["id"] for b in admin.get(f"{BASE}/meta/bases").json()}
equipment = {e["name"]: e["id"] for e in admin.get(f"{BASE}/meta/equipment-types").json()}
check("3 bases seeded", len(bases) == 3, str(bases))
check("4 equipment types seeded", len(equipment) == 4, str(equipment))

# ---- 3. dashboard math ----
print("== dashboard ==")
d = admin.get(f"{BASE}/dashboard?from=2025-10-01&to=2026-09-30").json()
net = d["purchases"] + d["transfersIn"] - d["transfersOut"]
check("net movement = purchases + transfers in - transfers out", d["netMovement"] == net,
      f"{d['netMovement']} vs {net}")
check("closing = opening + net - expended",
      d["closingBalance"] == d["openingBalance"] + d["netMovement"] - d["expended"],
      f"{d['closingBalance']} vs {d['openingBalance']}+{d['netMovement']}-{d['expended']}")
check("dashboard has data", d["openingBalance"] > 0 and d["purchases"] > 0)
check("equipment breakdown present", len(d["byEquipment"]) == 4)
check("monthly series present", len(d["monthly"]) == 12)

# base filter
d_alpha = admin.get(f"{BASE}/dashboard?from=2025-10-01&to=2026-09-30&baseId={bases['Base Alpha']}").json()
check("base filter changes numbers", d_alpha["openingBalance"] != d["openingBalance"])

# ---- 4. commander scoping ----
print("== commander (base scoping) ==")
cmdr, cme = login("cmdr_alpha", "commander123")
check("commander role returned", cme["role"] == "COMMANDER")

d_cmdr = cmdr.get(f"{BASE}/dashboard?from=2025-10-01&to=2026-09-30&baseId={bases['Base Charlie']}").json()
check("commander forced to own base (asking for Charlie returns Alpha)",
      d_cmdr["openingBalance"] == d_alpha["openingBalance"],
      f"{d_cmdr['openingBalance']} vs alpha {d_alpha['openingBalance']}")

r = cmdr.post(f"{BASE}/purchases", json={
    "baseId": bases["Base Bravo"], "equipmentTypeId": equipment["Weapons"],
    "quantity": 5, "purchaseDate": "2026-09-10"})
check("commander cannot purchase for another base (403)", r.status_code == 403)

r = cmdr.post(f"{BASE}/purchases", json={
    "baseId": bases["Base Alpha"], "equipmentTypeId": equipment["Weapons"],
    "quantity": 5, "purchaseDate": "2026-09-10"})
check("commander can purchase for own base", r.status_code == 200)

alpha_purchases_cmdr = cmdr.get(f"{BASE}/purchases").json()
check("commander only sees own-base purchases",
      all(p["base"] == "Base Alpha" for p in alpha_purchases_cmdr))

r = cmdr.post(f"{BASE}/expenditures", json={
    "baseId": bases["Base Alpha"], "equipmentTypeId": equipment["Weapons"],
    "quantity": 1, "reason": "test", "expendDate": "2026-09-12"})
check("commander can record expenditure for own base", r.status_code == 200)

# ---- 5. logistics officer limits ----
print("== logistics officer (limited access) ==")
logi, lme = login("logistics01", "logistics123")
check("logistics role returned", lme["role"] == "LOGISTICS")

r = logi.get(f"{BASE}/dashboard")
check("logistics blocked from dashboard (403)", r.status_code == 403)
r = logi.get(f"{BASE}/assignments")
check("logistics blocked from assignments (403)", r.status_code == 403)
r = logi.post(f"{BASE}/expenditures", json={
    "baseId": bases["Base Alpha"], "equipmentTypeId": equipment["Weapons"],
    "quantity": 1, "reason": "test", "expendDate": "2026-09-12"})
check("logistics blocked from expenditures (403)", r.status_code == 403)
r = logi.get(f"{BASE}/audit")
check("logistics blocked from audit (403)", r.status_code == 403)

r = logi.post(f"{BASE}/purchases", json={
    "baseId": bases["Base Bravo"], "equipmentTypeId": equipment["Ammunition"],
    "quantity": 500, "purchaseDate": "2026-09-15"})
check("logistics can record purchases", r.status_code == 200)
r = logi.get(f"{BASE}/purchases")
check("logistics can view purchases", r.status_code == 200)

# ---- 6. validation ----
print("== validation ==")
r = admin.post(f"{BASE}/transfers", json={
    "fromBaseId": bases["Base Alpha"], "toBaseId": bases["Base Alpha"],
    "equipmentTypeId": equipment["Weapons"], "quantity": 5, "transferDate": "2026-09-15"})
check("same-base transfer rejected (400)", r.status_code == 400)

r = admin.post(f"{BASE}/transfers", json={
    "fromBaseId": bases["Base Alpha"], "toBaseId": bases["Base Bravo"],
    "equipmentTypeId": equipment["Weapons"], "quantity": 999999, "transferDate": "2026-09-15"})
check("insufficient stock rejected (400)", r.status_code == 400)

r = admin.post(f"{BASE}/purchases", json={
    "baseId": bases["Base Alpha"], "equipmentTypeId": equipment["Weapons"],
    "quantity": -5, "purchaseDate": "2026-09-15"})
check("negative quantity rejected (400)", r.status_code == 400)

r = admin.post(f"{BASE}/assignments", json={
    "baseId": bases["Base Alpha"], "equipmentTypeId": equipment["Weapons"],
    "quantity": 5, "personnelName": "", "assignedDate": "2026-09-15"})
check("assignment without personnel rejected (400)", r.status_code == 400)

# ---- 7. transfers, assignments, expenditures ----
print("== transactions ==")
r = admin.post(f"{BASE}/transfers", json={
    "fromBaseId": bases["Base Alpha"], "toBaseId": bases["Base Charlie"],
    "equipmentTypeId": equipment["Weapons"], "quantity": 10,
    "reason": "smoke test", "transferDate": "2026-09-18"})
check("valid transfer recorded", r.status_code == 200 and r.json()["quantity"] == 10)

tlist = admin.get(f"{BASE}/transfers?from=2026-09-18&to=2026-09-18").json()
check("transfer visible in history with reason",
      any(t["reason"] == "smoke test" for t in tlist))

r = admin.post(f"{BASE}/assignments", json={
    "baseId": bases["Base Alpha"], "equipmentTypeId": equipment["Weapons"],
    "quantity": 3, "personnelName": "Lt. Test Officer", "assignedDate": "2026-09-19"})
check("assignment recorded", r.status_code == 200)
aid = r.json()["id"]

r = admin.post(f"{BASE}/assignments/{aid}/return")
check("assignment returned", r.status_code == 200 and r.json()["status"] == "RETURNED")
r = admin.post(f"{BASE}/assignments/{aid}/return")
check("double return rejected (400)", r.status_code == 400)

r = admin.post(f"{BASE}/expenditures", json={
    "baseId": bases["Base Alpha"], "equipmentTypeId": equipment["Ammunition"],
    "quantity": 100, "reason": "smoke test", "expendDate": "2026-09-20"})
check("expenditure recorded", r.status_code == 200)

# ---- 8. audit trail ----
print("== audit ==")
logs = admin.get(f"{BASE}/audit?limit=500").json()
check("audit shows successful purchases",
      any(l["action"] == "POST /api/purchases" and l["status"] == 200 for l in logs))
check("audit shows denied attempts (403)",
      any(l["status"] == 403 for l in logs))
check("audit shows failed business rules (400)",
      any(l["status"] == 400 and "Denied" in (l["details"] or "") for l in logs))
check("audit filter by user works",
      all(l["username"] == "logistics01"
          for l in admin.get(f"{BASE}/audit?user=logistics01").json()))

r = cmdr.get(f"{BASE}/audit")
check("commander blocked from audit (403)", r.status_code == 403)

# ---- 9. filters ----
print("== filters ==")
p_all = admin.get(f"{BASE}/purchases?from=2026-01-01&to=2026-06-30").json()
p_weapons = admin.get(f"{BASE}/purchases?from=2026-01-01&to=2026-06-30&equipmentTypeId={equipment['Weapons']}").json()
check("equipment filter narrows purchases",
      len(p_weapons) <= len(p_all) and all(p["equipment"] == "Weapons" for p in p_weapons))

x_all = admin.get(f"{BASE}/expenditures?from=2026-01-01&to=2026-06-30").json()
check("expenditure date filter works", isinstance(x_all, list))

print(f"\n{PASS} passed, {FAIL} failed")
raise SystemExit(1 if FAIL else 0)
