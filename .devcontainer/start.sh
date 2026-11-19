#!/usr/bin/env bash
# Runs every time the Codespace is opened: starts EventSphere and makes port 8080 public,
# so the app link works in any browser (Codespaces resets ports to private on restart).
if [ -n "$CODESPACE_NAME" ]; then
  (
    for _ in $(seq 1 30); do
      sleep 5
      gh codespace ports visibility 8080:public -c "$CODESPACE_NAME" >/dev/null 2>&1 && break
    done
  ) &
fi
exec ./mvnw spring-boot:run
