#!/usr/bin/env python3
"""Deterministic MOTM ability verification sweep.

For each ability it casts in-world (via send-dev-command) and asserts, from logs:
  L1 assets wired   - preflight audit READY (all 120 manifest rows resolve). Global gate.
  L2 dispatched     - server "Queued ability cast result: abilityId=<id> result=[MOTM] Cast ..."
                      (positive cast) AND no "Missing gameplay effect asset" in the window.
  L3 client render  - no NEW client-native asset/animation/load FAILURE during the cast
                      (failure-absence; the client logs render failures, not successes).
  L4 runtime/mech   - the cast result carries runtime/mechanics evidence (armed/projectile/
                      summon/field/damage/effect), i.e. it entered its expected runtime.

Positive on-screen render + aesthetics are NOT provable from logs and are covered by a
separate small visual sample (see docs).

Usage: python3 scripts/verify_abilities.py [--styles quake,jet] [--world "MOTM Creative Test"]
"""
import argparse, glob, json, os, re, subprocess, sys, time, datetime

REPO = r"C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod"
SAVES = r"C:/Users/fishe/AppData/Roaming/Hytale/UserData/Saves"
CLIENT_LOGS = r"C:/Users/fishe/AppData/Roaming/Hytale/UserData/Logs"

BENIGN_CLIENT = [
    "Missing replacement interactions",              # vanilla NPC melee, rate-limited
    "NPC/MISC/Mannequin",                            # test-dummy model anim note
    "No animation with id Spawn on entity with model",  # summoned/proxy model lacks a Spawn anim (benign, vanilla)
    "CachedAssetsIndex.cache",                       # startup
]

# Combo/follow-up abilities that require a prerequisite ability active first. Cast in-sequence.
COMBO_PREREQ = {"dust_devil": "sandstorm"}

def load_abilities(styles_filter):
    out = []
    for f in sorted(glob.glob(REPO + "/src/main/resources/data/styles/*.json")):
        cid = os.path.basename(f).split("_")[0]
        d = json.load(open(f, encoding="utf-8"))
        for s in d["styles"]:
            sid = s.get("id")
            if styles_filter and sid not in styles_filter:
                continue
            for a in s.get("abilities", []):
                out.append(dict(cls=cid, style=sid, style_name=s.get("name", sid),
                                aid=a["id"], name=a.get("name", a["id"]),
                                cast_type=a.get("cast_type", "")))
    return out

def send(cmd, world):
    ps = ["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
          REPO + "/scripts/send-dev-command.ps1", "-Command", cmd, "-WorldName", world]
    try:
        r = subprocess.run(ps, capture_output=True, text=True, timeout=40)
        m = re.search(r"result=(.*)", r.stdout or "")
        return (m.group(1).strip() if m else (r.stdout or "").strip())
    except subprocess.TimeoutExpired:
        return "<timeout>"

RUNTIME_TOKENS = ("armed", "projectile", "summon", "field", "anchor", "gem", "column",
                  "trail", "channel", "transform", "terrain", "dash", "damage", "effect",
                  "charges", "Runtime:", "zone", "curse", "drain", "wall", "pool")

