---
name: devbox-adb-tunnel
description: Dev box real address is max.coplan@10.251.236.182; the DNS name "devbox" is a different machine; adb reverse tunnel on port 5037
metadata:
  type: project
---

The Linux dev box is `max.coplan@10.251.236.182` (FQDN `n251-236-182.byted.org`,
its own hostname is `devbox`). The DNS name `devbox` resolves to `10.8.7.247`,
which is a **different** machine and times out on port 22. The FQDN fails host
key verification, so use the IP.

`~/.ssh/config` on the Mac has a `Host devbox` block that pins `HostName
10.251.236.182` and carries `RemoteForward 5037 localhost:5037`. This gives the
dev box's adb client access to the Mac's adb server, and so to the Pixel 4a
([[larklish-test-phone]]).

**Why:** The dev box has no route to the phone. A Claude session there needs the
tunnel to run adb.

**How to apply:** Start the tunnel with `ssh -N -o ExitOnForwardFailure=yes
devbox &`. Do **not** also pass `-R`: the config already supplies the forward,
and a duplicate request fails. Other `ssh devbox` sessions print "remote port
forwarding failed for listen port 5037" and still connect. That is expected.
Both adb versions must match (they are `1.0.41 / 37.0.1-15733141`).
