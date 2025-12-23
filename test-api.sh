#!/bin/bash

echo "=== Checking if port 8082 is in use ==="
lsof -i :8082

echo -e "\n=== Testing Health Endpoint ==="
curl -s http://localhost:8082/actuator/health || echo "Health endpoint not responding"

echo -e "\n\n=== Testing Shadow Balance GET API for Account A10 ==="
curl -s http://localhost:8082/accounts/A10/shadow-balance || echo "Shadow balance API not responding"

echo -e "\n\n=== Testing Shadow Balance GET API for Account A11 ==="
curl -s http://localhost:8082/accounts/A11/shadow-balance || echo "Shadow balance API not responding"

echo -e "\n\nDone!"