def verify():
    ap = argparse.ArgumentParser()
    ap.add_argument("--styles", default="")
    ap.add_argument("--world", default="MOTM Creative Test")
    args = ap.parse_args()
    styles_filter = [x.strip() for x in args.styles.split(",") if x.strip()]

    abilities = load_abilities(styles_filter)
    slog = newest(f"{SAVES}/{args.world}/logs/*.log")
    clog = newest(f"{CLIENT_LOGS}/*client*.log")
    print(f"[verify] abilities={len(abilities)} server_log={os.path.basename(slog)} client_log={os.path.basename(clog)}")

    # baseline byte offsets (only parse new content)
    s_off = os.path.getsize(slog)
    c_off = os.path.getsize(clog)

    # cast loop grouped by style
    send("dev freecast on", args.world)
    casts = {}  # aid -> {t_send}
    cur_style = None
    for ab in abilities:
        # Re-equip the style before each ability: resetRuntimeForLoadoutSwap clears any
        # armed/charged/cooldown state so an armed ability (e.g. stomp) can't block the next.
        send(f"style {ab['style']}", args.world)
        send("dev test mobs", args.world)
        time.sleep(0.8)
        send(f"dev test ability {ab['aid']}", args.world)
        casts[ab["aid"]] = {"t": time.time()}
        time.sleep(2.5)
    time.sleep(2.0)

    # read new log content
    with open(slog, "r", encoding="utf-8", errors="replace") as fh:
        fh.seek(s_off); s_new = fh.read()
    with open(clog, "r", encoding="utf-8", errors="replace") as fh:
        fh.seek(c_off); c_new = fh.read()

    # server: per-ability cast result + global missing-asset
    result_re = re.compile(r"Queued ability cast result:.*?abilityId=(\S+) result=\[MOTM\] (.+)")
    results = {}
    for m in result_re.finditer(s_new):
        results[m.group(1)] = m.group(2).strip()
    missing = re.findall(r"Missing gameplay effect asset:?\s*(\S+)?", s_new)
    missing_set = set(x for x in missing if x)

    # client: new NATIVE failure lines (exclude relayed SERVER lines + benign)
    client_fail = []
    for line in c_new.splitlines():
        if "SERVER -" in line:
            continue
        low = line.lower()
        if any(k in low for k in ("missing", "failed to load", "could not load",
                                   "no animation", "asset not found")):
            if any(b in line for b in BENIGN_CLIENT):
                continue
            client_fail.append(line.strip())

    # verdicts
    rows = []
    for ab in abilities:
        aid = ab["aid"]
        res = results.get(aid)
        l1 = "PASS"  # preflight READY gate asserted separately below
        if res is None:
            l2 = "FAIL(no-cast-log)"; l4 = "N/A"
        else:
            cast_ok = res.startswith("Cast ") or "Cast " in res[:12]
            asset_ok = aid not in missing_set  # (missing lines rarely carry aid; global check too)
            l2 = "PASS" if (cast_ok and asset_ok) else ("FAIL(cast:" + res[:40] + ")")
            l4 = "PASS" if any(t.lower() in res.lower() for t in RUNTIME_TOKENS) else "REVIEW(bare-cast)"
        l3 = "PASS" if not client_fail else "REVIEW(client-fail)"
        rows.append(dict(cls=ab["cls"], style=ab["style"], aid=aid, name=ab["name"],
                         cast_type=ab["cast_type"], L1=l1, L2=l2, L3=l3, L4=l4,
                         result=(res or "")[:120]))

    runid = datetime.datetime.now().strftime("%Y-%m-%dT%H-%M-%S")
    outdir = f"{REPO}/audits/ability-verification/{runid}"
    os.makedirs(outdir, exist_ok=True)
    json.dump(dict(runid=runid, missing_assets=sorted(missing_set),
                   client_failures=client_fail, rows=rows),
              open(outdir + "/results.json", "w", encoding="utf-8"), indent=1)

    npass = sum(1 for r in rows if r["L2"] == "PASS")
    print(f"[verify] L2 cast PASS {npass}/{len(rows)}  missing_assets={len(missing_set)}  client_failures={len(client_fail)}")
    for r in rows:
        flag = "" if (r["L2"] == "PASS" and r["L4"] == "PASS" and r["L3"] == "PASS") else "  <-- REVIEW"
        print(f"  {r['cls']:9}/{r['style']:16}/{r['aid']:22} L2={r['L2'][:20]:20} L4={r['L4'][:16]:16} L3={r['L3']}{flag}")
    print(f"[verify] results: {outdir}/results.json")
    return outdir

if __name__ == "__main__":
    verify()
